/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

import polyglot.types.*;

/**
 * A <code>FieldAssign</code> represents a Java assignment expression to
 * a field.  For instance, <code>this.x = e</code>.
 * 
 * The class of the <code>Expr</code> returned by
 * <code>FieldAssign.left()</code>is guaranteed to be a <code>Field</code>.
 */
public interface FieldAssign extends Assign
{
    public Receiver target();
    public FieldAssign target(Receiver target);

    public Id name();
    public FieldAssign name(Id name);

    public FieldAssign fieldInstance(FieldInstance fi);
    public FieldInstance fieldInstance();
    
    public boolean targetImplicit();
    FieldAssign targetImplicit(boolean f);
}
