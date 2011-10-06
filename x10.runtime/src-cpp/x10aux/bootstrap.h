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
#include <unistd.h>

namespace x10 { namespace array { template<class T> class Array; } }

namespace x10aux {
    class StaticInitClosure : public x10::lang::Closure
    {
        public:

        static x10::lang::VoidFun_0_0::itable<StaticInitClosure> _itable;
        static x10aux::itable_entry _itables[2];

        virtual x10aux::itable_entry* _getITables() { return _itables; }

        // closure body
        void __apply () {
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
        void __apply () {
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
    int real_x10_main(int, char**, ApplicationMainFunction);
    
    template<class T> int template_main(int ac, char **av) {
        return x10aux::real_x10_main(ac, av, &T::main);
    }
}

#endif

// vim:tabstop=4:shiftwidth=4:expandtab
