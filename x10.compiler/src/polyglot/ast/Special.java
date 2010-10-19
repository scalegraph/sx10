/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

import polyglot.util.Enum;

/**
 * A <code>Special</code> is an immutable representation of a
 * reference to <code>this</code> or <code>super</code in Java.  This
 * reference can be optionally qualified with a type such as 
 * <code>Foo.this</code>.
 */
public interface Special extends Expr 
{    
    /** Special expression kind: either "super" or "this". */
    public static class Kind extends Enum {
        private static final long serialVersionUID = 4498760711946203096L;
        public Kind(String name) { super(name); }
    }

    public static final Kind SUPER = new Kind("super");
    public static final Kind THIS  = new Kind("this");

    /** Get the kind of expression: SUPER or THIS. */
    Kind kind();

    /** Set the kind of expression: SUPER or THIS. */
    Special kind(Kind kind);

    /** Get the outer class qualifier of the expression. */
    TypeNode qualifier();

    /** Set the outer class qualifier of the expression. */
    Special qualifier(TypeNode qualifier);
}
