/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2014.
 */

package x10.lang;

/**
 * A representation of the range of integers [min..max].
 */
public struct IntRange(
               /**
                * The minimum value included in the range
                */
               min:Int,
               
               /**
                * The maximum value included in the range
                */
               max:Int
) implements Iterable[Int] {

    /**
     * Construct a IntRange from min..max
     * @param min the minimum value of the range
     * @param max the maximum value of the range
     */
    public def this(min:Int, max:Int) {
        property(min, max);
    }
    
    /**
     * Convert a given LongRange to an IntRange.
     * @param x the given LongRange
     * @return the given LongRange converted to an IntRange.
     */
    public static operator (x:LongRange) as IntRange = IntRange(x.min as Int, x.max as Int);

    public def toString():String = min+".."+max;
    
    public def equals(that:Any):Boolean {
        if (that instanceof IntRange) {
            val other = that as IntRange;
            return min == other.min && max == other.max;
        }
        return false;
    }
    
    public def hashCode():Int = (max-min).hashCode();
    
    public def iterator():Iterator[Int] {
        return new IntRangeIt(min, max);
    }  

    private static class IntRangeIt implements Iterator[Int] {
        var cur:Int;
        val max:Int;
        def this(min:Int, max:Int) {
            this.cur = min;
            this.max = max;
        }
        public def hasNext() { return cur <= max; }
        public def next() { return cur++; }
    }
}
