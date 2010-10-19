/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import java.util.*;

import polyglot.util.*;

/**
 * An <code>ArrayType</code> represents an array of base java types.
 */
public class ArrayType_c extends ReferenceType_c implements ArrayType
{
    private static final long serialVersionUID = 5957743833621743101L;

    protected Ref<? extends Type> base;
    protected List<FieldDef> fields;
    protected List<MethodDef> methods;
    protected List<Ref<? extends Type>> interfaces;

    /** Used for deserializing types. */
    protected ArrayType_c() { }

    public ArrayType_c(TypeSystem ts, Position pos, Ref<? extends Type> base) {
	super(ts, pos);
	this.base = base;

        methods = null;
        fields = null;
        interfaces = null;
    }

    protected void init() {
        if (methods == null) {
            methods = new ArrayList<MethodDef>(1);

            // Add method public Object clone()
            MethodDef mi = ts.methodDef(position(),
                                        Types.<ArrayType_c>ref(this),
                                        ts.Public(),
                                        Types.<Type>ref(ts.Object()),
                                        Name.make("clone"),
                                        Collections.<Ref<? extends Type>>emptyList());
            methods.add(mi);
        }

        if (fields == null) {
            fields = new ArrayList<FieldDef>(1);

            // Add field public final int length
            FieldDef fi = ts.fieldDef(position(),
                                        Types.<ArrayType_c>ref(this),
                                        ts.Public().Final(),
                                        Types.ref(ts.Int()),
                                        Name.make("length"));
            fi.setNotConstant();
            fields.add(fi);
        }

        if (interfaces == null) {
            interfaces = new ArrayList<Ref<? extends Type>>(2);
            interfaces.add(Types.<Type>ref(ts.Cloneable()));
            interfaces.add(Types.<Type>ref(ts.Serializable()));
        }
    }

    public Ref<? extends Type> theBaseType() {
        return base;
    }
    
    /** Get the base type of the array. */
    public Type base() {
        return Types.get(base);
    }

    /** Set the base type of the array. */
    public ArrayType base(Type base) {
        return base(Types.ref(base));
    }
    
    public ArrayType base(Ref<? extends Type> base) {
        if (base == this.base)
            return this;
	ArrayType_c n = (ArrayType_c) copy();
	n.base = base;
	return n;
    }

    /** Get the ulitimate base type of the array. */
    public Type ultimateBase() {
        if (base().isArray()) {
            return base().toArray().ultimateBase();
        }

        return base();
    }

    public int dims() {
        return 1 + (base().isArray() ? base().toArray().dims() : 0);
    }

    public String toString() {
        return base.toString() + "[]";
    }

    public void print(CodeWriter w) {
	base().print(w);
	w.write("[]");
    }

    /** Translate the type. */
    public String translate(Resolver c) {
        return base().translate(c) + "[]"; 
    }
    
    public boolean isArray() { return true; }
    public ArrayType toArray() { return this; }

    /** Get the methods implemented by the array type. */
    public List<MethodInstance> methods() {
        init();
        return new TransformingList<MethodDef,MethodInstance>(methods, new MethodAsTypeTransform());
    }

    /** Get the fields of the array type. */
    public List<FieldInstance> fields() {
        init();
        return new TransformingList<FieldDef,FieldInstance>(fields, new FieldAsTypeTransform());
    }

    /** Get the clone() method. */
    public MethodInstance cloneMethod() {
	return methods().get(0);
    }

    /** Get a field of the type by name. */
    public FieldInstance fieldNamed(Name name) {
        FieldInstance fi = lengthField();
        return name.equals(fi.name()) ? fi : null;
    }

    /** Get the length field. */
    public FieldInstance lengthField() {
	return fields().get(0);
    }

    /** Get the super type of the array type. */
    public Type superClass() {
	return ts.Object();
    }

    /** Get the interfaces implemented by the array type. */
    public List<Type> interfaces() {
        init();
        return new TransformingList<Ref<? extends Type>,Type>(interfaces, new DerefTransform<Type>());
    }

    public int hashCode() {
	return base().hashCode() << 1;
    }

    public boolean equalsImpl(TypeObject t) {
        if (t instanceof ArrayType) {
            ArrayType a = (ArrayType) t;
            return ts.equals((TypeObject) base(), (TypeObject) a.base());
        }
	return false;
    }
}
