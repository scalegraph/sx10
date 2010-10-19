/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

import polyglot.types.QName;
import polyglot.util.Enum;

/**
 * An <code>Import</code> is an immutable representation of a Java
 * <code>import</code> statement.  It consists of the string representing the
 * item being imported and the kind which is either indicating that a class
 * is being imported, or that an entire package is being imported.
 */
public interface Import extends Node 
{
    /** Import kinds: class (e.g., import java.util.Set) or package (e.g.,
     *  import java.util.*).
     *
     * PACKAGE is a bit of a misnomer, since we can import p.C.*, where p.C
     * is a class.  This puts the nested classes of p.C in scope.
     */
    public static class Kind extends Enum {
        private static final long serialVersionUID = 167043594626583539L;
        public Kind(String name) { super(name); }
    }

    public static final Kind CLASS   = new Kind("class");
    public static final Kind PACKAGE = new Kind("package");

    /** Get the name of the class or package to import. */
    QName name();
    /** Set the name of the class or package to import. */
    Import name(QName name);

    /** Get the kind of import. */
    Kind kind();
    /** Set the kind of import. */
    Import kind(Kind kind);
}
