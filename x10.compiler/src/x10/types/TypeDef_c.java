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

import java.util.Collections;
import java.util.List;

import polyglot.types.CodeInstance;
import polyglot.types.Def_c;
import polyglot.types.Flags;
import polyglot.types.LocalDef;
import polyglot.types.Name;
import polyglot.types.Package;
import polyglot.types.QName;
import polyglot.types.Ref;
import polyglot.types.StructType;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;
import polyglot.util.TypedList;
import x10.constraint.XVar;
import x10.constraint.XTerm;
import x10.types.constraints.CConstraint;
import x10.types.constraints.TypeConstraint;

public class TypeDef_c extends Def_c implements TypeDef {
	private static final long serialVersionUID = -5353460234705168368L;

	protected Ref<? extends StructType> container;
	protected Flags flags;
	protected Name name;
	protected Ref<? extends Package> package_;
	protected List<ParameterType> typeParameters;
	protected List<LocalDef> formalNames;
	protected List<Ref<? extends Type>> formalTypes;
	protected Ref<CConstraint> guard;
	protected Ref<TypeConstraint> typeGuard;
	protected Ref<? extends Type> type;
	protected MacroType asType;
	
	public TypeDef_c(TypeSystem ts, Position pos, Flags flags, Name name, Ref<? extends StructType> container, List<ParameterType> typeParams,
	        XVar thisVar, List<LocalDef> formalNames, List<Ref<? extends Type>> formalTypes, Ref<CConstraint> guard, Ref<TypeConstraint> typeGuard, Ref<? extends Type> type) {

		super(ts, pos);
		this.container = container;
		this.name = name;
		this.flags = flags;
		this.typeParameters = TypedList.copyAndCheck(typeParams, ParameterType.class, true);
		this.thisVar = thisVar;
		this.formalNames = TypedList.copyAndCheck(formalNames, LocalDef.class, true);
		this.formalTypes = TypedList.copyAndCheck(formalTypes, Ref.class, true);
		this.guard = guard;
		this.typeGuard = typeGuard;
		this.type = type;
	}
	
	public Ref<? extends Type> offerType() {
		return null;
	}
	// BEGIN ANNOTATION MIXIN
	List<Ref<? extends Type>> annotations;

	public List<Ref<? extends Type>> defAnnotations() {
		if (annotations == null) return Collections.<Ref<? extends Type>>emptyList();
		return Collections.unmodifiableList(annotations);
	}

	public void setDefAnnotations(List<Ref<? extends Type>> annotations) {
		this.annotations = TypedList.<Ref<? extends Type>> copyAndCheck(annotations, Ref.class, true);
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

	/* (non-Javadoc)
	 * @see x10.types.TypeDef#asType()
	 */
	public MacroType asType() {
		if (asType == null) {
			asType = new MacroType_c((X10TypeSystem) ts, position(), Types.<TypeDef>ref(this));
			asType = (MacroType) asType.container(Types.get(container));
		}
		return asType;
	}

	public Ref<? extends Package> package_() {
		return this.package_;
	}

	public void setPackage(Ref<? extends Package> p) {
		this.package_ = p;
	}

	/* (non-Javadoc)
	 * @see x10.types.TypeDef#container()
	 */
	public Ref<? extends StructType> container() {
		return container;
	}

	/* (non-Javadoc)
	 * @see x10.types.TypeDef#typeParameters()
	 */
	public List<ParameterType> typeParameters() {
		return Collections.unmodifiableList(typeParameters);
	}

	/* (non-Javadoc)
	 * @see x10.types.TypeDef#setTypeParameters(java.util.List)
	 */
	public void setTypeParameters(List<ParameterType> typeParameters) {
		this.typeParameters = TypedList.copyAndCheck(typeParameters, ParameterType.class, true);
	}

	public Ref<CConstraint> guard() {
		return guard;
	}

	public void setGuard(Ref<CConstraint> guard) {
		this.guard = guard;
	}

	public Ref<TypeConstraint> typeGuard() {
	    return typeGuard;
	}
	
	public void setTypeGuard(Ref<TypeConstraint> typeGuard) {
	    this.typeGuard = typeGuard;
	}
	
	/* (non-Javadoc)
	 * @see x10.types.TypeDef#setContainer(polyglot.types.Ref)
	 */
	public void setContainer(Ref<? extends StructType> container) {
		this.container = container;
	}

	
	/* (non-Javadoc)
	 * @see x10.types.TypeDef#type()
	 */
	public Ref<? extends Type> definedType() {
		return type;
	}
	public Ref<? extends Type> returnType() {
	    return definedType();
	}
	public void setReturnType(Ref<? extends Type> type) {
	    setType(type);
	}

	/* (non-Javadoc)
	 * @see x10.types.TypeDef#setType(polyglot.types.Ref)
	 */
	public void setType(Ref<? extends Type> type) {
		assert type != null;
		this.type = type;
	}

	XVar thisVar;
	public XVar thisVar() {
	    return this.thisVar;
	}

	public void setThisVar(XVar thisVar) {
	    this.thisVar = thisVar;
	}

	public List<LocalDef> formalNames() {
		return Collections.unmodifiableList(formalNames);
	}
	
	public void setFormalNames(List<LocalDef> formalNames) {
		this.formalNames = TypedList.copyAndCheck(formalNames, LocalDef.class, true);
	}
	
	/* (non-Javadoc)
	 * @see x10.types.TypeDef#formalTypes()
	 */
	public List<Ref<? extends Type>> formalTypes() {
		return Collections.unmodifiableList(formalTypes);
	}

	/* (non-Javadoc)
	 * @see x10.types.TypeDef#setFormalTypes(java.util.List)
	 */
	public void setFormalTypes(List<Ref<? extends Type>> formalTypes) {
		this.formalTypes = TypedList.copyAndCheck(formalTypes, Ref.class, true);
	}

	public String toString() {
            String s = "type " + X10Flags.toX10Flags(flags()).prettyPrint() + (container() == null ? "" : container() + ".") + signature() + (guard() != null ? guard() : "") + " = " + definedType();
            return s;
	}

	public Name name() {
		return name;
	}
	
	public void setName(Name name) {
		this.name = name;
	}
	
	public Flags flags() {
		return flags;
	}

	public void setFlags(Flags flags) {
		this.flags = flags;
	}

	public String designator() {
	    return "type";
	}

	public void setThrowTypes(List<Ref<? extends Type>> l) {
	    // TODO Auto-generated method stub
	    
	}

	public String signature() {
	        return name + (typeParameters.isEmpty() ? "" : typeParameters.toString()) + (
	                formalTypes.isEmpty() ? "" : "(" + CollectionUtil.listToString(formalTypes) + ")");
	}

	public List<Ref<? extends Type>> throwTypes() {
	    return Collections.<Ref<? extends Type>>emptyList();
	}

	public CodeInstance<?> asInstance() {
	    return asType();
	}

}
