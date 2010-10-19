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

#include <x10aux/short_utils.h>
#include <x10aux/basic_functions.h>

#include <x10/lang/String.h>                    \

using namespace x10::lang;
using namespace std;
using namespace x10aux;

const ref<String> x10aux::short_utils::toString(x10_short value, x10_int radix) {
    (void) value; (void) radix;
    UNIMPLEMENTED("toString");
    return X10_NULL;
}

const ref<String> x10aux::short_utils::toHexString(x10_short value) {
    return x10aux::short_utils::toString(value, 16);
}

const ref<String> x10aux::short_utils::toOctalString(x10_short value) {
    return x10aux::short_utils::toString(value, 8);
}

const ref<String> x10aux::short_utils::toBinaryString(x10_short value) {
    return x10aux::short_utils::toString(value, 2);
}

const ref<String> x10aux::short_utils::toString(x10_short value) {
    return to_string(value);
}

x10_short x10aux::short_utils::parseShort(const ref<String>& s, x10_int radix) {
    (void) s;
    UNIMPLEMENTED("parseShort");
    return radix; /* Bogus, but use radix to avoid warning about unused parameter */
}

x10_short x10aux::short_utils::parseShort(const ref<String>& s) {
    (void) s;
    UNIMPLEMENTED("parseShort");
    return 0; 
}

x10_short x10aux::short_utils::reverseBytes(x10_short x) {
    UNIMPLEMENTED("reverseBytes");
    return x; /* Bogus, but use x to avoid warning about unused parameter */
}
// vim:tabstop=4:shiftwidth=4:expandtab
