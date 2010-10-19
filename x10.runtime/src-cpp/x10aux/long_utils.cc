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

#include <x10aux/config.h>

#include <x10aux/long_utils.h>
#include <x10aux/basic_functions.h>

#include <x10/lang/String.h>
#if defined(__CYGWIN__) || defined(__FreeBSD__)
extern "C" long long atoll(const char *);
#endif

using namespace x10::lang;
using namespace std;
using namespace x10aux;

const ref<String> x10aux::long_utils::toString(x10_long value, x10_int radix) {
    (void) value; (void) radix;
    UNIMPLEMENTED("toString");
    return X10_NULL;
}

const ref<String> x10aux::long_utils::toHexString(x10_long value) {
    return x10aux::long_utils::toString(value, 16);
}

const ref<String> x10aux::long_utils::toOctalString(x10_long value) {
    return x10aux::long_utils::toString(value, 8);
}

const ref<String> x10aux::long_utils::toBinaryString(x10_long value) {
    return x10aux::long_utils::toString(value, 2);
}

const ref<String> x10aux::long_utils::toString(x10_long value) {
    return to_string(value);
}

x10_long x10aux::long_utils::parseLong(const ref<String>& s, x10_int radix) {
    (void) s;
    UNIMPLEMENTED("parseLong");
    return radix; /* Bogus, but use radix to avoid warning about unused parameter */
}

x10_long x10aux::long_utils::parseLong(const ref<String>& s) {
    // FIXME: what about null?
    // FIXME: NumberFormatException
    return atoll(nullCheck(s)->c_str());
}

x10_long x10aux::long_utils::highestOneBit(x10_long x) {
    UNIMPLEMENTED("highestOneBit");
    return x; /* Bogus, but use x to avoid warning about unused parameter */
}

x10_long x10aux::long_utils::lowestOneBit(x10_long x) {
    UNIMPLEMENTED("lowestOneBit");
    return x; /* Bogus, but use x to avoid warning about unused parameter */
}

x10_int x10aux::long_utils::numberOfLeadingZeros(x10_long x) {
    UNIMPLEMENTED("numberOfLeadingZeros");
    return x; /* Bogus, but use x to avoid warning about unused parameter */
}

x10_int x10aux::long_utils::numberOfTrailingZeros(x10_long x) {
    UNIMPLEMENTED("numberOfTrailingZeros");    
    return x; /* Bogus, but use x to avoid warning about unused parameter */
}

x10_int x10aux::long_utils::bitCount(x10_long x) {
    UNIMPLEMENTED("bitCount");    
    return x; /* Bogus, but use x to avoid warning about unused parameter */
}

x10_long x10aux::long_utils::rotateLeft(x10_long x) {
    UNIMPLEMENTED("rotateLeft");    
    return x; /* Bogus, but use x to avoid warning about unused parameter */
}

x10_long x10aux::long_utils::rotateRight(x10_long x) {
    UNIMPLEMENTED("rotateRight");    
    return x; /* Bogus, but use x to avoid warning about unused parameter */
}

x10_long x10aux::long_utils::reverse(x10_long x) {
    UNIMPLEMENTED("reverse");
    return x; /* Bogus, but use x to avoid warning about unused parameter */
}

x10_int x10aux::long_utils::signum(x10_long x) {
    UNIMPLEMENTED("signum");    
    return x; /* Bogus, but use x to avoid warning about unused parameter */
}

x10_long x10aux::long_utils::reverseBytes(x10_long x) {
    UNIMPLEMENTED("reverseBytes");    
    return x; /* Bogus, but use x to avoid warning about unused parameter */
}
// vim:tabstop=4:shiftwidth=4:expandtab
