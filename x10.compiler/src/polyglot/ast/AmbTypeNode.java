/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

/**
 * An <code>AmbTypeNode</code> is an ambiguous AST node composed of
 * dot-separated list of identifiers that must resolve to a type.
 */
public interface AmbTypeNode extends TypeNode, Ambiguous
{
    /**
     * Qualifier of the type.
     */
    Prefix prefix();

    /**
     * Set the qualifier of the type.
     */
    AmbTypeNode prefix(Prefix prefix);
    
    /**
     * Ambiguous name.
     */
    Id name();
    
    /**
     * Set the ambiguous name.
     */
    AmbTypeNode name(Id name);
}
