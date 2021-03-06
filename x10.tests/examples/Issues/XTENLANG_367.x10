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

import harness.x10Test;

/**
 * @author igor 4/2009
 */

class XTENLANG_367 extends x10Test {

    public def run(): boolean {
        var count: int = 0;
        try {
            try {
                count++;
                throw new Exception();
            } catch (e: Exception) {
                count++;
            } finally {
                count++;
                throw new Exception();
            }
        } catch (e: Exception) { }
        return count == 3;
    }

    public static def main(Array[String](1)) {
        new XTENLANG_367().execute();
    }
}
