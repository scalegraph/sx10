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

package x10.types;

import java.util.List;

import polyglot.types.LocalDef;
import polyglot.types.ProcedureDef;
import polyglot.types.Ref;
import polyglot.types.Type;
import x10.types.constraints.CConstraint;
import x10.types.constraints.TypeConstraint;

public interface X10ProcedureDef extends X10CodeDef, ProcedureDef, X10MemberDef {
    Ref<? extends Type> returnType();
    void setReturnType(Ref<? extends Type> rt);

    Ref<CConstraint> guard();
    void setGuard(Ref<CConstraint> s);
    
    Ref<TypeConstraint> typeGuard();
    void setTypeGuard(Ref<TypeConstraint> s);

    List<LocalDef> formalNames();
    void setFormalNames(List<LocalDef> formalNames);
    
    Ref<? extends Type> offerType();
}
