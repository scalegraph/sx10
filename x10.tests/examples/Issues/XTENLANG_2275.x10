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
 * @author vj
 */
public class XTENLANG_2275 extends x10Test {
    public static def main(Array[String](1)) {
        new XTENLANG_2275().execute();
    }

    public def run()=true;
    
    class ConstraintPropagationBug {
      def m() {
        val i: Iterator[Point{self.rank==2}] = ((1..1)*(1..1)).iterator(); // should compile
      }
    }
	}
