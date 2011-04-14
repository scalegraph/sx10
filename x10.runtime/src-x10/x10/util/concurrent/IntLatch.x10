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

package x10.util.concurrent;

import x10.compiler.Pinned;

/**
 * Int latch.
 * Inherited look/unlock/tryLock method from superclass can be used.
 *
 * @author tardieu
 */
@Pinned public class IntLatch extends Monitor implements ()=>Int {
    public def this() { super(); }

    private def this(Any) {
        throw new UnsupportedOperationException("Cannot deserialize "+typeName());
    }

    private var value:Int = 0;

    public operator this()=(i:Int):void { set(i); }
    public def set(i:Int):void {
        lock();
        value = i;
        super.release();
    }

    public def await():void {
        Runtime.ensureNotInAtomic();
        if (value == 0) {
            lock();
            while (value == 0) super.await();
                unlock();
            }
    }

    public operator this():Int = value;
}
