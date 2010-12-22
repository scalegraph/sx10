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
 * This class is an highly optimized implementation for a
 * the "unique" distribution that maps the region [0..Place.MAX_PLACES-1]
 * such that for every place <code>this.get(p) == [p..p].</code><p>
 */
class UniqueDist extends Dist(1) {
   
    /**
     * Cached restricted region for the current place.
     */
    private transient var regionForHere:Region(this.rank);


    def this() {
	super(0..Place.MAX_PLACES-1);
    }


    public def places():PlaceGroup = PlaceGroup.WORLD;

    public def numPlaces():int = Place.MAX_PLACES;

    public def regions():Sequence[Region(rank)] {
	return	new Array[Region(rank)](Place.MAX_PLACES, (i:int)=>((i..i) as Region(rank))).sequence();
    }

    public def get(p:Place):Region(rank) {
        if (p == here) {
            if (regionForHere == null) {
                regionForHere = (here.id..here.id) as Region(rank);
            }
	    return regionForHere;
        } else {
            return (p.id..p.id) as Region(rank);
        }
    }

    public operator this(p:Place):Region(rank) = get(p);

    public operator this(pt:Point(rank)):Place {
	return Place.place(pt(0));
    }

    public operator this(i0:int){rank==1} {
	return Place.place(i0);
    }

    public def offset(pt:Point(rank)):int {
        if (CompilerFlags.checkBounds() && !(pt(0) >= 0 && pt(0) < numPlaces())) {
            raiseBoundsError(pt);
        }
        if (CompilerFlags.checkPlace() && pt(0) != here.id) raisePlaceError(pt);
        return 0;
    }

    public def maxOffset():int = 0;

    public def restriction(r:Region(rank)):Dist(rank) {
	return new WrappedDistRegionRestricted(this, r) as Dist(rank); // TODO: cast should not be needed
    }

    public def restriction(p:Place):Dist(rank) {
	return new WrappedDistPlaceRestricted(this, p) as Dist(rank); // TODO: cast should not be needed
    }

    public def equals(thatObj:Any):boolean {
	return thatObj instanceof UniqueDist;
    }
}

