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
#include <x10aux/RTT.h>

#include <x10/lang/Object.h>
#include <x10/lang/Rail.h>
#include <x10/lang/Runtime.h>

using namespace x10aux;

namespace x10 {
    namespace lang {

        RuntimeType Rail<void>::rtt;

        void
        _initRTTHelper_Rail(RuntimeType *location, const RuntimeType *element,
                            const RuntimeType *p1, const RuntimeType *p2) {
            const RuntimeType *parents[3] = { Object::getRTT(), p1, p2 };
            const RuntimeType *params[1] = { element };
            RuntimeType::Variance variances[1] = { RuntimeType::invariant };
            location->initStageTwo("x10.lang.Rail", 3, parents, 1, params, variances);
        }
    }
}

// vim:tabstop=4:shiftwidth=4:expandtab
