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

#ifndef X10_LANG_DOUBLE_H
#define X10_LANG_DOUBLE_H

#include <x10aux/config.h>
#include <cmath>

namespace x10 {
    namespace lang {
        class String;
        
        class DoubleNatives {
        public:
            static String* toHexString(x10_double value);
            static String* toString(x10_double value);
            static x10_double parseDouble(String* s);
            static x10_boolean isNaN(x10_double value) {
#if defined(_AIX)
				return isnan(value);
#else
				return std::isnan(value);
#endif
            }
            static x10_boolean isInfinite(x10_double value) {
#if defined(_AIX)
				return isinf(value);
#else
				return std::isinf(value);
#endif
            }
            static x10_long toLongBits(x10_double value);
            static x10_long toRawLongBits(x10_double value);
            static x10_double fromLongBits(x10_long value);
            static inline x10_int compareTo(x10_double v1, x10_double v2) {
                return v1 == v2 ? 0 : (v1 < v2 ? -1 : 1);
            }
            static x10_byte toByte(x10_double value);
            static x10_ubyte toUByte(x10_double value);
            static x10_short toShort(x10_double value);
            static x10_ushort toUShort(x10_double value);
            static x10_int toInt(x10_double value);
            static x10_uint toUInt(x10_double value);
            static x10_long toLong(x10_double value);
            static x10_ulong toULong(x10_double value);
            static inline x10_double mod(x10_double a, x10_double b) {
                return (x10_double)::fmod(a, b);
            }
        };
    }
}

#endif /* X10_LANG_DOUBLE_H */
