/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2006-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */
package polyglot.ast;

import polyglot.types.Name;

/** A node that represents a simple identifier in the AST. */
public interface Id extends Node {
    Name id();
    Id id(Name id);
}
