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

/**
 * Represents a boxed Short value. Boxed representation is used when casting
 * an Short value to type Any, parameter type T or superinterfaces such
 * as Comparable<Short>.
 */
final public class Short extends Number implements StructI, java.lang.Comparable<Short>,
    x10.lang.Arithmetic<Short>, x10.lang.Bitwise<Short>, x10.util.Ordered<Short>
{
    private static final long serialVersionUID = 1L;
    
    public static final x10.rtt.RuntimeType<?> $RTT = Types.SHORT;
    public x10.rtt.RuntimeType<?> $getRTT() {return $RTT;}
    public x10.rtt.Type<?> $getParam(int i) {return null;}

    final short $value;

    private Short(short value) {
        this.$value = value;
    }
    
    private abstract static class Cache {
        static final boolean enabled = java.lang.Boolean.parseBoolean(System.getProperty("x10.lang.Short.Cache.enabled", "false"));
        static final int low = -128;
        static final int high = enabled ? 127 : low; // disable caching
        static final Short cache[] = new Short[high - low + 1];
        static {
            for (int i = 0; i < cache.length; ++i) {
                cache[i] = new Short((short)(low + i));
            }
        }
    }

    public static Short $box(short value) {
        if (Cache.enabled) {
            int valueAsInt = value;
            if (Cache.low <= valueAsInt && valueAsInt <= Cache.high) {
                return Cache.cache[valueAsInt - Cache.low];
            }
        }
        return new Short(value);
    }

    public static Short $box(int value) { // int because literals essentially have int type in Java
        return $box((short)value);
    }

    public static short $unbox(Short obj) {
        return obj.$value;
    }
    
    public static short $unbox(Object obj) {
        if (obj instanceof Short) return ((Short)obj).$value;
        else return ((java.lang.Short)obj).shortValue();
    }
    
    // make $box/$unbox idempotent
    public static Short $box(Short obj) {
        return obj;
    }

    public static short $unbox(int value) {
        return (short)value;
    }
    
    public boolean _struct_equals$O(Object obj) {
        if (obj instanceof Short && ((Short) obj).$value == $value)
            return true;
        return false;
    }
    
    @Override
    public boolean equals(Object value) {
        if (value instanceof Short) {
            return ((Short) value).$value == $value;
        } else if (value instanceof java.lang.Short) { // integer literals come here as Short autoboxed values
            return ((java.lang.Short) value).shortValue() == $value;
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return $value;
    }

    @Override
    public java.lang.String toString() {
        return java.lang.Short.toString($value);
    }
    
    // implements Comparable<Short>
    public int compareTo(Short o) {
        if ($value > o.$value) return 1;
        else if ($value < o.$value) return -1;
        return 0;
    }
    
    // implements Arithmetic<Short>
    public Short $plus$G() { return this; }
    public Short $minus$G() { return Short.$box(-$value); }
    public Short $plus(Short b, Type t) { return Short.$box($value + b.$value); }
    public Short $minus(Short b, Type t) { return Short.$box($value - b.$value); }
    public Short $times(Short b, Type t) { return Short.$box($value * b.$value); }
    public Short $over(Short b, Type t) { return Short.$box($value / b.$value); }
    
    // implements Bitwise<Short>
    public Short $tilde$G() { return Short.$box(~$value); }
    public Short $ampersand(Short b, Type t) { return Short.$box($value & b.$value); }
    public Short $bar(Short b, Type t) { return Short.$box($value | b.$value); }
    public Short $caret(Short b, Type t) { return Short.$box($value ^ b.$value); }
    public Short $left$G(final int count) { return Short.$box($value << count); }
    public Short $right$G(final int count) { return Short.$box($value >> count); }
    public Short $unsigned_right$G(final int count) { return Short.$box($value >>> count); }        
    
    // implements Ordered<Short>. Rely on autoboxing of booleans
    public Object $lt(Short b, Type t) { return ($value < b.$value); }
    public Object $gt(Short b, Type t) { return ($value > b.$value); }
    public Object $le(Short b, Type t) { return ($value <= b.$value); }
    public Object $ge(Short b, Type t) { return ($value >= b.$value); }
    
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
}
