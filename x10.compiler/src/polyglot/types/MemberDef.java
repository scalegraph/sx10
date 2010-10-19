/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

/**
 * A <code>MemberInstance</code> is an entity that can be a member of
 * a class.
 */
public interface MemberDef extends Def
{
    /**
     * Return the member's flags.
     */
    Flags flags();
    
    /**
     * Destructively set the member's flags.
     * @param flags
     */
    void setFlags(Flags flags);

    /**
     * Return the member's containing type.
     */
    Ref<? extends StructType> container();
    
    /**
     * Destructively set the member's container.
     * @param container
     */
    void setContainer(Ref<? extends StructType> container);
}
