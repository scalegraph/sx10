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

package x10.matrix;

import x10.io.Console;
import x10.io.File;
import x10.io.Printer;
import x10.util.Timer;
import x10.util.StringBuilder;

/**
 * For debugging purpose, this class provides methods to serialize the print out
 * from different places based on time stamps.
 */
public class PConsole {
	//
	public var disable:Boolean = false;
	//
	val bufsize:Int     = 128;
	var baseline:Int    = 0;
	var lastline:Int    = 0;
	var overflow:Boolean=false;
	//
	val startTime:Long =  Timer.milliTime();
	//
	val buffer:Array[String](1) = new Array[String](bufsize, "");
	//
	var bufln:StringBuilder = new StringBuilder();
	//public var outstr:String = "";
	//
	//
	//==============================================

	//==============================================
	def addline() {
		bufln.add(getStamp());
	}
	//
	def reset() {
		bufln = new StringBuilder();

	}
	//
	def getStamp():String {
		val pt:Long = Timer.milliTime() - startTime;
		return "<P"+here.id().toString()+" "+pt+"ms>:";
	}
	//============================================
	public def toString(d:Array[Double](1), st:Int, cnt:Int, inc:Int):String {
		var output:StringBuilder = new StringBuilder(); 
		output.add("[ ");
		for (var i:Int=st, c:Int=0; i<d.size && c<cnt; i+=inc, c++)
			output.add(d(i).toString() + " ");
		output.add("]");
		return output.toString(); 
	}
	//
	public def toString(d:Array[Int](1), st:Int, cnt:Int, inc:Int):String {
		var output:StringBuilder = new StringBuilder();
		output.add("[");
		for (var i:Int=st, c:Int=0; i<d.size && c<cnt; i+=inc, c++)
			output.add( d(i).toString() + " ");
		output.add("]");
		return output.toString(); 
	}
	//==============================================
	public def getOutStream(resetflag:Boolean):String {
		return bufln.toString();
	}
	//
	public def getOutStream() = getOutStream(false);
	//==============================================
	public def flush() {
		if (! disable) {		
			Console.OUT.println(getOutStream());
			Console.OUT.flush();
			reset();
		}
	}
	//
	public def flush(st:String) {
		if (! disable) {
			println(st);
			flush();
		}
	}
	//-----------
	public def flushln(str:String)  {
		if (! disable) {
			val outln:String = getStamp()+str; 
			Console.OUT.println(outln);
			Console.OUT.flush();
		}
	}
	//------------
	public def fflush() {
		if (!disable) {
			//
			val outFileName = "DebugOutPlace"+here.id+".log";
			val out = new File(outFileName);
			val prt = out.printer();
			//
			var outln:String= getOutStream();
			
			if (outln.length() > 0) {
				prt.println(outln);
				prt.flush();
			}
			//
			reset();
			prt.close();
		}
	}	
	//==========================================================
	public def print(str:String) {
		if (!disable) {
			bufln.add(str);
		}
		//buffer(lastline) +=  st;
	}
	//--------------
	public def print(d:Array[Int](1))    { print(this.toString(d, 0, d.size, 1));}
	public def print(d:Array[Double](1)) { print(this.toString(d, 0, d.size, 1));}
	//==========================================================
	public def println(str:String) {
		if (!disable) {
			bufln.add("\n");
			addline();
			bufln.add(str);
			bufln.add("\n");
			//buffer(lastline) += st;
		}
	}
	//---------------------
	public def println(d:Array[Int](1))    { println(this.toString(d, 0, d.size, 1));}
	public def println(d:Array[Double](1)) { println(this.toString(d, 0, d.size, 1));}
   	//
	//----------------------------------


}