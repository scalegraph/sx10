/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

/**
 * An <code>ArrayType</code> represents an array of other types.
 */
public interface ArrayType extends ObjectType 
{
    /**
     * Base type of the array.
     */
    Ref<? extends Type> theBaseType();
    Type base();

    /**
     * Set the base type of the array, returning a new type.
     */
    ArrayType base(Ref<? extends Type> base);

    /**
     * The ultimate base of the array.  Guaranteed not to be an array type.
     */
    Type ultimateBase();

    /**
     * The array's length field.
     */
    FieldInstance lengthField();

    /**
     * The array's clone() method.
     */
    MethodInstance cloneMethod();

    /**
     * Return the number of dimensions in this array type.
     * e.g., for A[], return 1; for A[][], return 2, etc.
     */
    int dims();
}
