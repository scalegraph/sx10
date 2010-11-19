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

package x10.lang;

import x10.compiler.Native;

public class Zero {
    @Native("java", "(#3==x10.rtt.Types.INT ? ((Integer)0) : #3==x10.rtt.Types.LONG ? ((Long)0l) : null)")
    @Native("c++",  "(0)")
	public static native def get[T]() {T haszero} :T;
}