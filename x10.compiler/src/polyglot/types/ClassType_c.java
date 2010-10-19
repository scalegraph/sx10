/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import java.util.*;

import polyglot.frontend.Globals;
import polyglot.frontend.Job;
import polyglot.util.*;

/**
 * A <code>ClassType</code> represents a class -- either loaded from a
 * classpath, parsed from a source file, or obtained from other source.
 */
public abstract class ClassType_c extends ReferenceType_c implements ClassType
{
    private static final long serialVersionUID = 3894454742809763539L;

    protected Ref<? extends ClassDef> def;

    /** Used for deserializing types. */
    protected ClassType_c() { }

    public ClassType_c(TypeSystem ts, Position pos, Ref<? extends ClassDef> def) {
        super(ts, pos);
        this.def = def;
    }
    
    public ClassDef def() {
        return def.get();
    }
    
    public boolean equalsImpl(TypeObject t) {
        if (t instanceof ClassType_c) {
            Ref<? extends ClassDef> thisDef = def;
            Ref<? extends ClassDef> thatDef = ((ClassType_c) t).def;
            return thisDef.get() == thatDef.get();
        }
        return false;
    }

    protected transient Resolver memberCache;
    
    public Resolver resolver() {
        if (memberCache == null) {
            memberCache = new AnotherCachingResolver(ts.createClassContextResolver(this));
        }
        return memberCache;
    }
    
    public Object copy() {
        ClassType_c n = (ClassType_c) super.copy();
        n.memberCache = null;
        return n;
    }
    
    public abstract Job job();
    
    /** Get the class's kind. */
    public abstract ClassDef.Kind kind();

    /** Get the class's outer class, or null if a top-level class. */
    public abstract ClassType outer();

    /** Get the short name of the class, if possible. */ 
    public abstract Name name();

    /** Get the container class if a member class. */
    public StructType container() {
        if (! isMember())
            throw new InternalCompilerError("Non-member class " + this + " cannot have container classes.");
        if (outer() == null)
            throw new InternalCompilerError("Member class " + this + " must have an outer class.");
        return outer();
    }

    /** Get the full name of the class, if possible. */
    public QName fullName() {
        if (isTopLevel()) {
            Name name = name();
            return QName.make(package_() != null ? package_().fullName() : null, name);
        }
        else if (isMember()) {
            Name name = name();
            return QName.make(container() instanceof Named ? ((Named) container()).fullName() : null, name);
        }
        else if (isLocal()) {
            return QName.make(null, name());
        }
        else if (isAnonymous()) {
            return QName.make(null, Name.make("<anonymous class>"));
        }
        else {
            return QName.make(null, Name.make("<unknown class>"));
        }
    }

    public boolean isTopLevel() { return kind() == ClassDef.TOP_LEVEL; }
    public boolean isMember() { return kind() == ClassDef.MEMBER; }
    public boolean isLocal() { return kind() == ClassDef.LOCAL; }
    public boolean isAnonymous() { return kind() == ClassDef.ANONYMOUS; }

    public boolean isGloballyAccessible() {
        return kind() == ClassDef.TOP_LEVEL || (kind() == ClassDef.MEMBER && outer().isGloballyAccessible());
    }

    public boolean isNested() {
        // Implement this way rather than with ! isTopLevel() so that
        // extensions can add more kinds.
        return kind() == ClassDef.MEMBER || kind() == ClassDef.LOCAL || kind() == ClassDef.ANONYMOUS;
    }
    
    public boolean isInnerClass() {
        // it's an inner class if it is not an interface, it is a nested
        // class, and it is not explicitly or implicitly static. 
        return !flags().isInterface() && isNested() && !flags().isStatic() && !inStaticContext();
    }
    
    public boolean isClass() { return true; }
    public ClassType toClass() { return this; }

    /** Get the class's package. */
    public abstract Package package_();

    /** Get the class's flags. */
    public abstract Flags flags();

    /** Get the class's constructors. */
    public abstract List<ConstructorInstance> constructors();

    /** Get the class's member classes. */
    public abstract List<ClassType> memberClasses();

    /** Get the class's methods. */
    public abstract List<MethodInstance> methods();

    /** Get the class's fields. */
    public abstract List<FieldInstance> fields();

    /** Get the class's interfaces. */
    public abstract List<Type> interfaces();

    /** Get the class's super type. */
    public abstract Type superClass();
    
    /** Get a list of all the class's MemberInstances. */
    public List<MemberInstance<?>> members() {
        List<MemberInstance<?>> l = new ArrayList<MemberInstance<?>>();
        l.addAll(methods());
        l.addAll(fields());
        l.addAll(constructors());
        l.addAll(memberClasses());
        return l;
    }

    /** Get a member class of the class by name. */
    public ClassType memberClassMatching(Matcher<Named> matcher) {
	for (ClassType t : memberClasses()) {
	    try {
		Named n = matcher.instantiate(t);
		if (n instanceof ClassType)
		    return (ClassType) n;
	    }
	    catch (SemanticException e) {
	    }
	}

	return null;
    }

    public Named memberTypeMatching(Matcher<Named> matcher) {
	    return memberClassMatching(matcher);
    }
    
    public String translate(Resolver c) {
        if (isTopLevel()) {
            if (package_() == null) {
                return name().toString();
            }

            // Use the short name if it is unique.
            if (c != null && !typeSystem().extensionInfo().getOptions().fully_qualified_names) {
                try {
                    Named x = c.find(ts.TypeMatcher(name()));
                    
                    if (x instanceof ClassType && def().equals(((ClassType) x).def())) {
                        return name().toString();
                    }
                }
                catch (SemanticException e) {
                }
            }

            return package_().translate(c) + "." + name();
        }
        else if (isMember()) {
            // Use only the short name if the outer class is anonymous.
            if (container().toClass().isAnonymous()) {
                return name().toString();
            }

            // Use the short name if it is unique.
            if (c != null && !typeSystem().extensionInfo().getOptions().fully_qualified_names) {
                try {
                    Named x = c.find(ts.TypeMatcher(name()));

                    if (x instanceof ClassType && def().equals(((ClassType) x).def())) {
                        return name().toString();
                    }
                }
                catch (SemanticException e) {
                }
            }

            return container().translate(c) + "." + name();
        }
        else if (isLocal()) {
            return name().toString();
        }
        else {
            throw new InternalCompilerError("Cannot translate an anonymous class.");
        }
    }

    public String toString() {
        if (isTopLevel()) {
            if (package_() != null) {
                return package_() + "." + name();
            }
            return name().toString();
        }
        else if (isMember()) {
            return container().toString() + "." + name();
        }
        else if (isLocal()) {
            return name().toString();
        }
        else if (isAnonymous()) {
            return "<anonymous class>";
        }
        else {
            return "<unknown class>";
        }
    }
    
    /** Pretty-print the name of this class to w. */
    public void print(CodeWriter w) {
	// XXX This code duplicates the logic of toString.
        if (isTopLevel()) {
            if (package_() != null) {
		package_().print(w);
		w.write(".");
		w.allowBreak(2, 3, "", 0);
            }
            w.write(name().toString());
        } else if (isMember()) {
            container().print(w);
	    w.write(".");
	    w.allowBreak(2, 3, "", 0);
	    w.write(name().toString());
        } else if (isLocal()) {
	    w.write(name().toString());
        } else if (isAnonymous()) {
	    w.write("<anonymous class>");
        } else {
	    w.write("<unknown class>");
        }
    }

    public boolean isEnclosed(ClassType maybe_outer) {
        if (isTopLevel())
            return false;
        else if (outer() != null)
            return outer().equals((Object) maybe_outer) ||
                  outer().isEnclosed(maybe_outer);
        else
            throw new InternalCompilerError("Non top-level classes " + 
                    "must have outer classes.");
    }

    /** 
     * Return true if an object of the class has
     * an enclosing instance of <code>encl</code>. 
     */
    public boolean hasEnclosingInstance(ClassType encl) {
        if (this.equals((Object) encl)) {
            // object o is the zeroth lexically enclosing instance of itself. 
            return true;
        }
        
        if (!isInnerClass() || inStaticContext()) {
            // this class is not an inner class, or was declared in a static
            // context; it cannot have an enclosing
            // instance of anything. 
            return false;
        }
        
        // see if the immediately lexically enclosing class has an 
        // appropriate enclosing instance
        return this.outer().hasEnclosingInstance(encl);
    }
}
