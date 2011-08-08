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
 * Represents a boxed Float value. Boxed representation is used when casting
 * an Float value to type Any, parameter type T or superinterfaces such
 * as Comparable<Float>.
 */
final public class Float extends Number implements StructI,
	java.lang.Comparable<Float>, x10.lang.Arithmetic<Float>, x10.util.Ordered<Float>
{
    private static final long serialVersionUID = 1L;
    private static final short _serialization_id = x10.x10rt.DeserializationDispatcher.addDispatcher(Float.class);
    
    public static final x10.rtt.RuntimeType<?> $RTT = Types.FLOAT;
    public x10.rtt.RuntimeType<?> $getRTT() {return $RTT;}
    public x10.rtt.Type<?> $getParam(int i) {return null;}

    final float $value;

    private Float(float value) {
        this.$value = value;
    }

    public static Float $box(float value) { // int because literals essentially have int type in Java
        return new Float(value);
    }

    public static float $unbox(Float obj) {
        return obj.$value;
    }
    
    public static float $unbox(Object obj) {
        if (obj instanceof Float) return ((Float)obj).$value;
        else return ((java.lang.Float)obj).floatValue();
    }
    
    // make $box/$unbox idempotent
    public static Float $box(Float obj) {
        return obj;
    }

    public static float $unbox(float value) {
        return value;
    }
    
    public boolean _struct_equals$O(Object obj) {
        if (obj instanceof Float && ((Float) obj).$value == $value)
            return true;
        return false;
    }
    
    @Override
    public boolean equals(Object value) {
        if (value instanceof Float) {
            return ((Float) value).$value == $value;
        } else if (value instanceof java.lang.Float) {
            return ((java.lang.Float) value).floatValue() == $value;
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return (int)$value;
    }

    @Override
    public java.lang.String toString() {
        return java.lang.Float.toString($value);
    }
    
    // implements Comparable<Float>
    public int compareTo(Float o) {
        if ($value > o.$value) return 1;
        else if ($value < o.$value) return -1;
        return 0;
    }
    
    // implements Arithmetic<Float>
    public Float $plus$G() { return this; }
    public Float $minus$G() { return Float.$box(-$value); }
    public Float $plus(Float b, Type t) { return Float.$box($value + b.$value); }
    public Float $minus(Float b, Type t) { return Float.$box($value - b.$value); }
    public Float $times(Float b, Type t) { return Float.$box($value * b.$value); }
    public Float $over(Float b, Type t) { return Float.$box($value / b.$value); }
    
    // implements Ordered<Float>. Rely on autoboxing of booleans
    public Object $lt(Float b, Type t) { return ($value < b.$value); }
    public Object $gt(Float b, Type t) { return ($value > b.$value); }
    public Object $le(Float b, Type t) { return ($value <= b.$value); }
    public Object $ge(Float b, Type t) { return ($value >= b.$value); }
    
    // extends abstract class java.lang.Number
    @Override
    public int intValue() {
        return (int)$value;
    }
    @Override
    public long longValue() {
        return (long)$value;
    }
    @Override
    public float floatValue() {
        return (float)$value;
    }
    @Override
    public double doubleValue() {
        return (double)$value;
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

    public static X10JavaSerializable $_deserialize_body(Float f, X10JavaDeserializer deserializer) throws IOException {
        float value  = deserializer.readFloat();
        f = new Float(value);
        deserializer.record_reference(f);
        return f;
    }
}