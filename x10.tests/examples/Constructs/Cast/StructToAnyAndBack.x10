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
 * Test cases for structs upcast to Any and then downcast to
 * various related types.
 *
 * See XTENLANG-1013 for discussion of language design decision
 * encoded in this test case.
 */
class StructToAnyAndBack extends x10Test {

    static struct S implements Comparable[S] {
       val x:int;
       def this(a:int) { x = a; }
       public def compareTo(o:S) { return x.compareTo(o.x); }
    }

    static def testAStruct[S,T](v:Any):boolean {
        val x = v as S;             // should succeed.
        val y = v as Comparable[S]; // should succeed
        try {
            val z = v as T; // should fail
            Console.OUT.println("Did not raise exception "+v.typeName());
            return false;
        } catch (e:ClassCastException) {
            return true;
        } catch (e:Throwable) {
            Console.OUT.println("Raised wrong exception "+e);
            return false;
        }
    }
    
    public def run():boolean {
        var res:boolean = true;
        res &= testAStruct[int,float](1);
        res &= testAStruct[ubyte, double](0yu);
        res &= testAStruct[float, short](3.0f);
        res &= testAStruct[S, int](S(10));

        return res;
    }

    public static def main(Array[String](1)) {
        new StructToAnyAndBack().execute();
    }
}
