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
 * @author yzibin 1/2011
 */
class XTENLANG_2388 extends x10Test {
    static struct S1 {}
    static struct S2(i:Int,j:Double) {}
    static struct S3(i:Int{self!=0},j:Double) {}
  
    public def run(): boolean {
        chk(Int haszero);
        chk(!(Int{self!=0} haszero));
        chk((Int{self!=1} haszero));
        chk((Int{self==0} haszero));
        chk(!(Int{self==1} haszero));
        chk(Char haszero);
        chk(Double haszero);
        chk(Float haszero);
        chk(Byte haszero);
        chk(Short haszero);
        chk(Long haszero);
        chk(UByte haszero);
        chk(UShort haszero);
        chk(UInt haszero);
        chk(ULong haszero);

        chk(Any haszero);
        chk(!(Any{self!=null} haszero));
        chk(String haszero);
        chk(String{self==null} haszero);
        chk(!(String{self!=null} haszero));
        chk(Empty haszero);

        chk(S1 haszero);
        chk(S2 haszero);
        chk(!(S3 haszero));
        chk(!(S2{i!=0} haszero));

        return true;
    }

    public static def main(Array[String](1)) {
        new XTENLANG_2388().execute();
    }
}
