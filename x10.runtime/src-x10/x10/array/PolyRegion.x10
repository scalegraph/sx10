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

import x10.io.Printer;
import x10.compiler.Incomplete;
import x10.compiler.TempNoInline_3;

/**
 * A PolyRegion represents a polyhedral region represented as the
 * intersection of a list of PolyRows. The halfspaces are stored in
 * a PolyMat object, which is essentially a constraint matrix
 * that defines the region. The PolyRegion object wraps a
 * HalfpaceList, adding some static factory methods for PolyRegions
 * and some methods such as region algebra that operate on
 * PolyMat objects.
 */
class PolyRegion extends Region {

    // XTENLANG-49
    static type PolyRegion(rank:Int) = PolyRegion{self.rank==rank};
  //  static type PolyRegionListBuilder(rank:Int) = PolyRegionListBuilder{self.rank==rank};
    static type PolyRow(rank:Int) = PolyRow{self.rank==rank};
    static type PolyMat(rank:Int) = PolyMat{self.rank==rank};
   // static type UnionRegion(rank:Int) = UnionRegion{self.rank==rank};

    //
    // value
    //

    public val mat: PolyMat{self.rank==this.rank};


    //
    // basic info
    //

    public def isConvex(): boolean {
        return true;
    }

    var size:Int = -1; // uninitialized
    public def size():int {
    	if (size < 0) {
    	  var s:Int=0;
          val it= iterator();
          for (p:Point in this)
        	 s++;
          size=s;
    	}
        return size;     
    }

    @Incomplete public def indexOf(Point):int {
        throw new UnsupportedOperationException();
    }


    public def iterator():Iterator[Point(rank)]
          = PolyScanner.make(mat).iterator() as Iterator[Point(rank)];
  

    //
    // Region methods
    //

    public def intersection(t: Region(rank)): Region(rank) {

        if (t instanceof PolyRegion) {

            // start
            val that = t as PolyRegion; // XXX
            val pmb = new PolyMatBuilder(rank);

            // these halfspaces
            for (r:PolyRow in this.mat)
                pmb.add(r);

            // those halfspaces
            for (r:PolyRow in that.mat)
                pmb.add(r);

            // done
            val pm = pmb.toSortedPolyMat(false);
            return PolyRegion.make(pm) as Region(rank); // XXXX why?

        } else if (t instanceof RectRegion) {
        	return intersection((t as RectRegion).toPolyRegion());
        } else if (t instanceof RectRegion1D) {
                return intersection((t as RectRegion1D).toRectRegion().toPolyRegion() as Region(rank));
        }
        /*else if (t instanceof UnionRegion) {

            return (t as Region(rank)).intersection(this);

        } */  else {
            throw new UnsupportedOperationException("intersection(" + t/*.getClass().getName()*/ + ")");
        }
    }
                          
                          
    public def contains(that: Region(rank)): boolean = 
    	computeBoundingBox().contains(that.computeBoundingBox());
     
    /**
     * Projection is computed by using FME to eliminate variables on
     * all but the axis of interest.
     */

    public def projection(axis: int): Region(1) {
        var pm: PolyMat{self.rank==this.rank} = mat;
        for (var k: int = 0; k<rank; k++)
            if (k!=axis)
                pm = pm.eliminate(k, true);
        return Region.makeRectangular(pm.rectMin(axis), pm.rectMax(axis)) as Region(1);
    }

    /**
     * Eliminate the ith axis.
     */

    // XXX add a test case for this; also for projection!
  
    public def eliminate(axis: int): Region/*(rank1)*/ {
        val pm = mat.eliminate(axis, true); 
        val result = PolyRegion.make(pm);
        return result /*as Region(rank1)*/;
    }

    /**
     * Cartesian product requires copying the halfspace matrices into
     * the result blockwise
     */

    public def product(r: Region): Region{self != null} {
        if (!(r instanceof PolyRegion))
            throw new UnsupportedOperationException("product(" + r/*.getClass().getName()*/ + ")");
        val that = r as PolyRegion;
        val pmb = new PolyMatBuilder(this.rank + that.rank);
        copy(pmb, this.mat, 0);         // padded w/ 0s on the right
        copy(pmb, that.mat, this.rank); // padded w/ 0s on the left
        val pm = pmb.toSortedPolyMat(false);
        return PolyRegion.make(pm);
    }

    private static def copy(tt: PolyMatBuilder, ff: PolyMat, offset: int): void {
        for (r:PolyRow in ff) {
            val f = r;
            val t = new Array[int](tt.rank+1);
            for (var i: int = 0; i<ff.rank; i++)
                t(offset+i) = f(i);
            t(tt.rank) = f(ff.rank);
            tt.add(new PolyRow(t));
        }
    }


    public def translate(v: Point(rank)): Region(rank) {
        val pmb = new PolyMatBuilder(this.rank);
        translate(pmb, this.mat, v);
        val pm = pmb.toSortedPolyMat(false);
        return PolyRegion.make(pm);
    }

    private static def translate(tt: PolyMatBuilder, ff: PolyMat, v: Point(ff.rank)): void {
        for (r:PolyRow in ff) {
            val f = r;
            val t = new Array[int](ff.rank+1);
            var s:Int = 0;
            for (var i: int = 0; i<ff.rank; i++) {
                t(i) = f(i);
                s += f(i)*v(i);
            }
            t(ff.rank) = f(ff.rank) - s;
            tt.add(new PolyRow(t));
        }
    }


    /**
     * -H0 || -H1 && H0 || -H2 && H1 && H0 || ...
     */

   /* public def complement(): Region(rank) {
        val prlb = new PolyRegionListBuilder(rank);

        for (r:PolyRow in mat) {
            val ri = r as PolyRow(rank); // XXXX
            val pmb = new PolyMatBuilder(rank);
            pmb.add(ri.complement());
            for (rj:PolyRow in mat) {
                if (rj==ri)
                    break;
                pmb.add(rj);
            }
            val pm = pmb.toSortedPolyMat(false);
            val region = PolyRegion.make(pm);
            prlb.add(region as Region(rank)); // XXXX
        }

        return new UnionRegion(prlb as PolyRegionListBuilder{self.rank == this.rank}); 
    }
*/
    public def isEmpty(): boolean {
        val tmp = mat.isEmpty();
        return tmp;
    }

    protected def computeBoundingBox(): Region(rank){self.rect} {
        val min = new Rail[int](rank);
        val max = new Rail[int](rank);
        var pm: PolyMat{self.rank==this.rank} = mat;
        for (var axis: int = 0; axis<rank; axis++) {
            var x: PolyMat = pm;
            for (var k: int = axis+1; k<rank; k++)
                x = x.eliminate(k, true);
            min(axis) = x.rectMin(axis);
            max(axis) = x.rectMax(axis);
            pm = pm.eliminate(axis, true);
        }
        return Region.makeRectangular(min, max);
    }


    /**
     * point
     */

    public def contains(p: Point): boolean {

        for (r:PolyRow in mat) {
            if (!r.contains(p))
                return false;
        }

        return true;
    }



    /**
     * lower==1 and lower==1 include the diagonal
     * lower==size and upper==size includes entire size x size square
     *
     * col-colMin >= row-rowMin - (lower-1)
     * col-colMin <= row-rowMin + (upper-1)
     *
     * col-row >= colMin-rowMin - (lower-1)
     * col-row <= colMin-rowMin + (upper-1)
     */

    private static ROW: int = PolyMatBuilder.X(0);
    private static COL: int = PolyMatBuilder.X(1);

    public static def makeBanded(rowMin: int, colMin: int, rowMax: int, colMax: int, upper: int, lower: int): Region(2) {
        val pmb = new PolyMatBuilder(2);
        pmb.add(ROW, pmb.GE, rowMin);
        pmb.add(ROW, pmb.LE, rowMax);
        pmb.add(COL, pmb.GE, colMin);
        pmb.add(COL, pmb.LE, colMax);
        pmb.add(COL-ROW, pmb.GE, colMin-rowMin-(lower-1));
        pmb.add(COL-ROW, pmb.LE, colMin-rowMin+(upper-1));
        val pm = pmb.toSortedPolyMat(false);
        return PolyRegion.make(pm);
    }

    public static def makeBanded(size: int, upper: int, lower: int): Region(2) {
        return makeBanded(0, 0, size-1, size-1, upper, lower);
    }

    public static def makeUpperTriangular2(rowMin: int, colMin: int, size: int): Region(2) {
        val pmb = new PolyMatBuilder(2);
        pmb.add(ROW, pmb.GE, rowMin);
        pmb.add(COL, pmb.LE, colMin+size-1);
        pmb.add(COL-ROW, pmb.GE, colMin-rowMin);
        val pm = pmb.toSortedPolyMat(true);
        return PolyRegion.make(pm);
    }

    public static def makeLowerTriangular2(rowMin: int, colMin: int, size: int): Region(2) {
        val pmb = new PolyMatBuilder(2);
        pmb.add(COL, pmb.GE, colMin);
        pmb.add(ROW, pmb.LE, rowMin+size-1);
        pmb.add(ROW-COL, pmb.GE, rowMin-colMin);
        val pm = pmb.toSortedPolyMat(true);
        return PolyRegion.make(pm);
    }



    /**
     * here's where we examine the halfspaces and generate
     * special-case subclasses, such as RectRegion, for efficiency
     */

    public static @TempNoInline_3 def make(pm: PolyMat): Region(pm.rank){self != null} {
        if (pm.isEmpty()) {
            return new EmptyRegion(pm.rank);
        } else {
            return new PolyRegion(pm, false);
        }
    }

    protected def this(pm: PolyMat, hack198:boolean): PolyRegion(pm.rank) {

        super(pm.rank, pm.isRect(), pm.isZeroBased());

        // simplifyAll catches more (all) stuff, but may be expensive.
        //this.mat = pm.simplifyParallel();
        this.mat = pm.simplifyAll() as PolyMat{self.rank==this.rank}; // safe..

        // cache stuff up front
        // vj: no dont. THis creates a cycle of global values
        // Not handled in 2.0
	//        cache = new Cache(this, hack198);
    }

    public def min(): (int)=>int {
        val t = boundingBox().min();
        return (i:int)=>t(i);
    }

    public def max(): (int)=>int {
        val t = boundingBox().max();
        return (i:int)=>t(i);
    }


    //
    // debugging
    //

    public def printInfo(out: Printer): void {
        mat.printInfo(out, /*this.getClass().getName()*/this.toString());
    }

    public def toString(): String {
        return mat.toString();
    }

}
