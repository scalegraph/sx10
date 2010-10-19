/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;


import polyglot.util.CodeWriter;

/**
 * An <code>Package</code> represents a Java package.
 */
public interface Package extends Qualifier, Named, Def
{
    /**
     * The package's outer package.
     */
    Ref<? extends Package> prefix();

    /**
     * Return a string that is the translation of this package.
     * @param c A resolver in which to look up the package.
     */
    String translate(Resolver c);
    
    /** Return true if this package is equivalent to <code>p</code>. */
    boolean packageEquals(Package p);

    /** A resolver to access member packages and classes of the package. */
    Resolver resolver();

    /** Pretty-print this package name to w. */
    void print(CodeWriter w);
}
