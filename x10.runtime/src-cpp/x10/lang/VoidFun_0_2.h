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

#ifndef X10_LANG_VOIDFUN_0_2_H
#define X10_LANG_VOIDFUN_0_2_H

#include <x10aux/config.h>
#include <x10aux/RTT.h>
#include <x10aux/fun_utils.h>

#include <x10/lang/Any.h>

namespace x10 {
    namespace lang {

        void _initRTTHelper_VoidFun_0_2(x10aux::RuntimeType *location,
                                        const x10aux::RuntimeType *rtt1,
                                        const x10aux::RuntimeType *rtt2);

        template<class P1, class P2> class VoidFun_0_2 : public x10aux::AnyFun {
            public:
            static x10aux::RuntimeVoidFunType rtt;
            static const x10aux::RuntimeType* getRTT() { if (!rtt.isInitialized) _initRTT(); return &rtt; }
            static void _initRTT();

            template <class I> struct itable {
                itable(void(I::*apply)(P1,P2),
                       x10_boolean (I::*equals)(x10aux::ref<x10::lang::Any>),
                       x10_int (I::*hashCode)(),
                       x10aux::ref<x10::lang::String> (I::*toString)(),
                       x10aux::ref<x10::lang::String> (I::*typeName)()
                    ) : apply(apply), equals(equals), hashCode(hashCode), toString(toString), typeName(typeName) {}
                void (I::*apply)(P1,P2);
                x10_boolean (I::*equals)(x10aux::ref<x10::lang::Any>);
                x10_int (I::*hashCode)();
                x10aux::ref<x10::lang::String> (I::*toString)();
                x10aux::ref<x10::lang::String> (I::*typeName)();
            };
        };

        template<class P1, class P2> void VoidFun_0_2<P1,P2>::_initRTT() {
            if (rtt.initStageOne(x10aux::getRTT<VoidFun_0_2<void,void> >())) return;
            x10::lang::_initRTTHelper_VoidFun_0_2(&rtt, x10aux::getRTT<P1>(), x10aux::getRTT<P2>());
        }

        template<class P1, class P2> x10aux::RuntimeVoidFunType VoidFun_0_2<P1,P2>::rtt;

        template<> class VoidFun_0_2<void,void> {
        public:
            static x10aux::RuntimeType rtt;
            static const x10aux::RuntimeType* getRTT() { return &rtt; }
        };
    }
}
#endif
// vim:tabstop=4:shiftwidth=4:expandtab
