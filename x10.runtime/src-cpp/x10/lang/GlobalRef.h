#ifndef __X10_LANG_GLOBALREF
#define __X10_LANG_GLOBALREF

#include <x10rt.h>

#include <x10/lang/GlobalRef.struct_h>

#endif // X10_LANG_GLOBALREF

namespace x10 { namespace util { 
template<class T> class GlobalRef;
} } 

#ifndef X10_LANG_GLOBALREF_NODEPS
#define X10_LANG_GLOBALREF_NODEPS
#include <x10/lang/Any.h>
#include <x10/lang/String.h>
#ifndef X10_LANG_GLOBALREF_GENERICS
#define X10_LANG_GLOBALREF_GENERICS
#endif // X10_LANG_GLOBALREF_GENERICS
#ifndef X10_LANG_GLOBALREF_IMPLEMENTATION
#define X10_LANG_GLOBALREF_IMPLEMENTATION
#include <x10/lang/GlobalRef.h>


// ITable junk, both for GlobalRef and IBox<GlobalRef>
namespace x10 {
    namespace lang { 

        template<class T> class GlobalRef_methods  {
        public:
            static inline GlobalRef<T> _make(T obj) {
                return GlobalRef<T>(obj);
            }
            static void _constructor (x10::lang::GlobalRef<T> &this_, T t) {
                this_.value = (size_t) t.operator->();
            }
        };
        
        
        template<class T> class GlobalRef_ithunk0 : public x10::lang::GlobalRef<T> {
        public:
            static x10::lang::Any::itable<GlobalRef_ithunk0<T> > itable;
        };

        template<class T> x10::lang::Any::itable<GlobalRef_ithunk0<T> >
            GlobalRef_ithunk0<T>::itable(&GlobalRef<T>::equals,
                                         &GlobalRef<T>::hashCode,
                                         &GlobalRef<T>::toString,
                                         &GlobalRef_ithunk0<T>::typeName);

        template<class T> class GlobalRef_iboxithunk0 : public x10::lang::IBox<x10::lang::GlobalRef<T> > {
        public:
            static x10::lang::Any::itable<GlobalRef_iboxithunk0<T> > itable;
            x10_boolean equals(x10aux::ref<x10::lang::Any> arg0) {
                return this->value->equals(arg0);
            }
            x10_int hashCode() {
                return this->value->hashCode();
            }
            x10aux::ref<x10::lang::String> toString() {
                return this->value->toString();
            }
            x10aux::ref<x10::lang::String> typeName() {
                return this->value->typeName();
            }
        };

        template<class T> x10::lang::Any::itable<GlobalRef_iboxithunk0<T> >
            GlobalRef_iboxithunk0<T>::itable(&GlobalRef_iboxithunk0<T>::equals,
                                             &GlobalRef_iboxithunk0<T>::hashCode,
                                             &GlobalRef_iboxithunk0<T>::toString,
                                             &GlobalRef_iboxithunk0<T>::typeName);
    }
} 


template<class T> void x10::lang::GlobalRef<T>::_serialize(x10::lang::GlobalRef<T> this_,
                                                           x10aux::serialization_buffer& buf) {
    buf.write(this_->location);
    buf.write(this_->value);
    #if defined(X10_USE_BDWGC) || defined(X10_DEBUG_REFERENCE_LOGGER)
    if (this_->location == x10aux::here) {
        if (!this_->__apply().isNull()) logGlobalReference(this_->__apply());
    }
    #endif
}

template<class T> void x10::lang::GlobalRef<T>::_deserialize_body(x10aux::deserialization_buffer& buf) {
    location = buf.read<x10aux::place>();
    value = buf.read<x10_ulong>();
}


template<class T> x10_boolean x10::lang::GlobalRef<T>::_struct_equals(x10aux::ref<x10::lang::Any> that) {
    if ((!(x10aux::instanceof<x10::lang::GlobalRef<T> >(that)))) {
        return false;
    }
    return _struct_equals(x10aux::class_cast<x10::lang::GlobalRef<T> >(that));
}

template<class T> x10_boolean x10::lang::GlobalRef<T>::_struct_equals(x10::lang::GlobalRef<T> that) { 
    return (location == that->location) && x10aux::struct_equals(value, that->value);
}

template<class T> x10aux::ref<x10::lang::String> x10::lang::GlobalRef<T>::toString() {
    char* tmp = x10aux::alloc_printf("x10.lang.GlobalRef<%s>", x10aux::getRTT<T>()->name());
    return x10::lang::String::Steal(tmp);
}

template<class T> x10_int x10::lang::GlobalRef<T>::hashCode() {
    return (x10_int)value;
}

template<class T> x10aux::ref<x10::lang::String> x10::lang::GlobalRef<T>::typeName() {
    char* tmp = x10aux::alloc_printf("x10.lang.GlobalRef<%s>", x10aux::getRTT<T>()->name());
    return x10::lang::String::Steal(tmp);
}

template<class T> x10aux::RuntimeType x10::lang::GlobalRef<T>::rtt;

template<class T> x10aux::itable_entry x10::lang::GlobalRef<T>::_itables[2] = {x10aux::itable_entry(&x10aux::getRTT<x10::lang::Any>, &GlobalRef_ithunk0<T>::itable),
                                                                               x10aux::itable_entry(NULL, (void*)x10aux::getRTT<x10::lang::GlobalRef<T> >())};

template<class T> x10aux::itable_entry x10::lang::GlobalRef<T>::_iboxitables[2] = {x10aux::itable_entry(&x10aux::getRTT<x10::lang::Any>, &GlobalRef_iboxithunk0<T>::itable),
                                                                                   x10aux::itable_entry(NULL, (void*)x10aux::getRTT<x10::lang::GlobalRef<T> >())};

template<class T> void x10::lang::GlobalRef<T>::_initRTT() {
    const x10aux::RuntimeType *canonical = x10aux::getRTT<x10::lang::GlobalRef<void> >();
    if (rtt.initStageOne(canonical)) return;
    const x10aux::RuntimeType* parents[2] = { x10aux::getRTT<x10::lang::Any>(), x10aux::getRTT<x10::lang::Any>()};
    const x10aux::RuntimeType* params[1] = { x10aux::getRTT<T>()};
    x10aux::RuntimeType::Variance variances[1] = { x10aux::RuntimeType::invariant};
    const char *baseName = "x10.lang.GlobalRef";
    rtt.initStageTwo(baseName, x10aux::RuntimeType::struct_kind, 2, parents, 1, params, variances);
}
#endif // X10_LANG_GLOBALREF_IMPLEMENTATION
#endif // __X10_LANG_GLOBALREF_NODEPS
