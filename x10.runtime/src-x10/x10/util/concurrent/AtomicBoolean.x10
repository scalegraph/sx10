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

import x10.compiler.Native;
import x10.compiler.NativeRep;
import x10.compiler.Volatile;

@NativeRep("java", "x10.core.concurrent.AtomicBoolean", null, "x10.core.concurrent.AtomicBoolean.$RTT")
public final class AtomicBoolean {
   /*
    * An int that will only contain 0 or 1 and is interpreted as an boolean.
    * We do this instead of using a boolean so that we know that compareAndSet_32 
    * can work on the whole memory word.
    */
    private @Volatile var value:int;
    
    public def this():AtomicBoolean {
        value = 0;
    }
    public def this(v:boolean):AtomicBoolean {
        value = v ? 1 : 0;
    }
    
    @Native("java", "#this.get()")
    public def get():boolean = value == 1;

    @Native("java", "#this.set(#v)")
    public def set(v:boolean):void {
        value = v ? 1 : 0;
    }

    @Native("java", "#this.compareAndSet(#expect,#update)")
    @Native("c++", "x10aux::atomic_boolean_funs::compareAndSet(#this, #expect, #update)")
    public native def compareAndSet(expect:boolean, update:boolean):boolean;

    @Native("java", "#this.weakCompareAndSet(#expect,#update)")
    @Native("c++", "x10aux::atomic_boolean_funs::weakCompareAndSet(#this, #expect, #update)")
    public native def weakCompareAndSet(expect:boolean, update:boolean):boolean;
    
    @Native("java", "#this.getAndSet(#v)")
    public def getAndSet(v:boolean):boolean {
	val oldVal = get();
	set(v);
	return oldVal;
    }

    @Native("java", "#this.toString()")
    public def toString():String = get().toString();
}
 
