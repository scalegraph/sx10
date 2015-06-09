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
 * Using clocks to do simple producer consumer synchronization
 * for this task DAG (arrows point downward)
 * in pipelined fashion. On each clock period,
 * each stage of the pipeline reads the previous clock period's
 * result from the previous stage and produces its new result
 * for the current clock period.
 *
 * <code>
 *   A   stage 0 A produces the stream 1,2,3,...
 *  / \
 *  B  C  stage 1 B is "double", C is "square" function
 *  \ /|
 *   D E  stage 2 D is \(x,y)(x+y+10), E is \(x)(x*7)
 * </code>
 *
 * @author kemal 4/2005
 */
public class ClockTest10 extends x10Test {
    val varA = new Rail[long](2, 0);
    val varB = new Rail[long](2, 0);
    val varC = new Rail[long](2, 0);
    val varD = new Rail[long](2, 0);
    val varE = new Rail[long](2, 0);
    public static N: long = 10;
    public static pipeDepth: long = 2;

    static def ph(var x: long): long = { return x % 2; }

    public def run(): boolean = {
	finish async {
	    val a: Clock = Clock.make();
	    val b: Clock = Clock.make();
	    val c: Clock = Clock.make();
	    async clocked(a) taskA(a);
	    async clocked(a, b) taskB(a, b);
	    async clocked(a, c) taskC(a, c);
	    async clocked(b, c) taskD(b, c);
	    async clocked(c) taskE(c);
	}
	return true;
    }

    def taskA(val a: Clock): void = {
	for (k in 1..N) {
	    varA(ph(k)) = k;
	    x10.io.Console.OUT.println( " " + k + " A producing " + varA(ph(k)));
	    Clock.advanceAll();
	}
    }
    def taskB(val a: Clock, val b: Clock): void = {
	for (k in 1..N) {
	    val tmp = new boxedLong();
	    finish tmp.value = varA(ph(k-1))+varA(ph(k-1));
	    x10.io.Console.OUT.println(" " + k + " B consuming oldA producing " + tmp.value);
	    a.resume();
	    varB(ph(k)) = tmp.value;
	    x10.io.Console.OUT.println(" " + "B before next");
	    Clock.advanceAll();
	}
    }
    def taskC(val a: Clock, val c: Clock): void = {
	for (k in 1 ..N) {
	    val tmp  = new boxedLong();
	    finish tmp.value = varA(ph(k-1))*varA(ph(k-1));
	    x10.io.Console.OUT.println(" " + k + " C consuming oldA "+ tmp.value);
	    a.resume();
	    varC(ph(k)) = tmp.value;
	    x10.io.Console.OUT.println(" " + "C before next");
	    Clock.advanceAll();
	}
    }
    def taskD(val b: Clock, val c: Clock): void = {
	for (k in 1 ..N) {
	    val tmp  = new boxedLong();
	    finish tmp.value = varB(ph(k-1))+varC(ph(k-1))+10;
	    x10.io.Console.OUT.println(" " + k + " D consuming oldB+oldC producing " + tmp.value);
	    c.resume();
	    b.resume();
	    varD(ph(k)) = tmp.value;
	    x10.io.Console.OUT.println(" " + k + " D before next");
	    var n: long = k-pipeDepth;
	    chk(!(k>pipeDepth) || varD(ph(k)) == n+n+n*n+10);
	    Clock.advanceAll();
	}
    }
    def taskE(val c: Clock): void = {
	for (k in 1 ..N) {
	    val tmp  = new boxedLong();
	    finish tmp.value = varC(ph(k-1))*7;
	    x10.io.Console.OUT.println(" " + k + " E consuming oldC producing " + tmp.value);
	    c.resume();
	    varE(ph(k)) = tmp.value;
	    x10.io.Console.OUT.println(" " + k + " E before next");
	    var n: long = k-pipeDepth;
	    chk(!(k>pipeDepth) || varE(ph(k)) == n*n*7);
	    Clock.advanceAll();
	}
    }

    public static def main(var args: Rail[String]): void = {
	new ClockTest10().execute();
    }

    static class boxedLong {
	var value: long;
    }
}
