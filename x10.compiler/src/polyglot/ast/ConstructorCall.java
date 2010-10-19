/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

import java.util.List;

import polyglot.types.ConstructorInstance;
import polyglot.util.Enum;

/**
 * A <code>ConstructorCall</code> represents a direct call to a constructor.
 * For instance, <code>super(...)</code> or <code>this(...)</code>.
 */
public interface ConstructorCall extends Stmt, ProcedureCall
{
    /** Constructor call kind: either "super" or "this". */
    public static class Kind extends Enum {
        private static final long serialVersionUID = -6862343120050782664L;
        public Kind(String name) { super(name); }
    }

    public static final Kind SUPER = new Kind("super");
    public static final Kind THIS    = new Kind("this");

    /** The qualifier of the call, possibly null. */
    Expr qualifier();

    /** Set the qualifier of the call, possibly null. */
    ConstructorCall qualifier(Expr qualifier);

    /** The kind of the call: THIS or SUPER. */
    Kind kind();

    /** Set the kind of the call: THIS or SUPER. */
    ConstructorCall kind(Kind kind);

    /**
     * Actual arguments.
     * @return A list of {@link polyglot.ast.Expr Expr}.
     */
    List<Expr> arguments();

    /**
     * Set the actual arguments.
     * @param arguments A list of {@link polyglot.ast.Expr Expr}.
     */
    ConstructorCall arguments(List<Expr> arguments);

    /**
     * The constructor that is called.  This field may not be valid until
     * after type checking.
     */
    ConstructorInstance constructorInstance();

    /** Set the constructor to call. */
    ConstructorCall constructorInstance(ConstructorInstance ci);
}
