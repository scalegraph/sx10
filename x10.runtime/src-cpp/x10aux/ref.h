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

#ifndef X10REF_H
#define X10REF_H

#include <cstdlib>
#include <cassert>

#include <x10aux/config.h>
#include <x10aux/alloc.h>
#include <x10aux/RTT.h>
#include <x10aux/network.h>

namespace x10 { namespace lang { class NullType; } }

namespace x10aux {

    //#ifndef NO_IOSTREAM
    //inline std::ostream &operator<<(std::ostream &o, const remote_ref &rr) {
    //    return o << "rr("<<rr.addr<<"@"<<rr.loc<<")";
    //}
    //#endif

    class __ref {
        protected:
        #ifndef REF_STRIP_TYPE
        GPUSAFE __ref(void* = NULL) { }
        #else
        GPUSAFE __ref(void* val = NULL) : _val(val) { }
        public: // [IP] temporary
        void* _val;
        #endif
    };

    #ifndef REF_STRIP_TYPE
    #  define REF_INIT(v) __ref(), _val(v)
    #else
    #  define REF_INIT(v) __ref(v)
    #endif

    template<class T> class ref : public __ref {
        public:
        typedef T Type;
        static const x10aux::RuntimeType* getRTT() { return T::getRTT(); }

        // Work around for an xlC ICE
        //GPUSAFE ~ref() { }

        // Copy between refs of the same type
        GPUSAFE ref(const ref<T>& _ref) : REF_INIT(_ref._val) {
            _R_("Copying reference " << &_ref << "(" << _ref._val
                                     << ") of type " << TYPENAME(T)
                                     << " to " << this);
        }

        // Copy between refs of the same type
        // FIXME: something is wrong with the return value;
        // r1 = r2 = r3 doesn't work in xlC
        GPUSAFE const ref<T>& operator=(const ref<T>& _ref) {
            _val = _ref._val;
            _R_("Assigning reference " << &_ref << "(" << _ref._val
                                       << ") of type " << TYPENAME(T)
                                       << " to " << this);
            return *this;
        }

        // Declare a ref: no need for a NULL __ref
        // The frontend should guarantee ref will not be accessed prior to initialization
        GPUSAFE ref() {
//            fprintf(stderr, "%s\n", TYPENAME(T));
        }

        // This is the big one -- turns a pointer into a ref
        // currently an implicit conversion
        GPUSAFE ref(T* const val) : REF_INIT(val) {
        }

        // Allow conversions between ref<S> and ref<T>.
        // Because we have no multiple inheritance, we can
        // use a re-interpret cast here. 
        // Bad casts should never happen, as the only places
        // this operation is used are: class_cast (which is guarded by a check)
        // and upcasts from x10 code all operator T are implicit conversions,
        // this is no exception

        // Allow the construction of a ref<T> from a ref<S>
        template<class S> GPUSAFE ref(const ref<S>& _ref)
            // (S*) cast needed when REF_STRIP_TYPE defined, otherwise harmless
          : REF_INIT(reinterpret_cast<T*>((S*)_ref._val)) {
            _R_("Casting reference " << &_ref << "(" << _ref._val
                                     << ") of type " << TYPENAME(S)
                                     << " to type " << TYPENAME(T)
                                     << " into " << this << "("<<_val<<")");
        }

        // Allow the assignment of a ref<S> to a ref<T>
        template<class S> GPUSAFE const ref<T> &operator=(const ref<S>& _ref) {
            // (S*) cast needed when REF_STRIP_TYPE defined, otherwise harmless
            _val = reinterpret_cast<T*>((S*)_ref._val);
            _R_("Casting reference " << &_ref << "(" << _ref._val
                                     << ") of type " << TYPENAME(S)
                                     << " to type " << TYPENAME(T)
                                     << " into " << this << "("<<_val<<")");
            return *this;
        }

        T& GPUSAFE operator*() const {
            _R_("Accessing object (*) via reference " << this << "(" << _val
                                      << ") of type " << TYPENAME(T));
            return *(T*)_val;
        }

        T* GPUSAFE operator->() const { 
            _R_("Accessing object (*) via reference " << this << "(" << _val
                                      << ") of type " << TYPENAME(T));
            return (T*)_val;
        }

        bool GPUSAFE isNull() const {
            _R_("Nullcheck reference " << this << "(" << _val
                                      << ") of type " << TYPENAME(T));
            return _val == NULL;
        }

        // trivial operations that compare the contents of the two refs
        bool operator==(const ref<T>& _ref) const { return _val == _ref._val; }
        bool operator!=(const ref<T>& _ref) const { return _val != _ref._val; }


#ifndef REF_STRIP_TYPE
        T* _val;
#endif

    };

#ifndef NO_IOSTREAM
    template<class T> std::ostream& operator<<(std::ostream& o, ref<T> s) {
        if (s.isNull()) {
            o << "null";
        } else {
            o << *s;
        }
        return o;
    }
#endif

    void throwNPE() X10_PRAGMA_NORETURN;

    template <class T> inline ref<T> nullCheck(ref<T> obj) {
        #if !defined(NO_NULL_CHECKS) && !defined(NO_EXCEPTIONS)
        if (obj.isNull()) throwNPE();
        #endif
        return obj;
    }

    // A no-op for non-refs
    template <class T> inline T nullCheck(T str) {
        return str;
    }

    #define X10_NULL x10aux::ref<x10::lang::NullType>(NULL)
    
    template<class F, class T> bool operator!=(F f, T t) { return !(f == t); }
    // comparison of a primitive with a ref
    template<class T> bool operator==(x10_boolean b, const ref<T>& _ref) { return false; }
    template<class T> bool operator==(x10_byte b, const ref<T>& _ref) { return false; }
    template<class T> bool operator==(x10_char c, const ref<T>& _ref) { return false; }
    template<class T> bool operator==(x10_short s, const ref<T>& _ref) { return false; }
    template<class T> bool operator==(x10_int i, const ref<T>& _ref) { return false; }
    template<class T> bool operator==(x10_long l, const ref<T>& _ref) { return false; }
    template<class T> bool operator==(x10_float f, const ref<T>& _ref) { return false; }
    template<class T> bool operator==(x10_double d, const ref<T>& _ref) { return false; }
    template<class T> bool operator==(x10_ubyte b, const ref<T>& _ref) { return false; }
    template<class T> bool operator==(x10_ushort s, const ref<T>& _ref) { return false; }
    template<class T> bool operator==(x10_uint i, const ref<T>& _ref) { return false; }
    template<class T> bool operator==(x10_ulong l, const ref<T>& _ref) { return false; }
    // comparison of a ref with a primitive
    template<class T> bool operator==(const ref<T>& _ref, x10_boolean b) { return false; }
    template<class T> bool operator==(const ref<T>& _ref, x10_byte b) { return false; }
    template<class T> bool operator==(const ref<T>& _ref, x10_char c) { return false; }
    template<class T> bool operator==(const ref<T>& _ref, x10_short s) { return false; }
    template<class T> bool operator==(const ref<T>& _ref, x10_int i) { return false; }
    template<class T> bool operator==(const ref<T>& _ref, x10_long l) { return false; }
    template<class T> bool operator==(const ref<T>& _ref, x10_float f) { return false; }
    template<class T> bool operator==(const ref<T>& _ref, x10_double d) { return false; }
    template<class T> bool operator==(const ref<T>& _ref, x10_ubyte b) { return false; }
    template<class T> bool operator==(const ref<T>& _ref, x10_ushort s) { return false; }
    template<class T> bool operator==(const ref<T>& _ref, x10_uint i) { return false; }
    template<class T> bool operator==(const ref<T>& _ref, x10_ulong l) { return false; }

} //namespace x10


#endif

// vim:tabstop=4:shiftwidth=4:expandtab
