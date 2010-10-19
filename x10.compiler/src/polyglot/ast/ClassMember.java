/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

import polyglot.types.MemberDef;

/**
 * A <code>ClassMember</code> is a method, a constructor, a field, an
 * initializer block, or another class declaration.  It is any node that may
 * occur directly inside a class body.
 */
public interface ClassMember extends Term 
{
    public MemberDef memberDef();
}
