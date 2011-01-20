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

import polyglot.types.ClassType;
import polyglot.types.CodeInstance;
import polyglot.types.Def_c;
import polyglot.types.LocalDef;
import polyglot.types.QName;
import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.types.VarDef;
import polyglot.types.VarInstance;
import polyglot.util.CollectionUtil; import x10.util.CollectionFactory;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.TypedList;
import x10.constraint.XTerms;
import x10.constraint.XVar;
import x10.constraint.XTerm;
import x10.types.constraints.CConstraint;
import x10.types.constraints.TypeConstraint;
import x10.types.constraints.XConstrainedTerm;

public class ClosureDef_c extends Def_c implements ClosureDef {
    private static final long serialVersionUID = -9082180217851254169L;

    protected Ref<? extends CodeInstance<?>> methodContainer;
    protected Ref<? extends ClassType> typeContainer;
    protected Ref<? extends Type> returnType;
    protected List<Ref<? extends Type>> formalTypes;
    protected List<LocalDef> formalNames;
    protected Ref<CConstraint> guard;
    //protected Ref<TypeConstraint> typeGuard;
    protected CodeInstance<?> asInstance;
    
    protected XConstrainedTerm placeTerm;
    protected Ref<? extends Type> offerType;
    
    protected List<VarInstance<? extends VarDef>> capturedEnvironment;

    public ClosureDef_c(TypeSystem ts, Position pos, 
            Ref<? extends ClassType> typeContainer,
            Ref<? extends CodeInstance<?>> methodContainer,
            Ref<? extends Type> returnType,
            List<Ref<? extends Type>> formalTypes,
            ThisDef thisDef,
            List<LocalDef> formalNames, 
            Ref<CConstraint> guard,
            //Ref<TypeConstraint> typeGuard,
            //List<Ref<? extends Type>> throwTypes,
            Ref<? extends Type> offerType) {

        super(ts, pos);
        this.typeContainer = typeContainer;
        this.methodContainer = methodContainer;
        this.returnType = returnType;
        this.formalTypes = TypedList.copyAndCheck(formalTypes, Ref.class, true);
        this.formalNames = TypedList.copyAndCheck(formalNames, LocalDef.class, true);
        this.guard = guard;
        //this.typeGuard = typeGuard;
        this.thisDef = thisDef;
        this.offerType = offerType;
        this.capturedEnvironment = new ArrayList<VarInstance<? extends VarDef>>();
    }
    
    public Ref<? extends Type> offerType() {
    	return offerType;
    }
    public ClosureDef position(Position pos) {
    	ClosureDef_c n = (ClosureDef_c) copy();
    	n.position = pos;
    	return n;
    	
    }
    FunctionType asType;
    
    public FunctionType asType() {
	if (asType == null) {
	    TypeSystem ts = (TypeSystem) this.ts;
	    asType = ts.closureType(position(), returnType, 
	    		// Collections.EMPTY_LIST, 
	    		formalTypes, formalNames, guard);
	}
	return asType;
    }
    
    protected boolean inferReturnType;
    public boolean inferReturnType() { return inferReturnType; }
    public void inferReturnType(boolean r) { this.inferReturnType = r; }

    // BEGIN ANNOTATION MIXIN
    List<Ref<? extends Type>> annotations;

    public List<Ref<? extends Type>> defAnnotations() {
        if (annotations == null)
            return Collections.<Ref<? extends Type>>emptyList();
        return Collections.unmodifiableList(annotations);
    }
    
    public void setDefAnnotations(List<Ref<? extends Type>> annotations) {
        this.annotations = TypedList.<Ref<? extends Type>>copyAndCheck(annotations, Ref.class, true);
    }
    
    public List<Type> annotations() {
        return X10TypeObjectMixin.annotations(this);
    }
    
    public List<Type> annotationsMatching(Type t) {
        return X10TypeObjectMixin.annotationsMatching(this, t);
    }
    
    public List<Type> annotationsNamed(QName fullName) {
        return X10TypeObjectMixin.annotationsNamed(this, fullName);
    }
    // END ANNOTATION MIXIN

    public ClosureInstance asInstance() {
        if (asInstance == null) {
            asInstance = ((TypeSystem) ts).createClosureInstance(position(), Types.ref(this));
        }
        return (ClosureInstance) asInstance;
    }
    
    public Ref<? extends ClassType> typeContainer() {
        return typeContainer;
    }

    public List<ParameterType> typeParameters() {
        return Collections.<ParameterType>emptyList();
    }

    public void setTypeParameters(List<ParameterType> typeParameters) {
        throw new InternalCompilerError("Attempt to set type parameters on a closure def: "+this, position());
    }
    
    public XVar thisVar() {
        if (this.thisDef != null)
            return this.thisDef.thisVar();
        return XTerms.makeEQV("#this");
    }

    ThisDef thisDef;

    public ThisDef thisDef() {
        return this.thisDef;
    }

    public void setThisDef(ThisDef thisDef) {
        this.thisDef = thisDef;
    }
    
    public void setPlaceTerm(XConstrainedTerm p) {
    	this.placeTerm = p;
    }
    
    public XConstrainedTerm placeTerm() { return placeTerm;}
    
    
    public List<LocalDef> formalNames() {
	return Collections.unmodifiableList(formalNames);
    }

    public void setFormalNames(List<LocalDef> formalNames) {
	this.formalNames = TypedList.copyAndCheck(formalNames, LocalDef.class, true);
    }

    public Ref<CConstraint> guard() {
	    return guard;
    }
    
    public void setGuard(Ref<CConstraint> s) {
	    this.guard = s;
    }
    
    public Ref<TypeConstraint> typeGuard() {
        return null; // typeGuard;
    }
    
    public void setTypeGuard(Ref<TypeConstraint> s) {
       //  this.typeGuard = s;
    	assert false;
    }
    
    /**
     * @param container The container to set.
     */
    public void setTypeContainer(Ref<? extends ClassType> container) {
        this.typeContainer = container;
    }

    public Ref<? extends CodeInstance<?>> methodContainer() {
        return methodContainer;
    }

    public void setMethodContainer(Ref<? extends CodeInstance<?>> container) {
        methodContainer = container;
    }

    public Ref<? extends Type> returnType() {
        return returnType;
    }

    public void setReturnType(Ref<? extends Type> returnType) {
        assert returnType != null;
        this.returnType = returnType;
    }


    public List<Ref<? extends Type>> formalTypes() {
        return Collections.unmodifiableList(formalTypes);
    }

    /**
     * @param formalTypes The formalTypes to set.
     */
     public void setFormalTypes(List<Ref<? extends Type>> formalTypes) {
         this.formalTypes = TypedList.copyAndCheck(formalTypes, Ref.class, true);
     }

     public List<VarInstance<? extends VarDef>> capturedEnvironment() {
         return Collections.unmodifiableList(capturedEnvironment);
     }

     public void setCapturedEnvironment(List<VarInstance<? extends VarDef>> env) {
         capturedEnvironment = TypedList.<VarInstance<? extends VarDef>>copy(env, VarInstance.class, false);
     }
     
     private static boolean containsDef(List<VarInstance<? extends VarDef>> l, VarInstance<? extends VarDef> v) {
         for (VarInstance<? extends VarDef> e : l) {
             if (e == v)
                 return true;
             if (e.def() == v.def())
                 return true;
         }
         return false;
     }

     public static void addCapturedVariable(List<VarInstance<? extends VarDef>> capturedEnvironment, VarInstance<? extends VarDef> vi) {
         if (!containsDef(capturedEnvironment, vi)) {
             capturedEnvironment.add(vi);
             //if (vi instanceof ThisInstance)
             //    System.err.println("Closure at "+position()+" captures this");
         }
     }

     public void addCapturedVariable(VarInstance<? extends VarDef> vi) {
         addCapturedVariable(this.capturedEnvironment, vi);
     }

     private boolean isStatic;

     public boolean staticContext() {
         return isStatic;
     }
     
     public void setStaticContext(boolean v) {
         isStatic = v;
     }
     
     
     public String signature() {
         return "(" + CollectionUtil.listToString(formalTypes) + ")" + Types.get(guard());
     }

     public String designator() {
         return "closure";
     }

     public String toString() {
         return designator() + " " + signature() + " => " + returnType();
     }
}
