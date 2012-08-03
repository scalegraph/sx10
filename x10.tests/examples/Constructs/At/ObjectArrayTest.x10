/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2011.
 */

import harness.x10Test;

/**
 * Unit test for serialization of Arrays of classes
 * with a mix of unique and repeated objects.
 * 
 * Testing at both unique class (Blat) and at String/Empty 
 * to catch interactions with Java serialization and Java interop.
 *
 * Also serves as a micro-benchmark for repeated reference detection.
 */
public class ObjectArrayTest  extends x10Test {
  val n:int;

  static class Blat { }

  def this(n:int) {
    this.n = n;
  }

  public def run() {
    val ub = new Blat();
    val a1 = new Array[Blat](n, (i:int)=>(i%2 == 0) ? ub : new Blat());
    val a2 = new Array[Blat](n, ub);

    var start:long = System.nanoTime();
    at (here.next()) {
        for (var i:int = 0; i<n-2; i += 2) {
          chk(a1(i) == a1(i+2));
          chk(a1(i) != a1(i+1));
        }
    }
    var end:long = System.nanoTime();
    Console.OUT.println("Mixed Blat time "+((end-start) as double/1e6)+" ms");

    start = System.nanoTime();
    at (here.next()) {
        for (var i:int = 0; i<n-2; i += 2) {
          chk(a2(i) == a2(i+2));
          chk(a2(i) == a2(i+1));
        }
    }
    end = System.nanoTime();
    Console.OUT.println("Unique Blat time "+((end-start) as double/1e6)+" ms");


    val uo = new Empty();
    val a3 = new Array[Empty](n, (i:int)=>(i%2 == 0) ? uo : new Empty());
    val a4 = new Array[Empty](n, uo);

    start = System.nanoTime();
    at (here.next()) {
        for (var i:int = 0; i<n-2; i += 2) {
          chk(a3(i) == a3(i+2));
          chk(a3(i) != a3(i+1));
        }
    }
    end = System.nanoTime();
    Console.OUT.println("Mixed Object time "+((end-start) as double/1e6)+" ms");

    start = System.nanoTime();
    at (here.next()) {
        for (var i:int = 0; i<n-2; i += 2) {
          chk(a4(i) == a4(i+2));
          chk(a4(i) == a4(i+1));
        }
    }
    end = System.nanoTime();
    Console.OUT.println("Unique Object time "+((end-start) as double/1e6)+" ms");


    val us = "hello";
    val a5 = new Array[String](n, (i:int)=>(i%2 == 0) ? us : i.toString());
    val a6 = new Array[String](n, us);

    start = System.nanoTime();
    at (here.next()) {
        for (var i:int = 0; i<n-2; i += 2) {
          chk(a5(i) == a5(i+2));
          chk(a5(i) != a5(i+1));
        }
    }
    end = System.nanoTime();
    Console.OUT.println("Mixed String time "+((end-start) as double/1e6)+" ms");

    start = System.nanoTime();
    at (here.next()) {
        for (var i:int = 0; i<n-2; i += 2) {
          chk(a6(i) == a6(i+2));
          chk(a6(i) == a6(i+1));
        }
    }
    end = System.nanoTime();
    Console.OUT.println("Unique String time "+((end-start) as double/1e6)+" ms");

    return true;
  }

  public static def main(args:Array[String](1)) {
    val n:int;
    if (args.size > 0) {
        n = Int.parseInt(args(0));
    } else {
        n = 100;
    }
    new ObjectArrayTest(n).execute();
  }

}