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

/**
 * This debug tool provides self defined assertion and print methods.
 * 
 * In a multi-place execution, print-out of the same place will be display together, after
 * calling flush() method.
 *
 */
public class Debug {

	//========================
	private static val instance:Debug = new Debug();
	//========================
	//public static val console:PConsole = new PConsole();
	public static val console = new DistPConsole();
	//
	//===========================
	public static final def get():Debug {
		return instance;
	}
	//
	public static val disable:Boolean            = false;

	//=========================================================
	public static def assure(v:Boolean) {  
		assure(v, "");
	}
	//
	public static def assure(v:Boolean, msg:String) {
		if (!v) {
			val fmsg = "Assertion fail at P" + here.id()+" - "+msg;
			console.flush();
			throw new UnsupportedOperationException(fmsg);
		}
	}
	// Alternative for exit
	public static def exit(msg:String)  {
		console.flush();
		throw new UnsupportedOperationException(msg);
	}
	//
	public static def exit()  {
 		console.flush();  //Flush to stdout
		throw new UnsupportedOperationException();
	}


	//-------------------------------------------------------------------
	//
	// Append input string at the tail of the last line
	public static def print(s:String)            { console.print(s);}
	public static def print(d:Array[Int](1))     { console.print(d);}
 	public static def print(d:Array[Double](1))  { console.print(d);}
	//
	// Use a new line in buffer, and then append the input string with time stamp
	// at the front
	public static def println(s:String)           { console.println(s); }
 	public static def println(d:Array[Int](1))    { console.println(d); }
	public static def println(d:Array[Double](1)) { console.println(d); }
	//-------------------------------
	public static def println(s:String, d:Array[Int](1)) {
		Debug.println(s); 
		Debug.print(d);
	}
	public static def println(s:String, d:Array[Double](1)) {
		Debug.println(s); 
		Debug.print(d);
		
	}

	// Flush the buffer
	public static def flush()          { console.flush();}    // Print all buffered to stdout 
	public static def fflush()         { console.fflush();}   // Print all buffered to file
	public static def flush(s:String)  { console.flush(s);}   // Print all buffered and s to stdout
	public static def flushln(s:String){ console.flushln(s);} // Print just s to stdout
	//
	public static def flushall()       { console.flushall();}
	//-------------------------------------------------------------------


}
