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

/**
 * A place-local worker-local handle.
 * 
 * @author tardieu
 */
public class WorkerLocalHandle[T] implements ()=>T,(T)=>void {
    private val store:PlaceLocalHandle[Array[T]{rail}];

    public def this(t:T) {
        store = PlaceLocalHandle.make[Array[T]{rail}](Dist.makeUnique(), ()=>new Array[T](Runtime.MAX_WORKERS, t));
    }

    public def apply():T {
        return store()(Runtime.workerId());
    }

    public def apply(t:T):void {
        store()(Runtime.workerId()) = t;
    }
}

// vim:shiftwidth=4:tabstop=4:expandtab
