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

package x10c.types;

import polyglot.types.Context;
import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.types.Context;
import polyglot.types.TypeSystem;
import x10.types.X10TypeSystem_c;

public class X10CTypeSystem_c extends X10TypeSystem_c implements TypeSystem {

    public Context emptyContext() {
        return new X10CContext_c(this);
    }

    public BackingArrayType createBackingArray(Position position, Ref<? extends Type> ref) {
        return new BackingArrayType(this, position, ref);
    }
}
