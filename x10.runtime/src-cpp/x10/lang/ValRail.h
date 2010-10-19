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

#ifndef X10_LANG_VALRAIL_H
#define X10_LANG_VALRAIL_H

#include <x10aux/config.h>
#include <x10aux/alloc.h>
#include <x10aux/RTT.h>
#include <x10aux/rail_utils.h>
#include <x10aux/basic_functions.h>

#include <x10/lang/Object.h>
#include <x10/lang/Fun_0_1.h>
#include <x10/lang/Iterable.h>

namespace x10 {

    namespace lang {

        void _initRTTHelper_ValRail(x10aux::RuntimeType *location, const x10aux::RuntimeType *element,
                                    const x10aux::RuntimeType *p1, const x10aux::RuntimeType *p2);

        template<class T> class ValRail : public Object {

            public:
            RTT_H_DECLS_CLASS;

            static typename Iterable<T>::template itable<ValRail<T> > _itable_iterable;
            static typename Fun_0_1<x10_int, T>::template itable<ValRail<T> > _itable_fun;
            static x10aux::itable_entry _itables[3];
            virtual x10aux::itable_entry* _getITables() { return _itables; }
    
            private:

            ValRail(const ValRail<T>& arr); // disabled

            public:

            // 32 bit array indexes
            const x10_int FMGL(length);

            // Support length() property method.
            // TODO: If we eliminate auto-generated property getters, then we should delete this method
            GPUSAFE x10_int length() { return FMGL(length); }
            
            // The ValRail's data.
            // As a locality optimization, we are going to allocate all of the storage for the
            // Rail object and its data array contiguously (ie, in a single allocate call),
            // but to avoid making assumptions about the C++ object model, we will always
            // access it via this pointer instead of using the data[1] "struct hack."
            // This may cost us an extra load instruction (but no extra cache misses).
            // By declaring the pointer const, we should enable the C++ compiler to be reasonably
            // effective at hoisting this extra load out of loop nests.
            T* const _data;

            ValRail() : FMGL(length)(0),  _data(NULL) { }
            ValRail(x10_int length_, T* storage) : FMGL(length)(length_),  _data(storage) {}

            GPUSAFE T apply(x10_int index) {
                return operator[](index);
            }   

            GPUSAFE T& operator[](x10_int index) {
                x10aux::checkRailBounds(index, FMGL(length));
                return _data[index];
            }
      
            T* raw() { return _data; }

            virtual x10aux::ref<x10::lang::Iterator<T> > iterator();

            virtual x10_int hashCode() { return 0; }

            virtual x10_boolean equals(x10aux::ref<Any> other);
            
            virtual x10aux::ref<x10::lang::String> toString();

            static x10aux::ref<ValRail<T> > make(x10_int length) {
                return makeAligned(length, 8);
            }
            static x10aux::ref<ValRail<T> > makeAligned(x10_int length, x10_int alignment);

            static x10aux::ref<ValRail<T> > make(x10_int length,
                                                 x10aux::ref<Fun_0_1<x10_int,T> > init) {
                return makeAligned(length, init, 8);
            }
            static x10aux::ref<ValRail<T> > makeAligned(x10_int length,
                                                        x10aux::ref<Fun_0_1<x10_int,T> > init,
                                                        x10_int alignment);

            static x10aux::ref<ValRail<T> > make(x10aux::ref<Rail<T> > other) {
                return makeAligned(other, 8);
            }
            static x10aux::ref<ValRail<T> > makeAligned(x10aux::ref<Rail<T> > other, x10_int alignment);

            // All ValRails are mortal
            virtual x10_boolean _isMortal() { return true; }

            // ValRail will take care of deserializing remote objects properly
            virtual bool _custom_deserialization() { return true; }

            static const x10aux::serialization_id_t _serialization_id;

            virtual x10aux::serialization_id_t _get_serialization_id() { return _serialization_id; };

            static void _serialize(x10aux::ref<ValRail<T> > this_,
                                   x10aux::serialization_buffer &buf);

            void _serialize_body(x10aux::serialization_buffer &buf);

            void _deserialize_body(x10aux::deserialization_buffer &buf);

            template<class S> static x10aux::ref<S> _deserializer(x10aux::deserialization_buffer &buf);

            template<class S> static x10aux::ref<S> _deserialize(x10aux::deserialization_buffer &buf);
        };

    }
}
#endif

#ifndef __X10_LANG_VALRAIL_H_NODEPS
#define __X10_LANG_VALRAIL_H_NODEPS
#include <x10/lang/ValRail__RailIterator.h>
#include <x10aux/itables.h>
#ifndef X10_LANG_VALRAIL_H_IMPLEMENTATION
#define X10_LANG_VALRAIL_H_IMPLEMENTATION

namespace x10 {
    namespace lang {
        
        template<class T> const x10aux::serialization_id_t ValRail<T>::_serialization_id =
            x10aux::DeserializationDispatcher
                ::addDeserializer(ValRail<T>::template _deserializer<Reference>);

        template<class T> void ValRail<T>::_initRTT() {
            if (rtt.initStageOne(x10aux::getRTT<ValRail<void> >())) return;
            x10::lang::_initRTTHelper_ValRail(&rtt, x10aux::getRTT<T>(),
                                              x10aux::getRTT<Fun_0_1<x10_int,T> >(),
                                              x10aux::getRTT<Iterable<T> >());
        }

        template<class T> x10aux::RuntimeType ValRail<T>::rtt;

        template<> class ValRail<void> {
        public:
            static x10aux::RuntimeType rtt;
            static const x10aux::RuntimeType* getRTT() { return &rtt; }
        };

        template<class T> typename Iterable<T>::template itable<ValRail<T> > ValRail<T>::_itable_iterable(&ValRail<T>::equals,
                                                                                                          &ValRail<T>::hashCode,
                                                                                                          &ValRail<T>::iterator,
                                                                                                          &ValRail<T>::toString,
                                                                                                          &ValRail<T>::typeName);

        template<class T> typename Fun_0_1<x10_int,T>::template itable<ValRail<T> > ValRail<T>::_itable_fun(&ValRail<T>::apply,
                                                                                                            &ValRail<T>::equals,
                                                                                                            &ValRail<T>::hashCode,
                                                                                                            &ValRail<T>::toString,
                                                                                                            &ValRail<T>::typeName);

        template<class T> x10aux::itable_entry ValRail<T>::_itables[3] = {
            x10aux::itable_entry(&x10aux::getRTT<Iterable<T> >, &ValRail<T>::_itable_iterable),
            x10aux::itable_entry(&x10aux::getRTT<Fun_0_1<x10_int, T> >, &ValRail<T>::_itable_fun),
            x10aux::itable_entry(NULL,  (void*)x10aux::getRTT<ValRail<T> >())
        };

        template<class T> x10_boolean ValRail<T>::equals(x10aux::ref<Any> other) {
            if (!_type()->concreteInstanceOf(other))
                return false;
            x10aux::ref<ValRail<T> > that = (x10aux::ref<ValRail<T> >)other;
            if (this->FMGL(length) != that->FMGL(length)) return false;
            for (int i=0; i<this->FMGL(length); i++) {
                if (!x10aux::equals(this->_data[i], x10aux::class_cast_unchecked<x10aux::ref<x10::lang::Any> >(that->_data[i]))) return false;
            }
            return true;
        }

        template<class T> x10aux::ref<ValRail<T> > ValRail<T>::makeAligned(x10_int length, x10_int alignment) {
            x10aux::ref<ValRail<T> > rail = x10aux::alloc_aligned_rail<T,ValRail<T> >(length, alignment);
            // Memset both for efficiency and to allow T to be a struct.
            memset(rail->raw(), 0, length * sizeof(T));
            return rail;
        }

        template<class T> x10aux::ref<ValRail<T> > ValRail<T>::makeAligned(x10_int length,
                                                                    x10aux::ref<Fun_0_1<x10_int,T> > init,
                                                                    x10_int alignment) {
            x10aux::ref<ValRail<T> > rail = x10aux::alloc_aligned_rail<T,ValRail<T> >(length, alignment);
            x10aux::ref<x10::lang::Reference> initAsRef = init;
            typename Fun_0_1<x10_int,T>::template itable<x10::lang::Reference> *it = x10aux::findITable<Fun_0_1<x10_int,T> >(initAsRef->_getITables());
            for (x10_int i=0 ; i<length ; ++i) {
                (*rail)[i] = (initAsRef.operator->()->*(it->apply))(i);
            }
            return rail;
        }

        template <class T> x10aux::ref<ValRail<T> > ValRail<T>::makeAligned(x10aux::ref<Rail<T> > other, x10_int alignment) {
            x10aux::nullCheck(other);
            x10_int length = other->FMGL(length);
            x10aux::ref<ValRail<T> > rail = x10aux::alloc_aligned_rail<T,ValRail<T> >(length, alignment);
            for (x10_int i=0 ; i<length ; ++i) {
                (*rail)[i] = (*other)[i];
            }
            return rail;
        }

        template <class T> x10aux::ref<x10::lang::String> ValRail<T>::toString() {
            return x10aux::railToString<T>(FMGL(length), raw());
        }
        
        template <class T> x10aux::ref<Iterator<T> > ValRail<T>::iterator() {
            return ValRail__RailIterator<T>::_make(this);
        }

        // Specialized serialization
        template <class T> void ValRail<T>::_serialize(x10aux::ref<ValRail<T> > this_,
                                                       x10aux::serialization_buffer &buf) {
            Object::_serialize_reference(this_, buf);
            if (!this_.isNull()) {
                this_->_serialize_body(buf);
            }
        }

        template <class T> void ValRail<T>::_serialize_body(x10aux::serialization_buffer &buf) {
            x10_int length = this->FMGL(length);
            buf.write(length);
            this->Object::_serialize_body(buf); // intentional change of order
            T* raw = this->raw();
            for (x10_int i=0 ; i<length ; ++i) {
                buf.write(raw[i]); // avoid bounds check
            }
        }

        template <class T> void ValRail<T>::_deserialize_body(x10aux::deserialization_buffer &buf) {
            // length read out earlier, in _deserializer()
            this->Object::_deserialize_body(buf);
            x10_int length = this->FMGL(length);
            T* raw = this->raw();
            for (x10_int i=0 ; i<length ; ++i) {
                raw[i] = buf.read<T>(); // avoid bounds check
            }
        }

        template <class T> template<class S> x10aux::ref<S> ValRail<T>::_deserializer(x10aux::deserialization_buffer &buf) {
            x10_int length = buf.read<x10_int>();
            x10aux::ref<ValRail<T> > this_ = x10aux::alloc_rail<T,ValRail<T> >(length);
            buf.record_reference(this_); 
            this_->_deserialize_body(buf);
            return this_;
        }

        // Specialized deserialization
        template <class T> template<class S> x10aux::ref<S> ValRail<T>::_deserialize(x10aux::deserialization_buffer &buf) {
            Object::_reference_state rr = Object::_deserialize_reference_state(buf);
            if (0 == rr.ref) {
                return X10_NULL;
            } else {
                x10aux::ref<ValRail<T> > res = ValRail<T>::template _deserializer<ValRail<T> >(buf);
                _S_("Deserialized a "<<ANSI_SER<<ANSI_BOLD<<"class"<<ANSI_RESET<<
                    " "<<res->_type()->name());
                return res;
            }
        }
    }
}

#endif
#endif

// vim:tabstop=4:shiftwidth=4:expandtab
