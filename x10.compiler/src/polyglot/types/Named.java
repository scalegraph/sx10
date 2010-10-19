/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

/**
 * A <code>Named</code> is a TypeObject that is named.
 */
public interface Named extends TypeObject
{
    /**
     * Simple name of the type object. Anonymous classes do not have names.
     */
    Name name();

    /**
     * Full dotted-name of the type object. For a package, top level class, 
     * top level interface, or primitive type, this is
     * the fully qualified name. For a member class or interface that is
     * directly enclosed in a class or interface with a fully qualified name,
     * then this is the fully qualified name of the member class or interface. 
     * For local and anonymous classes, this method returns a string that is
     * not the fully qualified name (as these classes do not have fully 
     * qualified names), but that may be suitable for debugging or error 
     * messages. 
     */
    QName fullName();
    
    /**
     * Return true if the class is global; that is top-level or a member of a global class.
     */
    boolean isGloballyAccessible();
}
