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
 * @author hhorii 02/2010
 */

class XTENLANG_637 extends x10Test {

    public def run(): boolean {
        val sub = new Sub_637();
        val csub = new Get_637[Sub_637](sub);
        val csup1 : Get_637[Super_637] = csub;
        val csup2 : Get_637[Super_637] = csub;
        return true;
    }

    public static def main(Array[String](1)) {
        new XTENLANG_637().execute();
    }
}

class Get_637[+X] {
  val x : X;
  def this(x:X) { this.x = x; }
}        
class Super_637  {
}

class Sub_637 extends Super_637 {
}