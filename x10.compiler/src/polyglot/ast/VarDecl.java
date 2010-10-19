/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

import polyglot.types.LocalDef;
import polyglot.types.Type;

/**
 * A <code>VarDecl</code> represents a local variable declaration, of either a formal
 * or a local variable.
 */
public interface VarDecl extends Term
{
    /** Get the type object for the declaration's type. */
    Type declType();

    /** Get the declaration's flags. */
    FlagsNode flags();

    /** Get the declaration's type. */
    TypeNode type();

    /** Get the declaration's name. */
    Id name();

    /**
     * Get the type object for the local we are declaring.  This field may
     * not be valid until after signature disambiguation.
     */
    LocalDef localDef();
}
