/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * Copyright (c) 2007 IBM Corporation
 * 
 */

package polyglot.types;

/**
 * A <code>CodeInstance</code> contains the type information for a Java
 * code-chunk (method, constructor, initializer, closure).
 */
public interface CodeDef extends Def
{
    CodeInstance<?> asInstance();
}
