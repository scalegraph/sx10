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

import x10.rtt.Type;
import x10.rtt.Types;
import x10.x10rt.X10JavaDeserializer;
import x10.x10rt.X10JavaSerializable;
import x10.x10rt.X10JavaSerializer;

import java.io.IOException;

/**
 * Represents a boxed UByte value. Boxed representation is used when casting
 * a UByte value into type Any or parameter type T.
 */
final public class UByte extends Number implements StructI, java.lang.Comparable<UByte>,
    x10.lang.Arithmetic<UByte>, x10.lang.Bitwise<UByte>, x10.util.Ordered<UByte>
{
    private static final long serialVersionUID = 1L;
    private static final short _serialization_id = x10.x10rt.DeserializationDispatcher.addDispatcher(x10.x10rt.DeserializationDispatcher.ClosureKind.CLOSURE_KIND_NOT_ASYNC, UByte.class);
    
    public static final x10.rtt.RuntimeType<?> $RTT = Types.UBYTE;
    public x10.rtt.RuntimeType<?> $getRTT() {return $RTT;}
    public x10.rtt.Type<?> $getParam(int i) {return null;}

    final byte $value;

    private UByte(byte value) {
        this.$value = value;
    }

    private abstract static class Cache {
        static final boolean enabled = java.lang.Boolean.parseBoolean(System.getProperty("x10.lang.UByte.Cache.enabled", "false"));
        static final int low = -128;
        static final int high = enabled ? 127 : low; // disable caching
        static final UByte cache[] = new UByte[high - low + 1];
        static {
            for (int i = 0; i < cache.length; ++i) {
                cache[i] = new UByte((byte)(low + i));
            }
        }
    }

    public static UByte $box(byte value) {
        if (Cache.enabled) {
            int valueAsInt = value;
            return Cache.cache[valueAsInt - Cache.low];  // fully cached
        }
        return new UByte(value);
    }
    
    public static UByte $box(int value) {   // int is required for literals
        return $box((byte) value);
    }

    public static byte $unbox(UByte o) {
        return o.$value;
    }
    
    public static byte $unbox(Object obj) {
        return ((UByte)obj).$value;
    }
    
    // make $box/$unbox idempotent
    public static UByte $box(UByte obj) {
        return obj;
    }

    public static byte $unbox(byte value) {
        return value;
    }
    
    public boolean _struct_equals$O(Object o) {
        if (o instanceof UByte && ((UByte)o).$value == $value)
            return true;
        return false;
    }
    
    @Override
    public boolean equals(Object o) {
        return _struct_equals$O(o);
    }
    
    @Override
    public int hashCode() {
        return (int)$value;
    }
    
    @Override
    public java.lang.String toString() {
        if ($value >= 0)
            return java.lang.Byte.toString($value);
        else
            return java.lang.Integer.toString((int)$value & 0xff);
    }
    
 	// implements Comparable<UByte>
    public int compareTo(UByte o) {
        int a = ((int)$value) & 0xFF;
        int b = ((int)o.$value) & 0xFF;
        if (a > b) return 1;
        else if (a < b) return -1;
        return 0;
    }

    public void $_serialize(X10JavaSerializer serializer) throws IOException {
        serializer.write($value);
    }

    public short $_get_serialization_id() {
        return _serialization_id;
    }

    public static X10JavaSerializable $_deserializer(X10JavaDeserializer deserializer) throws IOException {
        return $_deserialize_body(null, deserializer);
    }

    public static X10JavaSerializable $_deserialize_body(UByte ub, X10JavaDeserializer deserializer) throws IOException {
        byte value  = deserializer.readByte();
        ub = new UByte(value);
        deserializer.record_reference(ub);
        return ub;
    }
    
    // implements Arithmetic<UByte>
    public UByte $plus$G() { return this; }
    public UByte $minus$G() { return UByte.$box(-$value); }
    public UByte $plus(UByte a, Type t) { return UByte.$box($value + a.$value); }
    public UByte $minus(UByte a, Type t) { return UByte.$box($value - a.$value); }
    public UByte $times(UByte a, Type t) { return UByte.$box($value * a.$value); }
    public UByte $over(UByte a, Type t) { return UByte.$box((short)((0xff & $value) / (0xff & a.$value))); }
    
    // implements Bitwise<UByte>
    public UByte $tilde$G() { return UByte.$box(~$value); }
    public UByte $ampersand(UByte a, Type t) { return UByte.$box($value & a.$value); }
    public UByte $bar(UByte a, Type t) { return UByte.$box($value | a.$value); }
    public UByte $caret(UByte a, Type t) { return UByte.$box($value ^ a.$value); }
    public UByte $left$G(final int count) { return UByte.$box($value << count); }
    public UByte $right$G(final int count) { return UByte.$box((0xff & $value) >>> count); } // UByte is always unsigned
    public UByte $unsigned_right$G(final int count) { return UByte.$box((0xff & $value) >>> count); }
    
    // implements Ordered<UByte>. Rely on autoboxing of booleans
    public Object $lt(UByte a, Type t) { return Unsigned.lt($value,a.$value); }
    public Object $gt(UByte a, Type t) { return Unsigned.gt($value,a.$value); }
    public Object $le(UByte a, Type t) { return Unsigned.le($value,a.$value); }
    public Object $ge(UByte a, Type t) { return Unsigned.ge($value,a.$value); }
    
    // extends abstract class java.lang.Number
    @Override
    public int intValue() {
        return (int)(((int)$value)&0xff);
    }
    @Override
    public long longValue() {
        return (long)(((int)$value)&0xff);
    }
    @Override
    public float floatValue() {
        return (float)(((int)$value)&0xff);
    }
    @Override
    public double doubleValue() {
        return (double)(((int)$value)&0xff);
    }
}
