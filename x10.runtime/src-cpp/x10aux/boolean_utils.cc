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
#include <x10aux/boolean_utils.h>
#include <x10aux/basic_functions.h>

#include <x10/lang/String.h>

#include <strings.h>

using namespace x10::lang;
using namespace std;
using namespace x10aux;

String* x10aux::boolean_utils::toString(x10_boolean value) {
    return x10aux::to_string(value);
}

x10_boolean x10aux::boolean_utils::parseBoolean(const String* s) {
    return NULL != s && !::strcasecmp(s->c_str(), "true");
}

// vim:tabstop=4:shiftwidth=4:expandtab
