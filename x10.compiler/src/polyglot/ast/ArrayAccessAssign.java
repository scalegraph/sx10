/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

/**
 * A <code>ArrayAccessAssign</code> represents a Java assignment expression
 * to an array element.  For instance, <code>A[3] = e</code>.
 * 
 * The class of the <code>Expr</code> returned by
 * <code>ArrayAccessAssign.left()</code>is guaranteed to be an
 * <code>ArrayAccess</code>.
 */
public interface ArrayAccessAssign extends Assign
{
    boolean throwsArrayStoreException();

    Expr array();
    ArrayAccessAssign array(Expr array);
    
    Expr index();
    ArrayAccessAssign index(Expr index);

}
