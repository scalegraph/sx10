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

#ifndef X10AUX_RAIL_UTILS_H
#define X10AUX_RAIL_UTILS_H

#ifndef NO_IOSTREAM
#include <sstream>
#endif

#include <cstdarg>

#include <x10aux/config.h>
#include <x10aux/ref.h>

namespace x10 {
    namespace lang {
        class String;
    }
}

namespace x10aux {

    void throwArrayIndexOutOfBoundsException(x10_int index, x10_int length) X10_PRAGMA_NORETURN;
    
    inline void checkRailBounds(x10_int index, x10_int length) {
        #ifndef NO_BOUNDS_CHECKS
        // Since we know length is non-negative and Rails are zero-based,
        // the bounds check can be optimized to a single unsigned comparison.
        // The C++ compiler won't do this for us, since it doesn't know that length is non-negative.
        if (((x10_uint)index) >= ((x10_uint)length)) {
            x10aux::throwArrayIndexOutOfBoundsException(index, length);
        }
        #endif
    }

    template<class T, class R> R* alloc_aligned_rail(x10_int length, x10_int alignment);

    template<class T, class R> R* alloc_rail(x10_int length);
    template<class T, class R> R* alloc_rail(x10_int length, T v0);
    template<class T, class R> R* alloc_rail(x10_int length, T v0, T v1);
    template<class T, class R> R* alloc_rail(x10_int length, T v0, T v1, T v2);
    template<class T, class R> R* alloc_rail(x10_int length, T v0, T v1, T v2, T v3);
    template<class T, class R> R* alloc_rail(x10_int length, T v0, T v1, T v2, T v3, T v4);
    template<class T, class R> R* alloc_rail(x10_int length, T v0, T v1, T v2, T v3, T v4, T v5);
    template<class T, class R> R *alloc_rail(x10_int length, T v0, T v1, T v2, T v3, T v4, T v5, T v6, ...);
}

#include <x10aux/string_utils.h>
#include <x10/lang/Object.h>

namespace x10aux {

#ifndef NO_IOSTREAM
    void railToStringProcessElement(std::stringstream *ss, int i, ref<x10::lang::String> elemStr);
    ref<x10::lang::String> railToStringFinalize(std::stringstream *ss);
    std::stringstream *allocStringStream();
    void freeStringStream (std::stringstream *ss);
#endif

    template<class T> ref<x10::lang::String> railToString(x10_int length, T* data) {
        if (0 == length) {
            return string_utils::lit("[]");
        }
        #ifndef NO_IOSTREAM
        std::stringstream *ss = allocStringStream();
        for (x10_int i=0; i<length; i++) {
            railToStringProcessElement(ss, i, x10aux::safe_to_string(data[i]));
        }
        return railToStringFinalize(ss); // NOTE: Finalize is responsible for calling dealloc.
        #else
        return string_utils::lit("[NO_IOSTREAM: not printing rail contents]");
        #endif
    }

// platform-specific min rail alignment
#if defined(_POWER) || defined(__bgp__)
#define X10_MIN_RAIL_ALIGNMENT 16
#else
#define X10_MIN_RAIL_ALIGNMENT ((x10_int)sizeof(x10_double))
#endif
    
    
    template<class T, class R> R* alloc_rail_internal(x10_int length,
                                                      x10_int alignment) {
        bool containsPtrs = x10aux::getRTT<T>()->containsPtrs;
        // assert power of 2
        assert((alignment & (alignment-1)) == 0);
        if (alignment < X10_MIN_RAIL_ALIGNMENT) {
            alignment = X10_MIN_RAIL_ALIGNMENT;
        }
        /*
         * We allocate a single piece of storage that is big enough for both
         * R and its (aligned) backing data array. We then do some pointer arithmetic
         * to get the aligned "interior pointer" to use for data and pass it as
         * an argument to R's constructor (R's data ptr is declared const to enable compiler optimization).
         */
        size_t alignPad = alignment;
        size_t alignDelta = alignPad-1;
        size_t alignMask = ~alignDelta;
        size_t sz = sizeof(R) + alignPad + length*sizeof(T);
        R* uninitialized_rail = x10aux::alloc<R>(sz, containsPtrs);
        size_t raw_rail = (size_t)uninitialized_rail;
        size_t raw_data = (raw_rail + sizeof(R) + alignDelta) & alignMask;
        R *rail = new (uninitialized_rail) R(length, (T*)raw_data);
        _M_("In alloc_rail<"<<getRTT<T>()->name()
                            <<","<<getRTT<R>()->name()<<">"
            <<": rail = " << (void*)rail << "; length = " << length);
        return rail;
    }

    template<class T, class R> R* alloc_aligned_rail(x10_int length, x10_int alignment) {
        R* rail = alloc_rail_internal<T,R>(length, alignment);
        rail->x10::lang::Object::_constructor();
        return rail;
    }

    template<class T, class R> R* alloc_rail(x10_int length) {
        return alloc_aligned_rail<T,R>(length, 8);
    }

    template<class T, class R> R* alloc_rail(x10_int length, T v0) {
        R* rail = alloc_rail<T,R>(length);
        T* data = rail->raw();
        data[0] = v0;
        return rail;
    }

    template<class T, class R> R *alloc_rail(x10_int length, T v0,
                                                             T v1) {
        R* rail = alloc_rail<T,R>(length);
        T* data = rail->raw();
        data[0] = v0;
        data[1] = v1;
        return rail;
    }

    template<class T, class R> R *alloc_rail(x10_int length, T v0,
                                                             T v1,
                                                             T v2) {
        R* rail = alloc_rail<T,R>(length);
        T* data = rail->raw();
        data[0] = v0;
        data[1] = v1;
        data[2] = v2;
        return rail;
    }

    template<class T, class R> R *alloc_rail(x10_int length, T v0,
                                                             T v1,
                                                             T v2,
                                                             T v3) {
        R* rail = alloc_rail<T,R>(length);
        T* data = rail->raw();
        data[0] = v0;
        data[1] = v1;
        data[2] = v2;
        data[3] = v3;
        return rail;
    }

    template<class T, class R> R *alloc_rail(x10_int length, T v0,
                                                             T v1,
                                                             T v2,
                                                             T v3,
                                                             T v4) {
        R* rail = alloc_rail<T,R>(length);
        T* data = rail->raw();
        data[0] = v0;
        data[1] = v1;
        data[2] = v2;
        data[3] = v3;
        data[4] = v4;
        return rail;
    }

    template<class T, class R> R *alloc_rail(x10_int length, T v0,
                                                             T v1,
                                                             T v2,
                                                             T v3,
                                                             T v4,
                                                             T v5) {
        R* rail = alloc_rail<T,R>(length);
        T* data = rail->raw();
        data[0] = v0;
        data[1] = v1;
        data[2] = v2;
        data[3] = v3;
        data[4] = v4;
        data[5] = v5;
        return rail;
    }

    // init elements 7 though length
    template<class T> inline void init_rail(T *data,
                                            x10_int length,
                                            va_list init) {
        for (int i = 7; i < length; i++)
            data[i] = va_arg(init, T);
    }
    // init elements 7 though length: specialize for x10_ubyte
    template<> inline void init_rail<x10_ubyte>(x10_ubyte *data,
                                                x10_int length,
                                                va_list init) {
        for (int i = 7; i < length; i++)
            data[i] = (x10_ubyte)va_arg(init, int);
    }
    // init elements 7 though length: specialize for x10_ushort
    template<> inline void init_rail<x10_ushort>(x10_ushort *data,
                                                 x10_int length,
                                                 va_list init) {
        for (int i = 7; i < length; i++)
            data[i] = (x10_ushort)va_arg(init, int);
    }
    // init elements 7 though length: specialize for x10_uint
    template<> inline void init_rail<x10_uint>(x10_uint *data,
                                               x10_int length,
                                               va_list init) {
        for (int i = 7; i < length; i++)
            data[i] = (x10_uint)va_arg(init, int);
    }
    // init elements 7 though length: specialize for x10_ulong
    template<> inline void init_rail<x10_ulong>(x10_ulong *data,
                                                x10_int length,
                                                va_list init) {
        for (int i = 7; i < length; i++)
            data[i] = (x10_ulong)va_arg(init, int);
    }
    // init elements 7 though length: specialize for x10_byte
    template<> inline void init_rail<x10_byte>(x10_byte *data,
                                               x10_int length,
                                               va_list init) {
        for (int i = 7; i < length; i++)
            data[i] = (x10_byte)va_arg(init, int);
    }
    // init elements 7 though length: specialize for x10_char
    template<> inline void init_rail<x10_char>(x10_char *data,
                                               x10_int length,
                                               va_list init) {
        for (int i = 7; i < length; i++)
            data[i] = (x10_char)(char)va_arg(init, int);
    }
    // init elements 7 though length: specialize for x10_short
    template<> inline void init_rail<x10_short>(x10_short *data,
                                                x10_int length,
                                                va_list init) {
        for (int i = 7; i < length; i++)
            data[i] = (x10_short)va_arg(init, int);
    }
    // init elements 7 though length: specialize for x10_float
    template<> inline void init_rail<x10_float>(x10_float *data,
                                                x10_int length,
                                                va_list init) {
        for (int i = 7; i < length; i++)
            data[i] = (x10_float)va_arg(init, double);
    }
    
    template<class T, class R> R *alloc_rail(x10_int length, T v0,
                                                             T v1,
                                                             T v2,
                                                             T v3,
                                                             T v4,
                                                             T v5,
                                                             T v6,
                                                             ...) {
        R* rail = alloc_rail<T,R>(length);
        T* data = rail->raw();
        data[0] = v0;
        data[1] = v1;
        data[2] = v2;
        data[3] = v3;
        data[4] = v4;
        data[5] = v5;
        data[6] = v6;
        va_list init;
        va_start(init, v6);
        // Need to specialize this loop
        //for (int i = 7; i < length; i++)
        //    data[i] = va_arg(init, T);
        init_rail(data, length, init);
        va_end(init);
        return rail;
    }


    template<class T, class R> void free_rail(x10aux::ref<R> rail) {
        x10aux::dealloc<R >(&*rail);
    }

}

#endif
// vim:tabstop=4:shiftwidth=4:expandtab
