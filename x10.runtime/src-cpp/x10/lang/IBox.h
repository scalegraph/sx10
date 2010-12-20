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

#ifndef X10_LANG_IBOX_H
#define X10_LANG_IBOX_H

#include <x10/lang/IBox.struct_h>

namespace x10 {
    namespace lang {

        template <class T> inline x10aux::itable_entry* getITablesForIBox(T value) { return value->_getIBoxITables(); } 

        extern x10aux::itable_entry itable_Boolean[3];
        extern x10aux::itable_entry itable_Byte[3];
        extern x10aux::itable_entry itable_UByte[3];
        extern x10aux::itable_entry itable_Char[3];
        extern x10aux::itable_entry itable_Short[3];
        extern x10aux::itable_entry itable_UShort[3];
        extern x10aux::itable_entry itable_Int[3];
        extern x10aux::itable_entry itable_UInt[3];
        extern x10aux::itable_entry itable_Long[3];
        extern x10aux::itable_entry itable_ULong[3];
        extern x10aux::itable_entry itable_Float[3];
        extern x10aux::itable_entry itable_Double[3];
        
        inline x10aux::itable_entry *getITablesForIBox(x10_boolean) { return itable_Boolean; }
        inline x10aux::itable_entry *getITablesForIBox(x10_byte) { return itable_Byte; }
        inline x10aux::itable_entry *getITablesForIBox(x10_ubyte) { return itable_UByte; }
        inline x10aux::itable_entry *getITablesForIBox(x10_char) { return itable_Char; }
        inline x10aux::itable_entry *getITablesForIBox(x10_short) { return itable_Short; }
        inline x10aux::itable_entry *getITablesForIBox(x10_ushort) { return itable_UShort; }
        inline x10aux::itable_entry *getITablesForIBox(x10_int) { return itable_Int; }
        inline x10aux::itable_entry *getITablesForIBox(x10_uint) { return itable_UInt; }
        inline x10aux::itable_entry *getITablesForIBox(x10_long) { return itable_Long; }
        inline x10aux::itable_entry *getITablesForIBox(x10_ulong) { return itable_ULong; }
        inline x10aux::itable_entry *getITablesForIBox(x10_float) { return itable_Float; }
        inline x10aux::itable_entry *getITablesForIBox(x10_double) { return itable_Double; }

        template<class T> x10aux::itable_entry* IBox<T>::_getITables() { return getITablesForIBox(value); } 

        template<class T> const x10aux::RuntimeType* IBox<T>::_type() const { return x10aux::getRTT<T>(); }
        template<class T> const x10aux::RuntimeType* IBox<T>::getRTT() { return x10aux::getRTT<T>(); }

        template<class T> x10aux::ref<String> IBox<T>::toString() { return x10aux::to_string(value); }

        template<class T> x10_int IBox<T>::hashCode() { return x10aux::hash_code(value); }

        template <class T> x10_boolean IBox<T>::_struct_equals(x10aux::ref<Reference> other) {
            if (!other.isNull() && _type()->equals(other->_type())) {
                // implies that other is also an IBox<T>
                x10aux::ref<IBox<T> > otherAsIBox(other);
                return x10aux::struct_equals(value, otherAsIBox->value);
            } else {
                // If I'm an IBox<T> and the other guy is not an IBox<T> then has to be false.
                return false;
            }
        }
        
        template<class T> const x10aux::serialization_id_t x10::lang::IBox<T>::_serialization_id = 
            x10aux::DeserializationDispatcher::addDeserializer(x10::lang::IBox<T>::template _deserializer<x10::lang::Reference>, x10aux::CLOSURE_KIND_NOT_ASYNC);

        template<class T> void IBox<T>::_serialize_body(x10aux::serialization_buffer &buf) {
            buf.write(value);
        }
        
        template<class T> template<class __T> x10aux::ref<__T> x10::lang::IBox<T>::_deserializer(x10aux::deserialization_buffer& buf) {
            IBox<T> * storage = x10aux::alloc<IBox<T> >();
            buf.record_reference(x10aux::ref<IBox<T> >(storage));
            T tmp = buf.read<T>();
            x10aux::ref<x10::lang::IBox<T> > this_ = new (storage) x10::lang::IBox<T>(tmp);
            return this_;
        }
    }
}

#endif
