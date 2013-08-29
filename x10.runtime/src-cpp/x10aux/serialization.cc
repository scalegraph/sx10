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

#include <x10aux/serialization.h>
#include <x10aux/network.h>

#include <x10/lang/Reference.h>

#include <x10/lang/Runtime__Profile.h>

using namespace x10aux;
using namespace x10::lang;

void* addr_map::_get_or_add(void* key, void* val) {
	map_type::iterator it = map.find(key);
	if(it != map.end()) {
		map.insert( std::pair<void*, void*>( key, val ) );
		return val;
	}
    return it->second;
}

void addr_map::_add(void* key, void* val) {
	assert(map.find(key) == map.end());
	map[key] = val;
}

void* addr_map::_get(void* key) {
	return map[key];
}

void* addr_map::_set(void* key, void* val) {
    void* old = map[key];
    map[key] = val;
    return old;
}

void serialization_buffer::begin_count(void) {
    size_flag = true;
    buffer = cursor = NULL;
}

void serialization_buffer::begin_write(char *buf, int count) {
    size_flag = false;
    write_flag = true;
    buffer = cursor = buf;
    limit = cursor + count + 1;
}

void serialization_buffer::grow (void) {
    size_t old_capacity = capacity();
    size_t new_capacity = (size_t) (old_capacity * 2.0); // increase capacity by a factor
    if (new_capacity<16) new_capacity = 16; // biggest primitive we might serialise -- a SIMD variable

    grow(new_capacity);
}

void serialization_buffer::grow (size_t new_capacity) {
    size_t new_length = length(); // no change in used portion of buffer
    
    buffer = (char*)x10aux::system_realloc(buffer, new_capacity);

    // update pointers to use (potentially) new buffer
    limit = buffer + new_capacity;
    cursor = buffer + new_length;
}

void serialization_buffer::serialize_reference(serialization_buffer &buf,
                                               x10::lang::Reference* this_) {
    if (NULL == this_) {
        _S_("Serializing a "<<ANSI_SER<<ANSI_BOLD<<"null reference"<<ANSI_RESET<<" to buf: "<<&buf);
        buf.write((x10aux::serialization_id_t)0);
    } else {
        x10aux::serialization_id_t id = this_->_get_serialization_id();
        _S_("Serializing id "<<id<<" of type "<< ANSI_SER<<ANSI_BOLD<<this_->_type()->name()<<"and address "<<(void*)(this_));
        buf.write(id);
        this_->_serialize_body(buf);
        _S_("Completed serialization of "<<(void*)(this_));
    }
}

Reference* deserialization_buffer::deserialize_reference(deserialization_buffer &buf) {
    x10aux::serialization_id_t id = buf.read<x10aux::serialization_id_t>();
    if (id == 0) {
        _S_("Deserialized a "<<ANSI_SER<<ANSI_BOLD<<"null reference"<<ANSI_RESET);
        return NULL;
    } else {
        _S_("Deserializing non-null value with id "<<ANSI_SER<<ANSI_BOLD<<id<<ANSI_RESET<<" from buf: "<<&buf);
        return x10aux::DeserializationDispatcher::create(buf, id);
    }
}


void x10aux::set_prof_data(x10::lang::Runtime__Profile *prof, unsigned long long bytes, unsigned long long nanos)
{
    prof->FMGL(bytes) += bytes;
    prof->FMGL(serializationNanos) += nanos;
}

// vim:tabstop=4:shiftwidth=4:expandtab:textwidth=100

