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

package x10.core;

import x10.core.fun.Fun_0_1;
import x10.rtt.RuntimeType;
import x10.rtt.Type;
import x10.rtt.Types;

public abstract class RailFactory {
    public static <T> Rail<T> makeVarRail(Type type, int length) {
        Object value = type.makeArray(length);
        Rail<T> array = new Rail<T>(type, length, value);
        return array;
    }

    public static <T> Rail<T> makeVarRail(Type type, int length, T init) {
        Object o = type.makeArray(length);
        Rail.resetLocal(o, init);
        return new Rail<T>(type, length, o);
    }

    public static <T> Rail<T> makeVarRail(Type type, int length, Fun_0_1<Integer,T> init) {
        Object o = type.makeArray(length);
        initJavaArray(o, init);
        Rail<T> array = new Rail<T>(type, length, o);
        return array;
    }

    public static <T> Rail<T> makeVarRail(Type type, int length, int offset, Rail<T> init) {
        Object newArray = type.makeArray(length);
        System.arraycopy(init.getBackingArray(), offset, newArray, 0, length);
        return new Rail<T>(type, length, newArray);
    }

//    public static <T> Rail<T> makeRailFromJavaArray(Object array) {
//        if (array instanceof int[]) {
//            return new Rail<T>((Type) Types.INT, ((int[]) array).length, array);
//        }
//        if (array instanceof long[]) {
//            return new Rail<T>((Type) Types.LONG, ((long[]) array).length, array);
//        }
//        if (array instanceof float[]) {
//            return new Rail<T>((Type) Types.FLOAT, ((float[]) array).length, array);
//        }
//        if (array instanceof double[]) {
//            return new Rail<T>((Type) Types.DOUBLE, ((double[]) array).length, array);
//        }
//        if (array instanceof byte[]) {
//            return new Rail<T>((Type) Types.BYTE, ((byte[]) array).length, array);
//        }
//        if (array instanceof short[]) {
//            return new Rail<T>((Type) Types.SHORT, ((short[]) array).length, array);
//        }
//        if (array instanceof char[]) {
//            return new Rail<T>((Type) Types.CHAR, ((char[]) array).length, array);
//        }
//        if (array instanceof boolean[]) {
//            return new Rail<T>((Type) Types.BOOLEAN, ((boolean[]) array).length, array);
//        }
//        if (array instanceof String[]) {
//            return new Rail<T>(new RuntimeType(String.class), ((String[]) array).length, array);
//        }
//        // cannot get correct RTT info. from array
//        return new Rail<T>(new RuntimeType(array.getClass().getComponentType()), ((Object[]) array).length, array);
//    }

    public static <T> x10.array.Array<T> makeArrayFromJavaArray(Type type, Object array) {
        int len = type.arrayLength(array);
        x10.array.Array<T> arr = new x10.array.Array<T>(type, len);
        System.arraycopy(array, 0, arr.raw.value, 0, len);
        return arr;
    }

    public static <T> Rail<T> makeRailFromJavaArray(Type type, Object array) {
        return new Rail<T>(type, type.arrayLength(array) , array);
    }

    private static <T> void initJavaArray(Object value, Fun_0_1<Integer,T> init) {
        if (value instanceof int[]) {
            int[] typed_value = (int[]) value;
            for (int i = 0; i < typed_value.length; i++) {
                typed_value[i] = (Integer) init.apply(i, Types.INT);
            }
        } else if (value instanceof long[]) {
            long[] typed_value = (long[]) value;
            for (int i = 0; i < typed_value.length; i++) {
                typed_value[i] = (Long) init.apply(i, Types.INT);
            }
        } else if (value instanceof float[]) {
            float[] typed_value = (float[]) value;
            for (int i = 0; i < typed_value.length; i++) {
                typed_value[i] = (Float) init.apply(i, Types.INT);
            }
        } else if (value instanceof double[]) {
            double[] typed_value = (double[]) value;
            for (int i = 0; i < typed_value.length; i++) {
                typed_value[i] = (Double) init.apply(i, Types.INT);
            }
        } else if (value instanceof byte[]) {
            byte[] typed_value = (byte[]) value;
            for (int i = 0; i < typed_value.length; i++) {
                typed_value[i] = (Byte) init.apply(i, Types.INT);
            }
        } else if (value instanceof short[]) {
            short[] typed_value = (short[]) value;
            for (int i = 0; i < typed_value.length; i++) {
                typed_value[i] = (Short) init.apply(i, Types.INT);
            }
        } else if (value instanceof char[]) {
            char[] typed_value = (char[]) value;
            for (int i = 0; i < typed_value.length; i++) {
                typed_value[i] = (Character) init.apply(i, Types.INT);
            }
        } else if (value instanceof boolean[]) {
            boolean[] typed_value = (boolean[]) value;
            for (int i = 0; i < typed_value.length; i++) {
                typed_value[i] = (Boolean) init.apply(i, Types.INT);
            }
        } else {
            Object[] typed_value = (Object[]) value;
            for (int i = 0; i < typed_value.length; i++) {
                typed_value[i] = init.apply(i, Types.INT);
            }
        }
    }
}
