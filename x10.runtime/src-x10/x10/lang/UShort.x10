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

package x10.lang;

import x10.compiler.Native;
import x10.compiler.NativeRep;
import x10.util.Ordered;

/**
 * UShort is a 16-bit unsigned integral data type, with
 * values ranging from 0 to 65535, inclusive.  All of the normal
 * arithmetic and bitwise operations are defined on UShort, and UShort
 * is closed under those operations.  There are also static methods
 * that define conversions from other data types, including String,
 * as well as some UShort constants.
 */
// @NativeRep("java", "short", null, "x10.rtt.Types.USHORT")
@NativeRep("c++", "x10_ushort", "x10_ushort", null)
public struct UShort implements Comparable[UShort] /*TODO implements Arithmetic[UShort], Bitwise[UShort], Ordered[UShort]*/ {

    /** The actual number with Short representation */
    public val shortVal:Short;
    public def this(value:Short) {
        this.shortVal = value;
    }

    /**
     * A less-than operator.
     * Compares this UShort with another UShort and returns true if this UShort is
     * strictly less than the other UShort.
     * @param x the other UShort
     * @return true if this UShort is strictly less than the other UShort.
     */
    // @Native("java", "x10.core.Unsigned.lt(#this, #x)")
    @Native("c++",  "((#0) < (#1))")
    public operator this < (x:UShort): Boolean {
         return (shortVal + Short.MIN_VALUE) < (x.shortVal + Short.MIN_VALUE);
     }

    /**
     * A greater-than operator.
     * Compares this UShort with another UShort and returns true if this UShort is
     * strictly greater than the other UShort.
     * @param x the other UShort
     * @return true if this UShort is strictly greater than the other UShort.
     */
    // @Native("java", "x10.core.Unsigned.gt(#this, #x)")
    @Native("c++",  "((#0) > (#1))")
    public operator this > (x:UShort): Boolean {
        return (shortVal + Short.MIN_VALUE) > (x.shortVal + Short.MIN_VALUE);
    }

    /**
     * A less-than-or-equal-to operator.
     * Compares this UShort with another UShort and returns true if this UShort is
     * less than or equal to the other UShort.
     * @param x the other UShort
     * @return true if this UShort is less than or equal to the other UShort.
     */
    // @Native("java", "x10.core.Unsigned.le(#this, #x)")
    @Native("c++",  "((#0) <= (#1))")
    public operator this <= (x:UShort): Boolean {
         return (shortVal + Short.MIN_VALUE) <= (x.shortVal + Short.MIN_VALUE);
     }

    /**
     * A greater-than-or-equal-to operator.
     * Compares this UShort with another UShort and returns true if this UShort is
     * greater than or equal to the other UShort.
     * @param x the other UShort
     * @return true if this UShort is greater than or equal to the other UShort.
     */
    // @Native("java", "x10.core.Unsigned.ge(#this, #x)")
    @Native("c++",  "((#0) >= (#1))")
    public operator this >= (x:UShort): Boolean {
        return (shortVal + Short.MIN_VALUE) >= (x.shortVal + Short.MIN_VALUE);
    }


    /**
     * A binary plus operator.
     * Computes the result of the addition of the two operands.
     * Overflows result in truncating the high bits.
     * @param x the other UShort
     * @return the sum of this UShort and the other UShort.
     */
    // @Native("java", "((short) ((#this) + (#x)))")
    @Native("c++",  "((x10_ushort) ((#0) + (#1)))")
    public operator this + (x:UShort): UShort = UShort(shortVal + x.shortVal);

    /**
     * A binary minus operator.
     * Computes the result of the subtraction of the two operands.
     * Overflows result in truncating the high bits.
     * @param x the other UShort
     * @return the difference of this UShort and the other UShort.
     */
    // @Native("java", "((short) ((#this) - (#x)))")
    @Native("c++",  "((x10_ushort) ((#0) - (#1)))")
    public operator this - (x:UShort): UShort = UShort(shortVal - x.shortVal);

    /**
     * A binary multiply operator.
     * Computes the result of the multiplication of the two operands.
     * Overflows result in truncating the high bits.
     * @param x the other UShort
     * @return the product of this UShort and the other UShort.
     */
    // @Native("java", "((short) ((#this) * (#x)))")
    @Native("c++",  "((x10_ushort) ((#0) * (#1)))")
    public operator this * (x:UShort): UShort = UShort(shortVal * x.shortVal);

    /**
     * A binary divide operator.
     * Computes the result of the division of the two operands.
     * @param x the other UShort
     * @return the quotient of this UShort and the other UShort.
     */
    // @Native("java", "((short) x10.core.Unsigned.div(#this, #x))")
    @Native("c++",  "((x10_ushort) ((#0) / x10aux::zeroCheck(#1)))")
    public operator this / (x:UShort): UShort {
        return UShort(((shortVal as Long) / (x.shortVal as Long)) as Short);
    }

    /**
     * A binary remainder operator.
     * Computes a remainder from the division of the two operands.
     * @param x the other UShort
     * @return the remainder from dividing this UShort by the other UShort.
     */
    // @Native("java", "((short) x10.core.Unsigned.rem(#this, #x))")
    @Native("c++",  "((x10_ushort) ((#0) % x10aux::zeroCheck(#1)))")
    public operator this % (x:UShort): UShort {
        return UShort(((shortVal as Long) % (x.shortVal as Long)) as Short);
    }

    /**
     * A unary plus operator.
     * A no-op.
     * @return the value of this UShort.
     */
    // @Native("java", "((short) +(#this))")
    @Native("c++",  "((x10_ushort) +(#0))")
    public operator + this: UShort = this;

    /**
     * A unary minus operator.
     * Computes the two's complement of the operand.
     * Overflows result in truncating the high bits.
     * @return the two's complement of this UShort.
     */
    // @Native("java", "((short) -(#this))")
    @Native("c++",  "((x10_ushort) -(#0))")
    public operator - this: UShort = UShort(-(shortVal));


    /**
     * A bitwise and operator.
     * Computes a bitwise AND of the two operands.
     * @param x the other UShort
     * @return the bitwise AND of this UShort and the other UShort.
     */
    // @Native("java", "((short) ((#this) & (#x)))")
    @Native("c++",  "((x10_ushort) ((#0) & (#1)))")
    public operator this & (x:UShort): UShort = UShort(shortVal & x.shortVal);
    /**
     * A bitwise and operator (unsigned disambiguation).
     * @see #operator(UShort)&(UShort)
     */
    // @Native("java", "((short) ((#this) & (#x)))")
    @Native("c++",  "((x10_ushort) ((#0) & (#1)))")
    public operator (x:Short) & this: UShort = UShort(x & shortVal);
    /**
     * A bitwise and operator (unsigned disambiguation).
     * @see #operator(UShort)&(UShort)
     */
    // @Native("java", "((short) ((#this) & (#x)))")
    @Native("c++",  "((x10_ushort) ((#0) & (#1)))")
    public operator this & (x:Short): UShort = UShort(shortVal & x);

    /**
     * A bitwise or operator.
     * Computes a bitwise OR of the two operands.
     * @param x the other UShort
     * @return the bitwise OR of this UShort and the other UShort.
     */
    // @Native("java", "((short) ((#this) | (#x)))")
    @Native("c++",  "((x10_ushort) ((#0) | (#1)))")
    public operator this | (x:UShort): UShort = UShort(shortVal | x.shortVal);
    /**
     * A bitwise or operator (unsigned disambiguation).
     * @see #operator(UShort)|(UShort)
     */
    // @Native("java", "((short) ((#this) | (#x)))")
    @Native("c++",  "((x10_ushort) ((#0) | (#1)))")
    public operator (x:Short) | this: UShort = UShort(x | shortVal);
    /**
     * A bitwise or operator (unsigned disambiguation).
     * @see #operator(UShort)|(UShort)
     */
    // @Native("java", "((short) ((#this) | (#x)))")
    @Native("c++",  "((x10_ushort) ((#0) | (#1)))")
    public operator this | (x:Short): UShort = UShort(shortVal | x);

    /**
     * A bitwise xor operator.
     * Computes a bitwise XOR of the two operands.
     * @param x the other UShort
     * @return the bitwise XOR of this UShort and the other UShort.
     */
    // @Native("java", "((short) ((#this) ^ (#x)))")
    @Native("c++",  "((x10_ushort) ((#0) ^ (#1)))")
    public operator this ^ (x:UShort): UShort = UShort(shortVal ^ x.shortVal);
    /**
     * A bitwise xor operator (unsigned disambiguation).
     * @see #operator(UShort)^(UShort)
     */
    // @Native("java", "((short) ((#this) ^ (#x)))")
    @Native("c++",  "((x10_ushort) ((#0) ^ (#1)))")
    public operator (x:Short) ^ this: UShort = UShort(x ^ shortVal);
    /**
     * A bitwise xor operator (unsigned disambiguation).
     * @see #operator(UShort)^(UShort)
     */
    // @Native("java", "((short) ((#this) ^ (#x)))")
    @Native("c++",  "((x10_ushort) ((#0) ^ (#1)))")
    public operator this ^ (x:Short): UShort = UShort(shortVal ^ x);

    /**
     * A bitwise left shift operator.
     * Computes the value of the left-hand operand shifted left by the value of the right-hand operand.
     * If the right-hand operand is negative, the results are undefined.
     * @param count the shift count
     * @return this UShort shifted left by count.
     */
    // @Native("java", "((short) ((#this) << (#count)))")
    @Native("c++",  "((x10_ushort) ((#0) << (#1)))")
    public operator this << (count:Int): UShort = UShort(shortVal << count);

    /**
     * A bitwise right shift operator.
     * Computes the value of the left-hand operand shifted right by the value of the right-hand operand,
     * filling the high bits with zeros.
     * If the right-hand operand is negative, the results are undefined.
     * @param count the shift count
     * @return this UShort shifted right by count.
     */
    // @Native("java", "((short) ((#this) >>> (#count)))")
    @Native("c++",  "((x10_ushort) ((#0) >> (#1)))")
    public operator this >> (count:Int): UShort = UShort(shortVal >>> count);

    /**
     * A bitwise logical right shift operator (zero-fill).
     * Computes the value of the left-hand operand shifted right by the value of the right-hand operand,
     * filling the high bits with zeros.
     * If the right-hand operand is negative, the results are undefined.
     * @deprecated use the right-shift operator.
     * @param count the shift count
     * @return this UShort shifted right by count with high bits zero-filled.
     */
    // @Native("java", "((short) ((#this) >>> (#count)))")
    @Native("c++",  "((x10_ushort) ((#0) >> (#1)))")
    public operator this >>> (count:Int): UShort = UShort(shortVal >>> count);

    /**
     * A bitwise complement operator.
     * Computes a bitwise complement (NOT) of the operand.
     * @return the bitwise complement of this UShort.
     */
    // @Native("java", "((short) ~(#this))")
    @Native("c++",  "((x10_ushort) ~(#0))")
    public operator ~ this: UShort = UShort(~(shortVal));


    /**
     * Coerce a given UByte to a UShort.
     * @param x the given UByte
     * @return the given UByte converted to a UShort.
     */
    // @Native("java", "((short) ((#x) & 0xff))")
    @Native("c++",  "((x10_ushort) (#1))")
    public static operator (x:UByte): UShort = UShort(x.byteVal & 0xff as Short);

    /**
     * Convert a given UInt to a UShort.
     * @param x the given UInt
     * @return the given UInt converted to a UShort.
     */
    // @Native("java", "((short) (#x))")
    @Native("c++",  "((x10_ushort) (#1))")
    public static operator (x:UInt) as UShort = UShort(x.intVal as Short);

    /**
     * Convert a given ULong to a UShort.
     * @param x the given ULong
     * @return the given ULong converted to a UShort.
     */
    // @Native("java", "((short)(long)(#x))")
    @Native("c++",  "((x10_ushort) (#1))")
    public static operator (x:ULong) as UShort = UShort(x.longVal as Short);


    /**
     * Coerce a given Byte to a UShort.
     * @param x the given Byte
     * @return the given Byte converted to a UShort.
     */
    // @Native("java", "((short)(byte)(#x))")
    @Native("c++",  "((x10_ushort) (#1))")
    public static operator (x:Byte): UShort = UShort(x);

    /**
     * Convert a given Int to a UShort.
     * @param x the given Int
     * @return the given Int converted to a UShort.
     */
    // @Native("java", "((short)(int)(#x))")
    @Native("c++",  "((x10_ushort) (#1))")
    public static operator (x:Int) as UShort = UShort(x as Short);

    /**
     * Convert a given Long to a UShort.
     * @param x the given Long
     * @return the given Long converted to a UShort.
     */
    // @Native("java", "((short)(long)(#x))")
    @Native("c++",  "((x10_ushort) (#1))")
    public static operator (x:Long) as UShort = UShort(x as Short);

    /**
     * Convert a given Float to a UShort.
     * @param x the given Float
     * @return the given Float converted to a UShort.
     */
    // @Native("java", "x10.core.Floats.toUShort(#x)")
    @Native("c++",  "x10aux::float_utils::toUShort(#1)")
    public static operator (x:Float) as UShort {
        val temp : Int = x as Int;
        if (temp > 0xffff) return UShort(0xffff as Byte);
        else if (temp < 0) return UShort(0);
        else return UShort(temp as Short);
    }

    /**
     * Convert a given Double to a UShort.
     * @param x the given Double
     * @return the given Double converted to a UShort.
     */
    // @Native("java", "x10.core.Floats.toUShort(#x)")
    @Native("c++",  "x10aux::double_utils::toUShort(#1)")
    public static operator (x:Double) as UShort {
        val temp : Int = x as Int;
        if (temp > 0xffff) return UShort(0xffff as Byte);
        else if (temp < 0) return UShort(0);
        else return UShort(temp as Short);        
    }

    /**
     * Coerce a given Short to a UShort.
     * @param x the given Short
     * @return the given Short converted to a UShort.
     */
    // @Native("java", "((short)(short)(#x))")
    @Native("c++",  "((x10_ushort) (#1))")
    public static operator (x:Short) as UShort = UShort(x);


    /**
     * A constant holding the minimum value a UShort can have, 0.
     */
    // @Native("java", "((short)0)")
    @Native("c++", "((x10_ushort)0U)")
    public static MIN_VALUE = 0 as UShort;

    /**
     * A constant holding the maximum value a UShort can have, 2<sup>16</sup>-1.
     */
    // @Native("java", "((short)0xffff)")
    @Native("c++", "((x10_ushort)0xffffU)")
    public static MAX_VALUE = 0xffff as UShort;


    /**
     * Returns a String representation of this UShort in the specified radix.
     * @param radix the radix to use in the String representation
     * @return a String representation of this UShort in the specified radix.
     */
    // @Native("java", "java.lang.Integer.toString((#this) & 0xffff, #radix)")
    @Native("c++", "x10aux::int_utils::toString((#0) & 0xffff, #1)")
    public def toString(radix:Int): String = ((this.shortVal & 0xFFFF) as Int).toString(radix);

    /**
     * Returns a String representation of this UShort as a hexadecimal number.
     * @return a String representation of this UShort as a hexadecimal number.
     */
    // @Native("java", "java.lang.Integer.toHexString((#this) & 0xffff)")
    @Native("c++", "x10aux::int_utils::toHexString((#0) & 0xffff)")
    public def toHexString(): String = this.shortVal.toHexString();

    /**
     * Returns a String representation of this UShort as an octal number.
     * @return a String representation of this UShort as an octal number.
     */
    // @Native("java", "java.lang.Integer.toOctalString((#this) & 0xffff)")
    @Native("c++", "x10aux::int_utils::toOctalString((#0) & 0xffff)")
    public def toOctalString(): String = this.shortVal.toOctalString();

    /**
     * Returns a String representation of this UShort as a binary number.
     * @return a String representation of this UShort as a binary number.
     */
    // @Native("java", "java.lang.Integer.toBinaryString((#this) & 0xffff)")
    @Native("c++", "x10aux::int_utils::toBinaryString((#0) & 0xffff)")
    public def toBinaryString(): String = this.shortVal.toBinaryString();

    /**
     * Returns a String representation of this UShort as a decimal number.
     * @return a String representation of this UShort as a decimal number.
     */
    // @Native("java", "java.lang.Integer.toString((#this) & 0xffff)")
    @Native("c++", "x10aux::to_string(#0)")
    public def toString(): String = ((this.shortVal & 0xFFFF) as Int).toString();

    /**
     * @deprecated use {@link #parse(String,Int)} instead
     */
    // @Native("java", "((short) (java.lang.Integer.parseInt(#s, #radix) & 0xffff))")
    @Native("c++", "x10aux::short_utils::parseUShort(#1, #2)")
    public static def parseUShort(s:String, radix:Int): UShort //throwsNumberFormatException 
    {
        return parse(s, radix);
    }

    /**
     * @deprecated use {@link #parse(String)} instead
     */
    // @Native("java", "java.lang.Integer.parseInt(#s)")
    @Native("c++", "x10aux::short_utils::parseUShort(#1)")
    public static def parseUShort(s:String): UShort //throwsNumberFormatException 
    {
        return parse(s);
    }

    /**
     * Parses the String argument as a UShort in the radix specified by the second argument.
     * @param s the String containing the UShort representation to be parsed
     * @param radix the radix to be used while parsing s
     * @return the UShort represented by the String argument in the specified radix.
     * @throws NumberFormatException if the String does not contain a parsable UShort.
     */
    // @Native("java", "((short) (java.lang.Integer.parseInt(#s, #radix) & 0xffff))")
    @Native("c++", "x10aux::short_utils::parseUShort(#1, #2)")
    public static def parse(s:String, radix:Int): UShort //throwsNumberFormatException 
    {
    	val i = Int.parse(s, radix);
    	if (i < 0 || i > 0xffff) {
    		throw new NumberFormatException("Value out of range. Value:\"" + s + "\" Radix:" + radix);
    	}
    	return i as UShort;
    }

    /**
     * Parses the String argument as a decimal UShort.
     * @param s the String containing the UShort representation to be parsed
     * @return the UShort represented by the String argument.
     * @throws NumberFormatException if the String does not contain a parsable UShort.
     */
    // @Native("java", "((short) (java.lang.Integer.parseInt(#s) & 0xffff)")
    @Native("c++", "x10aux::short_utils::parseUShort(#1)")
    public static def parse(s:String): UShort //throwsNumberFormatException 
    {
        return parse(s, 10);
    }


    /**
     * Returns the value obtained by reversing the order of the bits in the
     * binary representation of this UShort.
     * @return the value obtained by reversing order of the bits in this UShort.
     */
    // @Native("java", "((short)(java.lang.Integer.reverse(#this)>>>16))")
    @Native("c++", "((x10_ushort)(x10aux::int_utils::reverse(#0)>>16))")
    public def reverse(): UShort = UShort(this.shortVal.reverse());

    /**
     * Returns the signum function of this UShort.  The return value is 0 if
     * this UShort is zero and 1 if this UShort is non-zero.
     * @return the signum function of this UShort.
     */
    // @Native("java", "(((#this)==0) ? 0 : 1)")
    @Native("c++",  "(((#0)==0U) ? 0 : 1)")
    public def signum(): Int = (this.shortVal == 0) ? 0 : 1;

    /**
     * Returns the value obtained by reversing the order of the bytes in the
     * representation of this UShort.
     * @return the value obtained by reversing (or, equivalently, swapping) the bytes in this UShort.
     */
    // @Native("java", "java.lang.Short.reverseBytes(#this)")
    @Native("c++", "((x10_ushort) x10aux::short_utils::reverseBytes((x10_short) #0))")
    public def reverseBytes(): UShort = UShort(this.shortVal.reverseBytes());


    /**
     * Return true if the given entity is a UShort, and this UShort is equal
     * to the given entity.
     * @param x the given entity
     * @return true if this UShort is equal to the given entity.
     */
    // @Native("java", "x10.rtt.Equality.equalsequals(#this, #x)")
    @Native("c++", "x10aux::equals(#0, #1)")
    public def equals(x:Any):Boolean = x instanceof UShort && (x as UShort).shortVal == this.shortVal;

    /**
     * Returns true if this UShort is equal to the given UShort.
     * @param x the given UShort
     * @return true if this UShort is equal to the given UShort.
     */
    // @Native("java", "x10.rtt.Equality.equalsequals(#this, #x)")
    @Native("c++", "x10aux::equals(#0, #1)")
    public def equals(x:UShort):Boolean = this.shortVal == x.shortVal;

    /**
    * Returns a negative Int, zero, or a positive Int if this UShort is less than, equal
    * to, or greater than the given UShort.
    * @param x the given UShort
    * @return a negative Int, zero, or a positive Int if this UShort is less than, equal
    * to, or greater than the given UShort.
    */
    // @Native("java", "x10.rtt.Equality.compareTo(#this.shortVal + java.lang.Short.MIN_VALUE, #x.shortVal + java.lang.Short.MIN_VALUE)")
    @Native("c++", "x10aux::short_utils::compareTo(#0, #1)")
    public def compareTo(x:UShort): Int = (this.shortVal + Short.MIN_VALUE).compareTo(x.shortVal + Short.MIN_VALUE);
}
