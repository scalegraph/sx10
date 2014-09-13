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

import x10.compiler.Native;
import x10.compiler.NativeCPPInclude;

import x10.io.Console;
import x10.util.Map;
import x10.util.Timer;
import x10.util.Pair;

@NativeCPPInclude("x10/lang/RuntimeNatives.h")
public class System {

    private def this() {}

    /**
     * Provides the current time in milliseconds.
     *
     * @return The current time in milliseconds.
     */
    public static def currentTimeMillis():Long = Timer.milliTime();

    /**
     * Provides the current time in nanoseconds, as precise as the system timers provide.
     *
     * @return The current time in nanoseconds.
     */
    public static def nanoTime():Long = Timer.nanoTime();

    /**
     * Kills the current place, as if due to a hardware or low level software failure.  Behaviour is only well-defined if executed at a place other than Place.FIRST_PLACE and the language is in resilient mode.
     */
    @Native("java", "java.lang.System.exit(1)")
    @Native("c++", "::x10::lang::RuntimeNatives::exit(1)")
    public static native def killHere(): void;

    /**
     * Sets the system exit code.
     * The exit code will be returned from the application when main() terminates.
     * Can only be invoked in place 0.
     */
    @Native("java", "x10.runtime.impl.java.Runtime.setExitCode(#exitCode)")
    @Native("c++", "(::x10aux::exitCode = (#exitCode))")
    public static def setExitCode(exitCode: Int){here==Place.FIRST_PLACE}: void {}

    /**
     * Provides an estimate in bytes of the size of the X10 heap
     * allocated to the current place. The accuracy of this estimate
     * is highly dependent on the implementation details of the
     * underlying memory management scheme being used by the X10 runtime,
     * and in some cases may simply return Long.MAX_VALUE or some other similarly
     * over conservative approximation.
     *
     * @return An upper bound in bytes on the size of the X10 heap allocated to the current place.
     */
    @Native("java", "java.lang.Runtime.getRuntime().totalMemory()")
    @Native("c++", "::x10aux::heap_size()")
    public static native def heapSize():Long;

    /**
     * Trigger a garbage collection.
     */
    @Native("java", "java.lang.System.gc()")
    @Native("c++", "::x10aux::trigger_gc()")
    public static native def gc():void;

    /**
     * Returns an immutable map from environment variables to values.
     */
    public static def getenv():Map[String,String] = Runtime.env;

    /**
     * Returns the value of the specified environment variable, or null if the variable is not defined.
     */
    public static def getenv(name:String):String = Runtime.env.getOrElse(name, null);

    /**
     * Sets the system property with the given name to the given value.
     *
     * @param p the name of the system property.
     * @param v the value for the system property.
     * TODO: @ return The previous value of the property, or null if it did not have one.
     */
    // TODO: XTENLANG-180.  Provide full System properties API in straight X10
    @Native("java", "java.lang.System.setProperty(#p,#v)")
    @Native("c++", "printf(\"not setting %s\\n\", (#p)->c_str())") // FIXME: Trivial definition to allow XRX compilation to go through.
    public static native def setProperty(p:String,v:String):void;

    /** Get the type name of T as a string
     * @param T a type
     * @return The name of type T 
     */
    @Native("java", "#T$rtt.typeName()")
    @Native("c++", "::x10aux::makeStringLit(x10aux::getRTT< #T>()->name())")
    static native def typeName[T]():String;

    @Native("java", "x10.rtt.Types.typeName(#o)")
    @Native("c++", "::x10aux::type_name(#o)")
    public static native def identityTypeName(o:Any) : String;

    @Native("java", "java.lang.System.identityHashCode(#o)")
    @Native("c++", "::x10aux::identity_hash_code(reinterpret_cast<x10::lang::Reference*>(#o))")
    public static native def identityHashCode(o:Any) : Int;

    public static def identityToString(o:Any) : String = o.typeName() + "@" + System.identityHashCode(o).toHexString();

    public static def identityEquals(o1:Any, o2:Any) : Boolean = o1==o2;

    /**
     * Sleep for the specified number of milliseconds.
     * [IP] NOTE: Unlike Java, x10 sleep() simply exits when interrupted.
     * @param millis the number of milliseconds to sleep
     * @return true if completed normally, false if interrupted
     */
    public static def sleep(millis:Long):Boolean {
        try {
            Runtime.increaseParallelism();
            Thread.sleep(millis);
            Runtime.decreaseParallelism(1n);
            return true;
        } catch (e:InterruptedException) {
            Runtime.decreaseParallelism(1n);
            return false;
        }
    }

    /**
     * Sleep for the specified number of milliseconds.
     * @param millis the number of milliseconds to sleep
     * @return true if completed normally, false if interrupted
     */
    public static def threadSleep(millis:Long):Boolean {
        try {
            Thread.sleep(millis);
            return true;
        } catch (e:InterruptedException) {
            return false;
        }
    }
}
