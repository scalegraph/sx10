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
 * UByte is an 8-bit unsigned integral data type, with
 * values ranging from 0 to 255, inclusive.  All of the normal
 * arithmetic and bitwise operations are defined on UByte, and UByte
 * is closed under those operations.  There are also static methods
 * that define conversions from other data types, including String,
 * as well as some UByte constants.
 */
// @NativeRep("java", "byte", null, "x10.rtt.Types.UBYTE")
@NativeRep("c++", "x10_ubyte", "x10_ubyte", null)
public struct UByte implements Comparable[UByte] /*TODO implements Arithmetic[UByte], Bitwise[UByte], Ordered[UByte]*/ {

    /** The actual number with Byte representation */
    public val byteVal:Byte;
    public def this(value:Byte) {
        this.byteVal = value;
    }

    /**
     * A less-than operator.
     * Compares this UByte with another UByte and returns true if this UByte is
     * strictly less than the other UByte.
     * @param x the other UByte
     * @return true if this UByte is strictly less than the other UByte.
     */
    // @Native("java", "x10.core.Unsigned.lt(#0, #1)")
    @Native("c++",  "((#0) < (#1))")
    public operator this < (x:UByte): Boolean {
        return (byteVal + Byte.MIN_VALUE) < (x.byteVal + Byte.MIN_VALUE);
    }

    /**
     * A greater-than operator.
     * Compares this UByte with another UByte and returns true if this UByte is
     * strictly greater than the other UByte.
     * @param x the other UByte
     * @return true if this UByte is strictly greater than the other UByte.
     */
    // @Native("java", "x10.core.Unsigned.gt(#0, #1)")
    @Native("c++",  "((#0) > (#1))")
    public operator this > (x:UByte): Boolean {
        return (byteVal + Byte.MIN_VALUE) > (x.byteVal + Byte.MIN_VALUE);
    }

    /**
     * A less-than-or-equal-to operator.
     * Compares this UByte with another UByte and returns true if this UByte is
     * less than or equal to the other UByte.
     * @param x the other UByte
     * @return true if this UByte is less than or equal to the other UByte.
     */
    // @Native("java", "x10.core.Unsigned.le(#0, #1)")
    @Native("c++",  "((#0) <= (#1))")
    public operator this <= (x:UByte): Boolean {
        return (byteVal + Byte.MIN_VALUE) <= (x.byteVal + Byte.MIN_VALUE);
    }

    /**
     * A greater-than-or-equal-to operator.
     * Compares this UByte with another UByte and returns true if this UByte is
     * greater than or equal to the other UByte.
     * @param x the other UByte
     * @return true if this UByte is greater than or equal to the other UByte.
     */
    // @Native("java", "x10.core.Unsigned.ge(#0, #1)")
    @Native("c++",  "((#0) >= (#1))")
    public operator this >= (x:UByte): Boolean {
        return (byteVal + Byte.MIN_VALUE) >= (x.byteVal + Byte.MIN_VALUE);
    }


    /**
     * A binary plus operator.
     * Computes the result of the addition of the two operands.
     * Overflows result in truncating the high bits.
     * @param x the other UByte
     * @return the sum of this UByte and the other UByte.
     */
    // @Native("java", "((byte) ((#0) + (#1)))")
    @Native("c++",  "((x10_ubyte) ((#0) + (#1)))")
    public operator this + (x:UByte): UByte = UByte(byteVal + x.byteVal);

    /**
     * A binary plus operator (unsigned disambiguation).
     * @see #operator(UByte)+(UByte)
     */
    // @Native("java", "((byte) ((#0) + (#1)))")
    @Native("c++",  "((x10_ubyte) ((#0) + (#1)))")
    public operator (x:Byte) + this: UByte = UByte(x + byteVal);
    /**
     * A binary plus operator (unsigned disambiguation).
     * @see #operator(UByte)+(UByte)
     */
    // @Native("java", "((byte) ((#0) + (#1)))")
    @Native("c++",  "((x10_ubyte) ((#0) + (#1)))")
    public operator this + (x:Byte): UByte = UByte(byteVal + x);

    /**
     * A binary minus operator.
     * Computes the result of the subtraction of the two operands.
     * Overflows result in truncating the high bits.
     * @param x the other UByte
     * @return the difference of this UByte and the other UByte.
     */
    // @Native("java", "((byte) ((#0) - (#1)))")
    @Native("c++",  "((x10_ubyte) ((#0) - (#1)))")
    public operator this - (x:UByte): UByte = UByte(byteVal - x.byteVal);

    /**
     * A binary minus operator (unsigned disambiguation).
     * @see #operator(UByte)-(UByte)
     */
    // @Native("java", "((byte) ((#0) - (#1)))")
    @Native("c++",  "((x10_ubyte) ((#1) - (#0)))")
    public operator (x:Byte) - this: UByte = UByte(x - byteVal);
    /**
     * A binary minus operator (unsigned disambiguation).
     * @see #operator(UByte)-(UByte)
     */
    // @Native("java", "((byte) ((#0) - (#1)))")
    @Native("c++",  "((x10_ubyte) ((#0) - (#1)))")
    public operator this - (x:Byte): UByte = UByte(byteVal - x);

    /**
     * A binary multiply operator.
     * Computes the result of the multiplication of the two operands.
     * Overflows result in truncating the high bits.
     * @param x the other UByte
     * @return the product of this UByte and the other UByte.
     */
    // @Native("java", "((byte) ((#0) * (#1)))")
    @Native("c++",  "((x10_ubyte) ((#0) * (#1)))")
    public operator this * (x:UByte): UByte = UByte(byteVal * x.byteVal);

    /**
     * A binary multiply operator (unsigned disambiguation).
     * @see #operator(UByte)*(UByte)
     */
    // @Native("java", "((byte) ((#0) * (#1)))")
    @Native("c++",  "((x10_ubyte) ((#0) * (#1)))")
    public operator (x:Byte) * this: UByte = UByte(x * byteVal);
    /**
     * A binary multiply operator (unsigned disambiguation).
     * @see #operator(UByte)*(UByte)
     */
    // @Native("java", "((byte) ((#0) * (#1)))")
    @Native("c++",  "((x10_ubyte) ((#0) * (#1)))")
    public operator this * (x:Byte): UByte = UByte(byteVal * x);

    /**
     * A binary divide operator.
     * Computes the result of the division of the two operands.
     * @param x the other UByte
     * @return the quotient of this UByte and the other UByte.
     */
    // @Native("java", "((byte) x10.core.Unsigned.div(#0, #1))")
    @Native("c++",  "((x10_ubyte) ((#0) / x10aux::zeroCheck(#1)))")
    public operator this / (x:UByte): UByte {
        return UByte(((byteVal as Long) / (x.byteVal as Long)) as Byte);
    }
    /**
     * A binary divide operator (unsigned disambiguation).
     * @see #operator(UByte)/(UByte)
     */
    // @Native("java", "((byte) x10.core.Unsigned.div(#0, #1))")
    @Native("c++",  "((x10_ubyte) ((#0) / x10aux::zeroCheck(#1)))")
    public operator (x:Byte) / this: UByte {
        return UByte(((x as Long) / (byteVal as Long)) as Byte);
    }
    /**
     * A binary divide operator (unsigned disambiguation).
     * @see #operator(UByte)/(UByte)
     */
    // @Native("java", "((byte) x10.core.Unsigned.div(#0, #1))")
    @Native("c++",  "((x10_ubyte) ((#0) / x10aux::zeroCheck(#1)))")
    public operator this / (x:Byte): UByte {
        return UByte(((byteVal as Long) / (x as Long)) as Byte);
    }

    /**
     * A binary remainder operator.
     * Computes a remainder from the division of the two operands.
     * @param x the other UByte
     * @return the remainder from dividing this UByte by the other UByte.
     */
    // @Native("java", "((byte) x10.core.Unsigned.rem(#0, #1))")
    @Native("c++",  "((x10_ubyte) ((#0) % x10aux::zeroCheck(#1)))")
    public operator this % (x:UByte): UByte {
        return UByte(((byteVal as Long) % (x.byteVal as Long)) as Byte);
    }
    /**
     * A binary remainder operator (unsigned disambiguation).
     * @see #operator(UByte)%(UByte)
     */
    // @Native("java", "((byte) x10.core.Unsigned.rem(#0, #1))")
    @Native("c++",  "((x10_ubyte) ((#0) % x10aux::zeroCheck(#1)))")
    public operator (x:Byte) % this: UByte {
        return UByte(((x as Long) % (byteVal as Long)) as Byte);
    }
    /**
     * A binary remainder operator (unsigned disambiguation).
     * @see #operator(UByte)%(UByte)
     */
    // @Native("java", "((byte) x10.core.Unsigned.rem(#0, #1))")
    @Native("c++",  "((x10_ubyte) ((#0) % x10aux::zeroCheck(#1)))")
    public operator this % (x:Byte): UByte {
        return UByte(((byteVal as Long) % (x as Long)) as Byte);
    }

    /**
     * A unary plus operator.
     * A no-op.
     * @return the value of this UByte.
     */
    // @Native("java", "((byte) +(#0))")
    @Native("c++",  "((x10_ubyte) +(#0))")
    public operator + this: UByte = this;

    /**
     * A unary minus operator.
     * Computes the two's complement of the operand.
     * Overflows result in truncating the high bits.
     * @return the two's complement of this UByte.
     */
    // @Native("java", "((byte) -(#0))")
    @Native("c++",  "((x10_ubyte) -(#0))")
    public operator - this: UByte = UByte(-(byteVal));


    /**
     * A bitwise and operator.
     * Computes a bitwise AND of the two operands.
     * @param x the other UByte
     * @return the bitwise AND of this UByte and the other UByte.
     */
    // @Native("java", "((byte) ((#0) & (#1)))")
    @Native("c++",  "((x10_ubyte) ((#0) & (#1)))")
    public operator this & (x:UByte): UByte = UByte(byteVal & x.byteVal);
    /**
     * A bitwise and operator (unsigned disambiguation).
     * @see #operator(UByte)&(UByte)
     */
    // @Native("java", "((byte) ((#0) & (#1)))")
    @Native("c++",  "((x10_ubyte) ((#0) & (#1)))")
    public operator (x:Byte) & this: UByte = UByte(x & byteVal);
    /**
     * A bitwise and operator (unsigned disambiguation).
     * @see #operator(UByte)&(UByte)
     */
    // @Native("java", "((byte) ((#0) & (#1)))")
    @Native("c++",  "((x10_ubyte) ((#0) & (#1)))")
    public operator this & (x:Byte): UByte = UByte(byteVal & x);

    /**
     * A bitwise or operator.
     * Computes a bitwise OR of the two operands.
     * @param x the other UByte
     * @return the bitwise OR of this UByte and the other UByte.
     */
    // @Native("java", "((byte) ((#0) | (#1)))")
    @Native("c++",  "((x10_ubyte) ((#0) | (#1)))")
    public operator this | (x:UByte): UByte = UByte(byteVal | x.byteVal);
    /**
     * A bitwise or operator (unsigned disambiguation).
     * @see #operator(UByte)|(UByte)
     */
    // @Native("java", "((byte) ((#0) | (#1)))")
    @Native("c++",  "((x10_ubyte) ((#0) | (#1)))")
    public operator (x:Byte) | this: UByte = UByte(x | byteVal);
    /**
     * A bitwise or operator (unsigned disambiguation).
     * @see #operator(UByte)|(UByte)
     */
    // @Native("java", "((byte) ((#0) | (#1)))")
    @Native("c++",  "((x10_ubyte) ((#0) | (#1)))")
    public operator this | (x:Byte): UByte = UByte(byteVal | x);

    /**
     * A bitwise xor operator.
     * Computes a bitwise XOR of the two operands.
     * @param x the other UByte
     * @return the bitwise XOR of this UByte and the other UByte.
     */
    // @Native("java", "((byte) ((#0) ^ (#1)))")
    @Native("c++",  "((x10_ubyte) ((#0) ^ (#1)))")
    public operator this ^ (x:UByte): UByte = UByte(byteVal ^ x.byteVal);
    /**
     * A bitwise xor operator (unsigned disambiguation).
     * @see #operator(UByte)^(UByte)
     */
    // @Native("java", "((byte) ((#0) ^ (#1)))")
    @Native("c++",  "((x10_ubyte) ((#0) ^ (#1)))")
    public operator (x:Byte) ^ this: UByte = UByte(x ^ byteVal);
    /**
     * A bitwise xor operator (unsigned disambiguation).
     * @see #operator(UByte)^(UByte)
     */
    // @Native("java", "((byte) ((#0) ^ (#1)))")
    @Native("c++",  "((x10_ubyte) ((#0) ^ (#1)))")
    public operator this ^ (x:Byte): UByte = UByte(byteVal ^ x);

    /**
     * A bitwise left shift operator.
     * Computes the value of the left-hand operand shifted left by the value of the right-hand operand.
     * If the right-hand operand is negative, the results are undefined.
     * @param count the shift count
     * @return this UByte shifted left by count.
     */
    // @Native("java", "((byte) ((#0) << (#1)))")
    @Native("c++",  "((x10_ubyte) ((#0) << (#1)))")
    public operator this << (count:Int): UByte = UByte(byteVal << count);

    /**
     * A bitwise right shift operator.
     * Computes the value of the left-hand operand shifted right by the value of the right-hand operand,
     * filling the high bits with zeros.
     * If the right-hand operand is negative, the results are undefined.
     * @param count the shift count
     * @return this UByte shifted right by count.
     */
    // @Native("java", "((byte) ((#0) >>> (#1)))")
    @Native("c++",  "((x10_ubyte) ((#0) >> (#1)))")
    public operator this >> (count:Int): UByte = UByte(byteVal >>> count);

    /**
     * A bitwise logical right shift operator (zero-fill).
     * Computes the value of the left-hand operand shifted right by the value of the right-hand operand,
     * filling the high bits with zeros.
     * If the right-hand operand is negative, the results are undefined.
     * @deprecated use the right-shift operator.
     * @param count the shift count
     * @return this UByte shifted right by count with high bits zero-filled.
     */
    // @Native("java", "((byte) ((#0) >>> (#1)))")
    @Native("c++",  "((x10_ubyte) ((#0) >> (#1)))")
    public operator this >>> (count:Int): UByte = UByte(byteVal >>> count);

    /**
     * A bitwise complement operator.
     * Computes a bitwise complement (NOT) of the operand.
     * @return the bitwise complement of this UByte.
     */
    // @Native("java", "((byte) ~(#0))")
    @Native("c++",  "((x10_ubyte) ~(#0))")
    public operator ~ this: UByte = UByte(~(byteVal));

    /**
     * Convert a given UShort to a UByte.
     * @param x the given UShort
     * @return the given UShort converted to a UByte.
     */
    // @Native("java", "((byte)(short)(#1))")
    @Native("c++",  "((x10_ubyte) (#1))")
    public static operator (x:UShort) as UByte = UByte(x.shortVal as Byte);

    /**
     * Convert a given UInt to a UByte.
     * @param x the given UInt
     * @return the given UInt converted to a UByte.
     */
    // @Native("java", "((byte)(int)(#1))")
    @Native("c++",  "((x10_ubyte) (#1))")
    public static operator (x:UInt) as UByte = UByte(x.intVal as Byte);

    /**
     * Convert a given ULong to a UByte.
     * @param x the given ULong
     * @return the given ULong converted to a UByte.
     */
    // @Native("java", "((byte)(long)(#1))")
    @Native("c++",  "((x10_ubyte) (#1))")
    public static operator (x:ULong) as UByte = UByte(x.longVal as Byte);


    /**
     * Convert a given Short to a UByte.
     * @param x the given Short
     * @return the given Short converted to a UByte.
     */
    // @Native("java", "((byte)(short)(#1))")
    @Native("c++",  "((x10_ubyte) (#1))")
    public static operator (x:Short) as UByte = UByte(x as Byte);

    /**
     * Convert a given Int to a UByte.
     * @param x the given Int
     * @return the given Int converted to a UByte.
     */
    // @Native("java", "((byte)(int)(#1))")
    @Native("c++",  "((x10_ubyte) (#1))")
    public static operator (x:Int) as UByte = UByte(x as Byte);

    /**
     * Convert a given Long to a UByte.
     * @param x the given Long
     * @return the given Long converted to a UByte.
     */
    // @Native("java", "((byte)(long)(#1))")
    @Native("c++",  "((x10_ubyte) (#1))")
    public static operator (x:Long) as UByte = UByte(x as Byte);

    /**
     * Convert a given Float to a UByte.
     * @param x the given Float
     * @return the given Float converted to a UByte.
     */
    // @Native("java", "x10.core.Floats.toUByte(#1)")
    @Native("c++",  "((x10_ubyte) (#1))")
    public static operator (x:Float) as UByte {
        val temp : Int = x as Int;
        if (temp > 0xff) return UByte(0xff as Byte);
        else if (temp < 0) return UByte(0);
        else return UByte(temp as Byte);
    }

    /**
     * Convert a given Double to a UByte.
     * @param x the given Double
     * @return the given Double converted to a UByte.
     */
    // @Native("java", "x10.core.Floats.toUByte(#1)")
    @Native("c++",  "((x10_ubyte) (#1))")
    public static operator (x:Double) as UByte {
        val temp : Int = x as Int;
        if (temp > 0xff) return UByte(0xff as Byte);
        else if (temp < 0) return UByte(0);
        else return UByte(temp as Byte);
    }

    /**
     * Coerce a given Byte to a UByte.
     * @param x the given Byte
     * @return the given Byte converted to a UByte.
     */
    // @Native("java", "((byte)(#1))")
    @Native("c++",  "((x10_ubyte) (#1))")
    public static operator (x:Byte): UByte = UByte(x);


    /**
     * A constant holding the minimum value a UByte can have, 0.
     */
    // @Native("java", "0")
    @Native("c++", "0U")
    public static MIN_VALUE = 0 as UByte;

    /**
     * A constant holding the maximum value a UByte can have, 2<sup>8</sup>-1.
     */
    // @Native("java", "((byte)0xff)")
    @Native("c++", "0xffU")
    public static MAX_VALUE = 0xff as UByte;


    /**
     * Returns a String representation of this UByte in the specified radix.
     * @param radix the radix to use in the String representation
     * @return a String representation of this UByte in the specified radix.
     */
    // @Native("java", "java.lang.Integer.toString((#0) & 0xff, #1)")
    @Native("c++", "x10aux::int_utils::toString((#0) & 0xff, #1)")
    public def toString(radix:Int): String = ((this.byteVal & 0xFF) as Int).toString(radix);

    /**
     * Returns a String representation of this UByte as a hexadecimal number.
     * @return a String representation of this UByte as a hexadecimal number.
     */
    // @Native("java", "java.lang.Integer.toHexString((#0) & 0xff)")
    @Native("c++", "x10aux::int_utils::toHexString((#0) & 0xff)")
    public def toHexString(): String = this.byteVal.toHexString();

    /**
     * Returns a String representation of this UByte as an octal number.
     * @return a String representation of this UByte as an octal number.
     */
    // @Native("java", "java.lang.Integer.toOctalString((#0) & 0xff)")
    @Native("c++", "x10aux::int_utils::toOctalString((#0) & 0xff)")
    public def toOctalString(): String = this.byteVal.toOctalString();

    /**
     * Returns a String representation of this UByte as a binary number.
     * @return a String representation of this UByte as a binary number.
     */
    // @Native("java", "java.lang.Integer.toBinaryString((#0) & 0xff)")
    @Native("c++", "x10aux::int_utils::toBinaryString((#0) & 0xff)")
    public def toBinaryString(): String = this.byteVal.toBinaryString();

    /**
     * Returns a String representation of this UByte as a decimal number.
     * @return a String representation of this UByte as a decimal number.
     */
    // @Native("java", "java.lang.Integer.toString((#0) & 0xff)")
    @Native("c++", "x10aux::to_string(#0)")
    public def toString(): String = ((this.byteVal & 0xFF) as Int).toString();

    /**
     * @deprecated use {@link #parse(String,Int)} instead
     */
    // @Native("java", "((byte) (java.lang.Integer.parseInt(#1, #2) & 0xff))")
    @Native("c++", "((x10_ubyte) x10aux::int_utils::parseInt(#1, #2))")
    public static def parseUByte(s:String, radix:Int): UByte //throws NumberFormatException 
    {
        return parse(s, radix);
    }

    /**
     * @deprecated use {@link #parse(String)} instead
     */
    // @Native("java", "java.lang.Integer.parseInt(#1)")
    @Native("c++", "x10aux::int_utils::parseInt(#1)")
    public static def parseUByte(s:String): UByte //throws NumberFormatException 
    {
        return parse(s);
    }

    /**
     * Parses the String argument as a UByte in the radix specified by the second argument.
     * @param s the String containing the UByte representation to be parsed
     * @param radix the radix to be used while parsing s
     * @return the UByte represented by the String argument in the specified radix.
     * @throws NumberFormatException if the String does not contain a parsable UByte.
     */
    // @Native("java", "((byte) (java.lang.Integer.parseInt(#1, #2) & 0xff))")
    @Native("c++", "((x10_ubyte) x10aux::int_utils::parseInt(#1, #2))")
    public static def parse(s:String, radix:Int): UByte //throws NumberFormatException 
    {
    	val i = Int.parse(s, radix);
    	if (i < 0 || i > 0xff) {
    		throw new NumberFormatException("Value out of range. Value:\"" + s + "\" Radix:" + radix);
    	}
    	return i as UByte;
    }

    /**
     * Parses the String argument as a decimal UByte.
     * @param s the String containing the UByte representation to be parsed
     * @return the UByte represented by the String argument.
     * @throws NumberFormatException if the String does not contain a parsable UByte.
     */
    // @Native("java", "java.lang.Integer.parseInt(#1)")
    @Native("c++", "x10aux::int_utils::parseInt(#1)")
    public static def parse(s:String): UByte //throws NumberFormatException 
    {
        return parse(s, 10);
    }


    /**
     * Returns the value obtained by reversing the order of the bits in the
     * binary representation of this UByte.
     * @return the value obtained by reversing order of the bits in this UByte.
     */
    // @Native("java", "((byte)(java.lang.Integer.reverse(#0)>>>24))")
    @Native("c++", "((x10_ubyte)(x10aux::int_utils::reverse(#0)>>24))")
    public def reverse(): UByte = UByte(this.byteVal.reverse());

    /**
     * Returns the signum function of this UByte.  The return value is 0 if
     * this UByte is zero and 1 if this UByte is non-zero.
     * @return the signum function of this UByte.
     */
    // @Native("java", "(((#0)==0) ? 0 : 1)")
    @Native("c++",  "(((#0)==0U) ? 0 : 1)")
    public def signum(): Int = (this.byteVal == 0) ? 0 : 1;


    /**
     * Return true if the given entity is a UByte, and this UByte is equal
     * to the given entity.
     * @param x the given entity
     * @return true if this UByte is equal to the given entity.
     */
    // @Native("java", "x10.rtt.Equality.equalsequals(#0, #1)")
    @Native("c++", "x10aux::equals(#0,#1)")
    public def equals(x:Any):Boolean = this.byteVal.equals(x);

    /**
     * Returns true if this UByte is equal to the given UByte.
     * @param x the given UByte
     * @return true if this UByte is equal to the given UByte.
     */
    // @Native("java", "x10.rtt.Equality.equalsequals(#0, #1)")
    @Native("c++", "x10aux::equals(#0,#1)")
    public def equals(x:UByte):Boolean = this.byteVal == x.byteVal;

    /**
    * Returns a negative Int, zero, or a positive Int if this UByte is less than, equal
    * to, or greater than the given UByte.
    * @param x the given UByte
    * @return a negative Int, zero, or a positive Int if this UByte is less than, equal
    * to, or greater than the given UByte.
    */
    // @Native("java", "x10.rtt.Equality.compareTo(#0.byteVal + java.lang.Byte.MIN_VALUE, #1.byteVal + java.lang.Byte.MIN_VALUE)")
    @Native("c++", "x10aux::_utils::compareTo(#0, #1)")
    public def compareTo(x:UByte): Int = (this.byteVal + Byte.MIN_VALUE).compareTo(x.byteVal + Byte.MIN_VALUE);
}
