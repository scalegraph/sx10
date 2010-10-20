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
import java.util.HashMap;

import polyglot.types.ClassDef;
import polyglot.types.ClassType;
import polyglot.types.CodeDef;
import polyglot.types.Context;
import polyglot.types.FieldInstance;
import polyglot.types.LocalDef;
import polyglot.types.LocalInstance;
import polyglot.types.MethodInstance;
import polyglot.types.Name;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.VarDef;
import x10.constraint.XFailure;
import x10.constraint.XVar;
import x10.constraint.XTerm;
import x10.types.constraints.CConstraint;
import x10.types.constraints.TypeConstraint;
import x10.types.constraints.XConstrainedTerm;

public interface X10Context extends Context {
	/**
	 * The prefix for compiler generated variables. No user-specified
	 * type or package or parameter name or local variable should begin
	 * with this prefix.
	 */
	String MAGIC_VAR_PREFIX = "x10$__var";
	// Use addVariable to add a PropertyInstance to the context.

	/** Context name table */
	String MAGIC_NAME_PREFIX = "X10$";
	HashMap<String,Name> contextNameTable = new HashMap<String,Name>();
	/** Return the same mangled name if has been created using the same string. */ 
	Name makeFreshName(String name);
	
	/** Return the locals declared in this scope (and only this scope). */
	List<LocalDef> locals();
	
	/** Current constraint on variables in scope. */
	CConstraint currentConstraint();
	void setCurrentConstraint(CConstraint c);

	/**
	 * Return any known constraint on this.home (as an XConstrainedTerm).
	 * If none is known, return null.
	 * 
	 * @return
	 */
	XConstrainedTerm currentPlaceTerm();

	/**
	 * Push a new context, and set currentPlaceTerm to t.
	 * Intended to be set when entering the scope of a place changing control construct
	 * such as at(p) S, or when entering the body of a method. 
	 * @param t, t != null
	 */
	X10Context pushPlace(XConstrainedTerm t);
	
	/**
	 * Get the place for this. When entering a class decl, thisPlace
	 * is set to the currentPlaceTerm. Thus |this| will not be in the currentPlace 
	 * within the scope of at's in the bodies of methods in the class. 
	 * 
	 * @param t
	 */
	XConstrainedTerm currentThisPlace();
	
	/**
	 * We are entering the scope of a collecting finish. All offers
	 * within this scope must return an expression of type t.
	 * @param t -- the type of the collecting finish.
	 * @return
	 */
	X10Context pushCollectingFinishScope(Type t);
	
	/**
	 * The type of the collecting finish whose scope we are in.
	 * null if we are not in the scope of a collecting finish
	 * @return
	 */
	Type collectingFinishType();
	
	/** Current constraint on here. */
	//CConstraint currentPlaceConstraint();
	
	//void setCurrentPlaceConstraint(CConstraint c);

	/** Current constraint on type variables in scope */
	TypeConstraint currentTypeConstraint();
	void setCurrentTypeConstraint(Ref<TypeConstraint> c);

	/**
	 * Looks up a property in the current scope.
	 * @param name
	 * @return
	 * @throws SemanticException
	 */
	X10FieldInstance findProperty(Name name) throws SemanticException;
	
	/**
     * Finds the type which added a property to the scope.
     * This is usually a subclass of <code>findProperty(name).container()</code>.
     */
    X10ClassType findPropertyScope(Name name) throws SemanticException;
	
    /**
     * Looks up a method in the current scope.
     */
    X10MethodInstance findMethod(X10TypeSystem_c.MethodMatcher matcher) throws SemanticException;

    /** Looks up a local variable in the current scope. */
    X10LocalInstance findLocal(Name name) throws SemanticException;

    /** Looks up a field in the current scope. */
    X10FieldInstance findField(Name name) throws SemanticException;

    /**
     * Finds the type which added a field to the scope.
     * This is usually a subclass of <code>findField(name).container()</code>.
     */
    X10ClassType findFieldScope(Name name) throws SemanticException;
    
    /**
     * Finds the type which added a method to the scope.
     * This is usually a subclass of <code>findMethod(name).container()</code>.
     */
    X10ClassType findMethodScope(Name name) throws SemanticException;

    // Set if we are in a supertype declaration of this type. 
    boolean inSuperTypeDeclaration();
    X10ClassDef supertypeDeclarationType();
    X10Context pushSuperTypeDeclaration(X10ClassDef type);

    /**
     * Disambiguating the LHS of an assignment?
     * @return
     */
    boolean inAssignment();
    void setInAssignment();
    X10Context pushAssignment();
    
    X10Context pushFinishScope(boolean isClocked);
    boolean inClockedFinishScope();
    /**
     * Push a new block, and sets its currentConstraint to old currentConstraint + env.
     * 
     * @param env: The new constraint to be pushed. Should have no self var.
     * @return
     * @throw SemanticException if adding this constraint would cause inconsistency
     */
    X10Context pushAdditionalConstraint(CConstraint env) throws SemanticException ;
    
    /** Enter the scope of a deptype. */
    X10Context pushDepType(Ref<? extends Type> ref);
    
    /** Return the current deptype, null if there is none. */
    X10NamedType currentDepType();
    Ref<? extends Type> depTypeRef();

    /** Return whether innermost scope is a deptype scope. */
    boolean inDepType();
    
    /**
     * Enter the scope of an atomic block. The body of such a block must be local,
     * sequential and nonblocking.
     * @return a new context
     */
    X10Context pushAtomicBlock(); 
    
    Name getNewVarName();
    
    void setVarWhoseTypeIsBeingElaborated(VarDef var);
    VarDef varWhoseTypeIsBeingElaborated();

    /** Return true if within an annotation. */
    boolean inAnnotation();
    void setAnnotation();
    void clearAnnotation();
    
    /**
     * Set that the body of a new Object() {...} has been entered. This is done
     * during code generation, e.g. for dep type casts. Now, references to this
     * must be fully qualified.
     */
    void setAnonObjectScope();
    boolean inAnonObjectScope();
    void restoreAnonObjectScope(boolean anonObjectScope);

    X10CodeDef definingCodeDef(Name name);

    XVar thisVar();

    CConstraint constraintProjection(CConstraint... cs) throws XFailure;
    
    /** 
     * Is the current code context clocked?
     * @return
     */
    boolean isClocked();
    
    X10Context pop();
    X10TypeSystem typeSystem();
    X10Context pushClass(ClassDef classScope, ClassType type);
    X10Context pushBlock();
    X10Context pushStatic();
    X10ClassDef currentClassDef();
    X10ClassType currentClass();
}
