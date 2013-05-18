/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2011.
 */

package x10.core;

import java.io.IOException;

import x10.rtt.NamedType;
import x10.rtt.ParameterizedType;
import x10.rtt.RuntimeType;
import x10.rtt.Type;
import x10.rtt.Types;
import x10.serialization.X10JavaDeserializer;
import x10.serialization.X10JavaSerializable;
import x10.serialization.X10JavaSerializer;

public final class Vec<T> extends x10.core.Struct {

    private static final long serialVersionUID = 1L;

    public int size;
    public Rail<T> backing;

    @Override
    public Vec<T> clone() {
        return new Vec<T>(T, this);
    }

    private Type<T> T;
    public static final RuntimeType<Vec<?>> $RTT = NamedType.<Vec<?>> make(
        "x10.util.Vec",
        Vec.class,
        RuntimeType.INVARIANTS(1),
        new Type[] { Types.STRUCT }
    );
    public RuntimeType<Vec<?>> $getRTT() { return $RTT; }
    public Type<?> $getParam(int i) { if (i == 0) return T; return null; }

    private void writeObject(java.io.ObjectOutputStream oos) throws java.io.IOException {
        if (x10.runtime.impl.java.Runtime.TRACE_SER) {
            java.lang.System.out.println("Serializer: writeObject(ObjectOutputStream) of " + this + " calling");
        }
        oos.defaultWriteObject();
    }
    
    // constructor just for allocation
    public Vec(java.lang.System[] $dummy) {
        // call default constructor instead of "constructor just for allocation" for x10.core.Struct
//        super($dummy);
    }

    public final Vec<T> x10$util$Vec$$init$S(Type<T> T, int s) {
        this.T = T;
        this.size = s;
        this.backing = new Rail<T>(T, size);
        return this;
    }

    public Vec(Type<T> T, int s) {
        this.T = T;
        this.size = s;
        this.backing = new Rail<T>(T, size);
    }

    public final Vec<T> x10$util$Vec$$init$S(Type<T> T, Vec<T> other) {
        this.T = T;
        this.size = other.size;
        this.backing = new Rail<T>(T, other.backing, (Rail.__0$1x10$lang$Rail$$T$2) null);
        return this;
    }

    public Vec(Type<T> T, Vec<T> other) {
        this.T = T;
        this.size = other.size;
        this.backing = new Rail<T>(T, other.backing, (Rail.__0$1x10$lang$Rail$$T$2) null);
    }

    // zero value constructor
    public Vec(Type<T> T, java.lang.System $dummy) {
        this.T = T;
        this.backing = null;
        this.size = 0;
    }

    final public static <U> Vec<U> make(Type U, int s) {
        return new Vec<U>(U, s);
    }

    final public T get(int i) {
        return backing.$apply$G(i);
    }

    final public T set(int i, T v) {
        return backing.$set__1x10$lang$Rail$$T$G(i, v);
    }

    final public int size() {
        return this.size;
    }

    @Override
    final public java.lang.String toString() {
        return "struct x10.util.Vec: size=" + size;
    }

    @Override
    final public int hashCode() {
        int result = 1;
        result = 8191 * result + ((java.lang.Object) this.size).hashCode();
        result = 8191 * result + this.backing.hashCode();
        return result;
    }

    // not needed
//    @Override
//    final public boolean equals(java.lang.Object other) {
//        if (!Vec.$RTT.isInstance(other, T)) return false;
//        return this.equals_0$1x10$util$Vec$$T$2((Vec) Types.asStruct(ParameterizedType.make(Vec.$RTT, T), other));
//    }
//
//    final public boolean equals_0$1x10$util$Vec$$T$2(Vec other) {
//        if (this.size != other.size) return false;
//        for (int i = 0; i < this.size; ++i) {
//            if (!this.backing.$apply$G(i).equals(other.backing.$apply$G(i))) return false;
//        }
//        return true;
//    }

    final public boolean _struct_equals$O(java.lang.Object otherObj) {
        if (!Vec.$RTT.isInstance(otherObj, T)) return false;
        Vec<T> other = (Vec<T>) Types.asStruct(ParameterizedType.make(Vec.$RTT, T), otherObj);
        // optimized
//      if (this.size != other.size) return false;
//      for (int i = 0; i < this.size; ++i) {
//          if (!this.backing.$apply$G(i).equals(other.backing.$apply$G(i))) return false;
//      }
//      return true;
        Object thisValue = this.backing.value;
        Object otherValue = other.backing.value;
        if (T == Types.BYTE || T == Types.UBYTE) {
            return java.util.Arrays.equals((byte[]) thisValue, (byte[]) otherValue);
        }
        if (T == Types.SHORT || T == Types.USHORT) {
            return java.util.Arrays.equals((short[]) thisValue, (short[]) otherValue);
        }
        if (T == Types.INT || T == Types.UINT) {
            return java.util.Arrays.equals((int[]) thisValue, (int[]) otherValue);
        }
        if (T == Types.LONG || T == Types.ULONG) {
            return java.util.Arrays.equals((long[]) thisValue, (long[]) otherValue);
        }
        if (T == Types.FLOAT) {
            return java.util.Arrays.equals((float[]) thisValue, (float[]) otherValue);
        }
        if (T == Types.DOUBLE) {
            return java.util.Arrays.equals((double[]) thisValue, (double[]) otherValue);
        }
        if (T == Types.CHAR) {
            return java.util.Arrays.equals((char[]) thisValue, (char[]) otherValue);
        }
        if (T == Types.BOOLEAN) {
            return java.util.Arrays.equals((boolean[]) thisValue, (boolean[]) otherValue);
        }
        return java.util.Arrays.equals((Object[]) thisValue, (Object[]) otherValue);
    }

    // not needed
//    final public boolean _struct_equals_0$1x10$util$Vec$$T$2(Vec other) {
//        if (this.size != other.size) return false;
//        for (int i = 0; i < this.size; ++i) {
//            if (!this.backing.$apply$G(i).equals(other.backing.$apply$G(i))) return false;
//        }
//        return true;
//    }

    final public Vec<T> x10$util$Vec$$x10$util$Vec$this() {
        return this;
    }

    public void $_serialize(X10JavaSerializer $serializer) throws IOException {
        $serializer.write(T);
        $serializer.write(size);
        $serializer.write(backing);
    }

    public static X10JavaSerializable $_deserializer(X10JavaDeserializer $deserializer) throws IOException {
        Vec $_obj = new Vec(null);
        $deserializer.record_reference($_obj);
        return $_deserialize_body($_obj, $deserializer);
    }

    public static X10JavaSerializable $_deserialize_body(Vec $_obj, X10JavaDeserializer $deserializer) throws IOException {
        $_obj.T = $deserializer.readRef();
        $_obj.size = $deserializer.readInt();
        $_obj.backing = $deserializer.readRef();
        return $_obj;
    }
}
