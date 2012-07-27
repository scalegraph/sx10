/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2012.
 */

import harness.x10Test;

// MANAGED_X10_ONLY

public class JavaException7 extends x10Test {

    static class Base {
        def f():Any throws java.io.IOException { throw new java.io.IOException("Exception from Base."); }
    }

    public def run(): Boolean {
        var ok:Boolean = true;
        var ex:x10.lang.CheckedThrowable;
        
        try {
        	ex = null;
            new Base().f();
        } catch (e:x10.lang.CheckedThrowable) {
            ex = e;
        } finally {
        	if (ex == null) {
        		ok = false;
        	} else {
        		// ex.printStackTrace();
        	}
        }
        
        try {
        	ex = null;
        	new Base() {
        		def f():Any throws java.io.IOException { throw new java.io.IOException("Exception from a local class."); }
            }.f();
        } catch (e:x10.lang.CheckedThrowable) {
            ex = e;
        } finally {
        	if (ex == null) {
        		ok = false;
        	} else {
        		// ex.printStackTrace();
        	}
        }
        
        return ok;
    }

    public static def main(args: Array[String](1)) {
        new JavaException7().execute();
    }

}
