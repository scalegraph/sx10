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
import x10.util.Stack;

/**
 * Lock with wait/notify capabilities.
 * Cooperates with runtime scheduler.
 * 
 * @author tardieu
 */
@Pinned public class Monitor extends Lock {
    public def this() { super(); }

    private def this(Any) {
        throw new UnsupportedOperationException("Cannot deserialize "+typeName());
    }

    static type Worker = Runtime.Worker;
    
    /**
     * Parked workers
     */
    private val workers = new Stack[Worker]();

    /**
     * Aquire the lock
     */
    public def lock():void {
        if (super.tryLock()) return;
        Runtime.increaseParallelism(); // likely to be blocked for a while
        super.lock();
        Runtime.decreaseParallelism(1);
    }

    /**
     * Try acquiring the lock
     */
    public def tryLock():boolean {
        return super.tryLock();
    }

    /**
     * Release the lock
     */
    public def unlock():void {
        super.unlock();
    }

    /**
     * Await notification
     * Must be called while holding the lock
     * Must not be called while holding the lock more than once
     */
    public def await():void {
        Runtime.increaseParallelism(); // likely to be blocked for a while
        val worker = Runtime.worker();
        workers.push(worker);
        while (workers.contains(worker)) {
            super.unlock();
            Worker.park();
            super.lock();
        }
    }

    /**
     * Notify and unlock
     * Must be called while holding the lock
     */
    public def release():void {
        val size = workers.size();
        if (size > 0) {
            Runtime.decreaseParallelism(size);
            for (var i:Int = 0; i<size; i++) workers.pop().unpark();
        }
        super.unlock();
    }
}

