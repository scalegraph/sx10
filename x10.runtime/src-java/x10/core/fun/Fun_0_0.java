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
import x10.rtt.RuntimeType.Variance;

public interface Fun_0_0<U> extends Fun {
    U apply$G();
    
    public static final RuntimeType<Fun_0_0<?>> _RTT = new RuntimeType<Fun_0_0<?>>(
        Fun_0_0.class,
        Variance.COVARIANT
    ) {
        @Override
        public String typeName(Object o) {
            return typeNameForFun(o);
        }
    };
}
