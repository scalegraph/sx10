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

package x10.core.fun;

import x10.rtt.RuntimeType;
import x10.rtt.VoidFunType;

public interface VoidFun_0_0 extends VoidFun {
    void $apply();
    
    public static final RuntimeType<VoidFun_0_0> $RTT = VoidFunType.<VoidFun_0_0> make(
        VoidFun_0_0.class
    );
}
