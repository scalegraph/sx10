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

package x10.lang;

import x10.compiler.Pinned;

/**
 * @author tardieu
 */
@Pinned public class SimpleIntLatch extends Lock {
    public def this() { super(); }

    private def this(Any) {
        throw new UnsupportedOperationException("Cannot deserialize "+typeName());
    }

    static type Worker = Runtime.Worker;

    private var worker:Worker = null;
    private var value:Int = 0;

    // can only be called once
    public def await():void {
        if (value != 0) return;
        lock();
        Runtime.increaseParallelism(); // likely to be blocked for a while
        worker = Runtime.worker();
        while (value == 0) {
            unlock();
            Worker.park();
            lock();
        }
        unlock();
    }

    public def set(v:Int):void {
        lock();
        value = v;
        if (worker != null) {
            Runtime.decreaseParallelism(1);
            worker.unpark();
        }
        unlock();
    }

    public def apply():Int = value;
}
