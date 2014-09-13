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
import x10.compiler.NativeRep;


@NativeRep("java", "java.lang.NullPointerException", null, "x10.rtt.Types.NULL_POINTER_EXCEPTION")
public class NullPointerException extends Exception {

    @Native("java", "new java.lang.NullPointerException()")
    public def this() { super(); }

    @Native("java", "new java.lang.NullPointerException(#message)")
    public def this(message: String) { super(message); } 

}
