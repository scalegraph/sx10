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

import harness.x10Test;

/**
 * Purpose: Checks cast from nullable to non-nullable that implies 
 *          boxing/unboxing operation.
 * Issue: The nullable primitive is null which makes the cast to a non-nullable primitive fail.
 * @author vcave
 **/
 public class CastNullAnyToPrimitive extends x10Test {

    public def run(): boolean {
        try {
            var k: Any = null; // ok
	    var p: int = k as int;  // --> fails because 'k' is null which throws a ClassCastException
	} catch (e: ClassCastException) {
            return true;
	}
        return false;
    }

    public static def main(var args: Rail[String]): void = {
        new CastNullAnyToPrimitive().execute();
    }
}
