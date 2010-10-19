/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

import java.util.List;

import polyglot.types.Context;
import polyglot.util.Position;

/**
 * A <code>SwitchBlock</code> is a list of statements within a switch.
 */
public class SwitchBlock_c extends AbstractBlock_c implements SwitchBlock
{
    public SwitchBlock_c(Position pos, List<Stmt> statements) {
	super(pos, statements);
    }
    
    /**
     * A <code>SwitchBlock</code> differs from a normal block in that 
     * declarations made in the context of the switch block are in the scope 
     * following the switch block. For example
     * <pre>
     * switch (i) { 
     *     case 0: 
     *       int i = 4; 
     *     case 1: 
     *       // i is in scope, but may not have been initialized.
     *     ...
     * } 
     * </pre>
     */
    public Context enterScope(Context c) {
        return c;
    }
    

}
