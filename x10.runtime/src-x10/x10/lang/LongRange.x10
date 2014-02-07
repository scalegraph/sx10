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
 * A representation of the range of longs [min..max].
 */
public struct LongRange(
               /**
                * The minimum value included in the range
                */
               min:Long,
               
               /**
                * The maximum value included in the range
                */
               max:Long
) implements Iterable[Long] {

    /**
     * Construct a LongRange from min..max
     * @param min the minimum value of the range
     * @param max the maximum value of the range
     */
    public def this(min:Long, max:Long) {
        property(min, max);
    }
    
    /**
     * Coerce a given IntRange to a LongRange.
     * @param x the given IntRange
     * @return the given IntRange converted to a LongRange.
     */
    public static operator (x:IntRange):LongRange = LongRange(x.min, x.max);

    public def toString():String = min+".."+max;
    
    public def equals(that:Any):Boolean {
        if (that instanceof LongRange) {
            val other = that as LongRange;
            return min == other.min && max == other.max;
        }
        return false;
    }
    
    public def hashCode():Int = (max-min).hashCode();
    
    public def iterator():Iterator[Long] {
        return new LongRangeIt(min, max);
    }  

    private static class LongRangeIt implements Iterator[Long] {
        var cur:Long;
        val max:Long;
        def this(min:Long, max:Long) {
            this.cur = min;
            this.max = max;
        }
        public def hasNext() { return cur <= max; }
        public def next() { return cur++; }
    }
}
