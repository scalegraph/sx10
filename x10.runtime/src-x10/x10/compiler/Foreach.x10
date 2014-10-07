/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2014.
 */

package x10.compiler;

import x10.array.BlockingUtils;
import x10.array.DenseIterationSpace_2;

/**
 * Parallel iteration over a set of indices using different patterns of
 * local activity creation; this is intended to be used by the compiler to
 * implement the <code>foreach</code> construct.
 * <p>
 * The <code>body</code> closure is executed for each value of the index, 
 * making use of available local parallelism. The iteration must be both 
 * <em>serializable</em> and <em>parallelizable</em>; in other words, it is 
 * correct to execute <code>body</code> for each index in sequence, and it is 
 * also correct to execute <code>body</code> in parallel for any subset of 
 * indices.</p>
 * <p>There is an implied <code>finish</code> i.e. all iterations must complete
 * before <code>foreach</code> is complete.</p>
 * <p>Restrictions:</p>
 * <ul>
 * <li>A conditional atomic statement (<code>when</code>) may not be included 
 * as it could introduce ordering dependencies. Unconditional <code>atomic</code>
 * may be included as it cannot create an ordering dependency.</li>
 * <li>Changing place with <code>at</code> is not recommended as it may introduce
 * arbitrary delays.</p>
 * </ul>
 */
public final class Foreach {
    /**
     * Iterate over a range of indices in parallel using a basic async
     * transformation. A separate async is started for every index in min..max
     * @param min the minimum value of the index
     * @param max the maximum value of the index
     * @param body a closure that executes over a single value of the index
     */
    public static @Inline def basic(min:Long, max:Long,
                                    body:(i:Long)=>void) {
        finish for (i in min..max) async body(i);
    }

    /**
     * Iterate over a range of indices in parallel using a basic async
     * transformation. A separate async is started for every index in min..max
     * @param min the minimum value of the index
     * @param max the maximum value of the index
     * @param body a closure that executes over a contiguous range of indices
     */
    public static @Inline def basic(min:Long, max:Long,
                                    body:(min:Long, max:Long)=>void) {
        finish for (i in min..max) async body(i, i);
    }

    /**
     * Iterate over a range of indices in parallel using a block decomposition.
     * <code>Runtime.NTHREADS</code> activities are created, one for each
     * worker thread.  Each activity executes sequentially over all indices in
     * a contiguous block, and the blocks are of approximately equal size.
     * @param min the minimum value of the index
     * @param max the maximum value of the index
     * @param body a closure that executes over a contiguous range of indices
     */
    public static @Inline def block(min:Long, max:Long,
                                    body:(min:Long, max:Long)=>void) {
        finish for (var t:Long = Runtime.NTHREADS-1; t >= 0; t--) {
            val myT = t;
            async {
                val block = BlockingUtils.partitionBlock(min, max, Runtime.NTHREADS, myT);
                body(block.min, block.max);
            }
        }
    }

    /**
     * Iterate over a range of indices in parallel using a block decomposition.
     * <code>Runtime.NTHREADS</code> activities are created, one for each
     * worker thread.  Each activity executes sequentially over all indices in
     * a contiguous block, and the blocks are of approximately equal size.
     * @param min the minimum value of the index
     * @param max the maximum value of the index
     * @param body a closure that executes over a contiguous range of indices
     */
    public static @Inline def block(min:Long, max:Long,
                                    body:(i:Long)=>void) {
        val executeRange = (start:Long, end:Long) => {
            for (i in start..end) body(i);
        };
        Foreach.block(min, max, executeRange);
    }

    /**
     * Iterate over a range of indices in parallel using a block decomposition.
     * <code>Runtime.NTHREADS</code> activities are created, one for each
     * worker thread.  Each activity executes sequentially over all indices in
     * a contiguous block, and the blocks are of approximately equal size.
     * @param min the minimum value of the index
     * @param max the maximum value of the index
     * @param body a closure that executes over a contiguous range of indices
     */
    public static @Inline def block(min0:Long, max0:Long,
                                    min1:Long, max1:Long,
                                    body:(i:Long, j:Long)=>void) {
        Foreach.block(new DenseIterationSpace_2(min0, min1, max0, max1), body);
    }

    /**
     * Iterate over a dense rectangular block of indices in parallel using
     * a block decomposition.
     * <code>Runtime.NTHREADS</code> activities are created, one for each
     * worker thread.  Each activity executes sequentially over all indices in
     * a contiguous block, and the blocks are of approximately equal size.
     * @param min the minimum value of the index
     * @param max the maximum value of the index
     * @param body a closure that executes over a single index
     */
    public static @Inline def block(space:DenseIterationSpace_2,
                                    body:(i:Long, j:Long)=>void) {
        finish for (var t:Long = Runtime.NTHREADS-1; t >= 0; t--) {
            val myT = t;
            async {
                val block = BlockingUtils.partitionBlockBlock(space, Runtime.NTHREADS, myT);
                for (i in block.min0..block.max0)
                    for (j in block.min1..block.max1)
                        body(i, j);
            }
        }
    }

    /**
     * Iterate over a range of indices in parallel using a cyclic decomposition.
     * <code>Runtime.NTHREADS</code> activities are created, one for each
     * worker thread.  Given T=Runtime.NTHREADS, activity for thread number 
     * <code>t</code> executes iterations (t, t+T, t+2 &times; T, ...).
     * @param min the minimum value of the index
     * @param max the maximum value of the index
     * @param body a closure that executes over a contiguous range of indices
     */
    public static @Inline def cyclic(min:Long, max:Long,
                                     body:(i:Long)=>void) {
        finish for (t in 0..(Runtime.NTHREADS-1)) async {
            for (var i:Long = min+t; i <= max; i += Runtime.NTHREADS) {
                body(i);
            }
        }
    }

    /**
     * Iterate over a range of indices in parallel using recursive bisection.
     * The range is divided into two approximately equal pieces, with each 
     * piece constituting an activity. Bisection recurs until each activity is
     * less than or equal to a maximum grain size.
     * @param min the minimum value of the index
     * @param max the maximum value of the index
     * @param grainSize the maximum grain size for an activity
     * @param body a closure that executes over a contiguous range of indices
     */
    public static @Inline def bisect(min:Long, max:Long,
                                     grainSize:Long,
                                     body:(min:Long, max:Long)=>void) {
        finish doBisect1D(min, max+1, grainSize, body);
    }

    /**
     * Iterate over a range of indices in parallel using recursive bisection.
     * The range is divided into two approximately equal pieces, with each 
     * piece constituting an activity. Bisection recurs until a minimum grain
     * size of (max-min+1) / (Runtime.NTHREADS &times; 8) is reached.
     * @param min the minimum value of the index
     * @param max the maximum value of the index
     * @param body a closure that executes over a contiguous range of indices
     */
    public static @Inline def bisect(min:Long, max:Long,
                                     body:(min:Long, max:Long)=>void) {
        val grainSize = Math.max(1, (max-min) / (Runtime.NTHREADS*8));
        Foreach.bisect(min, max, grainSize, body);
    }

    /**
     * Iterate over a range of indices in parallel using recursive bisection.
     * The range is divided into two approximately equal pieces, with each 
     * piece constituting an activity. Bisection recurs until each activity is
     * less than or equal to a maximum grain size.
     * @param min the minimum value of the index
     * @param max the maximum value of the index
     * @param grainSize the maximum grain size for an activity
     * @param body a closure that executes over a contiguous range of indices
     */
    public static @Inline def bisect(min:Long, max:Long,
                                     grainSize:Long,
                                     body:(i:Long)=>void) {
        // convert single index closure into execution over range
        val executeRange = (start:Long, end:Long) => {
            for (i in start..end) body(i);
        };
        finish doBisect1D(min, max+1, grainSize, executeRange);
    }

    /**
     * Iterate over a range of indices in parallel using recursive bisection.
     * The range is divided into two approximately equal pieces, with each 
     * piece constituting an activity. Bisection recurs until a minimum grain
     * size of (max-min+1) / (Runtime.NTHREADS &times; 8) is reached.
     * @param min the minimum value of the index
     * @param max the maximum value of the index
     * @param body a closure that executes over a contiguous range of indices
     */
    public static @Inline def bisect(min:Long, max:Long,
                                     body:(i:Long)=>void) {
        val grainSize = Math.max(1, (max-min) / (Runtime.NTHREADS*8));
        Foreach.bisect(min, max, grainSize, body);
    }

    // TODO XTENLANG-3435 should be @Inline with @NoInline on recursive calls
    private static def doBisect1D(start:Long, end:Long,
                                          grainSize:Long,
                                          body:(min:Long, max:Long)=>void) {
        if ((end-start) > grainSize) {
            async doBisect1D((start+end)/2L, end, grainSize, body);
            doBisect1D(start, (start+end)/2L, grainSize, body);
        } else {
            body(start, end-1);
        }
    }

    /**
     * Iterate over a dense rectangular set of indices in parallel using 
     * two-dimensional recursive bisection. The index set is divided along the
     * largest dimension into two approximately equal pieces, with each piece  
     * constituting an activity. Bisection recurs on each subblock until each 
     * activity is smaller than or equal to a maximum grain size in each 
     * dimension.
     * @param min0 the minimum value of the first index dimension
     * @param max0 the maximum value of the first index dimension
     * @param min1 the minimum value of the second index dimension
     * @param max1 the maximum value of the second index dimension
     * @param grainSize0 the maximum grain size for the first index dimension
     * @param grainSize1 the maximum grain size for the second index dimension
     * @param body a closure that executes over a rectangular block of indices
     */
    public static @Inline def bisect(min0:Long, max0:Long,
                                     min1:Long, max1:Long,
                                     grainSize0:Long, grainSize1:Long,
                                     body:(min0:Long, max0:Long, min1:Long, max1:Long)=>void) {
        finish doBisect2D(min0, max0+1, min1, max1+1, grainSize0, grainSize1, body);
    }

    /**
     * Iterate over a dense rectangular set of indices in parallel using 
     * two-dimensional recursive bisection. The index set is divided along the
     * largest dimension into two approximately equal pieces, with each piece  
     * constituting an activity. Bisection recurs on each subblock until each 
     * activity is smaller than or equal to a grain size of 
     * (max-min+1) / Runtime.NTHREADS in each dimension.
     * <p>TODO divide each dim by N ~= sqrt(Runtime.NTHREADS &times; 8), biased
     *   towards more divisions in longer dim
     * @param min0 the minimum value of the first index dimension
     * @param max0 the maximum value of the first index dimension
     * @param min1 the minimum value of the second index dimension
     * @param max1 the maximum value of the second index dimension
     * @param body a closure that executes over a rectangular block of indices
     */
    public static @Inline def bisect(min0:Long, max0:Long,
                                     min1:Long, max1:Long,
                                     body:(min0:Long, max0:Long, min1:Long, max1:Long)=>void) {
        val grainSize0 = Math.max(1, (max0-min0) / Runtime.NTHREADS);
        val grainSize1 = Math.max(1, (max1-min1) / Runtime.NTHREADS);
        Foreach.bisect(min0, max0, min1, max1, grainSize0, grainSize1, body);
    }

    /**
     * Iterate over a dense rectangular set of indices in parallel using 
     * two-dimensional recursive bisection. The index set is divided along the
     * largest dimension into two approximately equal pieces, with each piece  
     * constituting an activity. Bisection recurs on each subblock until each 
     * activity is smaller than or equal to a maximum grain size in each 
     * dimension.
     * @param min0 the minimum value of the first index dimension
     * @param max0 the maximum value of the first index dimension
     * @param min1 the minimum value of the second index dimension
     * @param max1 the maximum value of the second index dimension
     * @param grainSize0 the maximum grain size for the first index dimension
     * @param grainSize1 the maximum grain size for the second index dimension
     * @param body a closure that executes over a single index
     */
    public static @Inline def bisect(min0:Long, max0:Long,
                                     min1:Long, max1:Long,
                                     grainSize0:Long, grainSize1:Long,
                                     body:(i:Long, j:Long)=>void) {
        // convert single index closure into execution over range
        val executeRange = (min0:Long, max0:Long, min1:Long, max1:Long) => {
            for (i in min0..max0)
                for (j in min1..max1)
                    body(i, j);
        };
        Foreach.bisect(min0, max0, min1, max1, grainSize0, grainSize1, executeRange);
    }

    /**
     * Iterate over a dense rectangular set of indices in parallel using 
     * two-dimensional recursive bisection. The index set is divided along the
     * largest dimension into two approximately equal pieces, with each piece  
     * constituting an activity. Bisection recurs on each subblock until each 
     * activity is smaller than or equal to a grain size of 
     * (max-min+1) / Runtime.NTHREADS in each dimension.
     * <p>TODO divide each dim by N ~= sqrt(Runtime.NTHREADS &times; 8), biased
     *   towards more divisions in longer dim
     * @param min0 the minimum value of the first index dimension
     * @param max0 the maximum value of the first index dimension
     * @param min1 the minimum value of the second index dimension
     * @param max1 the maximum value of the second index dimension
     * @param body a closure that executes over a single index
     */
    public static @Inline def bisect(min0:Long, max0:Long,
                                     min1:Long, max1:Long,
                                     body:(i:Long, j:Long)=>void) {
        val grainSize0 = Math.max(1, (max0-min0) / Runtime.NTHREADS);
        val grainSize1 = Math.max(1, (max1-min1) / Runtime.NTHREADS);
        Foreach.bisect(min0, max0, min1, max1, grainSize0, grainSize1, body);
    }

    // TODO XTENLANG-3435 should be @Inline with @NoInline on recursive calls
    private static def doBisect2D(s0:Long, e0:Long,
                                  s1:Long, e1:Long,
                                  g1:Long, g2:Long,
                                  body:(min_i1:Long, max_i1:Long, min_i2:Long, max_i2:Long)=>void) {
        if ((e0-s0) > g1 && ((e0-s0) >= (e1-s1) || (e1-s1) <= g2)) {
            async doBisect2D((s0+e0)/2L, e0, s1, e1, g1, g2, body);
            doBisect2D(s0, (s0+e0)/2L, s1, e1, g1, g2, body);
        } else if ((e1-s1) > g2) {
            async doBisect2D(s0, e0, (s1+e1)/2L, e1, g1, g2, body);
            doBisect2D(s0, e0, s1, (s1+e1)/2L, g1, g2, body);
        } else {
            body(s0, e0-1, s1, e1-1);
        }
    }
}
