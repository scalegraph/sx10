/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * Copyright (c) 2007 IBM Corporation
 * 
 */
package polyglot.types;

import java.util.List;

/**
 * A <code>ProcedureInstance</code> contains the type information for a Java
 * procedure (either a method or a constructor).
 */
public interface ProcedureInstance<T extends ProcedureDef> extends CodeInstance<T> {
    /**
     * List of formal parameter types.
     * @return A list of <code>Type</code>.
     * @see polyglot.types.Type
     */
    List<Type> formalTypes();
    ProcedureInstance<T> formalTypes(List<Type> formalTypes);


    /**
     * Returns a String representing the signature of the procedure.
     * This includes just the name of the method (or name of the class, if
     * it is a constructor), and the argument types.
     */
    String signature();

    /**
     * String describing the kind of procedure, (e.g., "method" or "constructor").
     */
    String designator();

    /**
     * Return true if <code>this</code> is more specific than <code>pi</code>
     * in terms of method overloading.
     * @param context TODO
     */
    boolean moreSpecific(Type ct, ProcedureInstance<T> pi, Context context);

    /**
     * Returns true if the procedure has the given arguments.
     * @param context TODO
     */
    boolean hasFormals(List<Type> arguments, Context context);


    /**
     * Returns true if the procedure can be called with the given arguments.
     * @param thisType TODO
     * @param context TODO
     */
    boolean callValid(Type thisType, List<Type> actualTypes, Context context);
}
