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

#ifndef X10AUX_INT_UTILS_H
#define X10AUX_INT_UTILS_H

#include <x10aux/config.h>
#include <x10aux/ref.h>

namespace x10 {
    namespace lang {
        class String;
    }
}

namespace x10aux {
    class int_utils {
    public:
        static const ref<x10::lang::String> toString(x10_int value, x10_int radix);
        static const ref<x10::lang::String> toHexString(x10_int value);
        static const ref<x10::lang::String> toOctalString(x10_int value);
        static const ref<x10::lang::String> toBinaryString(x10_int value);
        static const ref<x10::lang::String> toString(x10_int value);
        static x10_int parseInt(ref<x10::lang::String> s, x10_int radix);
        static x10_int parseInt(ref<x10::lang::String> s);
        static x10_int highestOneBit(x10_int value);
        static x10_int lowestOneBit(x10_int value);
        static x10_int numberOfLeadingZeros(x10_int value);
        static x10_int numberOfTrailingZeros(x10_int value);
        static x10_int bitCount(x10_int value);
        static x10_int rotateLeft(x10_int value, x10_int distance);
        static x10_int rotateRight(x10_int value, x10_int distance);
        static x10_int reverse(x10_int value);
        static x10_int signum(x10_int value) {
            return (value >> 31) | (((x10_uint)(-value)) >> 31);
        }
        static x10_int reverseBytes(x10_int value);
        static x10_int compareTo(x10_int v1, x10_int v2) {
            return v1 == v2 ? 0 : (v1 < v2 ? -1 : 1);
        }
        static x10_int compareTo(x10_uint v1, x10_uint v2) {
            return v1 == v2 ? 0 : (v1 < v2 ? -1 : 1);
        }
            
    };
}

#endif
// vim:tabstop=4:shiftwidth=4:expandtab
