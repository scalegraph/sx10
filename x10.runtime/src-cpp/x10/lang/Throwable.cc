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
#include <x10aux/basic_functions.h>
#include <x10aux/class_cast.h>

#include <x10/lang/Throwable.h>
#include <x10/lang/String.h>
#include <x10/lang/Rail.h>
#include <x10/io/Printer.h>

#if defined(__GLIBC__) || defined(__APPLE__)
#   include <execinfo.h> // for backtrace()
#   include <cxxabi.h> // for demangling of symbol
#elif defined(_AIX)
#   include <unistd.h>
#   include <stdlib.h>
#   include <stdio.h>
#   include <string.h>
#   ifndef __GNUC__
#      include <demangle.h> // for demangling of symbol
#   else
#     include <cxxabi.h> // for demangling of symbol
#   endif
#endif

#include <stdio.h>

using namespace x10::lang;
using namespace x10aux;

const serialization_id_t Throwable::_serialization_id =
    DeserializationDispatcher::addDeserializer(Throwable::_deserializer<Reference>, x10aux::CLOSURE_KIND_NOT_ASYNC);

void
Throwable::_serialize_body(x10aux::serialization_buffer &buf) {
    this->Object::_serialize_body(buf);
    buf.write(FMGL(cause));
    buf.write(FMGL(message));
    getStackTrace(); // ensure cachedStackTrace has been computed before serializing it
    buf.write(FMGL(cachedStackTrace));
}

void
Throwable::_deserialize_body(x10aux::deserialization_buffer &buf) {
    this->Object::_deserialize_body(buf);
    FMGL(cause) = buf.read<x10aux::ref<Throwable> >();
    FMGL(message) = buf.read<x10aux::ref<String> >();
    FMGL(cachedStackTrace) = buf.read<x10aux::ref<Rail<x10aux::ref<String> > > >();
    FMGL(trace_size) = 0;
}

x10aux::ref<Throwable>
Throwable::_make() {
    return (new (x10aux::alloc<Throwable>()) Throwable())->_constructor();
}

x10aux::ref<Throwable>
Throwable::_make(x10aux::ref<String> message) {
    return (new (x10aux::alloc<Throwable>()) Throwable())->_constructor(message);
}

x10aux::ref<Throwable>
Throwable::_make(x10aux::ref<Throwable> cause) {
    return (new (x10aux::alloc<Throwable>()) Throwable())->_constructor(cause);
}
    
x10aux::ref<Throwable>
Throwable::_make(x10aux::ref<String> message, x10aux::ref<Throwable> cause) {
    return (new (x10aux::alloc<Throwable>()) Throwable())->_constructor(message, cause);
}

x10aux::ref<Throwable> Throwable::_constructor(x10aux::ref<String> message,
                                               x10aux::ref<Throwable> cause)
{
    this->Object::_constructor();
    this->FMGL(message) = message;
    this->FMGL(cause) = cause;
    this->FMGL(trace_size) = -1;
    this->FMGL(cachedStackTrace) = X10_NULL;
    return this;
}


ref<String> Throwable::toString() {
    ref<String> message = getMessage();
    if (message.isNull()) {
        return String::Lit(_type()->name());
    } else {
        return String::Steal(alloc_printf("%s: %s",_type()->name(),message->c_str()));
    }
}


#if !defined(__GLIBC__) && defined(_AIX)
#define BACKTRACE_SYM "backtrace__FPPvUl"
extern "C" int mt__trce(int, int, void*, int);
int backtrace(void** trace, size_t max_size) {
    int pid = ::getpid();
    int p[2];
    pipe(p);
    mt__trce(p[1], 0, NULL, 0);
    close(p[1]);
    FILE* pf = fdopen(p[0], "r");
    char m_buf[1001];
    size_t len = sizeof(m_buf) - 1;
    bool in_thread = false;
    bool in_trace = false;
    bool first_frame = false;
    char* s;
    size_t sz = 0;
    while ((s = fgets(m_buf, len, pf)) != NULL) {
        if (!in_thread) {
            if (!strncmp(s, "+++ID ", 6)) { // thread start
                char* p = strstr(s, " Process ");
                char* t = strstr(s, " Thread ");
                if (p == NULL || t == NULL)
                    continue;
                *strchr(t, '\n') = '\0';
                *t = '\0';
                int i = strtol(p+9, NULL, 10);
                if (i != pid) {
                    *t = ' ';
                    continue;
                }
                in_thread = true;
            }
            continue;
        }
        if (!strncmp(s, "---ID ", 6)) { // thread end
            in_thread = false;
            continue;
        }
        if (!in_trace) {
            if (!strcmp(s, "+++STACK\n")) { // stack trace start
               in_trace = true;
               first_frame = true;
            }
            continue;
        }
        if (!strcmp(s, "---STACK\n")) { // stack trace end
            in_trace = false;
            break; // assume we have the right thread -- we're done
        }
        if (first_frame) {
            // The first symbol has to be this function.  Skip it.
            // FIXME: theoretically, it's possible that another thread is here too
            if (strncmp(s, BACKTRACE_SYM, strlen(BACKTRACE_SYM))) {
                in_trace = false;
            }
            first_frame = false;
            continue;
        }
        if (sz >= max_size)
            break;
        trace[sz++] = string_utils::strdup(s);
    }
    fclose(pf);
    close(p[0]);
    return (int)sz;
}
#endif


ref<Throwable> Throwable::fillInStackTrace() {
#if defined(__GLIBC__) || defined(__APPLE__)
    if (FMGL(trace_size)>=0) return this;
    FMGL(trace_size) = ::backtrace(FMGL(trace), sizeof(FMGL(trace))/sizeof(*FMGL(trace)));
#elif defined(_AIX)
    if (FMGL(trace_size)>=0) return this;

    // walk the stack, saving the offsets for each stack frame into "trace".
    unsigned long stackAddr;
	#if defined(_LP64)
		__asm__ __volatile__ ("std 1, %0 \n\t" : "=m" (stackAddr));
	#else
		__asm__ __volatile__ ("stw 1, %0 \n\t" : "=m" (stackAddr));
	#endif
	FMGL(trace_size) = 0;
	while (FMGL(trace_size) < MAX_TRACE_SIZE)
	{
		FMGL(trace)[FMGL(trace_size)] = (void*)*(((unsigned long *)stackAddr)+2); // link register is saved here in the stack
		stackAddr = *((long *)stackAddr);
		FMGL(trace_size)++;
		if (stackAddr == 0) // the end of the stack (main)
			break;
	}
#endif
    return this;
}


#ifdef __GLIBC__
// This one gets the function name as a demangled string,
// the filename of the native executable/library that contains the function,
// and the value of the program counter (addr).
void extract_frame (const char *start, char * &filename, char * &symbol, size_t &addr) {
    // arbitrary_text + "(" + symbol + "+0x" + hex_offset + ") [0x" + address +"]"
    const char *lparen = strrchr(start,'(');
    const char *plus = strrchr(start,'+');
    const char *x = strrchr(start,'x');

    if (lparen==NULL || plus==NULL || x==NULL) {
        filename = NULL;
        symbol = string_utils::strdup(start);
        addr = 0;
        return;
    }

    filename = (char*)malloc(lparen-start+1);
    strncpy(filename,start,lparen-start);
    filename[lparen-start] = '\0';

    char *mangled = (char*)malloc(plus-lparen);
    strncpy(mangled,lparen+1,plus-lparen-1);
    mangled[plus-lparen-1] = '\0';

    size_t offset = strtol(plus+3, NULL, 16);
    addr = strtol(x+1, NULL, 16);
    (void)offset;
    //addr += offset;

    // don't free symbol, it's persistant
    symbol = NULL;
    symbol = abi::__cxa_demangle(mangled, NULL, NULL, NULL);
    if (symbol==NULL) {
        symbol = mangled;
    } else {
        free(mangled);
    }
}
#elif defined(__APPLE__)
// This one gets the function name as a demangled string,
// the filename of the native executable/library that contains the function,
// and the value of the program counter (addr).
void extract_frame (const char *start, char * &filename, char * &symbol, size_t &addr) {
    // arbitrary_text + " 0x" + address + " " + symbol + " + " offset
    // arbitrary_text + "(" + symbol + "+0x" + hex_offset + ") [0x" + address +"]"
    const char *x = strstr(start," 0x");
    const char *space = strchr(x+1,' ');
    const char *plus = strchr(space,'+');

    if (space==NULL || plus==NULL || x==NULL) {
        filename = NULL;
        symbol = string_utils::strdup(start);
        addr = 0;
        return;
    }

    filename = (char*)malloc(x-start+1);
    strncpy(filename,start,x-start);
    filename[x-start] = '\0';

    char *mangled = (char*)malloc(plus-space-1);
    strncpy(mangled,space+1,plus-space-2);
    mangled[plus-space-2] = '\0';

    size_t offset = strtol(plus+2, NULL, 10);
    addr = strtol(x+3, NULL, 16);
    (void)offset;
    //addr += offset;

    // don't free symbol, it's persistant
    symbol = NULL;
    symbol = abi::__cxa_demangle(mangled, NULL, NULL, NULL);
    if (symbol==NULL) {
        symbol = mangled;
    } else {
        free(mangled);
    }
}
#endif


#if !defined(__GLIBC__) && defined(_AIX)
static char* demangle_symbol(char* name) {
#if defined(__GNUC__)
    char* res = abi::__cxa_demangle(name, NULL, NULL, NULL);
    if (res == NULL)
        return name;
    return res;
#else
    char* rest;
    Name* n = Demangle(name, rest);
    if (n == NULL)
        return name;
    return n->Text();
#endif
}
#endif

ref<Rail<ref<String> > > Throwable::getStackTrace() {
    if (FMGL(cachedStackTrace).isNull()) {
        #if defined(__GLIBC__) || defined(__APPLE__)
        if (FMGL(trace_size) <= 0) {
            const char *msg = "No stacktrace recorded.";
            FMGL(cachedStackTrace) = alloc_rail<ref<String>,Rail<ref<String> > >(1, String::Lit(msg));
        } else {
            ref<Rail<ref<String> > > rail =
                alloc_rail<ref<String>,Rail<ref<String> > >(FMGL(trace_size));
            char **messages = ::backtrace_symbols(FMGL(trace), FMGL(trace_size));
            for (int i=0 ; i<FMGL(trace_size) ; ++i) {
                char *filename; char *symbol; size_t addr;
                extract_frame(messages[i],filename,symbol,addr);
                char *msg = symbol;
                (*rail)[i] = String::Lit(msg);
                ::free(msg);
                ::free(filename);
            }
            ::free(messages); // malloced by backtrace_symbols
            FMGL(cachedStackTrace) = rail;
        }
        #elif defined(_AIX)
        if (FMGL(trace_size) <= 0) {
            const char *msg = "No stacktrace recorded.";
            FMGL(cachedStackTrace) = alloc_rail<ref<String>,Rail<ref<String> > >(1, String::Lit(msg));
        }
        else
        {
			// build up a fake stack from our saved addresses
			// the fake stack doesn't need anything more than back-pointers and enough offset to hold the frame references
			unsigned long* fakeStack = (unsigned long *)malloc((FMGL(trace_size)+1) * 3 * sizeof(unsigned long)); // pointer, junk, link register, junk, junk, junk
			long i;
			for (i=0; i<FMGL(trace_size); i++)
			{
				fakeStack[i*3] = (unsigned long)&(fakeStack[(i+1)*3]);
				fakeStack[i*3+1] = 0xdeadbeef;
				fakeStack[i*3+2] = (unsigned long)FMGL(trace)[i];
			}
			fakeStack[i*3] = 0;

			// manipulate the existing stack to point to our fake stack
			unsigned long stackPointer;
			#if defined(_LP64)
				__asm__ __volatile__ ("std 1, %0 \n\t" : "=m" (stackPointer));
			#else
				__asm__ __volatile__ ("stw 1, %0 \n\t" : "=m" (stackPointer));
			#endif

			unsigned long originalStackPointer = stackPointer;
			*((unsigned long*)stackPointer) = (unsigned long)fakeStack; // this line overwrites the back chain pointer in the stack to the fake one.

			// call the original slow backtrace method to convert the offsets into text
			// this overwrites the contents of "trace" and value of "trace_size", which are no longer needed.
			FMGL(trace_size) = ::backtrace(FMGL(trace), sizeof(FMGL(trace))/sizeof(*FMGL(trace)));

			// replace the stack frame pointer to point to the real stack again
			*((unsigned long*)stackPointer) = originalStackPointer;

			// delete the fake stack, which is no longer needed
			free(fakeStack);

			// from here on down, proceed as before
			ref<Rail<ref<String> > > rail =
				alloc_rail<ref<String>,Rail<ref<String> > >(FMGL(trace_size));
			char *msg;
			for (int i=0 ; i<FMGL(trace_size) ; ++i) {
				char* s = (char*)FMGL(trace)[i];
				char* c = strstr(s, " : ");
				if (c == NULL) {
					(*rail)[i] = String::Lit("???????");
					continue;
				}
				c[0] = '\0';
				c += 3;
				char* n = strchr(c, '\n');
				if (n != NULL)
					*n = '\0';
				s = demangle_symbol(s);
				char* f = strstr(c, " # ");
				if (f != NULL) {
					unsigned long l = strtoul(c, NULL, 10);
					char* p = strchr(f, '<');
					if (p != NULL) {
						f = p + 1;
						char* z = strchr(f, '>');
						if (z != NULL)
							*z = '\0';
					} else {
						f += 3;
					}
					msg = alloc_printf("%s (%s:%d)", s, f, l);
				} else {
					msg = alloc_printf("%s (offset %s)", s, c);
					f = c;
				}
				(*rail)[i] = String::Lit(msg);
				::free(msg);

			}
			FMGL(cachedStackTrace) = rail;
        }
    #else
        const char *msg = "Detailed stacktraces not supported on this platform.";
        FMGL(cachedStackTrace) = alloc_rail<ref<String>,Rail<ref<String> > >(1, String::Lit(msg));
    #endif
    }

    return FMGL(cachedStackTrace);
}

void Throwable::printStackTrace() {
    fprintf(stderr, "%s\n", this->toString()->c_str());
    x10aux::ref<Rail<x10aux::ref<String> > > trace = this->getStackTrace();
    for (int i = 0; i < trace->FMGL(length); ++i)
        fprintf(stderr, "\tat %s\n", (*trace)[i]->c_str());
}

void Throwable::printStackTrace(x10aux::ref<x10::io::Printer> printer) {
    printer->println(toString());
    x10aux::ref<Rail<x10aux::ref<String> > > trace = this->getStackTrace();
    for (int i=0 ; i<trace->FMGL(length) ; ++i) { 
        printer->print(x10::lang::String::Lit("\tat "));
        printer->println((*trace)[i]);
    }
}

RTT_CC_DECLS1(Throwable, "x10.lang.Throwable", RuntimeType::class_kind, Object)

// vim:tabstop=4:shiftwidth=4:expandtab
