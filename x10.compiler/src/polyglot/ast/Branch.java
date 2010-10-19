/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

import polyglot.util.Enum;

/**
 * A <code>Branch</code> is an immutable representation of a branch
 * statment in Java (a break or continue).
 */
public interface Branch extends Stmt
{
    /** Branch kind: either break or continue. */
    public static class Kind extends Enum {
        private static final long serialVersionUID = 7666190675942868358L;
        public Kind(String name) { super(name); }
    }

    public static final Kind BREAK    = new Kind("break");
    public static final Kind CONTINUE = new Kind("continue");

    /**
     * The kind of branch.
     */
    Kind kind();

    /**
     * Set the kind of branch.
     */
    Branch kind(Kind kind);
    
    /**
     * Target label of the branch.
     */
    Id labelNode();
    
    /**
     * Set the target label of the branch.
     */
    Branch labelNode(Id label);
}
