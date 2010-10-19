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

#ifndef X10AUX_ATOMIC_BOOLEAN_FUNS_H
#define X10AUX_ATOMIC_BOOLEAN_FUNS_H

#include <x10aux/ref.h>

namespace x10 { namespace util { namespace concurrent { namespace atomic { class AtomicBoolean; }}}}

namespace x10aux {
                
    class atomic_boolean_funs {
    public:
        static x10_boolean compareAndSet(x10aux::ref<x10::util::concurrent::atomic::AtomicBoolean> obj,
                                         x10_boolean expect, x10_boolean update);
                    
        static x10_boolean weakCompareAndSet(x10aux::ref<x10::util::concurrent::atomic::AtomicBoolean> obj,
                                             x10_boolean expect, x10_boolean update);
    };
}
    
#endif /* X10_AUX_ATOMIC_BOOLEAN_FUNS_H */

// vim:tabstop=4:shiftwidth=4:expandtab
