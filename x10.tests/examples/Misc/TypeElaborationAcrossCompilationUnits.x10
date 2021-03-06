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
Test that methods whose return types are deptypes are handled correctly when
they are defined in another class, in a different source unit from the class
which references them. This program should not give a compile-time error -- the
compiler should be able to establish that the arguments for - are regions of
rank==3.
@author vj
**/
public class TypeElaborationAcrossCompilationUnits extends x10Test {

	
	public def run(): boolean = {
		var t: Temp = new Temp();
		var b: Region{rank==3} = t.m((1..10)*(1..10)*(1..10));
		return true;
	}
	
	public static def main(var args: Array[String](1)): void = {
		new TypeElaborationAcrossCompilationUnits().execute();
	}
}
