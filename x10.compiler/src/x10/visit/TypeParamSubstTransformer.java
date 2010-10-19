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

package x10.visit;

import polyglot.types.Type;
import x10.types.ParameterType;
import x10.types.TypeParamSubst;
import x10.types.X10ConstructorInstance;
import x10.types.X10FieldInstance;
import x10.types.X10LocalInstance;
import x10.types.X10MethodInstance;

/**
 * A {@link TypeTransformer} that transforms types by applying
 * a given type parameter substitution ({@link TypeParamSubst})
 * to each type object.
 */
public class TypeParamSubstTransformer extends TypeTransformer {
    protected final TypeParamSubst subst;

    protected TypeParamSubstTransformer(TypeParamSubst subst) {
        this.subst = subst;
    }

    @Override
    protected Type transformType(Type type) {
        return super.transformType(subst.reinstantiate(type));
    }

    @Override
    protected ParameterType transformParameterType(ParameterType pt) {
        ParameterType tpt = subst.reinstantiate(pt);
        if (tpt == pt) {
            assert false : "No substitution found for type parameter " + pt;
        }
        return super.transformParameterType(tpt);
    }

    @Override
    protected X10LocalInstance transformLocalInstance(X10LocalInstance li) {
        return super.transformLocalInstance(subst.reinstantiate(li));
    }

    @Override
    protected X10FieldInstance transformFieldInstance(X10FieldInstance fi) {
        return super.transformFieldInstance(subst.reinstantiate(fi));
    }

    @Override
    protected X10MethodInstance transformMethodInstance(X10MethodInstance mi) {
        return super.transformMethodInstance(subst.reinstantiate(mi));
    }

    @Override
    protected X10ConstructorInstance transformConstructorInstance(X10ConstructorInstance ci) {
        return super.transformConstructorInstance(subst.reinstantiate(ci));
    }
}
