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

package x10.util.concurrent.atomic;

import x10.compiler.Native;

public class Fences {
    @Native("java", "x10.runtime.impl.java.Fences.loadLoadBarrier()")
    @Native("c++", "x10aux::atomic_ops::load_load_barrier()")
    public static native def loadLoadBarrier():void;

    @Native("java", "x10.runtime.impl.java.Fences.loadStoreBarrier()")
    @Native("c++", "x10aux::atomic_ops::load_store_barrier()")
    public static native def loadStoreBarrier():void;

    @Native("java", "x10.runtime.impl.java.Fences.storeLoadBarrier()")
    @Native("c++", "x10aux::atomic_ops::store_load_barrier()")
    public static native def storeLoadBarrier():void;

    @Native("java", "x10.runtime.impl.java.Fences.storeStoreBarrier()")
    @Native("c++", "x10aux::atomic_ops::store_store_barrier()")
    public static native def storeStoreBarrier():void;
}
 
