/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2012.
 */

package x10.io;

import x10.compiler.Native;
import x10.compiler.NativeRep;


@NativeRep("java", "x10.core.io.NotSerializableException", null, "x10.rtt.Types.NOT_SERIALIZABLE_EXCEPTION")
public class NotSerializableException23 extends IOException23 {

    @Native("java", "new x10.core.io.NotSerializableException()")
    public def this() { super(); }

    @Native("java", "new x10.core.io.NotSerializableException(#message)")
    public def this(message: String) { super(message); }

}
