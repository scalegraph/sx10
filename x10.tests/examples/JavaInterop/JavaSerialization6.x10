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
import x10.interop.Java;
import x10.util.Pair;

// MANAGED_X10_ONLY

public class JavaSerialization6 extends x10Test {
    def testArray(): Boolean {
        type Data = Pair[Pair[Java.array[Byte],Int],Pair[Java.array[Byte],Int]];
        val arr = new Array[Any](10, (i:Int)=>Pair(Pair(Java.newArray[Byte](i),i),Pair(Java.newArray[Byte](i),i)));
        at (here.next()) {
            for (var i:Int = 0; i < arr.size; ++i) {
                val data = arr(i) as Data;
                val firstbytes = data.first.first;
                val firstlen = data.first.second;
                chk(firstbytes != null && firstbytes.typeName().equals("x10.interop.Java.array[x10.lang.Byte]") && firstbytes.length == firstlen);
                val secondbytes = data.second.first;
                val secondlen = data.second.second;
                chk(secondbytes != null && secondbytes.typeName().equals("x10.interop.Java.array[x10.lang.Byte]") && secondbytes.length == secondlen);
            }
        }
        return true;
    }

    public def run(): Boolean {
        return testArray();
    }

    public static def main(args: Array[String](1)) {
        new JavaSerialization6().execute();
    }
}
