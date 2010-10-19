/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import java.io.Serializable;

import polyglot.util.Copy;
import polyglot.util.Position;

/**
 * A <code>TypeObject</code> is a compile-time value created by the type system.
 * It is a static representation of a type that is not necessarily 
 * first-class.  It is similar to a compile-time meta-object.
 */
public interface TypeObject extends Copy, Serializable
{
    /**
     * The object's type system.
     */
    TypeSystem typeSystem();

    /**
     * The object's position, or null.
     */
    Position position();
    
    void equals(Type t);
//    void equals(TypeObject t);
    void equalsImpl(Type t);
    void equalsImpl(Object t);

    /**
     * Return true iff this type object is the same as <code>t</code>.
     * All Polyglot extensions should attempt to maintain pointer
     * equality between equal TypeObjects.  If this cannot be done,
     * extensions can override TypeObject_c.equalsImpl(), and
     * don't forget to override hashCode().
     *
     * @see polyglot.types.TypeObject_c#equalsImpl(TypeObject)
     * @see java.lang.Object#hashCode()
     */
    boolean equalsImpl(TypeObject t);
}
