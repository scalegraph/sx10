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


import x10.rtt.ParameterizedType;
import x10.rtt.RuntimeType;
import x10.rtt.RuntimeType.Variance;
import x10.rtt.Type;
import x10.rtt.Types;
import x10.rtt.UnresolvedType;
import x10.array.Array;

public final class GrowableRail<T> extends Ref implements x10.lang.Indexable<Integer,T>, x10.lang.Iterable<T>, x10.lang.Settable<Integer, T> {
    private Type<T> elementType;
    private Object array;
    private int length;

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < length; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(apply$G(i));
        }
        sb.append("]");
        return sb.toString();
    }

    public GrowableRail(Type<T> t) {
        this(t, 1);
    }

    public GrowableRail(Type<T> t, int size) {
        this.elementType = t;
        this.array = t.makeArray(size);
        this.length = 0;
    }

    public T set(T v, Type t1, Integer i, Type t2) {
        return set$G(v, (int)i);
    }

    public T set$G(T v, int i) {
        grow(i+1);
        /*
        assert i >= 0 : i + " < 0";
        assert i < length: i + " >= length==" + length;
        assert i < size(): i + " >= size()==" + size();
        */
        return elementType.setArray(array, i, v);
    }

    public void add(T v) {
        grow(length+1);
        elementType.setArray(array, length, v);
        length++;
    }

    public void insert(int loc, Rail<T> items) {
        int addLen = items.length;
        int movLen = length - loc;
        int newLen = length + addLen;
        grow(newLen);
        if (movLen > 0) {
            System.arraycopy(array, loc, array, loc + addLen, addLen);
        }
        System.arraycopy(items.value, 0, array, loc, addLen);
        length = addLen;
    }

    public void setLength(int newLength) {
        grow(newLength);
        length = newLength;
    }

    public void removeLast() {
        if (array instanceof Object[])
            set$G(null, length-1);
        length--;
        shrink(length+1);
    }

    public Rail<T> moveSectionToRail(int i, int j) {
        int len = j - i + 1;
        if (len < 1) return RailFactory.makeVarRail(elementType, 0);
        Object tmp = elementType.makeArray(len);
        System.arraycopy(array, i, tmp, 0, len);
        System.arraycopy(array, j+1, array, i, length-j-1);
        if (array instanceof Object[]) {
            java.util.Arrays.fill((Object[])array, length-len, length, null);
        }
        length-=len;
        shrink(length+1);
        return RailFactory.makeRailFromJavaArray(elementType, tmp);
    }

    public x10.lang.Iterator<T> iterator() {
        return new RailIterator();
    }

    protected class RailIterator implements x10.lang.Iterator<T> {
        int i = 0;

        public boolean hasNext() {
            return i < length;
        }

        public T next$G() {
            return apply$G(i++);
        }
    }

    private void grow(int newSize) {
        if (newSize <= size())
            return;
        newSize = java.lang.Math.max(newSize, size()*2);
        newSize = java.lang.Math.max(newSize, length);
        newSize = java.lang.Math.max(newSize, 8);
        Object tmp = elementType.makeArray(newSize);
        System.arraycopy(array, 0, tmp, 0, length);
        array = tmp;
    }

    private void shrink(int newSize) {
        if (newSize > size()/2 || newSize < 8)
            return;
        newSize = java.lang.Math.max(newSize, length);
        newSize = java.lang.Math.max(newSize, 8);
        Object tmp = elementType.makeArray(newSize);
        System.arraycopy(array, 0, tmp, 0, length);
        array = tmp;
    }

    public T apply(Object i, Type t) {
        return apply$G((int)(Integer)i);
    }
    public T apply$G(int i) {
        /*
        assert i >= 0 : i + " < 0";
        assert i < length: i + " >= length==" + length;
        assert i < size(): i + " >= size()==" + size();
        */
        return elementType.getArray(array, i);
    }

    public int length() {
        return length;
    }

    private int size() {
        return elementType.arrayLength(array);
    }

    public Rail<T> toRail() {
        Object tmp = elementType.makeArray(length);
        System.arraycopy(array, 0, tmp, 0, length);
        return RailFactory.makeRailFromJavaArray(elementType, tmp);
    }

    public Array<T> toArray() {
        Object tmp = elementType.makeArray(length);
        System.arraycopy(array, 0, tmp, 0, length);
        return RailFactory.makeArrayFromJavaArray(elementType, tmp);
    }

    //
    // Runtime type information
    //
    public static final RuntimeType<GrowableRail<?>> _RTT = new RuntimeType<GrowableRail<?>>(
        GrowableRail.class,
        new Variance[] {Variance.INVARIANT},
        new Type<?>[] {
            new ParameterizedType(x10.lang.Indexable._RTT, Types.INT, new UnresolvedType(0)),
            new ParameterizedType(x10.lang.Iterable._RTT, new UnresolvedType(0)),
            new ParameterizedType(x10.lang.Settable._RTT, Types.INT, new UnresolvedType(0))
        }
    ) {
        @Override
        public String typeName() {
            return "x10.lang.GrowableRail";
        }
    };
    public RuntimeType<GrowableRail<?>> getRTT() {return _RTT;}
    public Type<?> getParam(int i) {
        return i == 0 ? elementType : null;
    }
}
