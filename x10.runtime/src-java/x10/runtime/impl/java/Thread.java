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

import x10.lang.Place;
import x10.rtt.RuntimeType;
import x10.rtt.Type;

/**
 * @author Christian Grothoff
 * @author vj
 * @author Raj Barik, Vivek Sarkar
 * @author tardieu
 */
public class Thread extends java.lang.Thread {
    public RuntimeType<?> getRTT() { return null; }
    public Type<?> getParam(int i) { return null; }

	public static Thread currentThread() {
		return (Thread) java.lang.Thread.currentThread();
	}

	private Place home;    // the current place

    public Thread(final x10.core.fun.VoidFun_0_0 body, String name) {
        super(new Runnable() { public void run() { body.apply(); } }, name);
        if (!(java.lang.Thread.currentThread() instanceof Thread)) {
            home = Place.place(0);
        } else {
            home = currentThread().home();
        }
    }

	/**
	 * Update thread place (called by native runtime only)
	 */
	void home(int home) {
		this.home = Place.place(home);
	}

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
}
