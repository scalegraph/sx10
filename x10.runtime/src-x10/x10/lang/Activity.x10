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

import x10.io.SerialData;
import x10.util.HashMap;

/**
 * Runtime representation of an async. Only to be used in the runtime implementation.
 * 
 * @author tardieu
 */
class Activity {

    static class ClockPhases extends HashMap[Clock,Int] {
        // compute spawnee clock phases from spawner clock phases in async clocked(clocks)
        // and register spawnee on these on clocks
        static def make(clocks:Array[Clock](1){rail}) {
            val clockPhases = new ClockPhases();
            for(var i:Int = 0; i < clocks.size; i++) 
                clockPhases.put(clocks(i), clocks(i).register());
            return clockPhases;
        }

        // next statement
        def next() {
            for(clock:Clock in keySet()) clock.resumeUnsafe();
            for(clock:Clock in keySet()) clock.nextUnsafe();
        }

        // resume all clocks
        def resume() {
            for(clock:Clock in keySet()) clock.resume();
        }

        // drop all clocks
        def drop() {
            for(clock:Clock in keySet()) clock.dropInternal();
            clear();
        }

        // HashMap implements CustomSerialization, so we must as well
        public def serialize():SerialData {
	    // minor optimization instead of doing:
            //    new SerialData(null, super.serialize())
            // just return super.serialize() directly
            return super.serialize();
        }
        def this() { super(); }
        def this(a:SerialData) { 
            super(a);  // see optimization in serialize();
        }
    }

    /**
     * the finish state governing the execution of this activity (may be remote)
     */
    private var finishState:FinishState;

    /**
     * safe to run pending jobs while waiting for a finish (temporary)
     */
    private val safe:Boolean;

    /**
     * The user-specified code for this activity.
     */
    private val body:()=>void;

    /**
     * The mapping from registered clocks to phases for this activity.
     * Lazily created.
     */
    var clockPhases:ClockPhases;

    /**
     * Depth of enclosong atomic blocks
     */
    private var atomicDepth:int = 0;

    /**
     * The place of the activity (for the java backend).
     */
    val home = Runtime.hereInt();

    /**
     * Create activity.
     */
    def this(body:()=>void, finishState:FinishState, safe:Boolean) {
        this.finishState = finishState;
        this.safe = safe;
        finishState.notifyActivityCreation();
        this.body = body;
    }

    def this(body:()=>void, finishState:FinishState) {
        this.finishState = finishState;
        this.safe = true;
        finishState.notifyActivityCreation();
        this.body = body;
    }

    /**
     * Create clocked activity.
     */
    def this(body:()=>void, finishState:FinishState, clockPhases:ClockPhases) {
        this(body, finishState, false);
        this.clockPhases = clockPhases;
    }

    /**
     * Return the clock phases
     */
    def clockPhases():ClockPhases {
        if (null == clockPhases)
            clockPhases = new ClockPhases();
        return clockPhases;
    }

    /**
     * Return the innermost finish state
     */
    def finishState():FinishState = finishState;

    /**
     * Enter finish block
     */
    def swapFinish(f:FinishState) {
        val old = finishState;
        finishState = f;
        return old;
    }

    def safe():Boolean = safe && (null == clockPhases);

    // about atomic blocks

    def pushAtomic() {
        atomicDepth++;
    }

    def popAtomic() {
        atomicDepth--;
    }

    def ensureNotInAtomic() {
        if (atomicDepth > 0)
            throw new IllegalOperationException();
    }

    /**
     * Run activity.
     */
    def run():void {
        try {
            body();
        } catch (t:Throwable) {
            finishState.pushException(t);
        }
        if (null != clockPhases) clockPhases.drop();
        finishState.notifyActivityTermination();
        Runtime.dealloc(body);
    }
}

// vim:shiftwidth=4:tabstop=4:expandtab
