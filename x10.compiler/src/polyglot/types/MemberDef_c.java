/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import polyglot.util.Position;

/**
 * A <code>MemberDef</code> allows accessing the state of a class member.
 */
public abstract class MemberDef_c extends Def_c implements MemberDef
{
    private static final long serialVersionUID = 3410658974280570706L;

    protected Ref<? extends StructType> container;
    protected Flags flags;

    /** Used for deserializing types. */
    protected MemberDef_c() { }

    public MemberDef_c(TypeSystem ts, Position pos, Ref<? extends StructType> container, Flags flags) {
        super(ts, pos);
        this.container = container;
        this.flags = flags;
    }

    final public Ref<? extends StructType> container() {
        return container;
    }
    
    /**
     * @param container The container to set.
     */
    final public void setContainer(Ref<? extends StructType> container) {
        this.container = container;
    }

    final public Flags flags() {
        return flags;
    }

    /**
     * @param flags The flags to set.
     */
    final public void setFlags(Flags flags) {
        this.flags = flags;
    }

    public boolean staticContext() {
        return flags().isStatic();
    }

    public abstract String toString();
}
