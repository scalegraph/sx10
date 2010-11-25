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

package x10.util;

import x10.compiler.Native;

/** A collection of functions useful in/around CUDA kernels.
 * @author Dave Cunningham
 */
public class CUDAUtilities {

    /** Automatically choose enough blocks to saturate the GPU.  This takes
     * account features of the GPU and kernel in question.  If running on the CPU
     * it returns a fixed number of blocks.  Intended to be used with autoThreads().
     * <p>
     * <code>
     * async at (gpu) {
     *     val threads = CUDAUtilities.autoThreads(), blocks = CUDAUtilities.autoBlocks();
     *     for ((block) in 0..blocks-1) {
     *         ...
     *         for ((thread) in 0..threads-1) async { ... }
     *     }
     * }
     * </code>
     * @see autoThreads
     */
    public static def autoBlocks() : Int = 8;

    /** Automatically choose enough threads to saturate the GPU.  
      * @see autoBlocks
      */
    public static def autoThreads() : Int = 1;

    private static def initCUDAArray[T] (local:IndexedMemoryChunk[T],
                                         remote:RemoteIndexedMemoryChunk[T],
                                         numElements:Int) : Void {
          finish IndexedMemoryChunk.asyncCopy(local, 0, remote, 0, numElements);
    }

    private static def makeCUDAArray[T] (gpu:Place, numElements:Int, init:IndexedMemoryChunk[T])
      : RemoteArray[T]{self.home==gpu, self.rank()==1} {
        val reg = 0 .. numElements-1;
        @Native("c++",
            "x10_ulong addr = x10aux::remote_alloc(gpu.FMGL(id), ((size_t)numElements)*sizeof(FMGL(T)));\n"+
            "RemoteIndexedMemoryChunk<FMGL(T)> rimc(addr, numElements, gpu);\n"+
            "initCUDAArray<FMGL(T)>(init,rimc,numElements);\n"+
            "return x10::array::RemoteArray<FMGL(T)>::_make(gpu, reg, rimc, numElements);\n"
        ) { }
        throw new UnsupportedOperationException();
    }

    public static def makeRemoteArray[T] (place:Place, numElements:Int, init: Array[T]{rail})
        : RemoteArray[T]{self.rank==1, self.home==place}
    {
        if (place.isCUDA()) {
            return makeCUDAArray(place, numElements, init.raw());
        } else {
            return at (place) new RemoteArray(new Array[T](numElements, (p:Int)=>init(p)));
        }
    }

    public static def makeRemoteArray[T] (place:Place, numElements:Int, init: T)
        : RemoteArray[T]{self.rank==1, self.home==place}
    {
        if (place.isCUDA()) {
            val chunk = IndexedMemoryChunk.allocate[T](numElements);
            for ([i] in 0..numElements-1) chunk(i) = init;
            return makeCUDAArray(place, numElements, chunk);
        } else {
            return at (place) new RemoteArray(new Array[T](numElements, init));
        }
    }

    public static def makeRemoteArray[T] (place:Place, numElements:Int, init: (Int)=>T)
        : RemoteArray[T]{self.rank==1, self.home==place}
    {
        if (place.isCUDA()) {
            val chunk = IndexedMemoryChunk.allocate[T](numElements);
            for ([i] in 0..numElements-1) chunk(i) = init(i);
            return makeCUDAArray(place, numElements, chunk);
        } else {
            return at (place) new RemoteArray(new Array[T](numElements, (p:Int)=>init(p)));
        }
    }

    public static def deleteRemoteArray[T] (arr: RemoteArray[T]{self.rank==1}) : Void
    {
        val place = arr.home;
        if (place.isCUDA()) {
            @Native("c++",
                "IndexedMemoryChunk<FMGL(T)> imc = arr->FMGL(rawData)->raw();\n"+
                "x10aux::remote_free(place.FMGL(id), (x10_ulong)(size_t)imc->raw());\n"
            ) { }
        }
    }

    @Native("cuda","__mul24(#1,#2)")
    public static def mul24 (a:Int, b:Int) : Int = a * b;
}

// vim: shiftwidth=4:tabstop=4:expandtab
