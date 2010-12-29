/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import java.util.*;

import polyglot.frontend.*;
import polyglot.util.*;
import x10.types.MethodInstance;

/**
 * ParsedClassType
 *
 * Overview: 
 * A ParsedClassType represents a information that has been parsed (but not
 * necessarily type checked) from a .java file.
 * 
 * Mark this abstract so that the class is not accidentally instantiated in the X10 compiler. 
 * Ultimately  X10ParsedClassType_c and ParsedClassType_c will be merged.
 **/
public abstract class ParsedClassType_c extends ClassType_c implements ParsedClassType
{
    private static final long serialVersionUID = 8011708450784948917L;

    protected ParsedClassType_c() {
	super();
    }
    
    protected Flags flags;
    protected ContainerType container;
    
    public ContainerType container() {
        if (container == null) {
            return super.container();
        }
        return container;
    }
    
    public ClassType flags(Flags flags) {
        ParsedClassType_c t = (ParsedClassType_c) copy();
        t.flags = flags;
        return t;
    }
    
    public ClassType container(ContainerType container) {
        ParsedClassType_c t = (ParsedClassType_c) copy();
        t.container = container;
        return t;
    }
    
    public Job job() {
        return def().job();
    }
    
    public ParsedClassType_c(ClassDef def) {
        this(def.typeSystem(), def.position(), Types.ref(def));
    }

    public ParsedClassType_c(TypeSystem ts, Position pos, Ref<? extends ClassDef> def) {
        super(ts, pos, def);
    }
    
    public Source fromSource() {
        return def().sourceFile();
    }
    
    public ClassDef.Kind kind() {
        return def().kind();
    }

    public boolean inStaticContext() {
        return def().inStaticContext();
    }
    
    public ClassType outer() {
        ClassDef outer = Types.get(def().outer());
        if (outer == null) return null;
        return outer.asType();
    }

    public Name name() {
        return def().name();
    }

    /** Get the class's super type. */
    public Type superClass() {
        return Types.get(def().superType());
    }

    /** Get the class's package. */
    public Package package_() {
        return Types.get(def().package_());
    }

    /** Get the class's flags. */
    public Flags flags() {
        if (flags == null) {
            return def().flags();
        }
        return flags;
    }
    
    public boolean defaultConstructorNeeded() {
        if (flags().isInterface()) {
            return false;
        }
        return def().constructors().isEmpty();
    }
    
    /** Return an immutable list of constructors */
    public List<ConstructorInstance> constructors() {
        return new TransformingList<ConstructorDef,ConstructorInstance>(
                                    def().constructors(),
                                    new ConstructorAsTypeTransform());
}

    /** Return an immutable list of member classes */
    public List<ClassType> memberClasses() {
        return new TransformingList<Ref<? extends ClassType>,ClassType>(def().memberClasses(),
                                    new DerefTransform<ClassType>());
    }

    /** Return an immutable list of methods. */
    public List<MethodInstance> methods() {
        return new TransformingList<MethodDef,MethodInstance>(
                                    def().methods(),
                                    new MethodAsTypeTransform());
    }
    
    /** Return a list of all methods with the given name. */
    public List<MethodInstance> methodsNamed(Name name) {
        List<MethodInstance> l = new ArrayList<MethodInstance>();
        for (MethodInstance mi : methods()) {
            if (mi.name().equals(name)) {
                l.add(mi);
            }
        }

        return l;
    }

    /** Return an immutable list of fields */
    public List<FieldInstance> fields() {
        return new TransformingList<FieldDef, FieldInstance>(def().fields(),
                                                         new FieldAsTypeTransform());
    }
    
    /** Get a field of the class by name. */
    public FieldInstance fieldNamed(Name name) {
        for (Iterator<FieldInstance> i = fields().iterator(); i.hasNext(); ) {
            FieldInstance fi = (FieldInstance) i.next();
            if (fi.name().equals(name)) {
                return fi;
            }
        }

        return null;
    }

    /** Return an immutable list of interfaces */
    public List<Type> interfaces() {
        return new TransformingList<Ref<? extends Type>, Type>(
                                                    def().interfaces(),
                                                    new DerefTransform<Type>());
    }

    // May be overridden by subclasses.
    public boolean isX10Struct() {
    	return false;
    }
    public Type makeX10Struct() {
    	throw new InternalCompilerError("Should not have been called. Cannot make an Unknown type a struct.");
    }
   // public String typeToString() {
   //     return super.typeToString();
   // }
}
