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

import x10.compiler.Global;
import x10.compiler.Native;
import x10.compiler.Pinned;
import x10.util.Map;

/**
 * Ported from 2.0 to 2.1 via naive simulation of 
 *       2.0 style global object by injecting a root field
 *       that is a GlobalRef(this) and always accessing fields 
 *       as this.root().f instead of this.f.
 * TODO: Port to Dual Class implementation of global objects.
 */
public final class Clock(name:String) {
	
	private val root = GlobalRef[Clock](this);
	public def equals(a:Any) {
		if (a == null || ! (a instanceof Clock))
			return false;
		return (a as Clock).root == this.root;
	}
	public def hashCode() = root.hashCode();
	
    public static def make(): Clock = make("");
    public static def make(name:String):Clock {
        if (Runtime.STATIC_THREADS) throw new ClockUseException("Clocks are not compatible with static threads.");
        val clock = new Clock(name);
        Runtime.activity().clockPhases().put(clock, FIRST_PHASE);
        return clock;
    }

    public static FIRST_PHASE = 1n;
    // NOTE: all transient fields must always be accessed as this.root().f (and at place this.root.home), 
    // not this.f
    private transient var count:Int = 1n;
    private transient var alive:Int = 1n;
    private transient var phase:Int = FIRST_PHASE;

    private def this(name:String) {
        property(name);
    }

    // should be accessed through root()
    @Pinned private def resumeLocal()  {
        atomic 
            if (--alive == 0n) {
                alive = count;
                ++phase;
            }
    }
    // should be accessed through root()
    @Pinned private def dropLocal(ph:Int) {
        atomic {
            --count;
            if (-ph != phase) {
                if (--alive == 0n) {
                    alive = count;
                    ++phase;
                }
            }
        }
    }

    @Global private def get() = Runtime.activity().clockPhases().get(this).value;
    @Global private def put(ph:Int) = Runtime.activity().clockPhases().put(this, ph);
    @Global private def remove() = Runtime.activity().clockPhases().remove(this).value;
    @Global def register() {
        if (dropped()) clockUseException("async clocked");
        val ph = get();
        at (root) {
        	val me = root();
        	atomic {
        		 ++ me.count;
                 if (-ph != me.phase) 
                	 ++ me.alive;
        	}
        }   
        return ph;
     }
     @Global def resumeUnsafe() {
        Runtime.ensureNotInAtomic();
        val ph = get();
        if (ph < 0) return;
        at (root) {
        	val me = root();
        	me.resumeLocal();
        }
        put(-ph);
    }
    @Global def advanceUnsafe() {
    	Runtime.ensureNotInAtomic();
        val ph = get();
        val abs = Math.abs(ph);
        at (root) {
        	val me = root();
            if (ph > 0) me.resumeLocal();
            when (abs < me.phase);
        }
        put(abs + 1n);
    }
    @Global def dropUnsafe() {
        val ph = remove();
        at(root) {
        	val me = root();
        	me.dropLocal(ph);
        }
    }
    @Global def dropInternal() {
        val ph = get();
        at(root) {
            val me = root();
            me.dropLocal(ph);
        }
    }
    public @Global def registered():Boolean = Runtime.activity().clockPhases().containsKey(this);
    public @Global def dropped():Boolean = !registered();
    public @Global def phase():Int {
        if (dropped()) clockUseException("phase");
        return Math.abs(get());
    }
    public @Global def resume():void {
        if (dropped()) clockUseException("resume");
        resumeUnsafe();
    }
    public @Global def advance():void {
        if (dropped()) clockUseException("advance");
        advanceUnsafe();
    }
    public @Global def drop():void {
        if (dropped()) clockUseException("drop");
        dropUnsafe();
    }

    public def toString():String = name.equals("") ? System.identityToString(this) : name;
    
    private def clockUseException(method:String) {
        if (dropped()) throw new ClockUseException("invalid invocation of " + method + "() on clock " + toString() + "; calling activity is not clocked on this clock");
    }

    @Native("cuda", "__syncthreads()")
    public static def advanceAll():void {
        Runtime.ensureNotInAtomic();
        Runtime.activity().clockPhases().advanceAll();
    }

    public static def resumeAll():void { Runtime.activity().clockPhases().resumeAll(); }
}

// vim:shiftwidth=4:tabstop=4:expandtab
