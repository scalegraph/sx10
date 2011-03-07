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

#ifndef X10_IO_FILEINPUTSTREAM_H
#define X10_IO_FILEINPUTSTREAM_H

#include <x10/io/InputStreamReader__InputStream.h>
#include <x10aux/io/FILEPtrInputStream.h>

namespace x10 {

    namespace io {

        class FileReader__FileInputStream : public InputStreamReader__InputStream {
        protected:
            x10aux::io::FILEPtrInputStream _inputStream;
            
        public:
            RTT_H_DECLS_CLASS;

            FileReader__FileInputStream(FILE *f) : _inputStream(f) { } 
            FileReader__FileInputStream() : _inputStream(NULL) { } 

            static x10aux::ref<FileReader__FileInputStream> _make(x10aux::ref<x10::lang::String> name);

            void _constructor (x10aux::ref<x10::lang::String> file);
            void _constructor (FILE* file);
            void _constructor ();

            virtual char * gets(char *buf, int sz) {
                return _inputStream.gets(buf,sz);
            }

            virtual void close() {
                _inputStream.close();
            }

            virtual x10_int read() {
                return _inputStream.read();
            }

            virtual x10_int read(x10aux::ref<x10::lang::Rail<x10_byte> > b,
                                 x10_int off,
                                 x10_int len) {
                return _inputStream.read(b, off, len);
            }

            virtual void skip(x10_int bytes) {
                return _inputStream.skip(bytes);
            }

            // Serialization
            static const x10aux::serialization_id_t _serialization_id;
            virtual x10aux::serialization_id_t _get_serialization_id() {
                return _serialization_id;
            }
            virtual void _serialize_body(x10aux::serialization_buffer& buf);
            template<class __T> static x10aux::ref<__T> _deserializer(x10aux::deserialization_buffer& buf);
            void _deserialize_body(x10aux::deserialization_buffer& buf);
            // No specialized serialization methods - not optimizing this final class
        };

        template<class __T> x10aux::ref<__T> FileReader__FileInputStream::_deserializer(x10aux::deserialization_buffer& buf) {
            // TODO: attempting to serialize _outputStream is nonsensical.
            //       The old 1.7 definition of this class simply didn't work either,
            //       it just silently didn't serialize the FILEPtrInputSteam field.
            // assert(false);
            x10aux::ref<FileReader__FileInputStream> this_ = new (x10aux::alloc<FileReader__FileInputStream>()) FileReader__FileInputStream(NULL);
            buf.record_reference(this_);
            this_->_deserialize_body(buf);
            return this_;
        }
    }
}

#endif
// vim:tabstop=4:shiftwidth=4:expandtab
