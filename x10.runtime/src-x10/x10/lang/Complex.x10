/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2010.
 *  (C) Copyright Australian National University 2009-2010.
 */

package x10.lang;

import x10.compiler.Native;

/**
 * Complex is a struct representing a complex number (a + b*i).
 * The real and imaginary components are represented as Doubles.
 */
public struct Complex /*TODO implements Arithmetic[Complex]*/ {
    /** The real component of this complex number. */
    public val re:Double;
    /** The imaginary component of this complex number. */
    public val im:Double;

    /** The complex number that corresponds to 0.0 */
    @Native("c++", "x10::lang::Complex_methods::_make(0.0,0.0)")
    public static ZERO : Complex = Complex(0.0, 0.0);
    /** The complex number that corresponds to 1.0 */
    @Native("c++", "x10::lang::Complex_methods::_make(1.0,0.0)")
    public static ONE : Complex = Complex(1.0, 0.0);
    /** The complex number that corresponds to 1.0i */
    @Native("c++", "x10::lang::Complex_methods::_make(0.0,1.0)")
    public static I : Complex = Complex(0.0, 1.0);
    /** The complex number that corresponds to +Inf + +Inf*i */
    @Native("c++", "x10::lang::Complex_methods::_make(x10aux::double_utils::fromLongBits(0x7ff0000000000000LL),x10aux::double_utils::fromLongBits(0x7ff0000000000000LL))")
    public static INF : Complex = Complex(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    /** The complex number that corresponds to NaN + NaN*i */
    @Native("c++", "x10::lang::Complex_methods::_make(x10aux::double_utils::fromLongBits(0x7ff8000000000000LL),x10aux::double_utils::fromLongBits(0x7ff8000000000000LL))")
    public static NaN : Complex = Complex(Double.NaN, Double.NaN);

    /**
     * Construct a complex number with the specified real and imaginary components.
     *
     * @real the real component of the Complex number
     * @imaginary the imaginary component of the Complex number
     * @return the Complex number representing (real + imaginary*i)
     */
    public def this(real:Double, imaginary:Double) {
        this.re = real;
        this.im = imaginary;
    }

    /**
     * @return the sum of this complex number and the given complex number.
     */
    public operator this + (that:Complex):Complex {
        return Complex(re + that.re, im + that.im);
    }

    /**
     * @return the sum of the given double and the given complex number.
     */
    public static operator (x:Double) + (y:Complex): Complex = y + x;

    /**
     * @return the sum of this complex number and the given double.
     */
    public operator this + (that:Double):Complex {
        return Complex(re + that, im);
    }

    /**
     * @return the difference between this complex number and the given complex number.
     */
    public operator this - (that:Complex):Complex {
        return Complex(re - that.re, im - that.im);
    }

    /**
     * @return the difference between the given double and this complex number.
     */
    public static operator (x:Double) - (y:Complex): Complex = Complex(x - y.re, -y.im);

    /**
     * @return the difference between this complex number and the given double.
     */
    public operator this - (that:Double):Complex {
        return Complex(re - that, im);
    }

    /**
     * @return the product of this complex number and the given complex number.
     */
    public operator this * (that:Complex):Complex {
        return Complex(re * that.re - im * that.im,
                       re * that.im + im * that.re);
    }

    /**
     * @return the product of the given double and this complex number.
     */
    public static operator (x:Double) * (y:Complex): Complex = y * x;

    /**
     * @return the product of this complex number and the given double.
     */
    public operator this * (that:Double):Complex {
        return Complex(re * that, im * that);
    }

    /**
     * Return the quotient of this complex number and the given complex number.
     * Uses Smith's algorithm {@link http://doi.acm.org/10.1145/368637.368661}.
     * TODO: consider using Priest's algorithm {@link http://doi.acm.org/10.1145/1039813.1039814}.
     * @return the quotient of this complex number and the given complex number.
     */
    public operator this / (that:Complex):Complex {
        if (isNaN() || that.isNaN()) {
            return Complex.NaN;
        }

        val c:Double = that.re;
        val d:Double = that.im;
        if (c == 0.0 && d == 0.0) {
            return Complex.NaN;
        }

        if (that.isInfinite() && !isInfinite()) {
            return Complex.ZERO;
        }

        if ((Math.abs(d) <= Math.abs(c))) {
            if (c == 0.0) {
                return Complex(im/d, -re/c);
            }
            val r:Double =  d / c;
            val denominator:Double = c + d * r;
            return Complex((re + im * r) / denominator,
                           (im - re * r) / denominator);
        } else {
            if (d == 0.0) {
                return Complex(re/c, im/c);
            }
            val r:Double = c / d;
            val denominator:Double = c * r + d;
            return Complex((re * r + im) / denominator,
                           (im * r - re) / denominator);
        }
    }

    /**
     * @return the quotient of the given double and this complex number.
     */
    public static operator (x:Double) / (y:Complex): Complex = Complex(x, 0.0) / y;

    /**
     * @return the quotient of this complex number and the given double.
     */
    public operator this / (that:Double):Complex {
        return Complex(re / that, im / that);
    }

    /**
     * @return the conjugate of this complex number.
     */
    public def conjugate():Complex = Complex(re, -im);

    /**
     * @return this complex number.
     */
    public operator + this:Complex  = this;

    /**
     * @return the negation of this complex number.
     */
    public operator - this:Complex  = isNaN() ? Complex.NaN : Complex(-re, -im);

    /**
     * Return the absolute value of this complex number.
     * <p>
     * Returns <code>NaN</code> if either the real or imaginary part is
     * <code>NaN</code> and <code>Double.POSITIVE_INFINITY</code> if
     * neither part is <code>NaN</code>, but at least one part takes an infinite
     * value.
     *
     * @return the absolute value of this complex number.
     */
    public def abs():Double {
        if (isNaN()) {
            return Double.NaN;
        }

        if (isInfinite()) {
            return Double.POSITIVE_INFINITY;
        }

        if (im == 0.0) {
            return Math.abs(re);
        } else if (re == 0.0) {
            return Math.abs(im);
        } else {
            // use hypot to avoid unnecessary under/overflow
            return Math.hypot(re, im);
        }
    }

    /**
     * @return true if either part of this complex number is <code>NaN</code>.
     */
    public def isNaN():boolean {
        return re.isNaN() || im.isNaN();
    }

    /**
     * @return true if either part of this complex number is infinite
     * and neither part is <code>NaN</code>.
     */
    public def isInfinite():boolean {
        return !isNaN() &&
        (re.isInfinite() || im.isInfinite());
    }

    /**
     * @return the string representation of this complex number.
     */
    public def toString():String {
        return ("" + re + " + " + im + "i");
    }
}

// vim:shiftwidth=4:tabstop=4:expandtab
