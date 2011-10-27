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
public class StaticVoidFunType<T> extends RuntimeType<T> {
    
    private static final long serialVersionUID = 1L;

    // not used
//    protected StaticVoidFunType(Class<?> c) {
//        super(c);
//    }
//    
//    protected StaticVoidFunType(Class<?> c, Variance[] variances) {
//        super(c, variances);
//    }
//
//    protected StaticVoidFunType(Class<?> c, Type<?>[] parents) {
//        super(c, parents);
//    }
    
    protected StaticVoidFunType(Class<?> c, Variance[] variances, Type<?>[] parents) {
        super(c, variances, parents);
    }

    public static <T> StaticVoidFunType/*<T>*/ make(Class<?> c) {
        return new StaticVoidFunType<T>(c, null, null);
    }
    
    public static <T> StaticVoidFunType/*<T>*/ make(Class<?> c, Variance[] variances) {
        return new StaticVoidFunType<T>(c, variances, null);
    }

    public static <T> StaticVoidFunType/*<T>*/ make(Class<?> c, Type<?>[] parents) {
        return new StaticVoidFunType<T>(c, null, parents);
    }
    
    public static <T> StaticVoidFunType/*<T>*/ make(Class<?> c, Variance[] variances, Type<?>[] parents) {
        return new StaticVoidFunType<T>(c, variances, parents);
    }

    @Override
    public String typeName(Object o) {
        // Note: assume that the first parent in this RuntimeType is the parameterized type which corresponds to the function type
        assert o instanceof x10.core.fun.VoidFun;
        Type<?> parent = getParents()[0];
        String typeName;
        if (parent instanceof ParameterizedType<?>) {
        	typeName = ((ParameterizedType<?>) parent).typeNameForVoidFun(o);
        } else {
        	assert parent instanceof RuntimeType<?>;
        	typeName = ((RuntimeType<?>) parent).typeNameForVoidFun(o);
        }
        return typeName;
    }

}
