package x10doc.doc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import polyglot.types.ClassType;
import polyglot.types.QName;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import x10.constraint.XConstraint;
import x10.types.ParameterType;
import x10.types.SubtypeConstraint;
import x10.types.TypeConstraint;
import x10.types.TypeDef;
import x10.types.X10ClassDef;
import x10.types.X10ClassDef_c;
import x10.types.X10ConstructorDef;
import x10.types.X10FieldDef;
import x10.types.X10MethodDef;
import x10.types.X10TypeSystem;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationTypeDoc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.ExecutableMemberDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.ParameterizedType;
import com.sun.javadoc.SeeTag;
import com.sun.javadoc.SourcePosition;
import com.sun.javadoc.Tag;
import com.sun.javadoc.Type;
import com.sun.javadoc.TypeVariable;
import com.sun.javadoc.WildcardType;

public class X10ClassDoc extends X10Doc implements ClassDoc {
	X10ClassDef classDef;
	X10ClassDoc containingClass;
	X10ClassDoc superclass;
	Type superclassType;
	X10PackageDoc containingPackage;
	X10RootDoc rootDoc;
	LinkedHashMap<String, X10TypeVariable> typeParams;
	LinkedHashMap<String, X10FieldDoc> fields;
	LinkedHashMap<String, X10ConstructorDoc> constructors;
	LinkedHashMap<String, MethodDoc> methods;
	ArrayList<X10ClassDoc> innerClasses;
	ArrayList<X10ClassDoc> interfaces;
	ArrayList<Type> interfaceTypes;
	boolean included;

	public X10ClassDoc(X10ClassDef classDef, X10ClassDoc containingClass, String comment) {
		super(comment);
		this.classDef = classDef;
		this.containingClass = containingClass;
		this.rootDoc = X10RootDoc.getRootDoc();
		this.fields = new LinkedHashMap<String, X10FieldDoc>();
		this.constructors = new LinkedHashMap<String, X10ConstructorDoc>();
		this.methods = new LinkedHashMap<String, MethodDoc>();
		this.innerClasses = new ArrayList<X10ClassDoc>();
		this.interfaces = new ArrayList<X10ClassDoc>();
		this.interfaceTypes = new ArrayList<Type>();
		this.included = false;

		this.superclass = null;
		this.superclassType = null;
		
		initTypeParameters();
		
		// addDeclTag(declString());
	}
	
	public void setSuperclass(X10ClassDoc superclass) {
		this.superclass = superclass;
	}

	public void setSuperclassType(Type superclassType) {
		this.superclassType = superclassType;
	}

	void initTypeParameters() {
		List<ParameterType> params = classDef.typeParameters();
		typeParams = new LinkedHashMap<String, X10TypeVariable>(params.size());
		Ref inv = classDef.classInvariant();
		// System.out.println("classInvariant: " + ((inv == null) ? "" : inv.get()));
		TypeConstraint c = classDef.typeGuard().get();
		for (ParameterType p: params) {
			X10TypeVariable v = new X10TypeVariable(p, this);
			v.setTypeGuard(c);
			typeParams.put(typeParameterKey(p), v);
		}
		// the following are commented because classDef.{classInvariant, typeBounds} etc. returns 
		// null for x10.lang.Ref
		// System.out.println("TypeBounds: " + classDef.typeBounds().get());
		// System.out.println("TypeGuard: " + classDef.typeGuard().get());
		for (SubtypeConstraint s: classDef.typeGuard().get().terms()) {
			// System.out.println("SubtypeConstraint: " + s);
		}
	}

	// initializations that are common to specified and unspecified classes 
	public void initialize() {
		// set package of class
		this.containingPackage = rootDoc.getPackage(classDef.package_());
		this.containingPackage.addClass(this);

		// obtain ClassDoc and Type objects for superclass
		Ref<? extends polyglot.types.Type> reft = classDef.superType();
		polyglot.types.Type t = ((reft==null) ? null : reft.get());
		X10ClassDef cdef = (X10ClassDef) ((t == null) ? null : t.toClass().def());
		this.superclass = rootDoc.getUnspecClass(cdef);
		this.superclassType = rootDoc.getType(t);
		
		// add interfaces implemented by the class
		addInterfaces();
	}

	public void addInterfaces() {
		for (Ref<? extends polyglot.types.Type> ref: classDef.interfaces()) {
			this.interfaces.add(rootDoc.getUnspecClass((X10ClassDef) ref.get().toClass().def()));
			this.interfaceTypes.add(rootDoc.getType(ref.get()));
		}

//		System.out.println("---- start interface tree ----");
//		System.out.println("X10ClassDoc{" + classDef + "}.interfaceTypes = " + 
//				           Arrays.toString(interfaceTypes.toArray(new Type[0])));
//		for (Type y: interfaceTypes) {
//			printInterfaceTree(y);
//		}
//		System.out.println("---- end interface tree ----");
	}
	
	public static void printInterfaceTree(Type t) {
		if (t instanceof X10ParameterizedType) {
			X10ParameterizedType x = (X10ParameterizedType)t;
			System.out.println("X10ParameterizedType{" + x + "}.interfaceTypes = " + 
				               Arrays.toString(x.interfaceTypes()));
			for (Type y: x.interfaceTypes()) {
				printInterfaceTree(y);
			}
		}
	}

	public String declString() {
		Ref<XConstraint> refC = classDef.classInvariant(); 
		Ref<TypeConstraint> refG = classDef.typeGuard();
		if ((refC == null) && (refG == null)) {
			return null;
		}
		String temp = classDef.asType().toString();
		String result = "<PRE>\n</PRE><B>Declaration</B>: " + name();
		TypeVariable[] params = typeParameters();
		if (refG != null && (params.length > 0)) {
			result += Arrays.toString(params);
		}
		String constraint = "{";
		if (refC != null) {
			refC.get(); refC = classDef.classInvariant();
			String inv = refC.get().toString();
			int len = inv.length();
			if (len > 2) {
				constraint += inv.substring(1, len-2); // remove leading '{', trailing '}'
				if (refG != null) {
					constraint += ", ";
				}
			}
		}
		if (refG != null) {
//			String typeGuard = refG.get().toString();
//			int len = typeGuard.length();
//			if (len > 2) {
//				constraint += typeGuard.substring(1, len-2); // remove leading '[', trailing ']'
//			}
			boolean first = true;
			for (SubtypeConstraint st: refG.get().terms()) {
				// Type sub = rootDoc.getType(st.subtype());
				// Type sup = rootDoc.getType(st.supertype());
				if (first) {
					first = false;
				}
				else {
					constraint += ", ";
				}
				// constraint += linkTag(st.subtype()) + " <: " + linkTag(st.supertype());
				constraint += st.toString();
			}
		}
		if (constraint.equals("{")) {
			return null;
		}
		result += constraint + "}";
		return result;
	}

	public String linkTag(polyglot.types.Type t) {
//		if (t instanceof X10TypeVariable) {
//			return ("{@link " + name() + " " + ((X10TypeVariable) t).typeName() + "}"); 
//		}
//		else if (t instanceof X10ClassDoc) {
//			X10ClassDoc cd = ((X10ClassDoc) t);
//			if (cd.isIncluded()) {
//				return ("{@link " + cd.qualifiedName() + " " + cd.name() + "}");
//			}
//			else {
//				return cd.name();
//			}
//		}
//		else {
//			return t.typeName();
//		}
		if (t instanceof ParameterType) {
			ParameterType p = (ParameterType) t;
			return ("{@link " + name() + " " + ((ParameterType) t).name().toString() + "}"); 
		}
		X10ClassDef classDef = (X10ClassDef) t.toClass().def();
		if (classDef.typeParameters().size() == 0) {
			X10ClassDoc cd = (X10ClassDoc) rootDoc.getClass(classDef);
			if (cd == null) {
				return classDef.fullName().toString();
			}
			else {
				if (cd.isIncluded()) {
					return ("{@link " + cd.qualifiedName() + " " + cd.name() + "}");
				}
				else {
					return cd.name();
				}
			}
		}
		else {
			return t.toString();
		}

	}
	
	public void addDeclsToMethodComments() {
		for (MethodDoc md: methods.values()) {
			X10Doc d = (X10Doc) md;
			d.addDeclTag(d.declString());
		}
	}
	
	public static String fieldKey(X10FieldDef fd) {
		return fd.name().toString();
	}

	public static String methodKey(X10ConstructorDef cd) {
		return cd.signature();
	}

	public static String methodKey(X10MethodDef md) {
		// return md.name().toString() + X10MethodDoc.signature(md);
		return md.signature();
	}
	
	public static String methodKey(TypeDef td) {
		// return md.name().toString() + X10MethodDoc.signature(md);
		return td.signature();
	}

	public static String typeParameterKey(ParameterType p) {
		return p.name().toString();
	}

	public X10FieldDoc updateField(X10FieldDef fdef, String comments) {
		X10FieldDoc fd = getField(fdef);
		if (fd == null) {
			fd = new X10FieldDoc(fdef, this, comments);
			fields.put(fieldKey(fdef), fd);
		}
		else {
			// fd.setIncluded(true);
			fd.setRawCommentText(comments);
		}
		return fd;
	}

	public X10ConstructorDoc updateConstructor(X10ConstructorDef cdef, String comments) {
		X10ConstructorDoc cd = getConstructor(cdef);
		if (cd == null) {
			cd = new X10ConstructorDoc(cdef, this, comments);
			constructors.put(methodKey(cdef), cd);
		}
		else {
			// cd.setIncluded(true);
			cd.setRawCommentText(comments);
		}
		return cd;
	}

	public MethodDoc updateMethod(X10MethodDef mdef, String comments) {
		MethodDoc md = getMethod(mdef);
		if (md == null) {
			md = new X10MethodDoc(mdef, this, comments);
			methods.put(methodKey(mdef), md);
		}
		else {
			// md.setIncluded(true);
			// commented to avoid duplicate addition of declaration comments
			// TODO: determine what needs to be done here or use another method/method name
			// md.setRawCommentText(comments);
		}
		return md;
	}

	public MethodDoc updateTypeDef(TypeDef tdef, String comments) {
		MethodDoc td = getMethod(tdef);
		if (td == null) {
			td = new X10TypeDefDoc(tdef, this, comments);
			methods.put(methodKey(tdef), td);
		}
		else {
			// md.setIncluded(true);
			// commented to avoid duplicate addition of declaration comments
			// TODO: determine what needs to be done here or use another method/method name
			// md.setRawCommentText(comments);
		}
		return td;
	}

	public void addInnerClass(X10ClassDoc cd) {
		innerClasses.add(cd);
		if (X10RootDoc.printSwitch)
			System.out.println("X10ClassDoc.addInnerClass(" + cd.name() + 
					"); innerClasses.size() = " + innerClasses.size());	
	}
	
	public void addInterface(X10ClassDoc intClassDoc) {
		interfaces.add(intClassDoc);
	}

	public void setIncluded(boolean included) {
		this.included = included;
	}

	public void setPackage(X10PackageDoc pkg) {
		this.containingPackage = pkg;
	}

	public X10FieldDoc getField(String name) {
		return fields.get(name);
	}

	public X10FieldDoc getField(X10FieldDef fdef) {
		return fields.get(fieldKey(fdef));
	}

	public X10ConstructorDoc getConstructor(String name) {
		return constructors.get(name);
	}

	public X10ConstructorDoc getConstructor(X10ConstructorDef cdef) {
		return constructors.get(methodKey(cdef));
	}

	public MethodDoc getMethod(String name) {
		return methods.get(name);
	}

	public MethodDoc getMethod(X10MethodDef mdef) {
		// System.out.println("X10ClassDoc.getMethod: methods.keySet() = " + Arrays.toString(methods.keySet().toArray(new String[0])));
		return methods.get(methodKey(mdef));
	}

	public MethodDoc getMethod(TypeDef tdef) {
		return methods.get(methodKey(tdef));
	}

	public X10TypeVariable getTypeVariable(ParameterType p) {
		return typeParams.get(typeParameterKey(p));
	}
	
	public AnnotationDesc[] annotations() {
		// TODO Auto-generated method stub
		return new AnnotationDesc[0];
	}

	// Return this type as an AnnotationTypeDoc if it represents an annotation type, null otherwise. 
	public AnnotationTypeDoc asAnnotationTypeDoc() {
		return null;
	}

	public ClassDoc asClassDoc() {
		return this;
	}

	public ParameterizedType asParameterizedType() {
		return null;
	}

	public TypeVariable asTypeVariable() {
		return null;
	}

	public WildcardType asWildcardType() {
		return null;
	}

	public ConstructorDoc[] constructors() {
		if (X10RootDoc.printSwitch)
			System.out.println("ClassDoc.constructors() called for "+name());
		return constructors.values().toArray(new ConstructorDoc[0]);
	}

	public ConstructorDoc[] constructors(boolean arg0) {
		if (X10RootDoc.printSwitch)
			System.out.println("ClassDoc.constructors(boolean) called for "+name());
		return constructors();
	}

	public ClassDoc containingClass() {
		return containingClass;
	}

	public PackageDoc containingPackage() {
		if (X10RootDoc.printSwitch) {
			System.out.print("ClassDoc.containingPackage() called for "+name());
			// new Exception().printStackTrace();
			System.out.println("; containingPackage.name() = " + containingPackage.name());
		}
		return containingPackage;
	}

	public boolean definesSerializableFields() {
		// TODO Auto-generated method stub
		return false;
	}

	public String dimension() {
		ClassType classType = classDef.asType();
		return (classType.isArray() ? String.valueOf(classType.toArray().dims()) : "");
	}

	public FieldDoc[] enumConstants() {
		// TODO Auto-generated method stub
		return new FieldDoc[0];
	}

	public FieldDoc[] fields() {
		if (X10RootDoc.printSwitch)
			System.out.println("ClassDoc.fields() called. fields.size() = " + fields.size());
		return fields.values().toArray(new FieldDoc[0]);
	}

	public FieldDoc[] fields(boolean arg0) {
		if (X10RootDoc.printSwitch)
			System.out.println("ClassDoc.fields(boolean) called for "+name());
		return fields();
	}

	public ClassDoc findClass(String arg0) {
		// TODO Auto-generated method stub
		return rootDoc.classNamed(arg0);
	}

	public ClassDoc[] importedClasses() {
		// TODO Auto-generated method stub
		if (X10RootDoc.printSwitch)
			System.out.println("ClassDoc.importedClasses() called for "+name());
		return new ClassDoc[0];
	}

	public PackageDoc[] importedPackages() {
		// TODO Auto-generated method stub
		if (X10RootDoc.printSwitch)
			System.out.println("ClassDoc.importedPackages() called for "+name());
		return new PackageDoc[0];
	}

	public ClassDoc[] innerClasses() {
		if (X10RootDoc.printSwitch)
			System.out.println("" + name() + ".innerClasses() called; innerClasses.size() = " + 
					innerClasses.size());
		return innerClasses.toArray(new ClassDoc[0]);
	}

	public ClassDoc[] innerClasses(boolean arg0) {
		if (X10RootDoc.printSwitch)
			System.out.println("" + name() + ".innerClasses() called; innerClasses.size() = " + 
					innerClasses.size());
		return innerClasses.toArray(new ClassDoc[0]);
	}

	/**
	 * Return interfaces implemented by this class or interfaces extended by this interface. 
	 * Includes only directly-declared interfaces, not inherited interfaces.
	 */
	public ClassDoc[] interfaces() {
		if (X10RootDoc.printSwitch)
			System.out.println("ClassDoc.interfaces() called for "+name());
		return interfaces.toArray(new ClassDoc[0]);
	}

	/**
	 * Return interfaces implemented by this class or interfaces extended by this interface. 
	 * Includes only directly-declared interfaces, not inherited interfaces.
	 */
	public Type[] interfaceTypes() {
		if (X10RootDoc.printSwitch)
			System.out.println("ClassDoc.interfaceTypes() called for "+name());
		// needs to be updated to handle generic types; the result is an array of ClassDoc 
		// or ParametrizedType objects
		return interfaceTypes.toArray(new Type[0]);
	}

	public boolean isAbstract() {
		return (classDef.flags().isAbstract() || isInterface());
	}

	public boolean isClass() {
		return (!isInterface());
	}

	@Override
	public boolean isAnnotationType() {
//		X10TypeSystem ts = (X10TypeSystem) classDef.typeSystem();
//		try {
//			return ts.isSubtype(classDef.asType(), (polyglot.types.Type) ts.forName(QName.make("x10.lang.annotations.Annotation")), ts.emptyContext());
//		} catch (SemanticException e) {
			return false;
//		}
	}

	@Override
	public boolean isEnum() {
		// TODO Auto-generated method stub
		return super.isEnum();
	}

	@Override
	public boolean isError() {
		X10TypeSystem ts = (X10TypeSystem) classDef.typeSystem();
		return ts.isSubtype(classDef.asType(), ts.Error(), ts.emptyContext());
	}

	@Override
	public boolean isException() {
		X10TypeSystem ts = (X10TypeSystem) classDef.typeSystem();
		return ts.isSubtype(classDef.asType(), ts.Exception(), ts.emptyContext());
	}

	@Override
	public boolean isIncluded() {
		return included;
	}

	@Override
	public boolean isInterface() {
		return classDef.flags().isInterface();
	}

	// the following assumes that isEnum, isError, isException have been called earlier
	@Override
	public boolean isOrdinaryClass() {
		return !classDef.flags().isInterface();
	}

	public boolean isPackagePrivate() {
		return classDef.flags().isPackage();
	}

	public boolean isPrimitive() {
		// nothing in X10 is primitive
		return false;
	}

	public boolean isPrivate() {
 		return classDef.flags().isPrivate();
	}

	public boolean isProtected() {
		return classDef.flags().isProtected();
	}

	public boolean isPublic() {
		return classDef.flags().isPublic();
	}

	public boolean isSerializable() {
		// X10's notion of serialization is different from that of Java
		return false;
	}

	public boolean isStatic() {
		return classDef.flags().isStatic();
	}

	public MethodDoc[] methods() {
		if (X10RootDoc.printSwitch)
			System.out.println("ClassDoc.methods() called for "+name());
		return methods.values().toArray(new MethodDoc[0]);
	}

	public MethodDoc[] methods(boolean arg0) {
		if (X10RootDoc.printSwitch)
			System.out.println("ClassDoc.methods(boolean) called for "+name());
		return methods();
	}

	public String modifiers() {
		if (X10RootDoc.printSwitch)
			System.out.println("ClassDoc.modifiers() called for "+name());
		return classDef.flags().toString();
	}

	public int modifierSpecifier() {
		return X10Doc.flagsToModifierSpecifier(classDef.flags().flags());
	}

	public String name() {
		if (X10RootDoc.printSwitch)
			System.out.println("ClassDoc.name() called for "+classDef.name());
		String contClassName = 
			((containingClass == null) ? "" : (containingClass.name() + "."));
		return (contClassName + classDef.name().toString());
		// return (contClassName + classDef.name().toString() + 
		// 		classDef.classInvariant().get() + classDef.typeBounds().get());		
	}

	public String qualifiedName() {
		if (X10RootDoc.printSwitch)
			System.out.println("ClassDoc.qualifiedName() called for "+name());
		return classDef.fullName().toString();
		  // classDef.toString() also returns the fully qualified name
		// return classDef.asType().toString();
		// return "!!X10ClassDoc:qualifiedName!!";
	}

	public String qualifiedTypeName() {
		// classDef.asType().toString() = classDef.asType().fullName().toString()
		// for ValRail[Place]{...}
		String result = classDef.asType().fullName().toString(); 
		if (X10RootDoc.printSwitch)
			System.out.println("ClassDoc{" + name() + "}.qualifiedTypeName() = " + 
					result);
		return "!!X10ClassDoc:qualifiedTypeName!!";
	}

	public FieldDoc[] serializableFields() {
		// TODO Auto-generated method stub
		if (X10RootDoc.printSwitch)
			System.out.println("ClassDoc.serializableFields() called for "+name());
		return new FieldDoc[0];
	}

	public MethodDoc[] serializationMethods() {
		// TODO Auto-generated method stub
		if (X10RootDoc.printSwitch)
			System.out.println("ClassDoc.serializationMethods() called for "+name());
		return new MethodDoc[0];
	}

	public String simpleTypeName() {
		if (X10RootDoc.printSwitch)
			System.out.println("ClassDoc.simpleTypeName() called for "+name());
		return name();
		// return "X10ClassDoc!!simpleTypeName!!";
	}

	public boolean subclassOf(ClassDoc arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public ClassDoc superclass() {
		if (X10RootDoc.printSwitch)
			System.out.println("ClassDoc.superClass() called for "+name());
		if (isInterface()) {
			return null;
		}
		return superclass;
	}

	public Type superclassType() {
		if (X10RootDoc.printSwitch)
			System.out.println("ClassDoc.superClassType() called for "+name());
		if (isInterface()) {
			return null;
		}
		return superclassType;
	}

	public String typeName() {
		if (X10RootDoc.printSwitch)
			System.out.println("ClassDoc.typeName() called for "+name());
		// return classDef.asType().fullName().toString();
		return "!!X10ClassDoc:TYPENAME!!";
	}

	public TypeVariable[] typeParameters() {
		if (X10RootDoc.printSwitch)
			System.out.println("ClassDoc.typeParameters() called for "+name());
		return typeParams.values().toArray(new TypeVariable[0]);
	}

	public ParamTag[] typeParamTags() {
		// TODO Auto-generated method stub
		if (X10RootDoc.printSwitch)
			System.out.println("ClassDoc.typeParamTags() called for "+name());
		return new ParamTag[0];
	}

	public String toString() {
		return name() + classDef.classInvariant().get() + classDef.typeBounds().get();  
	}
	
	public boolean isExternalizable() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isFinal() {
		return classDef.flags().isFinal();
	}

//	public static String fieldKey(X10FieldDoc fd) {
//	return fd.name();
//}
//
//public static String methodKey(X10ConstructorDoc cd) {
//	return methodKey(cd.getConstructorDef());
//}
//
//public static String methodKey(X10MethodDoc m) {
//	return methodKey(m.getMethodDef());
//}

//	public X10FieldDoc addField(X10FieldDoc fd) {
//	String name = fieldKey(fd);
//	X10FieldDoc existingFD = fields.get(name);	
//	if (existingFD != null)
//		return existingFD;
//	else {
//		fields.put(fd.name(), fd);
//		return fd;
//	}
//}
//
//public X10ConstructorDoc addConstructor(X10ConstructorDoc cd) {
//	String name = methodKey(cd);
//	X10ConstructorDoc existingCD = constructors.get(name);	
//	if (existingCD != null)
//		return existingCD;
//	else {
//		constructors.put(name, cd);
//		return cd;
//	}
//}
//
//public X10MethodDoc addMethod(X10MethodDoc md) {
//	String sig = methodKey(md);
//	X10MethodDoc existingMD = methods.get(sig);	
//	if (existingMD != null)
//		return existingMD;
//	else {
//		methods.put(sig, md);
//		return md;
//	}
//}
}
