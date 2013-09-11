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

#ifndef X10AUX_SERIALIZATION_H
#define X10AUX_SERIALIZATION_H


#include <x10aux/config.h>

#include <x10aux/captured_lval.h>
#include <x10aux/alloc.h>
#include <x10aux/deserialization_dispatcher.h>
#include <x10/lang/RuntimeNatives.h>

#include <limits>
#include <map>

#if defined(__GNUC__) && __GNUC__ >=4
#include <tr1/unordered_map>
#endif

/* --------------------- 
 * Serialization support
 * ---------------------
 *
 * There are three separate mechanisms for (de)serialization --
 *
 * 1) Built-in primitives
 *
 * 2) Structs
 *
 * 3) Instances of T* (Classes, boxed structs, closures)
 *
 *
 * The mechanism (1) copies raw implementation-dependent bytes to/from the stream and we will
 * discuss it no further.
 *
 *
 * The mechanism (2) invokes a static function S::_serialize to write the bytes to the stream,
 * and a static function S::_deserialize to read bytes from the stream and produce a S.
 * These functions must be generated (or hand-written) for every struct S.
 *
 *
 * The mechanism (3) has two subcases:
 *   the value is null
 *   the value is non-null
 * Null values are indicated by the serialization id 0.
 * Non-null values are indicated by writing a non-zero serialization id,
 * followed by a serialization of the fields (if any) of the value generated
 * by recursively following this algorithm. The serialization id is used to
 * dispatch to a function that will create an object of the appropriate concrete type
 * and then deserialize its fields.
 *
 * There is an additional complication of breaking cyclic object graphs by indicating
 * that we are about to read an object that has already been read.  This is indicated
 * by the serialization id 0xFFFF followed by an offset (in objects) to the
 * object which has been repeatedly serialized.
 *
 * Serialization ids are generated at runtime in a place-independent fashion.  Classes obtain their id by
 * registering a deserialization function with DeserializationDispatcher at initialization time, and
 * storing this id in a static field.  Every non-abstract class and every closure has its own id.
 * The virtual _get_serialization_id function returns this id for writing into the buffer.
 *
 * To write data (of any kind) we use the method serialization_buffer::write(data) which does the
 * right thing, no matter which of the 3 methods applies.
 * An internal cursor is incremented ready for the next write().  Note that a class's
 * _serialize_body function should also serialize its super class's representation, e.g. by
 * deferring to the super class's _serialize_body function.
 *
 * Deserialization is more complicated as an object has to be constructed.  In case (3) 
 * the DeserializationDispatcher is invoked to read an id from the stream and decide what to do.
 * Note that in such cases, the value has always been serialized from a matching variable on the sending side,
 * so an id will always be present.  During initialization time, classes register deserialization functions with the
 * DeserializationDispatcher, which hands out the unique ids.  Thus the DeserializationDispatcher
 * can look up the id in its internal table and dispatch to the appropriate static function which
 * will construct the right kind of object and initialize it by deserializing the object's
 * representation from the stream (provided as a serialization_buffer).
 *
 * Classes define a _deserialize_body function that extracts the object's representation from the
 * stream.  The DeserializationDispatcher callback should usually just
 * create an object of the right type and then call _deserialize_body to handle the rest.  Arbitrary
 * data can be extracted from a serialization_buffer via its read<T>() function.  An internal cursor
 * is incremented so the buffer is ready for the next read().  This function will do the right thing
 * no matter what T is supplied.  Note that classes need to deserialize their parent class's
 * representation too, e.g. by calling their parent's _deserialize_body function.  The two functions
 * _serialize_body and _deserialize_body are dual, and obviously they should be written to match
 * each other.
 *
 * Classes, closures, and boxed structs must call buf.record_reference(R) on the deserialization buffer
 * buf right after allocating the object in the DeserializationDispatcher callback
 * (where R is the newly allocated object).
 */


namespace x10 {
    namespace lang {
        class Reference;
        class Runtime__Profile;
    }
}

namespace x10aux {

	template <class T>
	class gc_allocator
	{
	public:
		typedef size_t size_type;
		typedef ptrdiff_t difference_type;
		typedef T* pointer;
		typedef const T* const_pointer;
		typedef T& reference;
		typedef const T& const_reference;
		typedef T value_type;

		template <class U>
		struct rebind {
			typedef gc_allocator<U> other;
		};

		gc_allocator() throw(){}
		gc_allocator(const gc_allocator&) throw(){}
		template <class U> gc_allocator(const gc_allocator<U>&) throw(){}
		~gc_allocator() throw(){}

		pointer allocate(size_type num, const void* hint = 0) {
			return x10aux::alloc<T>(num * sizeof(T), true);
		}
		void construct(pointer p, const T& value) {
			new( (void*)p ) T(value);
		}
		void deallocate(pointer p, size_type num) {
			x10aux::dealloc( p );
		}
		void destroy(pointer p) { p->~T(); }
		pointer address(reference value) const { return &value; }
		const_pointer address(const_reference value) const { return &value; }
		size_type max_size() const throw() {
			return std::numeric_limits<size_t>::max() / sizeof(T);
		}
	};

}

template <class T1, class T2>
bool operator==(const x10aux::gc_allocator<T1>&, const x10aux::gc_allocator<T2>&) throw() { return true; }

template <class T1, class T2>
bool operator!=(const x10aux::gc_allocator<T1>&, const x10aux::gc_allocator<T2>&) throw() { return false; }

namespace x10aux {

    // addr_map can be used to detect and properly handle cycles when serializing object graphs
    // it can also be used to avoid serializing two copies of an object when serializing a DAG.
    class addr_map {
        // NB: Must call x10aux::alloc here because _ptrs may hold the only live reference
        //     to temporary objects allocated by custom serialization serialize methods.
        //     If we don't keep those objects live here, the storage may be reused during the
        //     serialization operation, resulting in incorrect detection of a repeated reference!

#if defined(__GNUC__) && __GNUC__ >=4
    	typedef std::tr1::unordered_map<void*, void*, std::tr1::hash<void*>, std::equal_to<void*>, gc_allocator< std::pair<void*, void*> > > map_type;
#else
    	typedef std::map<void*, void*, std::hash<void*>, std::equal_to<void*>, gc_allocator< std::pair<void*, void*> > > map_type;
#endif
    	map_type map;

        void* _get_or_add(void* key, void* val);
        void _add(void* key, void* val);
        void* _get(void* key);
        void* _set(void* key, void* val);

    public:
        addr_map(int init_size = 4)
        { }
        /* Returns 0 if the pointer has not been recorded yet */
        template<class T> x10_long get_position_or_add(const T* r, x10_long pos) {
            x10_long prev_pos = (x10_long)_get_or_add((void*)r, (void*)pos);
            if (pos == prev_pos) {
                _S_("\t\tRecorded new reference "<<((void*)r)<<" of type "<<ANSI_SER<<ANSI_BOLD<<TYPENAME(T)<<ANSI_RESET<<" at "<<pos<<" in map: "<<this);
            } else {
                _S_("\t\tFound repeated reference "<<((void*)r)<<" of type "<<ANSI_SER<<ANSI_BOLD<<TYPENAME(T)<<ANSI_RESET<<" at "<<prev_pos<<" in map: "<<this);
            }
            return prev_pos;
        }
        template<class T> x10_long get_position(T* r) {
        	x10_long pos = (x10_long)_get((void*)r);
            _S_("\t\tRetrieving repeated reference "<<((void*) r)<<" of type "<<ANSI_SER<<ANSI_BOLD<<TYPENAME(T)<<ANSI_RESET<<" at "<<pos<<" (absolute) in map: "<<this);
            return pos;
        }
        template<class T> void add_at_position(const T* r, x10_long pos) {
            _add((void*)pos, (void*)r);
            if (pos == 0) {
                _S_("\t\tRecorded new reference "<<((void*)r)<<" of type "<<ANSI_SER<<ANSI_BOLD<<TYPENAME(T)<<ANSI_RESET<<" at "<<pos<<" (absolute) in map: "<<this);
            }
        }
        template<class T> T* get_at_position(x10_long pos) {
            T* val = (T*)_get((void*)pos);
            _S_("\t\tRetrieving repeated reference "<<((void*) val)<<" of type "<<ANSI_SER<<ANSI_BOLD<<TYPENAME(T)<<ANSI_RESET<<" at "<<pos<<" (absolute) in map: "<<this);
            return val;
        }
        template<class T> T* set_at_position(int pos, T* newval) {
            T* val = (T*)_set(pos, newval);
            _S_("\t\tReplacing repeated reference "<<((void*) val)<<" of type "<<ANSI_SER<<ANSI_BOLD<<TYPENAME(T)<<ANSI_RESET<<" at "<<(pos)<<" (absolute) in map: "<<this<<" by "<<((void*) newval));
            return val;
        }
    };



    // copy bytes with Endian encoding/decoding support
    inline void copy_bytes(void *dest, const void *src, size_t n) {
        #if !defined(HOMOGENEOUS) && (defined(__i386__) || defined(__x86_64__))
        unsigned char *x = (unsigned char*) dest;
        unsigned char *y = (unsigned char*) src;
        for (size_t i=0,j=n-1; i<n; ++i,--j) {
            x[i] = y[j];
        }
        #else
        memcpy(dest, src, n);
        #endif
    }

    // copy bulk data of size n of a given length, with Endian encoding/decoding support
    inline void copy_bulk(void *dest, const void *src, x10_long length, size_t n) {
        #if !defined(HOMOGENEOUS) && (defined(__i386__) && defined(__x86_64__))
        unsigned char *x = (unsigned char*) dest;
        unsigned char *y = (unsigned char*) src;
        for (x10_long k=0; k<length; k++) {
            for (size_t i=0,j=n-1; i<n; ++i,--j) {
                x[i] = y[j];
            }
            x += n; y += n;
        }
        #else
        memcpy(dest, src, length*n);
        #endif
    }

    // A growable buffer for serializing into
    class serialization_buffer {
    private:
        char *buffer;
        char *limit;
        char *cursor;
        addr_map map;
        bool size_flag;
        bool write_flag;

        void grow (size_t new_capacity);

    public:

        serialization_buffer (void) : buffer(NULL), limit(NULL), cursor(NULL), map(), size_flag(false), write_flag(false) {}

        ~serialization_buffer (void) {
            if (buffer!=NULL && write_flag == false) {
                x10aux::system_dealloc(buffer);
            }
        }

        void begin_count(void);
        void begin_write(char *buf, int count);

        void grow (void);

        size_t length (void) { return cursor - buffer; }
        size_t capacity (void) { return limit - buffer; }

        char *steal() { char *buf = buffer; buffer = NULL; return buf; }
        char *borrow() { return buffer; }

        static void serialize_reference(serialization_buffer &buf, x10::lang::Reference*);
        static void copyIn(serialization_buffer &buf, const void* data, x10_long length, size_t sizeOfT);

        template <class T> void manually_record_reference(T* val) {
            map.get_position_or_add(val, length());
        }
        
        // Default case for primitives and other things that never contain pointers
        template<class T> struct Write;
        template<class T> struct Write<T*>;
        template<class T> struct Write<captured_ref_lval<T> >;
        template<class T> struct Write<captured_struct_lval<T> >;
        template<typename T> void write(const T &val);

        // So it can access the addr_map
        template<class T> friend struct Write;
    };

    
    // Case for non-refs (includes simple primitives like x10_int and all structs)
    template<class T> struct serialization_buffer::Write {
        static void size(serialization_buffer &buf, const T &val);
        static void write(serialization_buffer &buf, const T &val);
    };
    // General case for structs
    template<class T> void serialization_buffer::Write<T>::size(serialization_buffer &buf,
                                                             const T &val) {
        T::_serialize(val,buf);
    }
    template<class T> void serialization_buffer::Write<T>::write(serialization_buffer &buf,
                                                             const T &val) {
        _S_("Serializing a "<<ANSI_SER<<ANSI_BOLD<<TYPENAME(T)<<ANSI_RESET<<" into buf: "<<&buf);
        size(buf,val);
    }
    // Specializations for the simple primitives
    #define PRIMITIVE_WRITE_AS_INT(TYPE) \
	template<> inline void serialization_buffer::Write<TYPE>::size(serialization_buffer &buf, \
																const TYPE &val) {\
		buf.cursor += sizeof(TYPE); \
	} \
    template<> inline void serialization_buffer::Write<TYPE>::write(serialization_buffer &buf, \
                                                                const TYPE &val) {\
        _S_("Serializing "<<star_rating<TYPE>()<<" a "<<ANSI_SER<<TYPENAME(TYPE)<<ANSI_RESET<<": " \
                          <<(int)val<<" into buf: "<<&buf); \
        /* *(TYPE*) buf.cursor = val; // Cannot do this because of alignment */ \
        if (buf.cursor + sizeof(TYPE) >= buf.limit) buf.grow(); \
        copy_bytes(buf.cursor, &val, sizeof(TYPE)); \
        buf.cursor += sizeof(TYPE); \
    }
    #define PRIMITIVE_WRITE(TYPE) \
	template<> inline void serialization_buffer::Write<TYPE>::size(serialization_buffer &buf, \
																const TYPE &val) {\
		buf.cursor += sizeof(TYPE); \
	} \
    template<> inline void serialization_buffer::Write<TYPE>::write(serialization_buffer &buf, \
                                                                const TYPE &val) {\
        _S_("Serializing "<<star_rating<TYPE>()<<" a "<<ANSI_SER<<TYPENAME(TYPE)<<ANSI_RESET<<": " \
                          <<val<<" into buf: "<<&buf); \
        /* *(TYPE*) buf.cursor = val; // Cannot do this because of alignment */ \
        if (buf.cursor + sizeof(TYPE) >= buf.limit) buf.grow(); \
        copy_bytes(buf.cursor, &val, sizeof(TYPE)); \
        buf.cursor += sizeof(TYPE); \
    }
    #define PRIMITIVE_VOLATILE_WRITE(TYPE) \
	template<> inline void serialization_buffer::Write<volatile TYPE>::size(serialization_buffer &buf, \
																const volatile TYPE &val) {\
		buf.cursor += sizeof(TYPE); \
	} \
    template<> inline void serialization_buffer::Write<volatile TYPE>::write(serialization_buffer &buf, \
                                                                         const volatile TYPE &val) {\
        _S_("Serializing "<<star_rating<TYPE>()<<" a volatile "<<ANSI_SER<<TYPENAME(TYPE)<<ANSI_RESET<<": " \
                          <<val<<" into buf: "<<&buf); \
        /* *(TYPE*) buf.cursor = val; // Cannot do this because of alignment */ \
        if (buf.cursor + sizeof(TYPE) >= buf.limit) buf.grow(); \
        copy_bytes(buf.cursor, const_cast<TYPE*>(&val), sizeof(TYPE)); \
        buf.cursor += sizeof(TYPE); \
    }
    PRIMITIVE_WRITE(x10_boolean)
    PRIMITIVE_WRITE_AS_INT(x10_byte)
    PRIMITIVE_WRITE_AS_INT(x10_ubyte)
    PRIMITIVE_WRITE(x10_char)
    PRIMITIVE_WRITE(x10_short)
    PRIMITIVE_WRITE(x10_ushort)
    PRIMITIVE_WRITE(x10_int)
    PRIMITIVE_WRITE(x10_uint)
    PRIMITIVE_WRITE(x10_long)
    PRIMITIVE_WRITE(x10_ulong)
    PRIMITIVE_WRITE(x10_float)
    PRIMITIVE_WRITE(x10_double)
    //PRIMITIVE_WRITE(x10_addr_t) // already defined above
        
    PRIMITIVE_VOLATILE_WRITE(x10_int)
    PRIMITIVE_VOLATILE_WRITE(x10_long)

    // Case for references e.g. Reference*
    template<class T> struct serialization_buffer::Write<T*> {
       	static void size(serialization_buffer &buf, T* val);
        static void write(serialization_buffer &buf, T* val);
    };
    template<class T> void serialization_buffer::Write<T*>::size(serialization_buffer &buf,
                                                                   T* val) {
        if (NULL != val) {
            x10_long cur_pos = buf.length();
            x10_long prev_pos = buf.map.get_position_or_add(val, cur_pos);
            if (prev_pos != cur_pos) {
                _S_("\tRepeated ("<<prev_pos<<") serialization of a "<<ANSI_SER<<ANSI_BOLD<<TYPENAME(T)<<ANSI_RESET<<" into buf: "<<&buf);
                buf.write((x10aux::serialization_id_t) 0xFFFF);
                buf.write((x10_int) prev_pos);
                return;
            }
        }
        x10::lang::Reference* valAsRef = reinterpret_cast<x10::lang::Reference*>(val);
        serialize_reference(buf, valAsRef);
    }
    template<class T> void serialization_buffer::Write<T*>::write(serialization_buffer &buf,
                                                                   T* val) {
        _S_("Serializing a "<<ANSI_SER<<ANSI_BOLD<<TYPENAME(T)<<ANSI_RESET<<" into buf: "<<&buf);
        size(buf,val);
    }

    inline void serialization_buffer::copyIn(serialization_buffer &buf,
                                             const void *data,
                                             x10_long length,
                                             size_t sizeOfT) {
        size_t numBytes = length * sizeOfT;
        if (!buf.size_flag) {
            if (buf.cursor + numBytes >= buf.limit) buf.grow(buf.length() + numBytes);
            copy_bulk(buf.cursor, data, length, sizeOfT);
        }
        buf.cursor += numBytes;
    }

    
    // Case for captured stack variables e.g. captured_ref_lval<T> and captured_struct_lval<T>.
    template<class T> struct serialization_buffer::Write<captured_ref_lval<T> > {
        static void size(serialization_buffer &buf, captured_ref_lval<T> val);
        static void write(serialization_buffer &buf, captured_ref_lval<T> val);
    };
    template<class T> void serialization_buffer::Write<captured_ref_lval<T> >::size(serialization_buffer &buf,
                                                                                 captured_ref_lval<T> val) {
        x10_long capturedAddress = val.capturedAddress();
        buf.write(capturedAddress);
    }
    template<class T> void serialization_buffer::Write<captured_ref_lval<T> >::write(serialization_buffer &buf,
                                                                                 captured_ref_lval<T> val) {
        _S_("Serializing a stack variable of type ref<"<<ANSI_SER<<ANSI_BOLD<<TYPENAME(T)<<ANSI_RESET<<"> into buf: "<<&buf);
        x10_long capturedAddress = val.capturedAddress();
        _S_("\tCaptured address is "<<((void*)capturedAddress));
        buf.write(capturedAddress);
    }
    template<class T> struct serialization_buffer::Write<captured_struct_lval<T> > {
        static void size(serialization_buffer &buf, captured_struct_lval<T> val);
        static void write(serialization_buffer &buf, captured_struct_lval<T> val);
    };
    template<class T> void serialization_buffer::Write<captured_struct_lval<T> >::size(serialization_buffer &buf,
                                                                                    captured_struct_lval<T> val) {
        x10_long capturedAddress = val.capturedAddress();
        buf.write(capturedAddress);
    }
    template<class T> void serialization_buffer::Write<captured_struct_lval<T> >::write(serialization_buffer &buf,
                                                                                    captured_struct_lval<T> val) {
        _S_("Serializing a stack variable of type "<<ANSI_SER<<ANSI_BOLD<<TYPENAME(T)<<ANSI_RESET<<" into buf: "<<&buf);
        x10_long capturedAddress = val.capturedAddress();
        _S_("\tCaptured address is "<<((void*)capturedAddress));
        buf.write(capturedAddress);
    }

    
    template<typename T> void serialization_buffer::write(const T &val) {
        if (size_flag) {
            Write<T>::size(*this,val);
        }
        else {
            Write<T>::write(*this,val);
        }
    }


    // A buffer from which we can deserialize x10 objects
    class deserialization_buffer {
    private:
        char* buffer;
        const char* cursor;
        addr_map map;
        size_t len;
    public:
        char *getBuffer() { return buffer; }
        size_t getLen() { return len; }

        deserialization_buffer(char *buffer_, size_t len_)
            : buffer(buffer_), cursor(buffer_), map(), len(len_)
        { }

        size_t consumed (void) { return cursor - buffer; }

        static x10::lang::Reference* deserialize_reference(deserialization_buffer &buf);
        static void copyOut(deserialization_buffer &buf, void* data, x10_long length, size_t sizeOfT);
        
        // Default case for primitives and other things that never contain pointers
        template<class T> struct Read;
        template<class T> struct Read<T*>;
        template<typename T> GPUSAFE T read();
        template<typename T> GPUSAFE T peek() {
            const char* saved_cursor = cursor;
            T val = read<T>();
            cursor = saved_cursor;
            return val;
        }
        // This has to be called every time a reference is created, but
        // before the rest of the object is deserialized!
        template<typename T> void record_reference(T* r);

        template<typename T> void update_reference(T* r, T* newr);

        // So it can access the addr_map
        template<class T> friend struct Read;
    };

    template<typename T> void deserialization_buffer::record_reference(T* r) {
        x10_long cur_pos = consumed() - 2; // back by the length of id (2 bytes)
        map.add_at_position(r, cur_pos);
        /*
        x10_long prev_pos = map.add_at_position(r, cur_pos);
        if (prev_pos != cur_pos) {
            _S_("\t"<<ANSI_SER<<ANSI_BOLD<<"OOPS!"<<ANSI_RESET<<" Attempting to repeatedly record a reference "<<((void*)r)<<" (already found at position "<<prev_pos<<") in buf: "<<this);
        }
        return !prev_pos;
        */
    }

    // TODO: I do not understand the purpose of this method.
    template<typename T> void deserialization_buffer::update_reference(T* r, T* newr) {
        x10_long cur_pos = consumed() - 2; // back by the length of id (2 bytes)
        map.set_at_position(map._get(r), newr);
        /*
        x10_long prev_pos = map.add_at_position(r, cur_pos);
        if (prev_pos == cur_pos) {
            _S_("\t"<<ANSI_SER<<ANSI_BOLD<<"OOPS!"<<ANSI_RESET<<" Attempting to update a nonexistent reference "<<((void*)r)<<" in buf: "<<this);
        }
        map.set_at_position(prev_pos, newr);
        */
    }
    
    // Case for non-refs (includes simple primitives like x10_int and all structs)
    template<class T> struct deserialization_buffer::Read {
        GPUSAFE static T _(deserialization_buffer &buf);
    };
    // General case for structs
    template<class T> T deserialization_buffer::Read<T>::_(deserialization_buffer &buf) {
        _S_("Deserializing a "<<ANSI_SER<<ANSI_BOLD<<TYPENAME(T)<<ANSI_RESET<<" from buf: "<<&buf);
        // Call the struct's static deserialization method
        return T::_deserialize(buf);
    }

    inline void deserialization_buffer::copyOut(deserialization_buffer &buf,
                                                void *data,
                                                x10_long length,
                                                size_t sizeOfT) {
        copy_bulk(data, buf.cursor, length, sizeOfT);
        buf.cursor += length * sizeOfT;
    }

    // Specializations for all simple primitives
    #define PRIMITIVE_READ_AS_INT(TYPE) \
    template<> inline TYPE deserialization_buffer::Read<TYPE>::_(deserialization_buffer &buf) { \
        /* //TYPE &val = *(TYPE*) buf.cursor; // Cannot do this because of alignment */ \
        TYPE val; \
        copy_bytes(&val, buf.cursor, sizeof(TYPE)); \
        buf.cursor += sizeof(TYPE); \
        _S_("Deserializing "<<star_rating<TYPE>()<<" a "<<ANSI_SER<<TYPENAME(TYPE)<<ANSI_RESET<<": " \
            <<(int)val<<" from buf: "<<&buf); \
        return val; \
    }
    #define PRIMITIVE_READ(TYPE) \
    template<> inline TYPE deserialization_buffer::Read<TYPE>::_(deserialization_buffer &buf) { \
        /* //TYPE &val = *(TYPE*) buf.cursor; // Cannot do this because of alignment */ \
        TYPE val; \
        copy_bytes(&val, buf.cursor, sizeof(TYPE)); \
        buf.cursor += sizeof(TYPE); \
        _S_("Deserializing "<<star_rating<TYPE>()<<" a "<<ANSI_SER<<TYPENAME(TYPE)<<ANSI_RESET<<": " \
            <<val<<" from buf: "<<&buf); \
        return val; \
    }
    PRIMITIVE_READ(x10_boolean)
    PRIMITIVE_READ_AS_INT(x10_byte)
    PRIMITIVE_READ_AS_INT(x10_ubyte)
    PRIMITIVE_READ(x10_char)
    PRIMITIVE_READ(x10_short)
    PRIMITIVE_READ(x10_ushort)
    PRIMITIVE_READ(x10_int)
    PRIMITIVE_READ(x10_uint)
    PRIMITIVE_READ(x10_long)
    PRIMITIVE_READ(x10_ulong)
    PRIMITIVE_READ(x10_float)
    PRIMITIVE_READ(x10_double)
    //PRIMITIVE_READ(x10_addr_t) // already defined above

    // Case for references e.g. Reference*, 
    template<class T> struct deserialization_buffer::Read<T*> {
        GPUSAFE static T* _(deserialization_buffer &buf);
    };
    template<class T> T* deserialization_buffer::Read<T*>::_(deserialization_buffer &buf) {
        _S_("Deserializing a "<<ANSI_SER<<ANSI_BOLD<<TYPENAME(T)<<ANSI_RESET<<" from buf: "<<&buf);
        x10aux::serialization_id_t code = buf.peek<x10aux::serialization_id_t>();
        if (code == (x10aux::serialization_id_t) 0xFFFF) {
            buf.read<x10aux::serialization_id_t>();
            int pos = (int) buf.read<x10_int>();
            _S_("\tRepeated ("<<pos<<") deserialization of a "<<ANSI_SER<<ANSI_BOLD<<TYPENAME(T)<<ANSI_RESET<<" from buf: "<<&buf);
            return buf.map.get_at_position<T>(pos);
        }
        // Deserialize it
        return reinterpret_cast<T*>(deserialize_reference(buf));
    }

    // Case for captured stack addresses, captured_ref_lval<T> and captured_struct_lval<T>
    template<class T> struct deserialization_buffer::Read<captured_ref_lval<T> > {
        GPUSAFE static captured_ref_lval<T> _(deserialization_buffer &buf);
    };
    template<class T> captured_ref_lval<T> deserialization_buffer::Read<captured_ref_lval<T> >::_(deserialization_buffer &buf) {
        _S_("Deserializing a stack variable of type ref<"<<ANSI_SER<<ANSI_BOLD<<TYPENAME(T)<<ANSI_RESET<<"> from buf: "<<&buf);
        x10_long addr = buf.read<x10_long>();
        _S_("\tCaptured address is "<<((void*)addr));
        x10aux::captured_ref_lval<T> result;
        result.setCapturedAddress(addr);
        return result;
    }
    template<class T> struct deserialization_buffer::Read<captured_struct_lval<T> > {
        GPUSAFE static captured_struct_lval<T> _(deserialization_buffer &buf);
    };
    template<class T> captured_struct_lval<T> deserialization_buffer::Read<captured_struct_lval<T> >::_(deserialization_buffer &buf) {
        _S_("Deserializing a stack variable of type "<<ANSI_SER<<ANSI_BOLD<<TYPENAME(T)<<ANSI_RESET<<" from buf: "<<&buf);
        x10_long addr = buf.read<x10_long>();
        _S_("\tCaptured address is "<<((void*)addr));
        x10aux::captured_struct_lval<T> result;
        result.setCapturedAddress(addr);
        return result;
    }

    
    template<typename T> GPUSAFE T deserialization_buffer::read() {
        return Read<T>::_(*this);
    }

    // avoid header inclusion problem, do this in a cc file
    void set_prof_data(x10::lang::Runtime__Profile *prof, unsigned long long bytes, unsigned long long nanos);

    template <class T> T deep_copy(T o, x10::lang::Runtime__Profile *prof) {
        serialization_buffer buf;
        unsigned long long before_nanos, before_bytes;
        if (prof!=NULL) {
            before_nanos = x10::lang::RuntimeNatives::nanoTime();
        }
        buf.write(o);
        deserialization_buffer buf2(buf.borrow(), buf.length());
        T res = buf2.read<T>();
        if (prof!=NULL) {
            set_prof_data(prof, buf.length(), x10::lang::RuntimeNatives::nanoTime() - before_nanos);
        }
        return res;
    }

    template <typename T> int count_ser_size(T* data, int offset, int count) {
        serialization_buffer buf;
        buf.begin_count();
        for (int i = 0; i < count; ++i) {
            buf.write(data[offset + i]);
        }
        return buf.length();
    }

    template <typename T> void write_ser_data(T* data, int data_offset, int data_count, signed char* imc_ptr, int imc_offset, int imc_count) {
        serialization_buffer buf;
        buf.begin_write((char*)imc_ptr + imc_offset, imc_count);
        for (int i = 0; i < data_count; ++i) {
            buf.write(data[data_offset + i]);
        }
    }

    template <typename T> void read_deser_data(T* data, int data_offset, int data_count, signed char* imc_ptr, int imc_offset, int imc_count) {
        deserialization_buffer buf((char*)imc_ptr + imc_offset, imc_count);
        for (int i = 0; i < data_count; ++i) {
            data[data_offset + i] = buf.read<T>();
        }
    }
}

#endif
// vim:tabstop=4:shiftwidth=4:expandtab:textwidth=100
