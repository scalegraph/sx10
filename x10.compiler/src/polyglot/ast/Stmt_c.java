/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

import polyglot.util.Position;

/**
 * A <code>Stmt</code> represents any Java statement.  All statements must
 * be subtypes of Stmt.
 */
public abstract class Stmt_c extends Term_c implements Stmt
{
    public Stmt_c(Position pos) {
	super(pos);
    }
}
