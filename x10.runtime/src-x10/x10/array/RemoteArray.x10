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

import x10.compiler.Global;
import x10.compiler.Native;
import x10.compiler.TempNoInline_0;
import x10.util.IndexedMemoryChunk;
import x10.util.RemoteIndexedMemoryChunk;

/**
 * A class that encapsulates sufficient information about a remote
 * array to enable DMA operations via Array.copyTo and Array.copyFrom
 * to be performed on the encapsulated Array.<p>
 * 
 * The following relationships will always be true, but are not statically expressible
 * due to limitations of the current implementations of constrained types in X10.
 * <pre>
 * this.region.equals(at (array.home) (this.array)().region)
 * this.size == (at (array.home) (this.array)().size)
 * rawData.home() == this.array.home;
 * at (rawData.home()) { (this.rawData)() == (this.array)().raw() }
 * </pre>
 */
public final class RemoteArray[T](
        /**
         * The Region of the remote array
         */
        region:Region, 
        /**
         * The size of the remote array.
         */
        size:Int, 
        /**
         * The GlobalRef to the remote array.
         */
        array:GlobalRef[Array[T]]) 
        {
    
    /**
     * Caches a remote reference to the backing storage for the remote array
     * to enable DMA operations to be initiated remotely.  
     */
    val rawData:RemoteIndexedMemoryChunk[T];

    /**
     * The rank of the RemoteArray is equal to region.rank
     */
    public property rank():Int = region.rank;
    
    /**
     * The home location of the RemoteArray is equal to array.home
     */
    public property home():Place = array.home;

    /**
     * Create a RemoteArray wrapping the argument local Array.
     * @param a The array object to make accessible remotely.
     */
    public def this(a:Array[T]{self!=null}) {
        property(a.region, a.size, GlobalRef[Array[T]](a));
        rawData = RemoteIndexedMemoryChunk.wrap(a.raw());
    }
    
    /**
     * Create a RemoteArray that uses the argument Region to specify how to
     * view elements of the argument RemoteIndexedMemoryChunk.
     * This constructor is semantically equivalent to the code sequence
     * <pre>
     * at (raw.home()) new RemoteArray[T](reg, raw())
     * </pre>
     * if <code>raw.home()</code> is a normal (non-CUDA) place. 
     * If <code>raw.home().isCUDA()</code> is true, then this constructor
     * simulates that semantics and provides an Array view on the chunk of
     * GPU memory represented by raw.
     */
    public @TempNoInline_0 def this(reg:Region{self!=null}, raw:RemoteIndexedMemoryChunk[T]) {
        val arr:GlobalRef[Array[T]];
        if (raw.home().isCUDA()) @Native("c++", "{}") {
            // This block will never be executed; only here to placate the X10-level typechecker
            arr = GlobalRef[Array[T]](null);
        } else {
            arr = at (raw.home()) GlobalRef[Array[T]](new Array[T](reg, raw()) as Array[T]);
        }
        property(reg, reg.size(), arr);
        rawData = raw;
    }

    /**
     * Return the element of this array corresponding to the given index.
     * Only applies to one-dimensional arrays.
     * Can only  be called where <code>here == array.home</code>. 
     * Functionally equivalent to indexing the array via a one-dimensional point.
     * 
     * @param i0 the given index in the first dimension
     * @return the element of this array corresponding to the given index.
     * @see #operator(Point)
     * @see #set(T, Int)
     */
    @Native("cuda", "(#this).raw[#1]")
    public operator this(i:Int) {here==array.home, rank==1} = this()(i);

    /**
     * Return the element of this array corresponding to the given point.
     * The rank of the given point has to be the same as the rank of this array.
     * Can only  be called where <code>here == array.home</code>. 
     * 
     * @param pt the given point
     * @return the element of this array corresponding to the given point.
     * @see #operator(Int)
     * @see #set(T, Point)
     */
    public operator this(p:Point{self.rank==this.rank}) {here==array.home} = this()(p);

    /**
     * Set the element of this array corresponding to the given index to the given value.
     * Return the new value of the element.
     * Only applies to one-dimensional arrays.
     * Can only  be called where <code>here == array.home</code>. 
     * Functionally equivalent to setting the array via a one-dimensional point.
     * 
     * @param v the given value
     * @param i0 the given index in the first dimension
     * @return the new value of the element of this array corresponding to the given index.
     * @see #operator(Int)
     * @see #set(T, Point)
     */
    @Native("cuda", "(#this).raw[#i] = (#v)")
    public operator this(i:Int)=(v:T) {here==array.home, rank==1} = this()(i)=v;

    /**
     * Set the element of this array corresponding to the given point to the given value.
     * Return the new value of the element.
     * The rank of the given point has to be the same as the rank of this array.
     * Can only  be called where <code>here == array.home</code>. 
     * 
     * @param v the given value
     * @param p the given point
     * @return the new value of the element of this array corresponding to the given point.
     * @see #operator(Point)
     * @see #set(T, Int)
     */
    public operator this(p:Point{self.rank==this.rank})=(v:T) {here==home} = {
        return this()(p)=v;
    }

    /**
     * Access the Array that is encapsulated by this RemoteArray. 
     * Can only  be called where <code>here == array.home</code>. 
     */
    public operator this() {here==array.home} = (this.array)() as Array[T]{self.rank==this.rank};

    public def equals(other:Any) {
        if (!(other instanceof RemoteArray[T])) return false;
        val oRA = other as RemoteArray[T];
        return oRA.array.equals(array);
    }

    public def hashCode() = array.hashCode();
}
