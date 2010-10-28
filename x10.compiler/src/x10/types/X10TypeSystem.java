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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import polyglot.ast.Binary;
import polyglot.ast.Expr;
import polyglot.ast.Id;
import polyglot.ast.Receiver;
import polyglot.ast.Unary;
import polyglot.frontend.Source;
import polyglot.types.ClassDef;
import polyglot.types.ClassType;
import polyglot.types.CodeDef;
import polyglot.types.CodeInstance;
import polyglot.types.ConstructorDef;
import polyglot.types.ConstructorInstance;
import polyglot.types.Context;
import polyglot.types.FieldDef;
import polyglot.types.FieldInstance;
import polyglot.types.Flags;
import polyglot.types.LazyRef;
import polyglot.types.LocalDef;
import polyglot.types.LocalInstance;
import polyglot.types.MethodDef;
import polyglot.types.MethodInstance;
import polyglot.types.Name;
import polyglot.types.ParsedClassType;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.types.StructType;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.TypeSystem_c;
import polyglot.types.VarDef;
import polyglot.types.TypeSystem_c.ConstructorMatcher;
import polyglot.types.TypeSystem_c.FieldMatcher;
import polyglot.types.TypeSystem_c.MethodMatcher;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import x10.constraint.XLit;
import x10.constraint.XVar;
import x10.constraint.XTerm;
import x10.types.constraints.CConstraint;
import x10.types.constraints.TypeConstraint;
import x10.types.constraints.XConstrainedTerm;

/**
 * Parts of this code are taken from the pao extension in the polyglot
 * framework.
 *
 * @author Christoph von Praun
 * @author vj
 */
public interface X10TypeSystem extends TypeSystem {
	public Name DUMMY_PACKAGE_CLASS_NAME = Name.make("_");

    boolean isSubtype(Type t1, Type t2, Context context);

    // empty context
    boolean isSubtype(Type t1, Type t2);
    /**
     * Add an annotation to a type object, optionally replacing existing
     * annotations that are subtypes of annoType.
     */
    void addAnnotation(X10Def o, Type annoType, boolean replace);

    AnnotatedType AnnotatedType(Position pos, Type baseType, List<Type> annotations);

    X10MethodInstance findImplementingMethod(ClassType ct, MethodInstance jmi, boolean includeAbstract, Context context);

    Type boxOf(Position p, Ref<? extends Type> t);

    Type futureOf(Position p, Ref<? extends Type> t);

    FieldMatcher FieldMatcher(Type container, boolean contextKnowsReceiver, Name name, Context context);
    MethodMatcher MethodMatcher(Type container, Name name, List<Type> argTypes, Context context);
    MethodMatcher MethodMatcher(Type container, Name name, List<Type> typeArgs,  List<Type> argTypes, Context context);

    ConstructorMatcher ConstructorMatcher(Type container, List<Type> argTypes, Context context);

    /**
     * Returns the field named 'name' defined on 'type'.
     * @exception SemanticException if the field cannot be found or is
     * inaccessible.
     */
    X10FieldInstance findField(Type container, FieldMatcher matcher) throws SemanticException;

    /**
     * Find matching fields.
     *
     * @exception SemanticException if no matching field can be found.
     */
    Set<FieldInstance> findFields(Type container, FieldMatcher matcher) throws SemanticException;

    /**
     * Find a method. We need to pass the class from which the method is being
     * found because the method we find depends on whether the method is
     * accessible from that class. We also check if the field is accessible from
     * the context 'c'.
     *
     * @exception SemanticException
     *                    if the method cannot be found or is inaccessible.
     */
    X10MethodInstance findMethod(Type container, MethodMatcher matcher) throws SemanticException;

    /**
     * Find matching methods.
     *
     * @exception SemanticException if no matching method can be found.
     */
    Collection<X10MethodInstance> findMethods(Type container, MethodMatcher matcher) throws SemanticException;

    /**
     * Find a constructor. We need to pass the class from which the constructor
     * is being found because the constructor we find depends on whether the
     * constructor is accessible from that class.
     *
     * @exception SemanticException
     *                    if the constructor cannot be found or is inaccessible.
     */
    X10ConstructorInstance findConstructor(Type container, TypeSystem_c.ConstructorMatcher matcher) throws SemanticException;

    /**
     * Find matching constructors.
     *
     * @exception SemanticException if no matching constructor can be found.
     */
    Collection<X10ConstructorInstance> findConstructors(Type container, ConstructorMatcher matcher) throws SemanticException;

    X10ClassDef createClassDef(Source fromSource);

    X10ParsedClassType createClassType(Position pos, Ref<? extends ClassDef> def);
    X10ConstructorInstance createConstructorInstance(Position pos, Ref<? extends ConstructorDef> def);
    X10MethodInstance createMethodInstance(Position pos, Ref<? extends MethodDef> def);
    X10FieldInstance createFieldInstance(Position pos, Ref<? extends FieldDef> def);
    X10LocalInstance createLocalInstance(Position pos, Ref<? extends LocalDef> def);

    /**
     * Create a <code>ClosureType</code> with the given signature.
     */
    ClosureInstance createClosureInstance(Position pos, Ref<? extends ClosureDef> def);

    /**
     * Returns an immutable list of all the interfaces
     * which the type implements excluding itself and x10.lang.Object.
     * This is different from {@link #Interface()} in that this method
     * traverses the class hierarchy to collect all implemented interfaces
     * instead of shallowly returning just the interfaces directly implemented
     * by the type.
     */
    List<X10ClassType> allImplementedInterfaces(X10ClassType type);

    Type Place(); // needed for here, async (p) S, future (p) e, etc
    // Type Region();

    Type Point(); // needed for destructuring assignment

    Type Dist();

    Type Clock(); // needed for clocked loops

    Type FinishState();

    Type Runtime(); // used by asyncCodeInstance

    //Type Value();

    Type Object();
    Type GlobalRef();
    Type Any();

    Type NativeType();
    Type NativeRep();

    XLit FALSE();

    XLit TRUE();

    XLit NEG_ONE();

    XLit ZERO();

    XLit ONE();

    XLit TWO();

    XLit THREE();

    XLit NULL();

    CodeDef asyncCodeInstance(boolean isStatic);

    /**
     * Create a closure instance.
     * @param returnType
     *                The closure's return type.
     * @param argTypes
     *                The closure's formal parameter types.
     * @param thisVar TODO
     * @param typeGuard TODO
     * @param pos
     *                Position of the closure.
     * @param container
     *                Containing type of the closure.
     * @param excTypes
     *                The closure's exception throw types.
     */
    ClosureDef closureDef(Position p, Ref<? extends ClassType> typeContainer, 
    		Ref<? extends CodeInstance<?>> methodContainer, 
    				Ref<? extends Type> returnType,
    				List<Ref<? extends Type>> argTypes, 
    				XVar thisVar, 
    				List<LocalDef> formalNames,
    				Ref<CConstraint> guard,
    			
    				Ref<? extends Type> offerType);

  
    X10ConstructorDef constructorDef(Position pos, Ref<? extends ClassType> container,
            Flags flags, List<Ref<? extends Type>> argTypes,
            Ref<? extends Type> offerType);
    
    X10ConstructorDef constructorDef(Position pos, Ref<? extends ClassType> container, Flags flags, Ref<? extends ClassType> returnType,
            List<Ref<? extends Type>> argTypes, XVar thisVar, List<LocalDef> formalNames, Ref<CConstraint> guard,
            Ref<TypeConstraint> typeGuard, Ref<? extends Type> offerType);

    X10MethodDef methodDef(Position pos, Ref<? extends StructType> container,
            Flags flags, Ref<? extends Type> returnType, Name name,
            List<Ref<? extends Type>> argTypes,  Ref<? extends Type> offerType);
    
    X10MethodDef methodDef(Position pos, Ref<? extends StructType> container, Flags flags, Ref<? extends Type> returnType, Name name,
            List<ParameterType> typeParams, List<Ref<? extends Type>> argTypes, XVar thisVar, List<LocalDef> formalNames,
            Ref<CConstraint> guard, Ref<TypeConstraint> typeGuard, Ref<? extends Type> offerType, Ref<XTerm> body);

    X10FieldDef fieldDef(Position pos, Ref<? extends StructType> container, Flags flags, Ref<? extends Type> type, Name name);

    X10FieldDef fieldDef(Position pos, Ref<? extends StructType> container, Flags flags, Ref<? extends Type> type, Name name,
            XVar thisVar);

    X10LocalDef localDef(Position pos, Flags flags, Ref<? extends Type> type, Name name);

    /**
     * Return the ClassType object for the x10.array.Array class.
     */
    Type Array();

    
    /**
     * Return the ClassType object for the x10.array.DistArray class.
     */
    Type DistArray();

    /**
     * Return the ClassType object for the x10.lang.Rail interface.
     *
     * @return
     */
    Type Rail();

    
    /**
     * Return the ClassType object for the x10.lang.Runtime.Mortal interface.
     */
    Type Mortal();

    boolean isRail(Type t);

    public boolean isRailOf(Type t, Type p);

    boolean isArray(Type t);

    public boolean isArrayOf(Type t, Type p);

    Type Rail(Type arg);

    Type Array(Type arg);

    Type Settable();

    Type Settable(Type domain, Type range);

    Type Iterable();
    Type Iterable(Type index);
    
    Type CustomSerialization();

    boolean isSettable(Type me);

    boolean isAny(Type me);

    boolean isStruct(Type me);
    
    boolean isClock(Type me);

    boolean isPoint(Type me);

    boolean isPlace(Type me);
    
    boolean isStructType(Type me);

    boolean isObjectType(Type me, X10Context context);

    boolean isUByte(Type t);
    boolean isUShort(Type t);
    boolean isUInt(Type t);
    boolean isULong(Type t);

    boolean hasSameClassDef(Type t1, Type t2);
    
    X10TypeEnv env(Context c);

    /**
     * Is a type constrained (i.e. its depClause is != null) If me is a
     * nullable, then the basetype is checked.
     *
     * @param me
     *                Type to check
     * @return true if type has a depClause.
     */
    public boolean isTypeConstrained(Type me);

    XTypeTranslator xtypeTranslator();

    boolean entailsClause(Type me, Type other, X10Context context);
    boolean entailsClause(CConstraint me, CConstraint other, X10Context context, Type selfType);

    /**
     * True if the two types are equal, ignoring their dep clauses.
     * @param other
     * @param context TODO
     *
     * @return
     */

    boolean typeBaseEquals(Type me, Type other, Context context);
    /**
     * True if the two types are equal, ignoring their dep clauses and the dep clauses of their type arguments recursively.
     *
     * @param other
     * @return
     */
    boolean typeDeepBaseEquals(Type me, Type other, Context context);

    boolean equalTypeParameters(List<Type> a, List<Type> b, Context context);

    Type performBinaryOperation(Type t, Type l, Type r, Binary.Operator op);

    Type performUnaryOperation(Type t, Type l, Unary.Operator op);

    TypeDefMatcher TypeDefMatcher(Type container, Name name, List<Type> typeArgs, List<Type> argTypes, Context context);

    MacroType findTypeDef(Type t, TypeDefMatcher matcher, Context context) throws SemanticException;

    List<MacroType> findTypeDefs(Type container, Name name, ClassDef currClass) throws SemanticException;

    Type UByte();

    Type UShort();

    Type UInt();

    Type ULong();

    /** x10.lang.Box *
    Type Box();

    Type boxOf(Ref<? extends Type> base);

    boolean isBox(Type type);
*/
    boolean isFunctionType(Type type);

 

  //  List<ClosureType> getFunctionSupertypes(Type type, X10Context context);

    boolean isInterfaceType(Type toType);

    FunctionType closureType(Position position, Ref<? extends Type> typeRef, 
    	//	List<Ref<? extends Type>> typeParams, 
    		List<Ref<? extends Type>> formalTypes,
            List<LocalDef> formalNames, Ref<CConstraint> guard
           // Ref<TypeConstraint> typeGuard, 
            );


    Type expandMacros(Type arg);

//    /** Run fromType thorugh a coercion function to toType, if possible, returning the return type of the coercion function, or return null. */
//    Type coerceType(Type fromType, Type toType);

    boolean clausesConsistent(CConstraint c1, CConstraint c2, Context context);

    /** Return true if the constraint is consistent. */
    boolean consistent(CConstraint c);
    boolean consistent(TypeConstraint c, X10Context context);

    /** Return true if constraints in the type are all consistent.
     * @param context TODO*/
    boolean consistent(Type t, X10Context context);

    boolean isObjectOrInterfaceType(Type t, X10Context context);

    boolean isParameterType(Type toType);

    Type Region();

    Type Iterator(Type formalType);

    boolean isUnsigned(Type r);

    boolean isSigned(Type l);

    boolean numericConversionValid(Type toType, Type fromType, Object constantValue, Context context);
    
    public Long size(Type t);

    /**
     * Does there exist a struct with the given name, accessible at this point? 
     * Throw an exception if it is not.
     * @param name
     * @param tc
     * @throws SemanticException
     */
    void existsStructWithName(Id name, ContextVisitor tc) throws SemanticException;
   
    boolean isX10Array(Type me);

    boolean isX10DistArray(Type me);

    Context emptyContext();
    boolean isExactlyFunctionType(Type t);
    
    Name homeName();
    
    LazyRef<Type> lazyAny();
    
    ClassType load(String name);

    public boolean isRegion(Type me);

    public boolean isDistribution(Type me);

    public boolean isDistributedArray(Type me);

    public boolean isComparable(Type me);

    public boolean isIterable(Type me);

    public boolean isIterator(Type me);
    public boolean isReducible(Type me);
    public Type Reducible();

    public boolean isUnknown(Type t);
    public boolean hasUnknown(Type t);
}
