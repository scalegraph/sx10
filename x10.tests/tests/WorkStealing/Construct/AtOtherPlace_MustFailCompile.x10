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

/*
 * At other place. Cannot pass WS compile.
 */
public class AtOtherPlace_MustFailCompile {
    
	static class T {
		private val root = GlobalRef[T](this);
		var val_:Object;
	}
    
    public def run(): boolean = {
        val Other  = here.next();
        val t = (new T()).root;
        at (Other) {
            val t1 = new T();
            at (t) t().val_ = t1;
        }
        val result = (t().val_ as T).root.home == Other;
        Console.OUT.println("AtOtherPlace: result = " + result);
        return result;
    }

    public static def main(Array[String](1)) {
        val r = new AtOtherPlace_MustFailCompile().run();
        if(r){
             x10.io.Console.OUT.println("++++++Test succeeded.");
        }
    }
    
    
}