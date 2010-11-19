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
import java.util.Iterator;
import java.util.List;

import polyglot.frontend.Source;
import polyglot.types.ClassDef;
import polyglot.types.ClassDef_c;
import polyglot.types.ConstructorDef;
import polyglot.types.Context;
import polyglot.types.FieldDef;
import polyglot.types.Flags;
import polyglot.types.LazyRef_c;
import polyglot.types.Name;
import polyglot.types.Package;
import polyglot.types.QName;
import polyglot.types.Ref;
import polyglot.types.Ref_c;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.types.ClassType;
import polyglot.util.FilteringList;
import polyglot.util.Predicate;
import polyglot.util.Transformation;
import polyglot.util.TransformingList;
import polyglot.util.TypedList;
import x10.constraint.XFailure;
import x10.constraint.XName;
import x10.constraint.XNameWrapper;
import x10.constraint.XVar;
import x10.constraint.XTerm;
import x10.constraint.XTerms;
import x10.types.constraints.CConstraint;
import x10.types.constraints.CConstraint;
import x10.types.constraints.TypeConstraint;

public class X10ClassDef_c extends ClassDef_c implements X10ClassDef {
    private static final long serialVersionUID = -4644427081636650171L;

    protected List<ParameterType.Variance> variances;
    
    public X10ClassDef_c(TypeSystem ts, Source fromSource) {
        super(ts, fromSource);
        this.variances = new ArrayList<ParameterType.Variance>();
        this.typeParameters = new ArrayList<ParameterType>();
        this.typeMembers = new ArrayList<TypeDef>();
        this.thisDef = null;
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
    
    // Cached realClause of the root type.
    protected SemanticException rootClauseInvalid;

    /**
     * Set the realClause for this type. The realClause is the conjunction of the
     * depClause and the baseClause for the type -- it represents all the constraints
     * that are satisfied by an instance of this type. The baseClause is the invariant for
     * the base type. If the base type C has defined properties P1 p1, ..., Pk pk, 
     * and inherits from type B, then the baseClause for C is the baseClause for B
     * conjoined with r1[self.p1/self, self/this] && ... && rk[self.pk/self, self/this]
     * where ri is the realClause for Pi.
     * 
     * @return
     */
    boolean computing = false;

    Ref<TypeConstraint> typeBounds;
    
    public Ref<TypeConstraint> typeBounds() {
        return typeBounds;
    }

    public void setTypeBounds(Ref<TypeConstraint> c) {
        this.typeBounds = c;
    }

    // Cached realClause of the root type.
    Ref<CConstraint> rootClause;

    protected Ref<CConstraint> classInvariant;

    public void setClassInvariant(Ref<CConstraint> c) {
        this.classInvariant = c;
        this.rootClause = null;
        this.rootClauseInvalid = null;
    }

    public void setRootClause(Ref<CConstraint> c) {
    	this.rootClause = c;
    	this.rootClauseInvalid = null;
    }

    public Ref<CConstraint> classInvariant() {
        return classInvariant;
    }
    
    public void checkRealClause() throws SemanticException {
	if (rootClauseInvalid != null)
	    throw rootClauseInvalid;
    }
    
    public CConstraint getRootClause() {
	    if (rootClause == null) {
		    if (computing) {
			    /*this.rootClause = Types.<CConstraint>ref(new CConstraint());
			    this.rootClauseInvalid = 
				    new SemanticException("The real clause of " + this + " depends upon itself.", position());
			    return rootClause.get();*/
		    	return new CConstraint();
		    }
		    
		    computing = true;
		    
		    try {
			    List<X10FieldDef> properties = properties();
			    
			    TypeSystem xts = (TypeSystem) ts;

			    CConstraint result = new CConstraint();
			    
			    XVar oldThis = xts.xtypeTranslator().transThisWithoutTypeConstraint();
			    
			    try {
				    // Add in constraints from the supertypes.  This is
				    // no need to change self, and no occurrence of this is possible in 
				    // a type's base constraint.
			    	// vj: 08/12/09. Incorrect. this can occur in a type's base constraint.
				    {
					    Type type = Types.get(superType());
					    if (type != null) {
						CConstraint rs = X10TypeMixin.realX(type);
						if (rs != null) {
							if (rs.thisVar() != null)
								rs = rs.substitute(oldThis, (XVar) rs.thisVar());
						    result.addIn(rs);
						}
					    }
				    }

				    // Add in constraints from the interfaces.
				    for (Iterator<Ref<? extends Type>> i = interfaces().iterator(); i.hasNext(); ) {
					    Ref<? extends Type> it = (Ref<? extends Type>) i.next();
					    CConstraint rs = X10TypeMixin.realX(it.get());
					    // no need to change self, and no occurrence of this is possible in 
					    // a type's base constraint.
					    if (rs != null) {
//						    rs = rs.substitute(rs.self(), oldThis);
						    result.addIn(rs);
					    }
				    }
				    
				    // add in the bindings from the property declarations.
				    for (X10FieldDef fi : properties) {
					    Type type = fi.asInstance().type();   // ### check for recursive call here
					    XVar fiThis = fi.thisVar();
					    CConstraint rs = X10TypeMixin.realX(type);
					    if (rs != null) {
						    // Given: C(:c) f
						    // Add in: c[self.f/self,self/this]
						    XTerm newSelf = xts.xtypeTranslator().trans(rs, rs.self(), fi.asInstance());
						    CConstraint rs1 = rs.substitute(newSelf, rs.self());
						    CConstraint rs2;
						    if (fiThis != null)
						        rs2 = rs1.substitute(rs1.self(), fiThis);
						    else
						        rs2 = rs1;
						    result.addIn(rs2);
					    }
				    }

				    // Finally, add in the class invariant.
				    // It is important to do this last since we need avoid type-checking constraints
				    // until after the base type of the supertypes are resolved.
				    CConstraint ci = Types.get(classInvariant);
				    if (ci != null) {
					ci = ci.substitute(ci.self(), oldThis);
					result.addIn(ci);
				    }
				    
			    }
			    catch (XFailure f) {
				    result.setInconsistent();
				    this.rootClause = Types.ref(result);
				    this.rootClauseInvalid = new SemanticException("The class invariant and property constraints of " + this + " are inconsistent.", position());
			    }
			    
			    // Now, set the root clause and mark that we're no longer computing.
			    this.rootClause = Types.ref(result);
			    this.computing = false;
			    
			    // Now verify that the root clause entails the assertions of the properties.
			    // We need to set the root clause first, to avoid a spurious report of a cyclic dependency.
			    // This can happen when one of the properties is subtype of this type:
			    // class Ref(home: Place) { }
			    // class Place extends Ref { ... }
			    
			    // Disable this for now since it can cause an infinite loop.
			    // TODO: vj 08/12/09 Revisit this.
			    /*if (false && result.consistent()) {
				    // Verify that the realclause, as it stands, entails the assertions of the 
				    // property.
				    for (X10FieldDef fi : properties) {
					    Type ftype = fi.asInstance().type();
					    
					    CConstraint c = X10TypeMixin.realX(ftype);
					    XTerm newSelf = xts.xtypeTranslator().trans(c, c.self(), fi.asInstance());
					    c = c.substitute(newSelf, c.self());
					    
					    if (! result.entails(c, ((X10Context) ts.emptyContext()).constraintProjection(result, c))) {
						    this.rootClause = Types.ref(result);
						    this.rootClauseInvalid = 
							    new SemanticException("The real clause, " + result + ", does not satisfy constraints from " + fi + ".", position());
					    }
				    }
			    }*/
		    }
		  /*  catch (XFailure e) {
		    	CConstraint result = new CConstraint();
			    result.setInconsistent();
			    this.rootClause = Types.ref(result);
			    this.rootClauseInvalid = new SemanticException(e.getMessage(), position());
		    }*/
		    finally {
			    computing = false;
		    }
	    }
	    
	    assert rootClause != null;
	    return rootClause.get();
    }

    public boolean isJavaType() {
        return fromJavaClassFile();
    }

    public List<X10FieldDef> properties() {
	List<X10FieldDef> x10fields = new TransformingList<FieldDef, X10FieldDef>(fields(),
	    new Transformation<FieldDef,X10FieldDef>() {
	        public X10FieldDef transform(FieldDef o) {
	            return (X10FieldDef) o;
	        }
	});
        return new FilteringList<X10FieldDef>(x10fields, new Predicate<X10FieldDef>() {
            public boolean isTrue(X10FieldDef o) {
                return o.isProperty();
            }
        });
    }
    
    List<ParameterType> typeParameters;
    
    public List<ParameterType> typeParameters() {
	return Collections.unmodifiableList(typeParameters);
    }
    
    public List<ParameterType.Variance> variances() {
	return Collections.unmodifiableList(variances);
    }
    public ParameterType.Variance getVariance(ParameterType t) {
        int index = typeParameters.indexOf(t);
        assert index!=-1 : "Param "+t+" not found in "+typeParameters;
        return variances.get(index);
    }
    
    public void addTypeParameter(ParameterType p, ParameterType.Variance v) {
	typeParameters = new ArrayList<ParameterType>(typeParameters);
	typeParameters.add(p);
	variances = new ArrayList<ParameterType.Variance>(variances);
	variances.add(v);
    }

    List<TypeDef> typeMembers;

    // TODO:
    // Add .class property
    // Add class <: C to class invariant
    // Add code to not complain that it's not initialized; ignore when disambiguating C[T]
    public List<TypeDef> memberTypes() {
    	return Collections.unmodifiableList(typeMembers);
    }

    public void addMemberType(TypeDef t) {
    	typeMembers.add(t);
    }
    public Ref<TypeConstraint> typeGuard() {
    	return new LazyRef_c<TypeConstraint>(X10TypeMixin.parameterBounds(asType()));
    }
    public boolean isStruct() {
    	return X10Flags.toX10Flags(flags()).isStruct();
    }
    // This is overridden by the synthetic Fun_** classes created in X10TypeSystem_c.
    public boolean isFunction() {
    	return false;
    }
    private String typeParameterString() {
    	return ""; //(typeParameters.isEmpty() ? "" : typeParameters.toString());
    }
    public String toString() {
        Name name = name();
        
        if (kind() == null) {
            return "<unknown class " + name + typeParameterString() + ">";
        }
    
        if (kind() == ANONYMOUS) {
            if (interfaces != null && ! interfaces.isEmpty()) {
                return isFunction() ? "" + interfaces.get(0) : "<anonymous subtype of " + interfaces.get(0) + typeParameters() + ">";
            }
            if (superType != null) {
                return "<anonymous subclass of " + superType + ">" + typeParameterString();
            }
        }
    
        if (kind() == TOP_LEVEL) {
            Package p = Types.get(package_());
            return (p != null ? p.toString() + "." : "") + name + typeParameterString();
        }
        else if (kind() == MEMBER) {
            ClassDef outer = Types.get(outer());
            return (outer != null ? outer.toString() + "." : "") + name + typeParameterString();
        }
        else {
            return name.toString() + typeParameterString();
        }
    }
    
    /** Get the class's flags. */
    /** Override the implementation for Java in ClassDef_c which ignores the flag if the class is anonymous.
     * In the current X10 implementation closure types are final anonymous classes.
     */
    @Override
    public X10Flags flags() {
       if (kind() == ANONYMOUS)
            return X10Flags.toX10Flags(Flags.FINAL);
        return X10Flags.toX10Flags(flags);
    }

    public X10ClassType asType() {
        return (X10ClassType) super.asType();
    }
    
    public boolean hasDeserializationConstructor(Context context) {
        for (ConstructorDef cd: constructors()) {
            if (cd.formalTypes().size() == 1) {
                Type type = cd.formalTypes().get(0).get();
                if (type.isSubtype(type.typeSystem().SerialData(), context)) {
                    return true;
                }
            }
        }
        return false;
    }
    
}
