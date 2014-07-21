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

/**
 */
final class Configuration {

    @Native("c++", "PLATFORM_MAX_THREADS")
    private static PLATFORM_MAX_THREADS: Int = Int.MAX_VALUE;

    @Native("c++", "DEFAULT_STATIC_THREADS")
    private static DEFAULT_STATIC_THREADS: Boolean = false;

    @Native("java", "java.lang.Runtime.getRuntime().availableProcessors()")
    private static AVAILABLE_PROCESSORS:Int = 1n;

    @Native("java", "x10.runtime.impl.java.Runtime.loadenv()")
    @Native("c++", "::x10::lang::RuntimeNatives::loadenv()")
    static native def loadEnv():x10.util.HashMap[String,String];

    static def envOrElse(s:String, b:Boolean):Boolean {
        val v = Runtime.env.getOrElse(s, null);
        if (v == null) return b;
        return !(v.equalsIgnoreCase("false") || v.equalsIgnoreCase("f") || v.equals("0"));
    }

    @Native("java", "x10.runtime.impl.java.Runtime.sysPropOrElse(#s, #b)")
    static def sysPropOrElse(s:String, b:Boolean):Boolean {
        return b;
    }

    @Native("java", "x10.runtime.impl.java.Runtime.sysPropOrElse(#s, #i)")
    static def sysPropOrElse(s:String, i:Int):Int {
        return i;
    }

    static def strict_finish():Boolean = envOrElse("X10_STRICT_FINISH", false);

    static def static_threads():Boolean = envOrElse("X10_STATIC_THREADS", DEFAULT_STATIC_THREADS);

    static def warn_on_thread_creation():Boolean = envOrElse("X10_WARN_ON_THREAD_CREATION", false);

    static def busy_waiting():Boolean = envOrElse("X10_BUSY_WAITING", false);

    /**
     * Enable support for job cancellation
     * Off by default to mitigate performance penalty
     */
    static def cancellable():Boolean { 
        val envVar = envOrElse("X10_CANCELLABLE", false);
        val sysProp = sysPropOrElse("X10_CANCELLABLE", envVar);
        return sysProp;
    }

    static def nthreads():Int {
        var v:Int = 0n;
        try {
            v = Int.parse(Runtime.env.getOrElse("X10_NTHREADS", "1"));
        } catch (NumberFormatException) {
        }
        if (v <= 0) v = AVAILABLE_PROCESSORS;
        if (v > PLATFORM_MAX_THREADS) v = PLATFORM_MAX_THREADS;
        return v;
    }

    static def max_threads():Int {
        var v:Int = 0n;
        try {
           v = Int.parse(Runtime.env.getOrElse("X10_MAX_THREADS", "0"));
       } catch (NumberFormatException) {
       }
       if (v <= 0) v = nthreads();
       if (!static_threads() && v < 1000) v = 1000n;
       if (v > PLATFORM_MAX_THREADS) v = PLATFORM_MAX_THREADS;
       return v;
    }
    
    // Note that "X10_RESILIENT_MODE" is also checked in x10rt/sockets/Launcher.cc
    static val RESILIENT_MODE_NONE = 0n;
    static val RESILIENT_MODE_DEFAULT     = 1n;  // Most stable implementation of resilient finish (see FinishResilient.x10)
    static val RESILIENT_MODE_PLACE0      = 11n; // FinishResilientPlace0
    static val RESILIENT_MODE_X10RT_ONLY  = 99n; // Resilient/Elastic X10RT, no resilient finish
    // The modes below are under development and not yet complete.
    static val RESILIENT_MODE_SAMPLE      = 91n; // FinishResilientSample + ResilientStorePlace0
    static val RESILIENT_MODE_SAMPLE_HC   = 92n; // FinishResilientSample + ResilientStoreHC

    static def resilient_mode():Int { // called from Runtime.x10
        var v:Int = RESILIENT_MODE_NONE;
        try {
            v = Int.parse(Runtime.env.getOrElse("X10_RESILIENT_MODE", "0"));
        } catch (NumberFormatException) {
        }
        v = sysPropOrElse("X10_RESILIENT_MODE", v);
        return v;
    }
}

// vim:shiftwidth=4:tabstop=4:expandtab
