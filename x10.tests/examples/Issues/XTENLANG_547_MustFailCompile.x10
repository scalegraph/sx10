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

abstract class Writer {
    public abstract def write(x: Byte): Void;

    public def write(buf: GlobalRef[Rail[Byte]]): Void {
        val mybuf = buf as GlobalRef[Rail[Byte]]{self.home==here};
        write(mybuf, 0, mybuf().length);
    }

    public def write(buf: GlobalRef[Rail[Byte]]{self.home==here}, off: Int, len: Int) {
        for (var i: Int = off; i < off+len; i++) {
            write(buf()(i));
        }
    }
}

class OutputStreamWriter extends Writer {
    public def write(x: Byte): Void { }

    public def write(buf: GlobalRef[Rail[Byte]]): Void {
    }

    // This should cause the compiler to issue an error.
    // OutputStreamWriter inherits def write(buf:GlobalRef[Rail[Byte]]{self.home==here}, Int, Int)
    // and its constraint erasure is identical with the method below. But a class cant have two
    // different method definitions whose constraint erasures are identical. And a method
   // can only be overridden by a method which has the same constrained type signature.
    public def write(buf:GlobalRef[Rail[Byte]], off: Int, len: Int): Void {
    }
}

public class XTENLANG_547_MustFailCompile extends x10Test {
    public static def main(Array[String](1)) {
        new XTENLANG_547_MustFailCompile().execute();
    }
    public static def main(args:Rail[String]) {}

    public def run()=true;
    public def breakit(b:GlobalRef[Rail[byte]], w:OutputStreamWriter) {
        w.write(b, 0, 0);
    }
}

