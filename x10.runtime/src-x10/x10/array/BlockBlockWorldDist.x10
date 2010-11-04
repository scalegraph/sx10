/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2010.
 */

package x10.array;

import x10.compiler.CompilerFlags;

/**
 * This class is an optimized implementation for a
 * Block,Block distribution that maps points in its region
 * in a 2D blocked fashion to all the places (the world).<p>
 *
 * Since it includes all Places, it doesn't need to maintain an 
 * explicit Place set of the places it is defined over.
 * Since it is a simple Block,Block distribution, it doesn't
 * need very much information to compute the Region that is mapped 
 * to a given place from the overall Region.<p>
 * 
 * It caches the region for the current place as a transient field.
 * This makes the initial access to this information somewhat slow,
 * but optimizes the wire-transfer size of the Dist object. 
 * This appears to be the appropriate tradeoff, since Dist objects
 * are frequently serialized and usually the restriction operation is 
 * applied to get the Region for here, not for other places.
 */
public final class BlockBlockWorldDist extends Dist {
   
    /**
     * The first axis along which the region is distributed
     */
    private val axis0:int;

    /**
     * The second axis along which the region is distributed
     */
    private val axis1:int;

    /**
     * Cached restricted region for the current place.
     */
    private transient var regionForHere:Region(this.rank);


    public def this(r:Region, axis0:int, axis1:int):BlockBlockWorldDist{this.region==r} {
        super(r, false /* TODO: don't just assume this, check */, Place.MAX_PLACES==1, Place.place(0));
        this.axis0 = axis0;
        this.axis1 = axis1;
    }


    /**
     * The key algorithm for this class.
     * Compute the region for the given place by doing region algebra.
     * TODO: Create an optimized fast-path for RectRegion.
     */
    private def blockBlockRegionForPlace(place:Place):Region{self.rank==this.rank} {
        val b = region.boundingBox();
        val min0 = b.min(axis0);
        val max0 = b.max(axis0);
        val min1 = b.min(axis1);
        val max1 = b.max(axis1);
        val size0 = (max0 - min0 + 1);
        val size1 = (max1 - min1 + 1);
        val sizeFirst = (axis1 > axis0) ? size0 : size1;
        val sizeSecond = (axis1 > axis0) ? size1 : size0;
        val P = Math.min(Place.MAX_PLACES, size0 * size1);
        val divisions0 = Math.min(size0, Math.pow2(Math.ceil((Math.log(P as Double) / Math.log(2.0)) / 2.0) as Int));
        val divisions1 = Math.min(size1, Math.ceil((P as Double) / divisions0) as Int);
        val divisionsFirst = (axis1 > axis0) ? divisions0 : divisions1;
        val divisionsSecond = (axis1 > axis0) ? divisions1 : divisions0;
        val numElems = size0 * size1;
        val leftOver = divisions0*divisions1 - P;
        val minFirst = (axis1 > axis0) ? min0 : min1;
        val maxFirst = (axis1 > axis0) ? max0 : max1;

        val minSecond = (axis1 > axis0) ? min1 : min0;
        val maxSecond = (axis1 > axis0) ? max1 : max0;

        val i = place.id;

        val beforeAxes = (axis1 > axis0) ? Region.makeFull(axis0) : Region.makeFull(axis1);
        val betweenAxes = (axis1 > axis0) ? Region.makeFull(axis1-axis0-1) : Region.makeFull(axis0-axis1-1);
        val afterAxes = (axis1 > axis0) ? Region.makeFull(region.rank-axis1-1) : Region.makeFull(region.rank-axis0-1);
        val leftOverOddOffset = (divisions0 % 2 == 0) ? 0 : i*2/(divisions0+1);
        val lowFirst = Math.min(minFirst + (i < leftOver+leftOverOddOffset ? ((i*2-leftOverOddOffset) % divisions0) : ((i+leftOver) % divisions0)) * sizeFirst / divisionsFirst, maxFirst);
        val hiFirst = Math.min(lowFirst + sizeFirst / divisionsFirst - 1 + (i < leftOver+leftOverOddOffset ? sizeFirst / divisionsFirst : 0), maxFirst);
        val rFirst = (Math.round(lowFirst) as Int)..(Math.round(hiFirst) as Int);
        val rawLowSecond = (minSecond + ((i < leftOver ? (i*2) / divisions0 : ((i+leftOver)/divisions0)) * sizeSecond / divisionsSecond));
        val lowSecond = maxSecond - Math.round(maxSecond - rawLowSecond);
        val hiSecond = Math.min(maxSecond - (Math.round(maxSecond - (rawLowSecond + sizeSecond / divisionsSecond)) + 1.0), maxSecond);
        val rSecond = (lowSecond as Int)..(hiSecond as Int);
                   
        return (beforeAxes.product(rFirst).product(betweenAxes).product(rSecond).product(afterAxes) as Region(region.rank)).intersection(region);
    }

    /**
     * Given an index into the "axis dimension" determine which place it 
     * is mapped to.
     * Assumption: Caller has done error checking to ensure that index is 
     *   actually within the bounds of the axis dimension.
     */
    private def mapIndexToPlace(index0:int, index1:int) {
        val b = region.boundingBox();
        val min0 = b.min(axis0);
        val max0 = b.max(axis0);
        val min1 = b.min(axis1);
        val max1 = b.max(axis1);
        val size0 = (max0 - min0 + 1);
        val size1 = (max1 - min1 + 1);
        val P = Math.min(Place.MAX_PLACES, size0 * size1);
        val divisions0 = Math.min(size0, Math.pow2(Math.ceil((Math.log(P as Double) / Math.log(2.0)) / 2.0) as Int));
        val divisions1 = Math.min(size1, Math.ceil((P as Double) / divisions0) as Int);
        val numBlocks = divisions0 * divisions1;
        val leftOver = numBlocks - P;

        val blockIndex0 = ((index0 - min0) * divisions0 / size0) as Int;
        val blockIndex1 = ((index1 - min1) * divisions1 / size1) as Int;
        val blockIndex = (blockIndex1 * divisions0) + blockIndex0;

        //Console.OUT.println("divisions0 = " + divisions0);
        //Console.OUT.println("divisions1 = " + divisions1);
        //Console.OUT.println("blockIndex = " + blockIndex);

        if (blockIndex <= leftOver * 2) {
            return Place.place((blockIndex / 2) as Int);
        } else {
            return Place.place(blockIndex - leftOver);
        }
    }


    public def places():Sequence[Place]=Place.places();

    public def numPlaces():int = Place.MAX_PLACES;

    public def regions():Sequence[Region(rank)] {
        return new Array[Region(rank)](Place.MAX_PLACES, (i:int)=>blockBlockRegionForPlace(Place.place(i))).sequence();
    }

    public def get(p:Place):Region(rank) {
        if (p == here) {
            if (regionForHere == null) {
                regionForHere = blockBlockRegionForPlace(here);
            }
	        return regionForHere;
        } else {
            return blockBlockRegionForPlace(p);
        }
    }

    public def apply(pt:Point(rank)):Place {
        if (CompilerFlags.checkBounds() && !region.contains(pt)) raiseBoundsError(pt);
            return mapIndexToPlace(pt(axis0), pt(axis1));
    }

    public def apply(i0:int){rank==1}:Place {
        // block,block dist only supported for rank>=2
        throw new UnsupportedOperationException("apply(i0:int)");
    }

    public def apply(i0:int, i1:int){rank==2}:Place {
        if (CompilerFlags.checkBounds() && !region.contains(i0, i1)) raiseBoundsError(i0,i1);
        switch(axis0) {
            case 0: return mapIndexToPlace(i0,i1);
            case 1: return mapIndexToPlace(i1,i0);
            default: return here; // UNREACHABLE
        }
    }

    public def apply(i0:int, i1:int, i2:int){rank==3}:Place {
        if (CompilerFlags.checkBounds() && !region.contains(i0, i1, i2)) raiseBoundsError(i0,i1,i2);
        switch(axis0) {
            case 0: switch(axis1) {
                case 1:
                    return mapIndexToPlace(i0,i1);
                case 2:
                    return mapIndexToPlace(i0,i2);
                default: return here; // UNREACHABLE
            }
            case 1: switch(axis1) {
                case 0:
                    return mapIndexToPlace(i1,i0);
                case 2:
                    return mapIndexToPlace(i1,i2);
                default: return here; // UNREACHABLE
            }
            case 2: switch(axis1) {
                case 0:
                    return mapIndexToPlace(i2,i0);
                case 1:
                    return mapIndexToPlace(i2,i1);
                default: return here; // UNREACHABLE
            }
            default: return here; // UNREACHABLE
        }
    }

    public def apply(i0:int, i1:int, i2:int, i3:int){rank==4}:Place {
        val pt = Point.make(i0, i1, i2, i3);
        if (CompilerFlags.checkBounds() && !region.contains(pt)) raiseBoundsError(pt);
            return mapIndexToPlace(pt(axis0), pt(axis1));
    }

    public def restriction(r:Region(rank)):Dist(rank) {
        return new WrappedDistRegionRestricted(this, r) as Dist(rank); // TODO: cast should not be needed
    }

    public def restriction(p:Place):Dist(rank) {
        return new WrappedDistPlaceRestricted(this, p) as Dist(rank); // TODO: cast should not be needed
    }


    public def equals(thatObj:Any):boolean {
        if (!(thatObj instanceof BlockBlockWorldDist)) return false;
        val that = thatObj as BlockBlockWorldDist;
        return this.axis0.equals(that.axis0) && this.axis1.equals(that.axis1) && this.region.equals(that.region);
    }
}

