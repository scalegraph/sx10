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

#include <x10aux/alloc.h>
#include <x10aux/class_cast.h>
#include <x10aux/serialization.h>
#include <x10aux/basic_functions.h>
#include <x10aux/throw.h>
#include <x10aux/hash.h>
#include <x10aux/char_utils.h>

#include <x10/lang/String.h>
#include <x10/lang/Rail.h>
#include <x10/lang/StringIndexOutOfBoundsException.h>

#include <x10/array/Array.h>

#include <cstdarg>
#include <sstream>

using namespace std;
using namespace x10::lang;
using namespace x10aux;

static void throwStringIndexOutOfBoundsException(x10_int index, x10_int length) {
#ifndef NO_EXCEPTIONS
    char *msg = alloc_printf("index = %ld; length = %ld", (long)index, ((long)length));
    throwException(x10::lang::StringIndexOutOfBoundsException::_make(String::Lit(msg)));
#endif
}

static inline void checkStringBounds(x10_int index, x10_int length) {
#ifndef NO_BOUNDS_CHECKS
    if (((x10_uint)index) >= ((x10_uint)length)) {
        throwStringIndexOutOfBoundsException(index, length);
    }
#endif
}

void
String::_constructor(const char *content, bool steal) {
    size_t len = strlen(content);
    if (!steal) content = string_utils::strdup(content);
    this->Object::_constructor();
    this->FMGL(content) = content;
    this->FMGL(content_length) = len;
}

void
String::_constructor() {
    this->Object::_constructor();
    this->FMGL(content) = "";
    this->FMGL(content_length) = 0;
}

void
String::_constructor(x10aux::ref<String> s) {
    nullCheck(s);
    this->Object::_constructor();
    this->FMGL(content) = s->FMGL(content);
    this->FMGL(content_length) = s->FMGL(content_length);
}

void
String::_constructor(x10aux::ref<Rail<x10_char> > rail, x10_int start, x10_int length) {
    nullCheck(rail);
    x10_int i = 0;
    char *content= x10aux::alloc<char>(length+1);
    for (i=0; i<length; i++) {
        content[i] = (char)((*rail)[start + i].v);
    }
    content[i] = '\0';
    this->Object::_constructor();
    this->FMGL(content) = content;
    this->FMGL(content_length) = i;
}

void
String::_constructor(x10aux::ref<x10::array::Array<x10_byte> > array, x10_int start, x10_int length) {
    nullCheck(array);
    x10_int i = 0;
    char *content= x10aux::alloc<char>(length+1);
    for (i=0; i<length; i++) {
        content[i] = (char)(array->raw()[start + i]);
    }
    content[i] = '\0';
    this->Object::_constructor();
    this->FMGL(content) = content;
    this->FMGL(content_length) = i;
}

void
String::_constructor(x10aux::ref<x10::array::Array<x10_char> > array, x10_int start, x10_int length) {
    nullCheck(array);
    x10_int i = 0;
    char *content= x10aux::alloc<char>(length+1);
    for (i=0; i<length; i++) {
        content[i] = (char)(array->raw()[start + i].v);
    }
    content[i] = '\0';
    this->Object::_constructor();
    this->FMGL(content) = content;
    this->FMGL(content_length) = i;
}

x10_int String::hashCode() {
    x10_int hc = 0;
    x10_int l = length();
    const unsigned char* k = reinterpret_cast<const unsigned char*>(FMGL(content));
    for (; l > 0; k++, l--) {
        hc *= 31;
        hc += (x10_int) *k;
    }
    return hc;
}

static const char *strnstrn(const char *haystack, size_t haystack_sz,
                            const char *needle, size_t needle_sz)
{
    if (haystack_sz < needle_sz)
        return NULL;
    for (size_t i = 0; i <= haystack_sz-needle_sz; ++i) {
        if (!strncmp(&haystack[i], needle, needle_sz))
            return &haystack[i];
    }
    return NULL;
}

x10_int String::indexOf(ref<String> str, x10_int i) {
    nullCheck(str);
    if (i<0) i = 0;
    if (((size_t)i) >= FMGL(content_length)) return -1;

    const char *needle = str->FMGL(content);
    size_t needle_sz = str->FMGL(content_length);
    const char *haystack = &FMGL(content)[i];
    size_t haystack_sz = FMGL(content_length) - i;
    const char *pos = strnstrn(haystack, haystack_sz, needle, needle_sz);
    if (pos == NULL)
        return (x10_int) -1;
    return i + (x10_int) (pos - haystack);
}

static const char *strnchr(const char *haystack, size_t haystack_sz, char needle)
{
    for (size_t i = 0; i < haystack_sz; ++i) {
        if (haystack[i] == needle)
            return &haystack[i];
    }
    return NULL;
}

x10_int String::indexOf(x10_char c, x10_int i) {
    if (i < 0) i = 0;
    if (((size_t)i) >= FMGL(content_length)) return -1;

    int needle = (int)c.v;
    const char *haystack = &FMGL(content)[i];
    size_t haystack_sz = FMGL(content_length) - i;
    const char *pos = strnchr(haystack, haystack_sz, needle);
    if (pos == NULL)
        return (x10_int) -1;
    return i + (x10_int) (pos - haystack);
}

// [DC] Java defines whitespace as any unicode codepoint <= U0020
// ref: javadoc for java.lang.String.trim()
static bool isws (char x) { return x <= 0x20; }

x10aux::ref<String> String::trim() {
    const char *start = FMGL(content);
    x10_int l = FMGL(content_length);
    bool didSomething = false;
    if (l==0) { return this; } // string is empty
    while (isws(start[0]) && l>0) { start++; l--; didSomething = true; }
    while (isws(start[l-1]) && l>0) { l--; didSomething = true; }
    if (!didSomething) { return this; }
    char *trimmed = string_utils::strndup(start, l);
    return _make(trimmed, true);
}

static const char *strnrstrn(const char *haystack, size_t haystack_sz,
                             const char *needle, size_t needle_sz)
{
    if (haystack_sz < needle_sz)
        return NULL;
    for (size_t i = haystack_sz-needle_sz; i > 0; --i) {
        if (!strncmp(&haystack[i], needle, needle_sz))
            return &haystack[i];
    }
    if (!strncmp(haystack, needle, needle_sz))
        return haystack;
    return NULL;
}

x10_int String::lastIndexOf(ref<String> str, x10_int i) {
    nullCheck(str);
    if (i < 0) i = 0;
    if (((size_t)i) >= FMGL(content_length)) return -1;

    const char *needle = str->FMGL(content);
    size_t needle_sz = str->FMGL(content_length);
    const char *haystack = FMGL(content);
    size_t haystack_sz = (size_t)i+1;
    const char *pos = strnrstrn(haystack, haystack_sz, needle, needle_sz);
    if (pos == NULL)
        return (x10_int) -1;
    return (x10_int) (pos - haystack);
}

static const char *strnrchr(const char *haystack, size_t haystack_sz, int needle) {
    if (haystack_sz == 0)
        return NULL;
    for (size_t i = haystack_sz-1; i > 0; --i) {
        if (haystack[i] == needle)
            return &haystack[i];
    }
    if (*haystack == needle)
        return haystack;
    return NULL;
}

x10_int String::lastIndexOf(x10_char c, x10_int i) {
    if (i < 0) i = 0;
    if (((size_t)i) >= FMGL(content_length)) return -1;

    int needle = (int)c.v;
    const char *haystack = FMGL(content);
    size_t haystack_sz = (size_t)i+1;
    const char *pos = strnrchr(haystack, haystack_sz, needle);
    if (pos == NULL)
        return (x10_int) -1;
    return (x10_int) (pos - haystack);
}

ref<String> String::substring(x10_int start, x10_int end) {
#ifndef NO_BOUNDS_CHECKS
    if (start < 0) throwStringIndexOutOfBoundsException(start, FMGL(content_length));
    if (start > end) throwStringIndexOutOfBoundsException(start, end);
    if (((size_t)end) > FMGL(content_length)) throwStringIndexOutOfBoundsException(end, FMGL(content_length));
#endif
    size_t sz = end - start;
    char *str = x10aux::alloc<char>(sz+1);
    for (size_t i = 0; i < sz; ++i)
        str[i] = FMGL(content)[start+i];
    str[sz] = '\0';
    return String::Steal(str);
}

static ref<x10::array::Array<ref<String> > > split_all_chars(String* str) {
    size_t sz = (size_t)str->length();
    ref<x10::array::Array<ref<String> > > array = x10::array::Array<ref<String> >::_make(sz);
    for (size_t i = 0; i < sz; ++i) {
        array->__set(str->substring(i, i+1), i);
    }
    return array;
}

// FIXME: this does not treat pat as a regex
ref<x10::array::Array<ref<String> > > String::split(ref<String> pat) {
    nullCheck(pat);
    int l = pat->length();
    if (l == 0) // if splitting on an empty string, just return the chars
        return split_all_chars(this);
    int sz = 1; // we have at least one string
    int i = -1; // count first
    while ((i = indexOf(pat, i+l)) != -1) {
        sz++;
    }
    ref<x10::array::Array<ref<String> > > array = x10::array::Array<ref <String> >::_make(sz);
    int c = 0;
    int o = 0; // now fill in the array
    while ((i = indexOf(pat, o)) != -1) {
        array->__set(substring(o, i), c++);
        o = i+l;
    }
    array->__set(substring(o), c++);
    assert (c == sz);
    return array;
}

x10_char String::charAt(x10_int i) {
    checkStringBounds(i, FMGL(content_length));
    return (x10_char) FMGL(content)[i];
}


ref<x10::array::Array<x10_char> > String::chars() {
    x10_int sz = length();
    ref<x10::array::Array<x10_char> > array = x10::array::Array<x10_char>::_make(sz);
    for (int i = 0; i < sz; ++i)
        array->__set((x10_char) FMGL(content)[i], i);
    return array;
}

ref<x10::array::Array<x10_byte> > String::bytes() {
    x10_int sz = length();
    ref<x10::array::Array<x10_byte> > array = x10::array::Array<x10_byte>::_make(sz);
    for (int i = 0; i < sz; ++i)
        array->__set(FMGL(content)[i], i); 
    return array;
}

void String::_formatHelper(std::ostringstream &ss, char* fmt, ref<Any> p) {
    char* buf = NULL;
    if (p.isNull()) {
        ss << (buf = x10aux::alloc_printf(fmt, "null")); // FIXME: Ignore nulls for now
    } else if (x10aux::instanceof<ref<String> >(p)) {
        ss << (buf = x10aux::alloc_printf(fmt, class_cast_unchecked<ref<String> >(p)->c_str()));
    } else if (x10aux::instanceof<ref<Object> >(p)) {
        ss << (buf = x10aux::alloc_printf(fmt, class_cast_unchecked<ref<Object> >(p)->toString()->c_str()));
    } else if (x10aux::instanceof<x10_boolean>(p)) {
        ss << (buf = x10aux::alloc_printf(fmt, class_cast_unchecked<x10_boolean>(p)));
    } else if (x10aux::instanceof<x10_byte>(p)) {
        ss << (buf = x10aux::alloc_printf(fmt, class_cast_unchecked<x10_byte>(p)));
    } else if (x10aux::instanceof<x10_ubyte>(p)) {
        ss << (buf = x10aux::alloc_printf(fmt, class_cast_unchecked<x10_ubyte>(p)));
    } else if (x10aux::instanceof<x10_char>(p)) {
        ss << (buf = x10aux::alloc_printf(fmt, class_cast_unchecked<x10_char>(p).v));
    } else if (x10aux::instanceof<x10_short>(p)) {
        ss << (buf = x10aux::alloc_printf(fmt, class_cast_unchecked<x10_short>(p)));
    } else if (x10aux::instanceof<x10_ushort>(p)) {
        ss << (buf = x10aux::alloc_printf(fmt, class_cast_unchecked<x10_ushort>(p)));
    } else if (x10aux::instanceof<x10_int>(p)) {
        ss << (buf = x10aux::alloc_printf(fmt, class_cast_unchecked<x10_int>(p)));
    } else if (x10aux::instanceof<x10_uint>(p)) {
        ss << (buf = x10aux::alloc_printf(fmt, class_cast_unchecked<x10_uint>(p)));
    } else if (x10aux::instanceof<x10_long>(p)) {
        ss << (buf = x10aux::alloc_printf(fmt, class_cast_unchecked<x10_ulong>(p)));
    } else if (x10aux::instanceof<x10_float>(p)) {
        ss << (buf = x10aux::alloc_printf(fmt, class_cast_unchecked<x10_float>(p)));
    } else if (x10aux::instanceof<x10_double>(p)) {
        ss << (buf = x10aux::alloc_printf(fmt, class_cast_unchecked<x10_double>(p)));
    } else {
        ref<Reference> tmp(p);
        ss << (buf = x10aux::alloc_printf(fmt, tmp->toString()->c_str()));
    }
    if (buf != NULL)
        dealloc(buf);
}

ref<String> String::format(ref<String> format, ref<x10::array::Array<ref<Any> > > parms) {
    std::ostringstream ss;
    nullCheck(format);
    nullCheck(parms);
    //size_t len = format->FMGL(content_length);
    char* orig = string_utils::strdup(format->c_str());
    char* fmt = orig;
    char* next = NULL;
    for (x10_int i = 0; fmt != NULL; ++i, fmt = next) {
        next = strchr(fmt+1, '%'); // FIXME: this is only ok if we always null-terminate content
        if (next != NULL)
            *next = '\0';
        if (*fmt != '%') {
            ss << fmt;
            --i;
        } else {
            const ref<Reference> p = parms->__apply(i);
            _formatHelper(ss, fmt, p);
        }
        if (next != NULL)
            *next = '%';
    }
    dealloc(orig);
    return String::Lit(ss.str().c_str());
}

x10_boolean String::equals(ref<Any> p0) {
    if (p0.isNull()) return false;
    if (ref<String>(p0).operator->() == this) return true; // short-circuit trivial equality
    if (!x10aux::instanceof<ref<x10::lang::String> >(p0)) return false;
    ref<String> that = (ref<String>) p0;
    if (this->FMGL(content_length) != that->FMGL(content_length)) return false; // short-circuit trivial dis-equality
    if (strncmp(this->FMGL(content), that->FMGL(content), this->length()))
        return false;
    return true;
}

/* FIXME: Unicode support */
x10_boolean String::equalsIgnoreCase(ref<String> s) {
    if (s.isNull()) return false;
    if (ref<String>(s).operator->() == this) return true; // short-circuit trivial equality
    if (this->FMGL(content_length) != s->FMGL(content_length)) return false; // short-circuit trivial dis-equality
    if (strncasecmp(this->FMGL(content), s->FMGL(content), this->length()))
        return false;
    return true;
}

/* FIXME: Unicode support */
ref<String> String::toLowerCase() {
    char *str = x10aux::alloc<char>(FMGL(content_length)+1);
    bool all_lower = true;
    for (size_t i = 0; i < FMGL(content_length); ++i) {
        x10_char c = FMGL(content)[i];
        if (!x10aux::char_utils::isLowerCase(c))
            all_lower = false;
        x10_char l = x10aux::char_utils::toLowerCase(c);
        str[i] = (char)l.v;
    }
    if (all_lower) {
        x10aux::dealloc(str);
        return this;
    }
    str[FMGL(content_length)] = '\0';
    return String::Steal(str);
}

/* FIXME: Unicode support */
ref<String> String::toUpperCase() {
    char *str = x10aux::alloc<char>(FMGL(content_length)+1);
    bool all_upper = true;
    for (size_t i = 0; i < FMGL(content_length); ++i) {
        x10_char c = FMGL(content)[i];
        if (!x10aux::char_utils::isUpperCase(c))
            all_upper = false;
        x10_char u = x10aux::char_utils::toUpperCase(c);
        str[i] = (char)u.v;
    }
    if (all_upper) {
        x10aux::dealloc(str);
        return this;
    }
    str[FMGL(content_length)] = '\0';
    return String::Steal(str);
}

x10_int String::compareTo(ref<String> s) {
    nullCheck(s);
    if (ref<String>(s).operator->() == this) return 0; // short-circuit trivial equality
    int length_diff = this->FMGL(content_length) - s->FMGL(content_length);
    size_t min_length = length_diff < 0 ? this->FMGL(content_length) : s->FMGL(content_length);
    int cmp = strncmp(this->FMGL(content), s->FMGL(content), min_length);
    if (cmp != 0)
        return (x10_int) cmp;
    return (x10_int) length_diff;
}

/* FIXME: Unicode support */
x10_int String::compareToIgnoreCase(ref<String> s) {
    nullCheck(s);
    if (ref<String>(s).operator->() == this) return 0; // short-circuit trivial equality
    int length_diff = this->FMGL(content_length) - s->FMGL(content_length);
    if (length_diff != 0)
        return length_diff;
    return (x10_int) strncasecmp(this->FMGL(content), s->FMGL(content), this->length());
}

x10_boolean String::startsWith(ref<String> s) {
    nullCheck(s);
    size_t len = s->FMGL(content_length);
    if (len > this->FMGL(content_length))
        return false;
    int cmp = strncmp(this->FMGL(content), s->FMGL(content), len);
    return (cmp == 0);
}

x10_boolean String::endsWith(ref<String> s) {
    nullCheck(s);
    size_t len = s->FMGL(content_length);
    if (len > this->FMGL(content_length))
        return false;
    int length_diff = this->FMGL(content_length) - s->FMGL(content_length);
    int cmp = strncmp(this->FMGL(content) + length_diff, s->FMGL(content), len);
    return (cmp == 0);
}

const serialization_id_t String::_serialization_id =
    DeserializationDispatcher::addDeserializer(String::_deserializer<Reference>, x10aux::CLOSURE_KIND_NOT_ASYNC);

void String::_serialize_body(x10aux::serialization_buffer& buf) {
    this->Object::_serialize_body(buf);
    // only support strings that are shorter than 4billion chars
    x10_int sz = FMGL(content_length);
    buf.write(sz);
    const char* content = FMGL(content);
    for (x10_int i = 0; i < sz; ++i) {
        buf.write((x10_char)content[i]);
    }
}

void String::_destructor() {
    dealloc(FMGL(content));
}

void String::_deserialize_body(x10aux::deserialization_buffer &buf) {
    this->Object::_deserialize_body(buf);
    x10_int sz = buf.read<x10_int>();
    char *content = x10aux::alloc<char>(sz+1);
    for (x10_int i = 0; i < sz; ++i) {
        content[i] = (char)buf.read<x10_char>().v;
    }
    content[sz] = '\0';
    this->FMGL(content) = content;
    this->FMGL(content_length) = strlen(content);
    _S_("Deserialized string was: \""<<this<<"\"");
}

Fun_0_1<x10_int, x10_char>::itable<String> String::_itable_Fun_0_1(&String::equals, &String::hashCode,
                                                                   &String::__apply,                                                                    
                                                                   &String::toString, &String::typeName);
Comparable<ref<String> >::itable<String> String::_itable_Comparable(&String::compareTo,
                                                                   &String::equals, &String::hashCode,
                                                                   &String::toString, &String::typeName);

x10aux::itable_entry String::_itables[3] = {
    x10aux::itable_entry(&x10aux::getRTT<Fun_0_1<x10_int, x10_char> >, &String::_itable_Fun_0_1),
    x10aux::itable_entry(&x10aux::getRTT<Comparable<ref<String> > >, &String::_itable_Comparable),
    x10aux::itable_entry(NULL,  (void*)x10aux::getRTT<String>())
};

x10aux::RuntimeType String::rtt;

void String::_initRTT() {
    if (rtt.initStageOne(&rtt)) return;
    const x10aux::RuntimeType* parents[3] =
        {Object::getRTT(),
         Fun_0_1<x10_int, x10_char>::getRTT(),
         Comparable<String>::getRTT() };
    
    rtt.initStageTwo("x10.lang.String", RuntimeType::class_kind, 3, parents, 0, NULL, NULL);
}    

// vim:tabstop=4:shiftwidth=4:expandtab
