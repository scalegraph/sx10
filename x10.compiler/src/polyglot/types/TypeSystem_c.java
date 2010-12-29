/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 *
 */

package polyglot.types;

import java.lang.reflect.Modifier;
import java.util.*;

import polyglot.ast.Id;
import polyglot.ast.NodeFactory;
import polyglot.ast.TypeNode;
import polyglot.frontend.*;
import polyglot.main.Report;
import polyglot.types.reflect.ClassFile;
import polyglot.types.reflect.ClassFileLazyClassInitializer;
import polyglot.util.*;
import polyglot.visit.ContextVisitor;
import polyglot.visit.TypeBuilder;
import x10.parser.X10ParsedName;
import x10.types.X10ClassDef_c;
import x10.types.X10ClassDef;
import x10.types.X10ClassType;
import x10.types.X10ConstructorInstance;
import x10.types.X10FieldInstance;
import x10.types.MethodInstance;
import x10.types.X10ParsedClassType;
import x10.types.constraints.CConstraint;
import x10.types.matcher.X10ConstructorMatcher;
import x10.types.matcher.X10FieldMatcher;
import x10.types.matcher.X10MethodMatcher;
import x10.visit.X10TypeBuilder;

/**
 * TypeSystem_c
 *
 * Overview:
 *    A TypeSystem_c is a universe of types, including all Java types.
 **/
public abstract class TypeSystem_c implements TypeSystem
{
    private static volatile int counter = 0;

    protected SystemResolver systemResolver;
    protected TopLevelResolver loadedResolver;
    protected ExtensionInfo extInfo;

    private Throwable creator;
    private int creationTime;
    public TypeSystem_c() {
        creator = new Throwable().fillInStackTrace();
        creationTime = counter++;
        if (Report.should_report("TypeSystem", 1))
            Report.report(1, "Creating " + getClass() + " at " + creationTime);
    }

    /**
     * Initializes the type system and its internal constants (which depend on
     * the resolver).
     */
    public void initialize(TopLevelResolver loadedResolver, ExtensionInfo extInfo)
    throws SemanticException {

	if (Report.should_report(Report.types, 1))
	    Report.report(1, "Initializing " + getClass().getName());

	this.extInfo = extInfo;

	// The loaded class resolver.  This resolver automatically loads types
	// from class files and from source files not mentioned on the command
	// line.
	this.loadedResolver = loadedResolver;

	// The system class resolver. The class resolver contains a map from
	// fully qualified names to instances of Named. A pass over a
	// compilation unit looks up classes first in its
	// import table and then in the system resolver.
	this.systemResolver = new SystemResolver(loadedResolver, extInfo);

	initEnums();
	initTypes();
    }

    protected void initEnums() {
	// Ensure the enums in the type system are initialized and interned
	// before any deserialization occurs.

	// Just force the static initializers of ClassType and PrimitiveType
	// to run.
	Object o;
	o = ClassDef.TOP_LEVEL;
    }

    protected void initTypes() throws SemanticException {
	// FIXME: don't do this when rewriting a type system!

	// Prime the resolver cache so that we don't need to check
	// later if these are loaded.

	// We cache the most commonly used ones in fields.
	/* DISABLED CACHING OF COMMON CLASSES; CAUSES PROBLEMS IF
           COMPILING CORE CLASSES (e.g. java.lang package).
           TODO: Longer term fix. Maybe a flag to tell if we are compiling
                 core classes? XXX
        Object();
        Class();
        String();
        Throwable();

        systemResolver.find("java.lang.Error");
        systemResolver.find("java.lang.Exception");
        systemResolver.find("java.lang.RuntimeException");
        systemResolver.find("java.lang.Cloneable");
        systemResolver.find("java.io.Serializable");
        systemResolver.find("java.lang.NullPointerException");
        systemResolver.find("java.lang.ClassCastException");
        systemResolver.find("java.lang.ArrayIndexOutOfBoundsException");
        systemResolver.find("java.lang.ArrayStoreException");
        systemResolver.find("java.lang.ArithmeticException");
	 */
    }

    /** Return the language extension this type system is for. */
    public ExtensionInfo extensionInfo() {
	return extInfo;
    }

    public SystemResolver systemResolver() {
	return systemResolver;
    }

    /**
     * Return the system resolver.  This used to return a different resolver.
     * enclosed in the system resolver.
     * @deprecated
     */
    public CachingResolver parsedResolver() {
	return systemResolver;
    }

    public TopLevelResolver loadedResolver() {
	return loadedResolver;
    }

    public ClassFileLazyClassInitializer classFileLazyClassInitializer(ClassFile clazz) {
	return new ClassFileLazyClassInitializer(clazz, this);
    }

    public ImportTable importTable(String sourceName, Ref<? extends Package> pkg) {
	assert_(pkg);
	return new ImportTable(this, pkg, sourceName);
    }

    public ImportTable importTable(Ref<? extends Package> pkg) {
	assert_(pkg);
	return new ImportTable(this, pkg);
    }

    /**
     * Returns true if the package named <code>name</code> exists.
     */
    public boolean packageExists(QName name) {
	return systemResolver.packageExists(name);
    }

    protected void assert_(Collection<?> l) {
	for (Iterator<?> i = l.iterator(); i.hasNext(); ) {
	    Object o = i.next();
	    if (o instanceof TypeObject) {
		assert_((TypeObject) o);
	    }
	    else if (o instanceof Ref<?>) {
		assert_((Ref<?>) o);
	    }
	}
    }

    protected void assert_(Ref<?> ref) { }

    protected void assert_(TypeObject o) {
	if (o != null && o.typeSystem() != this) {
            TypeSystem_c ots = (TypeSystem_c) o.typeSystem();
            System.err.print("we are " + this + "(" + creationTime + "): ");
            this.creator.printStackTrace(System.err);
            System.err.print(" but " + o + " ("+o.getClass()+")" + " is from " + ots + "(" + ots.creationTime + "): ");
            ots.creator.printStackTrace(System.err);
            throw new InternalCompilerError("we are " + this + " but " + o + " ("+o.getClass()+")" +
                                            " is from " + ots);
	}
    }

    public String wrapperTypeString(Type t) {
	assert_(t);

	if (t.isBoolean())
	    return "java.lang.Boolean";
	if (t.isChar())
	    return "java.lang.Character";
	if (t.isByte())
	    return "java.lang.Byte";
	if (t.isShort())
	    return "java.lang.Short";
	if (t.isInt())
	    return "java.lang.Integer";
	if (t.isLong())
	    return "java.lang.Long";
	if (t.isFloat())
	    return "java.lang.Float";
	if (t.isDouble())
	    return "java.lang.Double";
	if (t.isVoid())
	    return "java.lang.Void";

	throw new InternalCompilerError("Unrecognized primitive type " + t);
    }

    /** @deprecated */
    public Resolver packageContextResolver(Resolver cr, Package p) {
	return packageContextResolver(p);
    }

    public AccessControlResolver createPackageContextResolver(Package p) {
	assert_(p);
	return new PackageContextResolver(this, p);
    }

    public Resolver packageContextResolver(Package p, ClassDef accessor, Context context) {
	if (accessor == null) {
	    return p.resolver();
	}
	else {
	    return new AccessControlWrapperResolver(createPackageContextResolver(p), context);
	}
    }

    public Resolver packageContextResolver(Package p) {
	assert_(p);
	return packageContextResolver(p, null, emptyContext());
    }

    public Resolver classContextResolver(final Type type, Context context) {
	assert_(type);
	if (context == null) {
	    if (type instanceof ClassType) {
		return ((ClassType) type).resolver();
	    }
	    return new Resolver() {
		public Named find(Matcher<Named> matcher) throws SemanticException {
		    throw new NoClassException(matcher.name().toString(), type);
		}
	    };
	}
	else {
	    return new AccessControlWrapperResolver(createClassContextResolver(type), context);
	}
    }

    public Resolver classContextResolver(Type type) {
	return classContextResolver(type, emptyContext());
    }

    public AccessControlResolver createClassContextResolver(Type type) {
	assert_(type);
	return new ClassContextResolver(this, type);
    }

    public ConstructorDef defaultConstructor(Position pos,
	    Ref<? extends ClassType> container) {
	assert_(container);

	// access for the default constructor is determined by the
	// access of the containing class. See the JLS, 2nd Ed., 8.8.7.
	Flags access = Flags.NONE;
	Flags flags = container.get().flags();
	if (flags.isPrivate()) {
	    access = access.Private();
	}
	if (flags.isProtected()) {
	    access = access.Protected();
	}
	if (flags.isPublic()) {
	    access = access.Public();
	}
	return constructorDef(pos, container,
	                      access, Collections.<Ref<? extends Type>>emptyList()
	                      );
    }

    public ConstructorDef constructorDef(Position pos,
	    Ref<? extends ClassType> container,
	    Flags flags, List<Ref<? extends Type>> argTypes
	    ) {
	assert_(container);
	assert_(argTypes);

	return new ConstructorDef_c(this, pos, container, flags,
	                            argTypes);
    }

    public InitializerDef initializerDef(Position pos,
	    Ref<? extends ClassType> container,
	    Flags flags) {
	assert_(container);
	return new InitializerDef_c(this, pos, container, flags);
    }

    public MethodDef methodDef(Position pos,
	    Ref<? extends ContainerType> container, Flags flags,
	    Ref<? extends Type> returnType, Name name,
	    List<Ref<? extends Type>> argTypes) {

	assert_(container);
	assert_(returnType);
	assert_(argTypes);
	return new MethodDef_c(this, pos, container, flags,
	                       returnType, name, argTypes);
    }

    public ClassDef classDefOf(Type t) {
	if (t instanceof ClassType)
	    return ((ClassType) t).def();
	return null;
    }

    /**
     * Returns true iff child and ancestor are object types and child descends
     * from or equals ancestor.
     **/
    public boolean descendsFrom(ClassDef child, ClassDef ancestor) {
	if (child == ancestor)
	    return true;

	ClassDef o = classDefOf(Object());

	if (ancestor == o)
	    return true;

	if (child == o)
	    return false;

	Type sup = Types.get(child.superType());

	if (sup != null && classDefOf(sup) != null) {
	    if (descendsFrom(classDefOf(sup), ancestor)) {
		return true;
	    }
	}

	// Next check interfaces.
	for (Ref<? extends Type> parentType : child.interfaces()) {
	    Type t = Types.get(parentType);
	    if (t != null && classDefOf(t) != null) {
		if (descendsFrom(classDefOf(t), ancestor)) {
		    return true;
		}
	    }
	}

	return false;
    }

    /**
     * Requires: all type arguments are canonical.  ToType is not a NullType.
     *
     * Returns true iff a cast from fromType to toType is valid; in other
     * words, some non-null members of fromType are also members of toType.
     **/
    public boolean isCastValid(Type fromType, Type toType, Context context) {
	assert_(fromType);
	assert_(toType);
	return env(context).isCastValid(fromType, toType);
    }

    /**
     * Requires: all type arguments are canonical.
     *
     * Returns true iff an implicit cast from fromType to toType is valid;
     * in other words, every member of fromType is member of toType.
     *
     * Returns true iff child and ancestor are non-primitive
     * types, and a variable of type child may be legally assigned
     * to a variable of type ancestor.
     *
     */
    public boolean isImplicitCastValid(Type fromType, Type toType, Context context) {
	assert_(fromType);
	assert_(toType);
	return env(context).isImplicitCastValid(fromType, toType);
    }

    /**
     * Returns true iff type1 and type2 represent the same type object.
     */
    public boolean equals(TypeObject type1, TypeObject type2) {
	assert_(type1);
	assert_(type2);
	if (type1 == type2) return true;
	if (type1 == null || type2 == null) return false;
	return type1.equalsImpl(type2);
    }

    public final void equals(Type type1, Type type2) {
	assert false;
    }

    public final void equals(Type type1, TypeObject type2) {
	assert false;
    }

    public final void equals(TypeObject type1, Type type2) {
	assert false;
    }

    /**
     * Returns true iff type1 and type2 are equivalent.
     */
    public boolean typeEquals(Type type1, Type type2, Context context) {
	assert_(type1);
	assert_(type2);
	return env(context).typeEquals(type1, type2);
    }

    /**
     * Returns true iff type1 and type2 are equivalent.
     */
    public boolean packageEquals(Package type1, Package type2) {
	assert_(type1);
	assert_(type2);
	return env(emptyContext()).packageEquals(type1, type2);
    }

    /**
     * Returns true if <code>value</code> can be implicitly cast to Primitive
     * type <code>t</code>.
     */
    public boolean numericConversionValid(Type t, Object value, Context context) {
	assert_(t);
	return env(context).numericConversionValid(t, value);
    }

    /**
     * Returns true if <code>value</code> can be implicitly cast to Primitive
     * type <code>t</code>.  This method should be removed.  It is kept for
     * backward compatibility.
     */
    public boolean numericConversionValid(Type t, long value, Context context) {
	assert_(t);
	return env(context).numericConversionValid(t, value);
    }

    ////
    // Functions for one-type checking and resolution.
    ////

    /**
     * Checks whether the member mi can be accessed from Context "context".
     */
    public boolean isAccessible(MemberInstance<?> mi, Context context) {
	assert_(mi);
	return env(context).isAccessible(mi);
    }

    /** True if the class targetClass accessible from the context. */
    public boolean classAccessible(ClassDef targetClass, Context context) {
	assert_(targetClass);
	return env(context).classAccessible(targetClass);
    }

    /** True if the class targetClass accessible from the package pkg. */
    public boolean classAccessibleFromPackage(ClassDef targetClass, Package pkg) {
	assert_(targetClass);
	return env(emptyContext()).classAccessibleFromPackage(targetClass, pkg);
    }

    /**
     * Return true if a member (in an accessible container) or a
     * top-level class with access flags <code>flags</code>
     * in package <code>pkg1</code> is accessible from package
     * <code>pkg2</code>.
     */
    protected boolean accessibleFromPackage(Flags flags, Package pkg1, Package pkg2) {
	return env(emptyContext()).accessibleFromPackage(flags, pkg1, pkg2);
    }

    public boolean isEnclosed(ClassDef inner, ClassDef outer) {
	return inner.asType().isEnclosed(outer.asType());
    }

    public boolean hasEnclosingInstance(ClassDef inner, ClassDef encl) {
	return inner.asType().hasEnclosingInstance(encl.asType());
    }

    public void checkCycles(Type goal) throws SemanticException {
	checkCycles(Types.getDef(goal), Types.getDef(goal));
    }

    protected void checkCycles(X10ClassDef_c curr, X10ClassDef_c goal) throws SemanticException {
        assert_(curr);
        assert_(goal);

        if (curr == null) {
            return;
        }
        final Ref<? extends Type> superRef = curr.superType();
        X10ClassDef_c superType = superRef==null ? null : Types.getDef(superRef.get());

        if (goal == superType) {
        throw new SemanticException("Circular inheritance involving " + goal,curr.position());
        }

        checkCycles(superType, goal);

        for (Ref<? extends Type> siType : curr.interfaces()) {
            X10ClassDef_c si = Types.getDef(siType.get());
            if (si == goal) {
                throw new SemanticException("Circular inheritance involving " + goal,curr.position());
            }
            checkCycles(si, goal);
        }
        final Ref<? extends ClassDef> outerRef = curr.outer();
        if (outerRef!=null) checkCycles((X10ClassDef_c) outerRef.get(), goal);
    }

    ////
    // Various one-type predicates.
    ////

    /**
     * Returns true iff the type t can be coerced to a String in the given
     * Context. If a type can be coerced to a String then it can be
     * concatenated with Strings, e.g. if o is of type T, then the code snippet
     *         "" + o
     * would be allowed.
     */
    public boolean canCoerceToString(Type t, Context c) {
	return env(c).canCoerceToString(t);
    }

    public boolean isNumeric(Type t) {
	return t.isLongOrLess() || t.isFloat() || t.isDouble();
    }

    public boolean isUnsignedNumeric(Type t) {
        return false; // No such thing in Java type system
    }

    public boolean isIntOrLess(Type t) {
	return t.isByte() || t.isShort() || t.isChar() || t.isInt();
    }

    public boolean isLongOrLess(Type t) {
	return t.isByte() || t.isShort() || t.isChar() || t.isInt() || t.isLong();
    }

    public boolean isVoid(Type t) {
	Context context = emptyContext();
	return typeEquals(t, Void(), context);
    }

    public boolean isBoolean(Type t) {
	Context context = emptyContext();
	return isSubtype(t, Boolean(), context);
    }

    public boolean isChar(Type t) {
	Context context = emptyContext();
	return isSubtype(t, Char(), context);
    }

    public boolean isByte(Type t) {
	Context context = emptyContext();
	return isSubtype(t, Byte(), context);
    }

    public boolean isShort(Type t) {
	Context context = emptyContext();
	return isSubtype(t, Short(), context);
    }

    public boolean isInt(Type t) {
	Context context = emptyContext();
	return isSubtype(t, Int(), context);
    }

    public boolean isLong(Type t) {
	Context context = emptyContext();
	return isSubtype(t, Long(), context);
    }

    public boolean isFloat(Type t) {
	Context context = emptyContext();
	return isSubtype(t, Float(), context);
    }

    public boolean isDouble(Type t) {
	Context context = emptyContext();
	return isSubtype(t, Double(), context);
    }

    /**
     * Returns true iff an object of type <type> may be thrown.
     **/
    public boolean isThrowable(Type type) {
	assert_(type);
	Context context = emptyContext();
	return isSubtype(type, Throwable(), context);
    }

    /**
     * Returns a true iff the type or a supertype is in the list
     * returned by uncheckedExceptions().
     */
    public boolean isUncheckedException(Type type) {
	assert_(type);
	Context context = emptyContext();

	if (type.isThrowable()) {
	    for (Type t : uncheckedExceptions()) {
		if (isSubtype(type, t, context)) {
		    return true;
		}
	    }
	}

	return false;
    }

    /**
     * Returns a list of the Throwable types that need not be declared
     * in method and constructor signatures.
     */
    public Collection<Type> uncheckedExceptions() {
	List<Type> l = new ArrayList<Type>(2);
	l.add(Error());
	l.add(RuntimeException());
	return l;
    }

    public boolean isSubtype(Type t1, Type t2, Context context) {
	assert_(t1);
	assert_(t2);
	return env(context).isSubtype(t1, t2);
    }

    public boolean isSubtype(Type t1, Type t2) {
        return isSubtype(t1, t2, emptyContext());
    }
    ////
    // Functions for type membership.
    ////

    /**
     * Returns a set of fields named <code>name</code> defined
     * in type <code>container</code> or a supertype.  The list
     * returned may be empty.
     */
    public Set<FieldInstance> findFields(Type container, TypeSystem_c.FieldMatcher matcher) {
        if (!matcher.visit(container)) return Collections.<FieldInstance>emptySet();
        
	Name name = matcher.name();

	Context context = matcher.context();
	assert_(container);

	if (container == null) {
	    throw new InternalCompilerError("Cannot access field \"" + name +
	    "\" within a null container type.");
	}

	if (container instanceof ContainerType) {
	    FieldInstance fi = ((ContainerType) container).fieldNamed(name);
	    if (fi != null) {
		try {
		    fi = matcher.instantiate(fi);
		    if (fi != null)
			return Collections.singleton(fi);
		}
		catch (SemanticException e) {
		}
		return Collections.<FieldInstance>emptySet();
	    }
	}

	Set<FieldInstance> fields = new HashSet<FieldInstance>();

	if (container instanceof ObjectType) {
	    ObjectType ot = (ObjectType) container;
	    if (ot.superClass() != null && ot.superClass() instanceof ContainerType) {
		Set<FieldInstance> superFields = findFields((ContainerType) ot.superClass(), matcher);
		fields.addAll(superFields);
	    }

	    for (Type it : ot.interfaces()) {
		if (it instanceof ContainerType) {
		    Set<FieldInstance> superFields = findFields((ContainerType) it, matcher);
		    fields.addAll(superFields);
		}
	    }
	}

	return fields;
    }



    public Type findMemberType(Type container, Name name,
	    Context context) throws SemanticException
	    {
	return env(context).findMemberType(container, name);
	    }

    /**
     * Returns the list of methods with the given name defined or inherited
     * into container, checking if the methods are accessible from the
     * body of currClass
     */
    
    public boolean hasMethodNamed(Type container, Name name) {
    	if (container != null)
    		container = Types.baseType(container);

    	// HACK: use the def rather than the type to avoid gratuitous
    	// reinstantiation of methods.
    	if (container instanceof ClassType) {
    		ClassType ct = (ClassType) container;
    		for (MethodDef md : ct.def().methods()) {
    			if (md.name().equals(name))
    				return true;
    		}
    		Type superType = Types.get(ct.def().superType());
    		if (superType != null && hasMethodNamed(superType, name))
    			return true;
    		for (Ref<? extends Type> tref : ct.def().interfaces()) {
    			Type ti = Types.get(tref);
    			if (ti != null && hasMethodNamed(ti, name))
    				return true;
    		}
    	}

    	if (container == null) {
    		throw new InternalCompilerError("Cannot access method \"" + name +
    		"\" within a null container type.");
    	}

    	if (container instanceof ContainerType) {
    		if (! ((ContainerType) container).methodsNamed(name).isEmpty()) {
    			return true;
    		}
    	}

    	if (container instanceof ObjectType) {
    		ObjectType ot = (ObjectType) container;
    		if (ot.superClass() != null && ot.superClass() instanceof ContainerType) {
    			if (hasMethodNamed((ContainerType) ot.superClass(), name)) {
    				return true;
    			}
    		}

    		for (Type it : ot.interfaces()) {
    			if (it instanceof ContainerType) {
    				if (hasMethodNamed((ContainerType) it, name)) {
    					return true;
    				}
    			}
    		}
    	}

    	return false;
    }

    public static abstract class ConstructorMatcher extends BaseMatcher<ConstructorInstance> {
	protected Type container;
	protected List<Type> argTypes;
	protected Context context;

	protected ConstructorMatcher(Type receiverType, List<Type> argTypes, Context context) {
	    super();
	    this.container = receiverType;
	    this.argTypes = argTypes;
	    this.context = context;
	}

	public Context context() {
	    return context;
	}
	public Type container() {
		return container;
	}

	public Name name() {
	    return Name.make("this");
	}

	public String signature() {
	    return container + argumentString();
	}

	public ConstructorInstance instantiate(ConstructorInstance ci) throws SemanticException {
	    TypeSystem ts = ci.typeSystem();
	    if (ci.formalTypes().size() != argTypes.size()) {
		return null;
	    }
	    Context context = ts.emptyContext();
	    if (! ts.callValid(ci, container, argTypes, context)) {
		throw new SemanticException("Cannot invoke " + ci + " on " + signature() + ".");
	    }
	    return ci;
	}

	public String argumentString() {
	    return  "(" + CollectionUtil.listToString(argTypes) + ")";
	}

	public String toString() {
	    return signature();
	}

	public Object key() {
	    return null;
	}
    }

    public abstract static class MethodMatcher extends BaseMatcher<MethodInstance> implements Copy {
	protected Type container;
	protected Name name;
	protected List<Type> argTypes;
	protected Context context;

	protected MethodMatcher(Type container, Name name, List<Type> argTypes, Context context) {
	    super();
	    this.container = container;
	    this.name = name;
	    this.argTypes = argTypes;
	    this.context = context;
	}

	public MethodMatcher container(Type container) {
	    MethodMatcher n = copy();
	    n.container = container;
	    return n;
	}
	public Type container() {
		return container;
	}

	public MethodMatcher copy() {
	    try {
		return (MethodMatcher) super.clone();
	    }
	    catch (CloneNotSupportedException e) {
		throw new InternalCompilerError(e);
	    }
	}

	public String signature() {
	    return name + argumentString();
	}

	public Name name() {
	    return name;
	}

	public MethodInstance instantiate(MethodInstance mi) throws SemanticException {
	    if (! mi.name().equals(name)) {
		return null;
	    }
	    if (mi.formalTypes().size() != argTypes.size()) {
		return null;
	    }
	    TypeSystem ts = mi.typeSystem();
	    if (! ts.callValid(mi, container, argTypes, context)) {
		throw new SemanticException("Cannot invoke " + mi + " on " + signature() + ".");
	    }
	    return mi;
	}

	public String argumentString() {
	    return  "(" + CollectionUtil.listToString(argTypes) + ")";
	}

	public String toString() {
	    return signature();
	}

	public Object key() {
	    return null;
	}

	public Context context() {
	    return context;
	}
    }

    public abstract static class FieldMatcher extends BaseMatcher<FieldInstance> implements Copy {
	protected Type container;
	protected Name name;
	protected Context context;

	protected FieldMatcher(Type container, Name name, Context context) {
	    super();
	    this.container = container;
	    this.name = name;
	    this.context = context;
	}

	public Context context() {
	    return context;
	}

	public Type container() {
		return container;
	}
	public FieldMatcher container(Type container) {
	    FieldMatcher n = copy();
	    n.container = container;
	    return n;
	}

	public FieldMatcher copy() {
	    try {
		return (FieldMatcher) super.clone();
	    }
	    catch (CloneNotSupportedException e) {
		throw new InternalCompilerError(e);
	    }
	}

	public String signature() {
	    return name.toString();
	}

	public Name name() {
	    return name;
	}

	public FieldInstance instantiate(FieldInstance mi) throws SemanticException {
	    if (! mi.name().equals(name)) {
		return null;
	    }
	    return mi;
	}

	public String toString() {
	    return signature();
	}

	public Object key() {
	    return null;
	}
    }


    // To prevent infinite recursion due to searching the field in the superclass/superinterface
    // e.g., class Q extends Q{i==1} {}
    public static abstract class BaseMatcher<T> implements Matcher<T> {
        private HashSet<Type> visitedDefs;
        public boolean visit(Type t) {
            if (visitedDefs==null) visitedDefs = new HashSet<Type>();
            final Type p = Types.baseType(t);
            if (visitedDefs.contains(p)) return false;
            visitedDefs.add(p);
            return true;
        }
    }
    public static class MemberTypeMatcher extends BaseMatcher<Named> {
	protected Type container;
	protected Name name;
	protected Context context;

	protected MemberTypeMatcher(Type container, Name name, Context context) {
	    super();
	    this.container = container;
	    this.name = name;
	    this.context = context;
	}

	public Type container() {
		return container;
	}
	public String signature() {
	    return name.toString();
	}

	public Name name() {
	    return name;
	}

	public Named instantiate(Named t) throws SemanticException {
	    if (! t.name().equals(name)) {
		return null;
	    }
	    return t;
	}

	public String toString() {
	    return signature();
	}

	public Object key() {
	    return name;
	}
    }

    public Matcher<Named> MemberTypeMatcher(Type container, Name name, Context context) {
	return new MemberTypeMatcher(container, name, context);
    }

    public Matcher<Named> TypeMatcher(Name name) {
	return new TypeMatcher(name);
    }

    public static class TypeMatcher extends BaseMatcher<Named> {
	protected Name name;

	protected TypeMatcher(Name name) {
	    super();
	    this.name = name;
	}

	public String signature() {
	    return name.toString();
	}

	public Name name() {
	    return name;
	}


	public Named instantiate(Named t) throws SemanticException {
	    if (! t.name().equals(name)) {
		return null;
	    }
	    return t;
	}

	public String toString() {
	    return signature();
	}

	public Object key() {
	    return name;
	}
    }



    public MethodInstance SUPER_findMethod(Type container, MethodMatcher matcher)
    throws SemanticException {

	assert_(container);

	Context context = matcher.context();

	List<MethodInstance> acceptable = findAcceptableMethods(container, matcher);

	if (acceptable.size() == 0) {
	    throw new NoMemberException(NoMemberException.METHOD,
	                                "No valid method call found for call in given type."
	    		+ "\n\t Call: " + matcher.signature()
	    		+ "\n\t Type: " + container);

	}

	Collection<MethodInstance> maximal =
	    findMostSpecificProcedures(container, acceptable, (Matcher<MethodInstance>) matcher, context);

	if (maximal.size() > 1) {
	    StringBuffer sb = new StringBuffer();
	    for (Iterator<MethodInstance> i = maximal.iterator(); i.hasNext();) {
		MethodInstance ma =  i.next();
		sb.append(ma.container());
		sb.append(".");
		sb.append(ma.signature());
		if (i.hasNext()) {
		    if (maximal.size() == 2) {
			sb.append(" and ");
		    }
		    else {
			sb.append(", ");
		    }
		}
	    }

	    throw new SemanticException("Reference to " + matcher.name() +
	                                " is ambiguous, multiple methods match: "
	                                + sb.toString());
	}

	MethodInstance mi = maximal.iterator().next();
	return mi;
    }

    public ConstructorInstance SUPER_findConstructor(Type container, ConstructorMatcher matcher)
    throws SemanticException {

	assert_(container);
	Context context = matcher.context();

	List<ConstructorInstance> acceptable = findAcceptableConstructors(container, matcher);

	if (acceptable.size() == 0) {
	    throw new NoMemberException(NoMemberException.CONSTRUCTOR,
	                                "No valid constructor found for " + matcher.signature() + ").");
	}

	Collection<ConstructorInstance> maximal = findMostSpecificProcedures(acceptable, matcher, context);

	if (maximal.size() > 1) {
	    throw new NoMemberException(NoMemberException.CONSTRUCTOR,
	                                "Reference to " + container + " is ambiguous, multiple " +
	                                "constructors match: " + maximal);
	}

	ConstructorInstance ci = maximal.iterator().next();
	return ci;
    }

    public <S extends ProcedureDef, T extends ProcedureInstance<S>> Collection<T>
    findMostSpecificProcedures(List<T> acceptable, Matcher<T> matcher, Context context)
    throws SemanticException {
    	return findMostSpecificProcedures(null, acceptable, matcher, context);
    }
    public <S extends ProcedureDef, T extends ProcedureInstance<S>> Collection<T>
    findMostSpecificProcedures(Type container, List<T> acceptable, Matcher<T> matcher, Context context)
    throws SemanticException {

	// now, use JLS 15.11.2.2
	// First sort from most- to least-specific.
	Comparator<T> msc = mostSpecificComparator(container, matcher, context);
	ArrayList<T> acceptable2 = new ArrayList<T>(acceptable); // make into array list to sort

	Collections.<T>sort(acceptable2, msc);

	List<T> maximal = new ArrayList<T>(acceptable2.size());

	Iterator<T> i = acceptable2.iterator();

	T first = i.next();
	maximal.add(first);

	// Now check to make sure that we have a maximal most-specific method.
	while (i.hasNext()) {
	    T p = i.next();

	    if (msc.compare(first, p) == 0) {
		maximal.add(p);
	    }
	}

	if (maximal.size() > 1) {
	    // If exactly one method is not abstract, it is the most specific.
	    List<T> notAbstract = new ArrayList<T>(maximal.size());
	    for (Iterator<T> j = maximal.iterator(); j.hasNext(); ) {
		T p = j.next();
		if (! (p instanceof MemberInstance<?>) || ! ((MemberInstance<?>) p).flags().isAbstract()) {
		    notAbstract.add(p);
		}
	    }

	    if (notAbstract.size() == 1) {
		maximal = notAbstract;
	    }
	    else if (notAbstract.size() == 0) {
		// all are abstract; if all signatures match, any will do.
		Iterator<T> j = maximal.iterator();
		first = j.next();
		S firstDecl = first.def();
		List<Type> firstFormals = new TransformingList<Ref<? extends Type>,Type>(firstDecl.formalTypes(), new DerefTransform<Type>());
		while (j.hasNext()) {
		    T p = j.next();

		    // Use the declarations to compare formals.
		    S pDecl = p.def();

		    List<Type> pFormals = new TransformingList<Ref<? extends Type>,Type>(pDecl.formalTypes(), new DerefTransform<Type>());

		    if (! CollectionUtil.allElementwise(firstFormals, pFormals, new TypeEquals(context))) {
			// not all signatures match; must be ambiguous
			return maximal;
		    }
		}

		// all signatures match, just take the first
		maximal = Collections.<T>singletonList(first);
	    }
	}

	return maximal;
    }

    protected <S extends ProcedureDef, T extends ProcedureInstance<S>> Comparator<T> mostSpecificComparator(Type container, Matcher<T> matcher, Context context) {
	return new MostSpecificComparator<S,T>(context);
    }

    public static class TypeEquals implements Predicate2<Type> {
	Context context;
	public TypeEquals(Context context) {
	    this.context = context;
	}
	public boolean isTrue(Type o, Type p) {
	    TypeSystem ts = context.typeSystem();
	    return ts.typeEquals(o, p, context);
	}
    }

    public static class ImplicitCastValid implements Predicate2<Type> {
	Context context;
	public ImplicitCastValid(Context context) {
	    this.context = context;
	}
	public boolean isTrue(Type o, Type p) {
	    TypeSystem ts = context.typeSystem();
	    return ts.isImplicitCastValid(o, p, context);
	}
    }

    public static class Subtype implements Predicate2<Type> {
	Context context;
	public Subtype(Context context) {
	    this.context = context;
	}
	public boolean isTrue(Type o, Type p) {
	    TypeSystem ts = context.typeSystem();
	    return ts.isSubtype(o, p, context);
	}
    }

    /**
     * Class to handle the comparisons; dispatches to moreSpecific method.
     */
    public static class MostSpecificComparator<S extends ProcedureDef, T extends ProcedureInstance<S>> implements Comparator<T> {
	protected Context context;

	public MostSpecificComparator(Context context) {
	    this.context = context;

	}
	public int compare(T p1, T p2) {
	    if (p1.moreSpecific(null, p2, context))
		return -1;
	    if (p2.moreSpecific(null, p1, context))
		return 1;
	    return 0;
	}
    }
    
    public List<MethodInstance> findAcceptableMethods(Type container, MethodMatcher matcher) throws SemanticException {

        List<MethodInstance> candidates = new ArrayList<MethodInstance>();

        List<Type> types = env(matcher.context()).upperBounds(container, true);
        for (Type t : types) {
            List<MethodInstance> ms = superFindAcceptableMethods(t, matcher);
            candidates.addAll(ms);
        }

        return candidates;
    }
    /**
     * Populates the list acceptable with those MethodInstances which are
     * Applicable and Accessible as defined by JLS 15.11.2.1
     */
    private List<MethodInstance> superFindAcceptableMethods(Type container, MethodMatcher matcher)
    throws SemanticException {

	assert_(container);

	Context context = matcher.context();

	SemanticException error = null;

	// The list of acceptable methods. These methods are accessible from
	// currClass, the method call is valid, and they are not overridden
	// by an unacceptable method (which can occur with protected methods
	// only).
	List<MethodInstance> acceptable = new ArrayList<MethodInstance>();

	// A list of unacceptable methods, where the method call is valid, but
	// the method is not accessible. This list is needed to make sure that
	// the acceptable methods are not overridden by an unacceptable method.
	List<MethodInstance> unacceptable = new ArrayList<MethodInstance>();

	Set<Type> visitedTypes = new HashSet<Type>();

	LinkedList<Type> typeQueue = new LinkedList<Type>();
	typeQueue.addLast(container);

	Q:
	    while (! typeQueue.isEmpty()) {
		Type t = typeQueue.removeFirst();

		if (t instanceof ContainerType) {
		    ContainerType type = (ContainerType) t;

		    for (Type s : visitedTypes) {
			if (typeEquals(type, s, context))
			    continue Q;
		    }

		    if (visitedTypes.contains(type)) {
			continue;
		    }

		    visitedTypes.add(type);

		    if (Report.should_report(Report.types, 2))
			Report.report(2, "Searching type " + type + " for method " + matcher.signature());

		    for (Iterator<MethodInstance> i = type.methodsNamed(matcher.name()).iterator(); i.hasNext(); ) {
			MethodInstance mi = i.next();

			if (Report.should_report(Report.types, 3))
			    Report.report(3, "Trying " + mi);

			try {
				MethodInstance oldmi = mi;
			    mi = matcher.instantiate(mi);

			    if (mi == null) {
				continue;
			    }
			    mi.setOrigMI(oldmi);
			    if (isAccessible(mi, context)) {
				if (Report.should_report(Report.types, 3)) {
				    Report.report(3, "->acceptable: " + mi + " in "
				                  + mi.container());
				}

				acceptable.add(mi);
			    }
			    else {
				// method call is valid, but the method is
				// unacceptable.
				unacceptable.add(mi);
				if (error == null) {
				    error = new NoMemberException(NoMemberException.METHOD,
				                                  "Method " + mi.signature() +
				                                  " in " + container +
				    " is inaccessible.");
				}
			    }

			    continue;
			}
			catch (SemanticException e) {
			    // Treat any instantiation errors as call invalid errors.
			    if (error == null)
				error = new NoMemberException(NoMemberException.METHOD,
				                              "Method " + mi.signature() +
				                              " in " + container +
				                              " cannot be called with arguments " +
				                              matcher.argumentString() + "; " + e.getMessage());
			}

			if (error == null) {
			    error = new NoMemberException(NoMemberException.METHOD,
			                                  "Method " + mi.signature() +
			                                  " in " + container +
			                                  " cannot be called with arguments " +
			                                  matcher.argumentString() + ".");
			}
		    }
		}

		if (t instanceof ObjectType) {
		    ObjectType ot = (ObjectType) t;

		    if (ot.superClass() != null) {
			typeQueue.addLast(ot.superClass());
		    }

		    typeQueue.addAll(ot.interfaces());
		}
	    }

	if (acceptable.size() > 0) {
		// remove any method in acceptable that are overridden by an
		// unacceptable
		// method.
		for (MethodInstance mi : unacceptable) {
		    acceptable.removeAll(mi.overrides(context));
		}
	}

	if (acceptable.size() == 0) {
	    if (error == null) {
	        if (matcher.toString().contains("equals")) {
	            System.out.println("Golden!");
	        }
	            
	    	  throw new NoMemberException(NoMemberException.METHOD,
                      "No valid method call found for call in given type."
	+ "\n\t Call: " + matcher.signature()
	+ "\n\t Type: " + container);
	    }
	    throw error;
	}

	return acceptable;
    }

    /**
     * Populates the list acceptable with those MethodInstances which are
     * Applicable and Accessible as defined by JLS 15.11.2.1
     */
    public List<ConstructorInstance> findAcceptableConstructors(Type container, ConstructorMatcher matcher) throws SemanticException {
	return env(matcher.context()).findAcceptableConstructors(container, matcher);
    }

    /**
     * Returns whether method 1 is <i>more specific</i> than method 2,
     * where <i>more specific</i> is defined as JLS 15.11.2.2
     */
    public <T extends ProcedureDef> boolean moreSpecific(ProcedureInstance<T> p1, ProcedureInstance<T> p2, Context context) {
	return env(context).moreSpecific(p1, p2);
    }

    /**
     * Returns the supertype of type, or null if type has no supertype.
     **/
    public Type superClass(Type type) {
	assert_(type);
	if (type instanceof ObjectType)
	    return ((ObjectType) type).superClass();
	return null;
    }

    /**
     * Returns an immutable list of all the interface types which type
     * implements.
     **/
    public List<Type> interfaces(Type type) {
	assert_(type);
	if (type instanceof ObjectType)
	    return ((ObjectType) type).interfaces();
	return Collections.<Type>emptyList();
    }

    /**
     * Requires: all type arguments are canonical.
     * Returns the least common ancestor of Type1 and Type2
     **/
    public Type leastCommonAncestor(Type type1, Type type2, Context context)
    throws SemanticException
    {
	assert_(type1);
	assert_(type2);
	return env(context).leastCommonAncestor(type1, type2);
    }

    ////
    // Functions for method testing.
    ////

    /** Return true if t overrides mi */
    public boolean hasMethod(Type t, MethodInstance mi, Context context) {
	assert_(t);
	assert_(mi);
	return env(context).hasMethod(t, mi);
    }

    public X10ConstructorInstance findConstructor(Type container, polyglot.types.TypeSystem_c.ConstructorMatcher matcher) throws SemanticException {
        return (X10ConstructorInstance) SUPER_findConstructor(container, matcher);
    }


    public Collection<X10ConstructorInstance> findConstructors(Type container, polyglot.types.TypeSystem_c.ConstructorMatcher matcher) throws SemanticException {
        assert_(container);
        Context context = matcher.context();
        List<ConstructorInstance> acceptable = findAcceptableConstructors(container, matcher);
        if (acceptable.size() == 0) {
            throw new NoMemberException(NoMemberException.CONSTRUCTOR,
                                        "No valid constructor found for " + matcher.signature() + ").");
        }
        Collection<ConstructorInstance> maximal = findMostSpecificProcedures(acceptable, matcher, context);
        Collection<X10ConstructorInstance> result = new ArrayList<X10ConstructorInstance>();
        for (ConstructorInstance mi : maximal) {
            result.add((X10ConstructorInstance) mi);
        }
        return result;
    }

    public MethodInstance findMethod(Type container, MethodMatcher matcher) throws SemanticException {
        return (MethodInstance) SUPER_findMethod(container, matcher);
    }
    
    public Collection<MethodInstance> findMethods(Type container, MethodMatcher matcher) throws SemanticException {
        assert_(container);
        Context context = matcher.context();
        List<MethodInstance> acceptable = findAcceptableMethods(container, matcher);
        if (acceptable.size() == 0) {
              throw new NoMemberException(NoMemberException.METHOD,
                      "No valid method call found for call in given type."
                      + "\n\t Call: " + matcher.signature()
                      + "\n\t Type: " + container);
        }
        Collection<MethodInstance> maximal =
            findMostSpecificProcedures(acceptable, (Matcher<MethodInstance>) matcher, context);
        Collection<MethodInstance> result = new ArrayList<MethodInstance>();
        for (MethodInstance mi : maximal) {
            result.add(mi);
        }
        return result;
    }


    public X10MethodMatcher MethodMatcher(Type container, Name name, List<Type> argTypes, Context context) {
        return new X10MethodMatcher(container, name, argTypes, context);
    }

    public X10MethodMatcher MethodMatcher(Type container, Name name, List<Type> typeArgs, List<Type> argTypes, Context context) {
        return new X10MethodMatcher(container, name, typeArgs, argTypes, context);
    }

    public X10ConstructorMatcher ConstructorMatcher(Type container, List<Type> argTypes, Context context) {
        return new X10ConstructorMatcher(container, argTypes, context);
    }

    public X10ConstructorMatcher ConstructorMatcher(Type container, List<Type> typeArgs, List<Type> argTypes, Context context) {
        return new X10ConstructorMatcher(container, typeArgs, argTypes, context);
    }

    public X10FieldMatcher FieldMatcher(Type container, Name name, Context context) {
    	//container = X10TypeMixin.ensureSelfBound( container);
        return new X10FieldMatcher(container, name, context);
    }

    public X10FieldMatcher FieldMatcher(Type container, boolean contextKnowsReceiver, Name name, Context context) {
    	//container = X10TypeMixin.ensureSelfBound( container);
        return new X10FieldMatcher(container, contextKnowsReceiver, name, context);
    }
    

    /** Return true if t overrides mi */
    public boolean hasFormals(ProcedureInstance<? extends ProcedureDef> pi, List<Type> formalTypes, Context context) {
	assert_(pi);
	assert_(formalTypes);
	return env(context).hasFormals(pi, formalTypes);
    }

    public List<MethodInstance> overrides(MethodInstance mi, Context context) {
	return env(context).overrides(mi);
    }

    public List<MethodInstance> implemented(MethodInstance mi, Context context) {
	return env(context).implemented(mi);
    }

    public List<MethodInstance> implemented(MethodInstance mi, ContainerType st, Context context) {
	return env(context).implemented(mi, st);
    }


    public boolean canOverride(MethodInstance mi, MethodInstance mj, Context context) {
	return env(context).canOverride(mi, mj);
    }

    public void checkOverride(MethodInstance mi, MethodInstance mj, Context context) throws SemanticException {
	env(context).checkOverride(mi, mj);
    }

    public void checkOverride(MethodInstance mi, MethodInstance mj, boolean allowCovariantReturn, Context context) throws SemanticException {
	env(context).checkOverride(mi, mj, allowCovariantReturn);
    }

    /**
     * Returns true iff <m1> is the same method as <m2>
     */
    public boolean isSameMethod(MethodInstance m1, MethodInstance m2, Context context) {
	assert_(m1);
	assert_(m2);
	return env(context).isSameMethod(m1, m2);
    }

    public boolean callValid(ProcedureInstance<? extends ProcedureDef> prototype, Type thisType, List<Type> argTypes, Context context) {
	assert_(prototype);
	assert_(argTypes);
	return env(context).callValid(prototype, thisType, argTypes);
    }

    ////
    // Functions which yield particular types.
    ////
    public NullType Null()         { return NULL_; }
    public Type Void()    { return VOID_; }
    public Type Boolean() { return BOOLEAN_; }
    public Type Char()    { return CHAR_; }
    public Type Byte()    { return BYTE_; }
    public Type Short()   { return SHORT_; }
    public Type Int()     { return INT_; }
    public Type Long()    { return LONG_; }
    public Type Float()   { return FLOAT_; }
    public Type Double()  { return DOUBLE_; }

    public ClassType load(String name) {
	try {
	    return (ClassType) typeForName(QName.make(name));
	}
	catch (SemanticException e) {
	    throw new InternalCompilerError("Cannot find class \"" +
	                                    name + "\"; " + e.getMessage(),
	                                    e);
	}
    }

    public Named forName(QName name) throws SemanticException {
	try {
	    return systemResolver.find(name);
	}
	catch (SemanticException e) {
	    if (name.qualifier() != null) {
		try {
		    Named container = forName(name.qualifier());
		    if (container instanceof ClassType) {
			return classContextResolver((ClassType) container).find(MemberTypeMatcher((ClassType) container, name.name(), emptyContext()));
		    }
		}
		catch (SemanticException e2) {
		}
	    }

	    // throw the original exception
	    throw e;
	}
    }

    public Type typeForName(QName name) throws SemanticException {
	return (Type) forName(name);
    }

    protected Type OBJECT_;
    protected Type CLASS_;
    protected Type STRING_;
    protected Type THROWABLE_;

    public Type Object()  { if (OBJECT_ != null) return OBJECT_;
    return OBJECT_ = load("java.lang.Object"); }
    public Type Class()   { if (CLASS_ != null) return CLASS_;
    return CLASS_ = load("java.lang.Class"); }
    public Type String()  { if (STRING_ != null) return STRING_;
    return STRING_ = load("java.lang.String"); }
    public Type Throwable() { if (THROWABLE_ != null) return THROWABLE_;
    return THROWABLE_ = load("java.lang.Throwable"); }
    public Type Error() { return load("java.lang.Error"); }
    public Type Exception() { return load("java.lang.Exception"); }
    public Type RuntimeException() { return load("java.lang.RuntimeException"); }
    public Type Cloneable() { return load("java.lang.Cloneable"); }
    public Type Serializable() { return load("java.io.Serializable"); }
    public Type NullPointerException() { return load("java.lang.NullPointerException"); }
    public Type ClassCastException()   { return load("java.lang.ClassCastException"); }
    public Type OutOfBoundsException() { return load("java.lang.ArrayIndexOutOfBoundsException"); }
    public Type ArrayStoreException()  { return load("java.lang.ArrayStoreException"); }
    public Type ArithmeticException()  { return load("java.lang.ArithmeticException"); }

    protected NullType createNull() {
	return new NullType(this);
    }

    protected JavaPrimitiveType createPrimitive(Name name) {
	return new JavaPrimitiveType(this, name);
    }

    public static final Name voidName = Name.make("void");
    protected final NullType NULL_         = createNull();
    protected final JavaPrimitiveType VOID_    = createPrimitive(voidName);
    protected final JavaPrimitiveType BOOLEAN_ = createPrimitive(Name.make("boolean"));
    protected final JavaPrimitiveType CHAR_    = createPrimitive(Name.make("char"));
    protected final JavaPrimitiveType BYTE_    = createPrimitive(Name.make("byte"));
    protected final JavaPrimitiveType SHORT_   = createPrimitive(Name.make("short"));
    protected final JavaPrimitiveType INT_     = createPrimitive(Name.make("int"));
    protected final JavaPrimitiveType LONG_    = createPrimitive(Name.make("long"));
    protected final JavaPrimitiveType FLOAT_   = createPrimitive(Name.make("float"));
    protected final JavaPrimitiveType DOUBLE_  = createPrimitive(Name.make("double"));

    public Object placeHolder(TypeObject o) {
	return placeHolder(o, Collections.<TypeObject>emptySet());
    }

    public Object placeHolder(TypeObject o, Set<TypeObject> roots) {
	assert_(o);

	if (o instanceof Ref_c<?>) {
	    Ref_c<?> ref = (Ref_c<?>) o;

	    if (ref.get() instanceof ClassDef) {
		ClassDef ct = (ClassDef) ref.get();

		// This should never happen: anonymous and local types cannot
		// appear in signatures.
		if (ct.isLocal() || ct.isAnonymous()) {
		    throw new InternalCompilerError("Cannot serialize " + o + ".");
		}

		// Use the transformed name so that member classes will
		// be sought in the correct class file.
		QName name = getTransformedClassName(ct);

		TypeSystem_c ts = this;
		LazyRef<ClassDef> sym = Types.lazyRef(unknownClassDef(), null);
		Goal resolver = extInfo.scheduler().LookupGlobalTypeDef(sym, name);
		resolver.update(Goal.Status.SUCCESS);
		sym.setResolver(resolver);
		return sym;
	    }
	}

	return o;
    }

    public ClassDef unknownClassDef() {
	if (unknownClassDef == null) {
	    unknownClassDef = new ClassDef_c(this, null);
	    unknownClassDef.name(Name.make("<unknown class>"));
	    unknownClassDef.kind(ClassDef.TOP_LEVEL);
	}
	return unknownClassDef;
    }

    protected ClassDef unknownClassDef = null;

    protected UnknownType unknownType = new UnknownType(this);
    protected UnknownPackage unknownPackage = new UnknownPackage_c(this);
    protected UnknownQualifier unknownQualifier = new UnknownQualifier_c(this);

    public UnknownType unknownType(Position pos) {
	return unknownType;
    }

    public UnknownPackage unknownPackage(Position pos) {
	return unknownPackage;
    }

    public UnknownQualifier unknownQualifier(Position pos) {
	return unknownQualifier;
    }

    public Package packageForName(Package prefix, Name name) throws SemanticException {
	return createPackage(prefix, name);
    }

    public Package packageForName(Ref<? extends Package> prefix, Name name) throws SemanticException {
	return createPackage(prefix, name);
    }

    public Package packageForName(QName name) throws SemanticException {
	if (name == null) {
	    return null;
	}

	return packageForName(packageForName(name.qualifier()), name.name());
    }

    /** @deprecated */
    public Package createPackage(Package prefix, Name name) {
	return createPackage(prefix != null ? Types.ref(prefix) : null, name);
    }

    /** @deprecated */
    public Package createPackage(Ref<? extends Package> prefix, Name name) {
	assert_(prefix);
	return new Package_c(this, prefix, name);
    }

    /** @deprecated */
    public Package createPackage(QName name) {
	if (name == null) {
	    return null;
	}

	return createPackage(createPackage(name.qualifier()), name.name());
    }

    /**
     * Returns a type identical to <type>, but with <dims> more array
     * dimensions.
     */
    public Type arrayOf(Type type) {
	assert_(type);
	return arrayOf(type.position(), Types.ref(type));
    }

    public Type arrayOf(Ref<? extends Type> type) {
	assert_(type);
	return arrayOf(null, type);
    }

    public Type arrayOf(Position pos, Ref<? extends Type> type) {
	return arrayType(pos, type);
    }

    public Type arrayOf(Position pos, Type type) {
	assert_(type);
	return arrayOf(pos, Types.ref(type));
    }

    public Type arrayOf(Type type, int dims) {
	return arrayOf(Types.ref(type), dims);
    }

    public Type arrayOf(Position pos, Type type, int dims) {
	return arrayOf(pos, Types.ref(type), dims);
    }

    Map<Ref<? extends Type>,Type> arrayTypeCache = new HashMap<Ref<? extends Type>,Type>();

    /**
     * Factory method for ArrayTypes.
     */
    protected JavaArrayType arrayType(Position pos, Ref<? extends Type> type) {
	JavaArrayType t = (JavaArrayType) arrayTypeCache.get(type);
	if (t == null) {
	    t = createArrayType(pos, type);
	    arrayTypeCache.put(type, t);
	}
	return t;
    }

    protected JavaArrayType createArrayType(Position pos, Ref<? extends Type> type) {
	return new JavaArrayType_c(this, pos, type);
    }

    public Type arrayOf(Ref<? extends Type> type, int dims) {
	return arrayOf(null, type, dims);
    }

    public Type arrayOf(Position pos, Ref<? extends Type> type, int dims) {
	if (dims > 1) {
	    return arrayOf(pos, arrayOf(pos, type, dims-1));
	}
	else if (dims == 1) {
	    return arrayOf(pos, type);
	}
	else {
	    throw new InternalCompilerError(
	                                    "Must call arrayOf(type, dims) with dims > 0");
	}
    }

    /**
     * Returns a canonical type corresponding to the Java Class object
     * theClass.  Does not require that <theClass> have a JavaClass
     * registered in this typeSystem.  Does not register the type in
     * this TypeSystem.  For use only by JavaClass implementations.
     **/
    public Type typeForClass(Class<?> clazz) throws SemanticException
    {
	if (clazz == Void.TYPE)      return VOID_;
	if (clazz == Boolean.TYPE)   return BOOLEAN_;
	if (clazz == Byte.TYPE)      return BYTE_;
	if (clazz == Character.TYPE) return CHAR_;
	if (clazz == Short.TYPE)     return SHORT_;
	if (clazz == Integer.TYPE)   return INT_;
	if (clazz == Long.TYPE)      return LONG_;
	if (clazz == Float.TYPE)     return FLOAT_;
	if (clazz == Double.TYPE)    return DOUBLE_;

	if (clazz.isArray()) {
	    return arrayOf(typeForClass(clazz.getComponentType()));
	}

	return (Type) systemResolver.find(QName.make(clazz.getName()));
    }

    /**
     * Return the set of objects that should be serialized into the
     * type information for the given TypeObject.
     * Usually only the object itself should get encoded, and references
     * to other classes should just have their name written out.
     * If it makes sense for additional types to be fully encoded,
     * (i.e., they're necessary to correctly reconstruct the given clazz,
     * and the usual class resolvers can't otherwise find them) they
     * should be returned in the set in addition to clazz.
     */
    public Set<TypeObject> getTypeEncoderRootSet(TypeObject t) {
	return Collections.singleton(t);
    }

    /**
     * Get the transformed class name of a class.
     * This utility method returns the "mangled" name of the given class,
     * whereby all periods ('.') following the toplevel class name
     * are replaced with dollar signs ('$'). If any of the containing
     * classes is not a member class or a top level class, then null is
     * returned.
     *
     * Return null if the class is not globally accessible.
     */
    public QName getTransformedClassName(ClassDef ct) {
	assert ct != null;
	assert ct.fullName() != null;

	if (!ct.isMember() && !ct.isTopLevel()) {
	    assert ! ct.asType().isGloballyAccessible();
	    return null;
	}

	StringBuilder sb = new StringBuilder();

	while (ct.isMember()) {
	    sb.insert(0, ct.name());
	    sb.insert(0, '$');
	    ct = ct.outer().get();
	    if (!ct.isMember() && !ct.isTopLevel()) {
		assert ! ct.asType().isGloballyAccessible();
		return null;
	    }
	}

	assert ct.asType().isGloballyAccessible();

	if (sb.length() > 0)
	    return QName.make(ct.fullName(), Name.make(sb.toString()));
	else
	    return QName.make(ct.fullName());
    }

    public String translatePackage(Resolver c, Package p) {
	return p.translate(c);
    }

    public String translateArray(Resolver c, JavaArrayType t) {
	return t.translate(c);
    }

    public String translateClass(Resolver c, ClassType t) {
	return t.translate(c);
    }

    public String translatePrimitive(Resolver c, JavaPrimitiveType t) {
	return t.translate(c);
    }

    public Type primitiveForName(Name sname)
    throws SemanticException {
	String name = sname.toString();
	if (name.equals("void")) return Void();
	if (name.equals("boolean")) return Boolean();
	if (name.equals("char")) return Char();
	if (name.equals("byte")) return Byte();
	if (name.equals("short")) return Short();
	if (name.equals("int")) return Int();
	if (name.equals("long")) return Long();
	if (name.equals("float")) return Float();
	if (name.equals("double")) return Double();

	throw new SemanticException("Unrecognized primitive type \"" +
	                            name + "\".");
    }

    public final ClassDef createClassDef() {
	return createClassDef((Source) null);
    }

    public InitializerInstance createInitializerInstance(Position pos, Ref<? extends InitializerDef> def) {
	return new InitializerInstance_c(this, pos, def);
    }

    public List<QName> defaultOnDemandImports() {
	List<QName> l = new ArrayList<QName>(1);
	l.add(QName.make("java.lang"));
	return l;
    }

    public Type promote(Type t1, Type t2) throws SemanticException {
	if (! t1.isNumeric()) {
	    throw new SemanticException(
	                                "Cannot promote non-numeric type " + t1);
	}

	if (! t2.isNumeric()) {
	    throw new SemanticException(
	                                "Cannot promote non-numeric type " + t2);
	}

	if (t1.isDouble() || t2.isDouble()) {
	    return Double();
	}

	if (t1.isFloat() || t2.isFloat()) {
	    return Float();
	}

	if (t1.isLong() || t2.isLong()) {
	    return Long();
	}

	return Int();
    }

    public Type promote(Type t) throws SemanticException {
	if (! t.isNumeric()) {
	    throw new SemanticException(
	                                "Cannot promote non-numeric type " + t);
	}

	if (t.isIntOrLess()) {
	    return Int();
	}

	return t;
    }

    /** All possible <i>access</i> flags. */
    public Flags legalAccessFlags() {
	return Public().Protected().Private();
    }

    protected final Flags ACCESS_FLAGS = legalAccessFlags();

    /** All flags allowed for a local variable. */
    public Flags legalLocalFlags() {
	return Final();
    }

    protected final Flags LOCAL_FLAGS = legalLocalFlags();

    /** All flags allowed for a field. */
    public Flags legalFieldFlags() {
	return legalAccessFlags().Static().Final().Transient();
    }

    protected final Flags FIELD_FLAGS = legalFieldFlags();

    /** All flags allowed for a constructor. */
    public Flags legalConstructorFlags() {
	return legalAccessFlags();
    }

    protected final Flags CONSTRUCTOR_FLAGS = legalConstructorFlags();

    /** All flags allowed for an initializer block. */
    public Flags legalInitializerFlags() {
	return Static();
    }

    protected final Flags INITIALIZER_FLAGS = legalInitializerFlags();

    /** All flags allowed for a method. */
    public Flags legalMethodFlags() {
	return legalAccessFlags().Abstract().Static().Final().Native();
    }

    protected final Flags METHOD_FLAGS = legalMethodFlags();

    public Flags legalAbstractMethodFlags() {
	return legalAccessFlags().clear(Private()).Abstract();
    }

    protected final Flags ABSTRACT_METHOD_FLAGS = legalAbstractMethodFlags();

    /** All flags allowed for a top-level class. */
    public Flags legalTopLevelClassFlags() {
	return legalAccessFlags().clear(Private()).Abstract().Final().Interface();
    }

    protected final Flags TOP_LEVEL_CLASS_FLAGS = legalTopLevelClassFlags();

    /** All flags allowed for an interface. */
    public Flags legalInterfaceFlags() {
	return legalAccessFlags().Abstract().Interface().Static();
    }

    protected final Flags INTERFACE_FLAGS = legalInterfaceFlags();

    /** All flags allowed for a member class. */
    public Flags legalMemberClassFlags() {
	return legalAccessFlags().Static().Abstract().Final().Interface();
    }

    protected final Flags MEMBER_CLASS_FLAGS = legalMemberClassFlags();

    /** All flags allowed for a local class. */
    public Flags legalLocalClassFlags() {
	return Abstract().Final().Interface();
    }

    protected final Flags LOCAL_CLASS_FLAGS = legalLocalClassFlags();

    public void checkMethodFlags(Flags f) throws SemanticException {
	if (! f.clear(METHOD_FLAGS).equals(Flags.NONE)) {
	    throw new SemanticException(
	                                "Cannot declare method with flags " +
	                                f.clear(METHOD_FLAGS) + ".");
	}

	if (f.isAbstract() && ! f.clear(ABSTRACT_METHOD_FLAGS).equals(Flags.NONE)) {
	    throw new SemanticException(
	                                "Cannot declare abstract method with flags " +
	                                f.clear(ABSTRACT_METHOD_FLAGS) + ".");
	}

	checkAccessFlags(f);
    }

    public void checkLocalFlags(Flags f) throws SemanticException {
	if (! f.clear(LOCAL_FLAGS).equals(Flags.NONE)) {
	    throw new SemanticException(
	                                "Cannot declare local variable with flags " +
	                                f.clear(LOCAL_FLAGS) + ".");
	}
    }

    public void checkFieldFlags(Flags f) throws SemanticException {
	if (! f.clear(FIELD_FLAGS).equals(Flags.NONE)) {
	    throw new SemanticException(
	                                "Cannot declare field with flags " +
	                                f.clear(FIELD_FLAGS) + ".");
	}

	checkAccessFlags(f);
    }

    public void checkConstructorFlags(Flags f) throws SemanticException {
	if (! f.clear(CONSTRUCTOR_FLAGS).equals(Flags.NONE)) {
	    throw new SemanticException(
	                                "Cannot declare constructor with flags " +
	                                f.clear(CONSTRUCTOR_FLAGS) + ".");
	}

	checkAccessFlags(f);
    }

    public void checkInitializerFlags(Flags f) throws SemanticException {
	if (! f.clear(INITIALIZER_FLAGS).equals(Flags.NONE)) {
	    throw new SemanticException(
	                                "Cannot declare initializer with flags " +
	                                f.clear(INITIALIZER_FLAGS) + ".");
	}
    }

    public void checkTopLevelClassFlags(Flags f) throws SemanticException {
	if (! f.clear(TOP_LEVEL_CLASS_FLAGS).equals(Flags.NONE)) {
	    throw new SemanticException(
	                                "Cannot declare a top-level class with flag(s) " +
	                                f.clear(TOP_LEVEL_CLASS_FLAGS) + ".");
	}


	if (f.isInterface() && ! f.clear(INTERFACE_FLAGS).equals(Flags.NONE)) {
	    throw new SemanticException("Cannot declare interface with flags " +
	                                f.clear(INTERFACE_FLAGS) + ".");
	}

	checkAccessFlags(f);
    }

    public void checkMemberClassFlags(Flags f) throws SemanticException {
	if (! f.clear(MEMBER_CLASS_FLAGS).equals(Flags.NONE)) {
	    throw new SemanticException(
	                                "Cannot declare a member class with flag(s) " +
	                                f.clear(MEMBER_CLASS_FLAGS) + ".");
	}

	checkAccessFlags(f);
    }

    public void checkLocalClassFlags(Flags f) throws SemanticException {
	if (f.isInterface()) {
	    throw new SemanticException("Cannot declare a local interface.");
	}

	if (! f.clear(LOCAL_CLASS_FLAGS).equals(Flags.NONE)) {
	    throw new SemanticException(
	                                "Cannot declare a local class with flag(s) " +
	                                f.clear(LOCAL_CLASS_FLAGS) + ".");
	}

	checkAccessFlags(f);
    }

    public void checkAccessFlags(Flags f) throws SemanticException {
	int count = 0;
	if (f.isPublic()) count++;
	if (f.isProtected()) count++;
	if (f.isPrivate()) count++;

	if (count > 1) {
	    throw new SemanticException(
	                                "Invalid access flags: " + f.retain(ACCESS_FLAGS) + ".");
	}
    }

    /**
     * Utility method to gather all the superclasses and interfaces of
     * <code>ct</code> that may contain abstract methods that must be
     * implemented by <code>ct</code>. The list returned also contains
     * <code>rt</code>.
     */
    public List<Type> abstractSuperInterfaces(Type t) {
	if (t instanceof ObjectType) {
	    ObjectType rt = (ObjectType) t;

	    List<Type> superInterfaces = new LinkedList<Type>();
	    superInterfaces.add(rt);

	    for (Type interf : rt.interfaces()) {
		superInterfaces.addAll(abstractSuperInterfaces(interf));
	    }

	    if (rt.superClass() instanceof ClassType) {
		ClassType c = (ClassType) rt.superClass();
		if (c.flags().isAbstract()) {
		    // the superclass is abstract, so it may contain methods
		    // that must be implemented.
		    superInterfaces.addAll(abstractSuperInterfaces(c));
		}
		else {
		    // the superclass is not abstract, so it must implement
		    // all abstract methods of any interfaces it implements, and
		    // any superclasses it may have.
		}
	    }
	    // A work-around for the current transient state of the system in which
	 	   // Object is an interface.
	 	   if (isStructType(t)) {
	 		   superInterfaces.remove(Object());
	 	   }

	    return superInterfaces;
	}
	return Collections.<Type>emptyList();
    }
   

    /**
     * Assert that <code>ct</code> implements all abstract methods required;
     * that is, if it is a concrete class, then it must implement all
     * interfaces and abstract methods that it or it's superclasses declare, and if
     * it is an abstract class then any methods that it overrides are overridden
     * correctly.
     */
    public void checkClassConformance(ClassType ct, Context context) throws SemanticException {
	env(context).checkClassConformance(ct);
    }

    public MethodInstance findImplementingMethod(ClassType ct, MethodInstance mi, Context context) {
	return findImplementingMethod(ct, mi, false, context);
    }


    public Flags NoFlags()      { return Flags.NONE; }
    public Flags Public()       { return Flags.PUBLIC; }
    public Flags Private()      { return Flags.PRIVATE; }
    public Flags Protected()    { return Flags.PROTECTED; }
    public Flags Static()       { return Flags.STATIC; }
    public Flags Final()        { return Flags.FINAL; }
    public Flags Transient()    { return Flags.TRANSIENT; }
    public Flags Native()       { return Flags.NATIVE; }
    public Flags Interface()    { return Flags.INTERFACE; }
    public Flags Abstract()     { return Flags.ABSTRACT; }

    public Flags flagsForBits(int bits) {
	Flags f = Flags.NONE;

	if ((bits & Modifier.PUBLIC) != 0)       f = f.Public();
	if ((bits & Modifier.PRIVATE) != 0)      f = f.Private();
	if ((bits & Modifier.PROTECTED) != 0)    f = f.Protected();
	if ((bits & Modifier.STATIC) != 0)       f = f.Static();
	if ((bits & Modifier.FINAL) != 0)        f = f.Final();
	if ((bits & Modifier.TRANSIENT) != 0)    f = f.Transient();
	if ((bits & Modifier.NATIVE) != 0)       f = f.Native();
	if ((bits & Modifier.INTERFACE) != 0)    f = f.Interface();
	if ((bits & Modifier.ABSTRACT) != 0)     f = f.Abstract();

	return f;
    }

    protected String getCreatorStack(int limit) {
        StackTraceElement[] trace = creator.getStackTrace();
        // The first 3 elements will be the factory methods and the constructor
        int size = trace.length-3 < limit ? trace.length-3 : limit;
        StackTraceElement[] res = new StackTraceElement[size];
        for (int i = 0; i < res.length; i++)
            res[i] = trace[i+3];
        return Arrays.toString(res);
    }

    Name homeName = Name.make("home");
    public Name homeName() { return homeName;}
    
    public String toString() {
	return StringUtil.getShortNameComponent(getClass().getName()) + " created at " + creationTime;
    }

    /* Not used.
    public void existsStructWithName(Id name, ContextVisitor tc) throws SemanticException {
   	  NodeFactory nf = (NodeFactory) tc.nodeFactory();
  			TypeSystem ts = (TypeSystem) tc.typeSystem();
  			Context c = tc.context();
  			TypeBuilder tb = new X10TypeBuilder(tc.job(),  ts, nf);
  			// First, try to determine if there in fact a struct in scope with the given name.
  			TypeNode otn = new X10ParsedName(nf, ts, Position.COMPILER_GENERATED, name).toType();//
  			//	nf.AmbDepTypeNode(position(), null, name(), typeArguments, Collections.EMPTY_LIST, null);

  			TypeNode tn = (TypeNode) otn.visit(tb);

  			// First ensure that there is a type associated with tn.
  			tn = (TypeNode) tn.disambiguate(tc);

  			// ok, if we made it this far, then there is a type. Check that it is a struct.
  			Type t = tn.type();
  			t = ts.expandMacros(t);

  			CConstraint xc = Types.xclause(t);
  			t = Types.baseType(t);

  			if (!(ts.isStructType(t))) { // bail
  				throw new SemanticException();
  			}
       }
       */
    
    private boolean isIn(Collection<FieldInstance> newFields, FieldInstance fi) {
        for (FieldInstance fi2 : newFields)
            if (fi.def()==fi2.def()) return true;
        return false;
   }
    public X10FieldInstance findField(Type container, TypeSystem_c.FieldMatcher matcher)
	throws SemanticException {


		Context context = matcher.context();

		Collection<FieldInstance> fields = findFields(container, matcher);

		if (fields.size() >= 2) {
            // if the field is defined in a class, then it will appear only once in "fields".
            // if it is defined in an interface (then it is either a "static val" or a property such as home), then it may appear multiple times in "fields", so we need to filter duplicates.
            // e.g.,
//            interface I1 { static val a = 1;}
//            interface I2 extends I1 {}
//            interface I3 extends I1 {}
//            interface I4 extends I2,I3 {}
//            class Example implements I4 {
//              def example() = a;
//              def m(a:Example{self.home.home.home==here}) = 1;
//            }
			Collection<FieldInstance> newFields = new HashSet<FieldInstance>();
			for (FieldInstance fi : fields) {
				if ((fi.flags().isStatic())){
                    if (!isIn(newFields,fi))
                            newFields.add(fi);
					continue;
				}

				if (! (fi.container().toClass().flags().isInterface())){
					newFields.add(fi);
				}


			}
			fields = newFields;
		}
		if (fields.size() == 0) {
		    throw new NoMemberException(NoMemberException.FIELD,
		                                "Field " + matcher.signature() +
		                                " not found in type \"" +
		                                container + "\".");
		}

		Iterator<FieldInstance> i = fields.iterator();
		X10FieldInstance fi = (X10FieldInstance) i.next();

		if (i.hasNext()) {
		    FieldInstance fi2 = i.next();

		    throw new SemanticException("Field " + matcher.signature() +
		                                " is ambiguous; it is defined in both " +
		                                fi.container() + " and " +
		                                fi2.container() + ".");
		}

		if (context != null && ! isAccessible(fi, context)) {
		    throw new SemanticException("Cannot access " + fi + ".");
		}

		return fi;

   }
    // Returns the number of bytes required to represent the type, or null if unknown (e.g. involves an address somehow)
    // Note for rails this returns the size of 1 element, this will have to be scaled
    // by the number of elements to get the true figure.
    public Long size(Type t) {
        if (t.isFloat()) return 4l;
        if (t.isDouble()) return 8l;
        if (t.isChar()) return 2l;
        if (t.isByte()) return 1l;
        if (t.isShort()) return 2l;
        if (t.isInt()) return 4l;
        if (t.isLong()) return 8l;
        if (isRail(t)) {
            X10ClassType ctyp = (X10ClassType)t;
            assert ctyp.typeArguments().size() == 1;
            return size(ctyp.typeArguments().get(0));
        }
        return null;
    }
    


   
}
