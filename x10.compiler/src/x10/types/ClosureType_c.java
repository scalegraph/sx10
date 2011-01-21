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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import polyglot.ast.Expr;
import polyglot.frontend.Job;
import polyglot.frontend.Source;
import polyglot.types.ClassDef;
import polyglot.types.ClassType;
import polyglot.types.ClassType_c;
import polyglot.types.ConstructorInstance;
import polyglot.types.DerefTransform;
import polyglot.types.FieldInstance;
import polyglot.types.Flags;
import polyglot.types.LazyRef;
import polyglot.types.LocalInstance;
import polyglot.types.Matcher;
import polyglot.types.MethodAsTypeTransform;
import polyglot.types.MethodDef;

import polyglot.types.Named;
import polyglot.types.Package;
import polyglot.types.Ref;
import polyglot.types.Resolver;
import polyglot.types.Name;
import polyglot.types.ContainerType;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.Types;
import polyglot.types.TypeSystem;
import polyglot.types.ClassDef.Kind;
import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.Transformation;
import polyglot.util.TransformingList;
import x10.constraint.XConstraint;

/**
 * A representation of the type of a closure. Treated as a ClassType implementing a FunctionType, with 
 * the signature for the function type retrieved from the sole method (the apply method) defined on the
 * class type.
 * @author nystrom
 * @author vj
 *
 */
public class ClosureType_c extends X10ParsedClassType_c implements FunctionType {
    private static final long serialVersionUID = 2768150875334536668L;

//    protected ClosureInstance ci;

    public ClosureType_c(final TypeSystem ts, Position pos, final X10ClassDef def) {
	super(ts, pos, Types.ref(def));
    }
    
    public MethodInstance applyMethod() {
        try {
        return (MethodInstance) methods().get(0);
        } catch (Exception z) {
            System.out.println("check.");
            return null;
        }
    }
    
    public Type returnType() {
        return applyMethod().returnType();
    }

    public XConstraint guard() {
        return applyMethod().guard();
    }

    public List<Type> typeParameters() {
        return applyMethod().typeParameters();
    }

    public List<LocalInstance> formalNames() {
        return applyMethod().formalNames();
    }

    public List<Type> argumentTypes() {
        return applyMethod().formalTypes();
    }

    
    @Override
    public String typeToString() {
        MethodInstance mi = applyMethod();
        if (mi==null) // this could happen if the method is installed before the type is properly formed, e.g. in -report types=2 execution.
            return "???"; 
        StringBuilder sb = new StringBuilder();
        List<LocalInstance> formals = mi.formalNames();
        for (int i=0; i < formals.size(); ++i) {
        	LocalInstance f = formals.get(i);
        	 if (sb.length() > 0)
                 sb.append(", ");
             sb.append(f.name());
             sb.append(':');
             sb.append(f.type());
        }
        /*
        for (LocalInstance f : formals) {
        	 if (sb.length() > 0)
                 sb.append(", ");
             sb.append(f.name());
             sb.append(':');
             sb.append(f.type());
        }
      */
        XConstraint guard = guard();
        return "(" + sb.toString() + ")" + (guard==null? "" : guard) + "=> " + mi.returnType();
    }


	@Override
	public int hashCode() {
		return def.get().hashCode();
	}
    

    public void print(CodeWriter w) {
        w.write(toString());
    }
   
}
