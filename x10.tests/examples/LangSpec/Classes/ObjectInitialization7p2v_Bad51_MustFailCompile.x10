/* Current test harness gets confused by packages, but it would be in package ObjectInitialization7p2v_Bad51_MustFailCompile;
*/
// Warning: This file is auto-generated from the TeX source of the language spec.
// If you need it changed, work with the specification writers.


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

import x10.compiler.*;

public class ObjectInitialization7p2v_Bad51_MustFailCompile extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new ObjectInitialization7p2v_Bad51_MustFailCompile().execute();
    }


// file Classes line 2864
 static class UseNoThisAccess {
  static  class IDed {
    protected static val counts = [0 as Int,0];
    protected var code : Int;
    val id: Float;
    public def this(kind:Int) {
      code = kind;
      this.id = this.count(kind);
    }
    protected static def kind2count(kind:Int) = ++counts(kind % 2);
    @NoThisAccess def count(kind:Int) : Float = kind2count(kind);
  }
  static  class SubIDed extends IDed {
    protected static val subcounts = [0 as Int, 0, 0];
    public static val all = new x10.util.ArrayList[SubIDed]();
    public def this(kind:Int) {
       super(kind);
    }
    @NoThisAccess
    def count(kind:Int) : Float {
       val subcount <: Int = ++subcounts(kind % 3);
       val supercount <: Float = kind2count(kind);
 val badSuperCount = super.count(kind); //(A) // ERR
       //ERROR: code = kind;                           //(B)
       //ERROR: all.add(this);                         //(C)
       return  supercount + 1.0f / subcount;
    }
  }
}

 static class Hook {
   def run():Boolean = true;
}

}
