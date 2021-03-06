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

package x10.compiler;

import x10.io.CustomSerialization;
import x10.io.SerialData;

/** 
 * A class that is used in elimination of finally clauses for the C++ backend.
 * 
 * NOT INTENDED FOR USE BY X10 PROGRAMMERS
 */
public class Finalization extends x10.lang.Exception implements CustomSerialization {
    
    public var value: Any          = null;
    public var label: String       = null;
    public var isReturn: boolean   = false;
    public var isBreak: boolean    = false;
    public var isContinue: boolean = false;

    public static val FALLTHROUGH = new Finalization(false, false, false);
    public static val RETURN_VOID = new Finalization(true, false, false);
    public static val SIMPLE_BREAK = new Finalization(false, true, false);
    public static val SIMPLE_CONTINUE = new Finalization(false, false, true);

    public def this(ret:boolean, br:boolean, cont:boolean) {
        isReturn = ret;
        isBreak = br;
        isContinue = cont;
    }

    /** */
    public static def throwReturn(): void {
        throw RETURN_VOID;
    }

    /** */
    public static def throwReturn(v: Any): void {
        val f      = new Finalization();
        f.value    = v;
        f.isReturn = true;
        throw f;
    }

    /** */
    public static def throwBreak(): void {
        throw SIMPLE_BREAK;
    }

    /** */
    public static def throwBreak(l: String): void {
        val f     = new Finalization();
        f.label   = l;
        f.isBreak = true;
        throw f;
    }

    /** */
    public static def throwContinue(): void {
        throw SIMPLE_CONTINUE;
    }

    /** */
    public static def throwContinue(l: String): void {
        val f        = new Finalization();
        f.label      = l;
        f.isContinue = true;
        throw f;
    }

    /** */ // justify a catch block for the Java compiler
    public static def plausibleThrow(): void {
        if (x10.compiler.CompilerFlags.TRUE()) return;
        throw FALLTHROUGH;
    }

    /**
     * Serialization of Finalization objects is forbidden.
     * @throws UnsupportedOperationException
     */
    public def serialize():SerialData {
    	throw new UnsupportedOperationException("Cannot serialize "+typeName());
    }

    /**
     * Serialization of Finalization objects is forbidden.
     * @throws UnsupportedOperationException
     */
    public def this(SerialData) {
    	throw new UnsupportedOperationException("Cannot deserialize "+typeName());
    }

    public def this(){}

}
