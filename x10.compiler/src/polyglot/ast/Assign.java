/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

import polyglot.types.Type;
import polyglot.util.Enum;

/**
 * An <code>Assign</code> represents a Java assignment expression.
 */
public interface Assign extends Expr
{
    /** Assignment operator. */
    public static class Operator extends Enum {
        private static final long serialVersionUID = 574369171510341962L;
        private final Binary.Operator binOp;
        public Operator(String name, Binary.Operator binOp) { 
            super(name);
            this.binOp = binOp;
        }
        public Binary.Operator binaryOperator() {
            return binOp;
        }
    }

    public static final Operator ASSIGN         = new Operator("=", null);
    public static final Operator ADD_ASSIGN     = new Operator("+=", Binary.ADD);
    public static final Operator SUB_ASSIGN     = new Operator("-=", Binary.SUB);
    public static final Operator MUL_ASSIGN     = new Operator("*=", Binary.MUL);
    public static final Operator DIV_ASSIGN     = new Operator("/=", Binary.DIV);
    public static final Operator MOD_ASSIGN     = new Operator("%=", Binary.MOD);
    public static final Operator BIT_AND_ASSIGN = new Operator("&=", Binary.BIT_AND);
    public static final Operator BIT_OR_ASSIGN  = new Operator("|=", Binary.BIT_OR);
    public static final Operator BIT_XOR_ASSIGN = new Operator("^=", Binary.BIT_XOR);
    public static final Operator SHL_ASSIGN     = new Operator("<<=", Binary.SHL);
    public static final Operator SHR_ASSIGN     = new Operator(">>=", Binary.SHR);
    public static final Operator USHR_ASSIGN    = new Operator(">>>=", Binary.USHR);

    /**
     * Left child (target) of the assignment.
     * The target must be a Variable, but this is not enforced
     * statically to keep Polyglot backward compatible.
     */
    Expr left();

    Type leftType();	

    /**
     * The assignment's operator.
     */
    Operator operator();

    /**
     * Set the assignment's operator.
     */
    Assign operator(Operator op);

    /**
     * Right child (source) of the assignment.
     */
    Expr right();

    /**
     * Set the right child (source) of the assignment.
     */
    Assign right(Expr right);
    
    boolean throwsArithmeticException();
}
