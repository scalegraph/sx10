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

package x10.io;

import x10.compiler.Native;

/**
 * Usage:
 *
 * try {
 *   val in = new File(inputFileName);
 *   val out = new File(outputFileName);
 *   val p = out.printer();
 *   for (line in in.lines()) {
 *      line = line.chop();
 *      p.println(line);
 *   }
 * }
 * catch (IOException e) { }
 */    
public class Printer extends FilterWriter {
    public def this(w: Writer) { super(w); }

    private static NEWLINE:Char = '\n'; // System.getProperty("line.separator");

    public def println(): void = print(NEWLINE);
    
    public final def println(o:Any): void {
        print(o==null? "null\n" : o.toString()+"\n");
    }
    public final def print(o:Any): void {
    	print(o==null? "null" : o.toString());
    }

    public def print(s:String): void {
        try {
            val b = s.bytes();
            write(b, 0, b.length);
        }
        catch (e: IOException) {
            throw new IORuntimeException(e.getMessage());
        }
    }

    public def printf(fmt: String): void { printfArray(fmt, new Array[Any][]); }
    public def printf(fmt: String, o1: Any): void { printfArray(fmt, new Array[Any][o1]); }
    public def printf(fmt: String, o1: Any, o2: Any): void { printfArray(fmt, new Array[Any][o1,o2]); }
    public def printf(fmt: String, o1: Any, o2: Any, o3: Any): void { printfArray(fmt, new Array[Any][o1,o2,o3]); }
    public def printf(fmt: String, o1: Any, o2: Any, o3: Any, o4: Any): void { 
        printfArray(fmt, [o1,o2,o3,o4]); 
    }
    public def printf(fmt: String, o1: Any, o2: Any, o3: Any, o4: Any, o5: Any): void { 
       printfArray(fmt, [o1,o2,o3,o4,o5]); 
    }
    public def printf(fmt: String, o1: Any, o2: Any, o3: Any, o4: Any, o5: Any, o6: Any): void { 
       printfArray(fmt, [o1,o2,o3,o4,o5,o6]); 
    }

    public def printf(fmt: String, args: Rail[Any]): void { 
        print(String.format(fmt, new Array[Any](args.length, (i:int)=>args(i))));
    }
    public def printfArray(fmt: String, args: Array[Any](1)): void { 
        print(String.format(fmt, args));
    }
        
    public def flush(): void {
        try {
            super.flush();
        }
        catch (IOException) { }
    }
    
    public def close(): void {
        try {
            super.close();
        }
        catch (IOException) { }
    }
    
}
