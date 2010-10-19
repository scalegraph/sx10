/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import java.util.List;

/**
 * A <code>ClassType</code> represents a class, either loaded from a
 * classpath, parsed from a source file, or obtained from other source.
 * A <code>ClassType</code> is not necessarily named.
 */
public interface ClassType extends Importable, ObjectType, MemberInstance<ClassDef>, Use<ClassDef>
{
    ClassType flags(Flags flags);
    ClassType container(StructType container);
    
    /**
     * A resolver to access member classes of the class.
     */
    Resolver resolver();
    
    /** Get the class's kind. */
    ClassDef.Kind kind();

    /**
     * Return true if the class is top-level (i.e., not inner).
     * Equivalent to kind() == TOP_LEVEL.
     */
    boolean isTopLevel();

    /**
     * Return true if the class is a nested.
     * Equivalent to kind() == MEMBER || kind() == LOCAL || kind() == ANONYMOUS.
     */
    boolean isNested();

    /**
     * Return true if the class is an inner class, that is, it is a nested
     * class that is not explicitly or implicitly declared static; an interface
     * is never an inner class.
     */
    boolean isInnerClass();

    /**
     * Return true if the class is a member class.
     * Equivalent to kind() == MEMBER.
     */
    boolean isMember();

    /**
     * Return true if the class is a local class.
     * Equivalent to kind() == LOCAL.
     */
    boolean isLocal();

    /**
     * Return true if the class is an anonymous class.
     * Equivalent to kind() == ANONYMOUS.
     */
    boolean isAnonymous();

    /**
     * Return true if the class declaration occurs in a static context.
     * Is used to determine if a nested class is implicitly static.
     */
    boolean inStaticContext();
    
    /**
     * The class's constructors.
     * A list of <code>ConstructorInstance</code>.
     * @see polyglot.types.ConstructorDef
     */
    List<ConstructorInstance> constructors();

    /**
     * The class's member classes.
     * A list of <code>ClassType</code>.
     * @see polyglot.types.ClassType
     */
    List<ClassType> memberClasses();

    /** Returns the member class with the given name, or null. */
    ClassType memberClassMatching(Matcher<Named> name);
    Named memberTypeMatching(Matcher<Named> matcher);
    
    /** Return true if the class is strictly contained in <code>outer</code>. */
    boolean isEnclosed(ClassType outer);

    /** Return true if an object of the class has
     * an enclosing instance of <code>encl</code>. */
    boolean hasEnclosingInstance(ClassType encl);

    /** The class's outer class if this is a nested class, or null. */
    ClassType outer();
}
