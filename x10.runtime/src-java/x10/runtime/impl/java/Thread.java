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

package x10.runtime.impl.java;

import x10.io.SerialData;
import x10.lang.Place;
import x10.rtt.NamedType;
import x10.rtt.RuntimeType;
import x10.rtt.Type;
import x10.rtt.Types;
import x10.x10rt.X10JavaSerializer;
import x10.x10rt.X10RT;

import java.io.IOException;

/**
 * @author Christian Grothoff
 * @author vj
 * @author Raj Barik, Vivek Sarkar
 * @author tardieu
 */
public class Thread extends java.lang.Thread implements x10.core.RefI {
    private static final long serialVersionUID = 1L;
    public static final RuntimeType<Thread> $RTT = new NamedType<Thread>("x10.lang.Thread", Thread.class, new Type<?>[] { Types.OBJECT });
    public RuntimeType<?> $getRTT() { return $RTT; }
    public Type<?> $getParam(int i) { return null; }

    public static Thread currentThread() {
    	java.lang.Thread jthread = java.lang.Thread.currentThread();
    	if (!(jthread instanceof Thread)) {
    		System.out.println("X10 thread is required to run X10 method.");
    		System.out.flush();
    	}
        return (Thread) jthread;
    }

    private Place home;    // the current place
	
    public x10.core.fun.VoidFun_0_0 body;

    // constructor just for allocation
    public Thread(java.lang.System[] $dummy) {}
    public Thread(SerialData $dummy) {
        throw new UnsupportedOperationException("Cannot deserialize Thread");
    }

    public Thread $init(String name) {
        setName(name);
        if (!(java.lang.Thread.currentThread() instanceof Thread)) {
            // WIP for Emitter.mangleSignedNumeric
            home = Place.place(X10RT.here());
//            home = Place.place$s0(X10RT.here());
        } else {
            home = currentThread().home();
        }
        return this;
    }
	
    public Thread(String name) {
        super(name);
        if (!(java.lang.Thread.currentThread() instanceof Thread)) {
            // WIP for Emitter.mangleSignedNumeric
            home = Place.place(X10RT.here());
//            home = Place.place$s0(X10RT.here());
        } else {
            home = currentThread().home();
        }
    }

    public void run() {
        if (null != body) {
            body.$apply();
        } else {
            $apply();
        }
    }

    public void $apply() {}

    /**
     * Return current place
     */
    public Place home() {
        return home;
    }

    public String name() {
        return getName();
    }

    public void name(String name) {
        setName(name);
    }

    public static void park() {
        java.util.concurrent.locks.LockSupport.park();
    }

    public void unpark() {
        java.util.concurrent.locks.LockSupport.unpark(this);
    }

    public static void parkNanos(Long nanos) {
        java.util.concurrent.locks.LockSupport.parkNanos(nanos);
    }

    public static long getTid() {
        return Thread.currentThread().getId();
    }

    public static void sleep(long time) {
        Thread.sleep(time, 0);
    }
    // for Emitter.mangleSignedNumeric
    public static void sleep$s0(long time) {
        Thread.sleep$s0$s1(time, 0);
    }

    public static void sleep(long time, int nanos) {
        try {
            java.lang.Thread.sleep(time, nanos);
        } catch (InterruptedException e) {
            x10.core.Throwable e1 = null;
            try {
                e1 = (x10.core.Throwable)Class.forName("x10.lang.InterruptedException").newInstance();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            throw e1;
        }
    }

    public void $_serialize(X10JavaSerializer serializer) throws IOException {
        throw new UnsupportedOperationException("Cannot serialize " + getClass());
    }

        public int $_get_serialization_id() {
            throw new UnsupportedOperationException("Cannot serialize " + getClass());
        }

    // for Emitter.mangleSignedNumeric
    public static void sleep$s0$s1(long time, int nanos) {
        try {
            java.lang.Thread.sleep(time, nanos);
        } catch (InterruptedException e) {
            x10.core.Throwable e1 = null;
            try {
                e1 = (x10.core.Throwable)Class.forName("x10.lang.InterruptedException").newInstance();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            throw e1;
        }
    }
}
