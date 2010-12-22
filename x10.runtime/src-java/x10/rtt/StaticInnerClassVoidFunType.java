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

package x10.rtt;

// for static inner classes that are compiled from closures
public class StaticInnerClassVoidFunType<T> extends RuntimeType<T> {
    
    public StaticInnerClassVoidFunType(Class<?> c) {
        super(c);
    }
    
    public StaticInnerClassVoidFunType(Class<?> c, Variance[] variances) {
        super(c, variances);
    }

    public StaticInnerClassVoidFunType(Class<?> c, Type<?>[] parents) {
        super(c, parents);
    }
    
    public StaticInnerClassVoidFunType(Class<?> c, Variance[] variances, Type<?>[] parents) {
        super(c, variances, parents);
    }

    @Override
    public String typeName(Object o) {
        // Note: assume that the first parent in this RuntimeType is the parameterized type which corresponds to the function type
        assert o instanceof x10.core.fun.VoidFun;
        return ((x10.rtt.ParameterizedType<?>) getParents()[0]).typeNameForVoidFun();
    }

}
