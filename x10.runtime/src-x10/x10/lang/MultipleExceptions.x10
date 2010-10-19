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

import x10.util.Stack;
import x10.io.Printer;


/**
 * @author Christian Grothoff
 * @author tardieu
 */
public class MultipleExceptions(exceptions:Array[Throwable]{rail}) extends RuntimeException {

    public def this(stack:Stack[Throwable]) {
        val s = new Stack[Throwable]();
        // flatten MultipleExceptions in the stack
        for (t in stack) {
            if (t instanceof MultipleExceptions) {
                for (u: Throwable in (t as MultipleExceptions).exceptions.values()) 
		    s.push(u); 
            } else {
                s.push(t);
            }
        }
        property(s.toArray());
    }

    public def this(t: Throwable) {
        val s = new Stack[Throwable]();
        if (t instanceof MultipleExceptions) {
            for (u: Throwable in (t as MultipleExceptions).exceptions.values()) 
		s.push(u); 
        } else {
            s.push(t);
        }
        property(s.toArray());
    }

    // workarounds for XTENLANG-283, 284

    public def printStackTrace(): void {
        //super.printStackTrace();
        for (t: Throwable in exceptions.values()) {
	        t.printStackTrace();
        }
    }

    public def printStackTrace(p:Printer): void {
        //super.printStackTrace(p);
        for (t: Throwable in exceptions.values()) {
            t.printStackTrace(p);
        }
    }
}
