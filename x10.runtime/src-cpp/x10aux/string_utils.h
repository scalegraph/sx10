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

#ifndef X10AUX_STRING_UTILS_H
#define X10AUX_STRING_UTILS_H

#include <x10aux/config.h>
#include <x10aux/ref.h>
#include <x10aux/captured_lval.h>

namespace x10 {
    namespace lang {
        template<class T> class Rail;
        class String;
    }
    namespace array {
        template<class T> class Array;
    }        
}

namespace x10aux {

    ref<x10::array::Array<ref<x10::lang::String> > > convert_args(int ac, char **av);

    // Used by x10/util/StringBuilder.x10
    ref<x10::lang::String> vrc_to_string(x10aux::ref<x10::lang::Rail<x10_char> > v);

    namespace string_utils {

        ref<x10::lang::String> lit(const char*);

        const char* cstr(ref<x10::lang::String>);

    }

    /*
     * Wrapers around to_string to translate null to "null"
     */
    template<class T> ref<x10::lang::String> safe_to_string(ref<T> v) {
        if (v.isNull()) return string_utils::lit("null");
        return to_string(v);
    }
    template<class T> ref<x10::lang::String> safe_to_string(captured_ref_lval<T> v) {
        return safe_to_string(*v);
    }
    template<class T> ref<x10::lang::String> safe_to_string(captured_struct_lval<T> v) {
        return to_string(*v);
    }
    template<class T> ref<x10::lang::String> safe_to_string(T v) {
        return to_string(v);
    }
}

    

#endif
// vim:tabstop=4:shiftwidth=4:expandtab
