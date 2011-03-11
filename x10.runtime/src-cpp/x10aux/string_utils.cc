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
#include <x10aux/string_utils.h>
#include <x10aux/rail_utils.h>
#include <x10aux/alloc.h>
#include <x10aux/math.h>

#include <x10/lang/String.h>
#include <x10/lang/Rail.h>
#include <x10/array/Array.h>

using namespace x10::array;
using namespace x10::lang;
using namespace x10aux;

ref<Array<ref<String> > >x10aux::convert_args(int ac, char **av) {
    assert(ac>=1);
    x10_int x10_argc = ac  - 1;
    ref<Array<ref<String> > > arr(Array<ref<String> >::_make(x10_argc));
    for (int i = 1; i < ac; i++) {
        ref<String> val = String::Lit(av[i]);
        arr->__set(val, i-1);
    }
    return arr;
}

ref<String> x10aux::string_utils::lit(const char* s) {
    return String::Lit(s);
}

const char* x10aux::string_utils::cstr(ref<String> s) {
    return s->c_str();
}

char * x10aux::string_utils::strdup(const char* old) {
#ifdef X10_USE_BDWGC
    int len = strlen(old);
    char *ans = x10aux::alloc<char>(len+1);
    memcpy(ans, old, len);
    ans[len] = 0;
    return ans;
#else
    return ::strdup(old);
#endif
}

char * x10aux::string_utils::strndup(const char* old, int len) {
#ifdef X10_USE_BDWGC
    char *ans = x10aux::alloc<char>(len+1);
    memcpy(ans, old, len);
    ans[len] = 0;
    return ans;
#else
    return ::strndup(old, len);
#endif
}

// vim:tabstop=4:shiftwidth=4:expandtab
