/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

import java.util.List;

import polyglot.types.ClassDef;
import polyglot.types.Flags;

/**
 * A <code>ClassDecl</code> represents a top-level, member, or local class
 * declaration.
 */
public interface ClassDecl extends Term, TopLevelDecl, ClassMember
{
    /**
     * The type of the class declaration.
     */
    ClassDef classDef();

    /**
     * Set the type of the class declaration.
     */
    ClassDecl classDef(ClassDef type);

    /**
     * The class declaration's flags.
     */
    FlagsNode flags();

    /**
     * Set the class declaration's flags.
     */
    ClassDecl flags(FlagsNode flags);

    /**
     * The class declaration's name.
     */
    Id name();
    
    /**
     * Set the class declaration's name.
     */
    ClassDecl name(Id name);

    /**
     * The class's super class.
     */
    TypeNode superClass();

    /**
     * Set the class's super class.
     */
    ClassDecl superClass(TypeNode superClass);

    /**
     * The class's interface list.
     * @return A list of {@link polyglot.ast.TypeNode TypeNode}.
     */
    List<TypeNode> interfaces();

    /**
     * Set the class's interface list.
     * @param interfaces A list of {@link polyglot.ast.TypeNode TypeNode}.
     */
    ClassDecl interfaces(List<TypeNode> interfaces);

    /**
     * The class's body.
     */
    ClassBody body();

    /**
     * Set the class's body.
     */
    ClassDecl body(ClassBody body);
}
