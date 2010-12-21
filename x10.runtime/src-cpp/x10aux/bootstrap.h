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

#ifndef X10AUX_BOOTSTRAP_H
#define X10AUX_BOOTSTRAP_H

#include <x10aux/config.h>
#include <x10aux/alloc.h>
#include <x10aux/place_local.h>
#include <x10aux/string_utils.h>
#include <x10aux/system_utils.h>
#include <x10aux/init_dispatcher.h>
#include <x10aux/deserialization_dispatcher.h>

#include <x10/lang/VoidFun_0_0.h>

#include <x10/lang/Throwable.h>

#include <x10/lang/Thread.h>
#include <x10/lang/Runtime__Worker.h>
#include <x10/lang/Closure.h>

#include <stdio.h>

#ifdef __CYGWIN__
extern "C" int setlinebuf(FILE *);
#endif
namespace x10 { namespace array { template<class T> class Array; } }

namespace x10aux {

    class StaticInitClosure : public x10::lang::Closure
    {
        public:

        static x10::lang::VoidFun_0_0::itable<StaticInitClosure> _itable;
        static x10aux::itable_entry _itables[2];

        virtual x10aux::itable_entry* _getITables() { return _itables; }

        // closure body
        void apply () {
            // Initialise the static fields of x10 classes.
            x10aux::InitDispatcher::runInitializers();
        }

        StaticInitClosure() { }

        const x10aux::RuntimeType *_type() const {return x10aux::getRTT<x10::lang::VoidFun_0_0>();}

        ref<x10::lang::String> toString() {
            return x10aux::string_utils::lit("x10aux::StaticInitClosure ("__FILELINE__")");
        }

        virtual x10aux::serialization_id_t _get_serialization_id() {
            assert(false); // We should never be serializing this closure
            return 0;
        }

        virtual void _serialize_body(x10aux::serialization_buffer &) {
            assert(false); // We should never be serializing this closure
        }
    };

    typedef void (*ApplicationMainFunction)(ref<x10::array::Array<ref<x10::lang::String> > >);

    class BootStrapClosure : public x10::lang::Closure
    {
        protected:

        ApplicationMainFunction main;
        ref<x10::array::Array<ref<x10::lang::String> > > args;
        public:

        static x10::lang::VoidFun_0_0::itable<BootStrapClosure> _itable;
        static x10aux::itable_entry _itables[2];

        virtual x10aux::itable_entry* _getITables() { return _itables; }

        // closure body
        void apply () {
            // Invoke the application main().
            main(args);
        }

        BootStrapClosure(ApplicationMainFunction main_,
                         ref<x10::array::Array<ref<x10::lang::String> > > args_)
          : main(main_), args(args_)
        { }

        const x10aux::RuntimeType *_type() const {return x10aux::getRTT<x10::lang::VoidFun_0_0>();}

        ref<x10::lang::String> toString() {
            return x10aux::string_utils::lit("x10aux::BootStrapClosure ("__FILELINE__")");
        }

        virtual x10aux::serialization_id_t _get_serialization_id() {
            assert(false); // We should never be serializing this closure
            return 0;
        }

        virtual void _serialize_body(x10aux::serialization_buffer &) {
            assert(false); // We should never be serializing this closure
        }
    };

    void initialize_xrx();

    template<class Runtime, class T> int template_main(int ac, char **av) {

        setlinebuf(stdout);

        x10aux::network_init(ac,av);

#ifdef X10_USE_BDWGC
        GC_INIT();
#endif
        x10aux::ref<x10::array::Array<x10aux::ref<x10::lang::String> > > args = X10_NULL;

#ifndef NO_EXCEPTIONS
        try {
#endif
            x10aux::place_local::initialize();

            // Initialise enough state to make this 'main' thread look like a normal x10 thread
            // (e.g. make Thread::CurrentThread work properly).
            x10::lang::Runtime__Worker::_make((x10_int)0);
            x10aux::initialize_xrx();

            args = x10aux::convert_args(ac, av);

            // Construct closure to invoke the static initialisers at place 0
            x10aux::ref<x10::lang::VoidFun_0_0> init_closure =
                x10aux::ref<StaticInitClosure>(new (x10aux::alloc<x10::lang::VoidFun_0_0>(sizeof(x10aux::StaticInitClosure)))
                                               x10aux::StaticInitClosure());

            // Construct closure to invoke the user's "public static def main(Array[String]) : void"
            // if at place 0 otherwise wait for asyncs.
            x10aux::ref<x10::lang::VoidFun_0_0> main_closure =
                x10aux::ref<BootStrapClosure>(new (x10aux::alloc<x10::lang::VoidFun_0_0>(sizeof(x10aux::BootStrapClosure)))
                                              x10aux::BootStrapClosure(T::main,args));

            x10aux::DeserializationDispatcher::registerHandlers();
            Runtime::start(init_closure, main_closure); // use XRX
            //init_closure->apply(); // bypass XRX
            //main_closure->apply(); // bypass XRX
            //sleep(3);

#ifndef NO_EXCEPTIONS
        } catch(int exitCode) {

            x10aux::exitCode = exitCode;

        } catch(x10aux::__ref& e) {

            // Assume that only throwables can be thrown
            // and things are never thrown by interface (always cast to a value/object class)
            x10aux::ref<x10::lang::Throwable> &e_ =
                static_cast<x10aux::ref<x10::lang::Throwable>&>(e);

            fprintf(stderr, "Uncaught exception at place %ld: %s\n", (long)x10aux::here,
                    x10aux::string_utils::cstr(nullCheck(nullCheck(e_)->toString())));

            e_->printStackTrace();

            x10aux::exitCode = 1;

        } catch(...) {

            fprintf(stderr, "Caught unrecognised exception at place %ld\n", (long)x10aux::here);
            x10aux::exitCode = 1;

        }
#endif
        x10aux::shutdown();

        if (getenv("X10_RXTX")!=NULL)
            fprintf(stderr, "Place: %ld   rx: %lld/%lld   tx: %lld/%lld\n",
                (long)x10aux::here,
                (long long)x10aux::deserialized_bytes, (long long)x10aux::asyncs_received,
                (long long)x10aux::serialized_bytes, (long long)x10aux::asyncs_sent);

        return x10aux::exitCode;
    }


}

#endif

// vim:tabstop=4:shiftwidth=4:expandtab
