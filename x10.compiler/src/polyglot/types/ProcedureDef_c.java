/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import java.util.*;

import polyglot.util.Position;
import polyglot.util.TypedList;

/**
 * A <code>ProcedureInstance_c</code> contains the type information for a Java
 * procedure (either a method or a constructor).
 */
public abstract class ProcedureDef_c extends Def_c implements ProcedureDef
{
    private static final long serialVersionUID = 7146402627770404357L;

    protected Ref<? extends StructType> container;
    protected Flags flags;
    protected List<Ref<? extends Type>> formalTypes;

    /** Used for deserializing types. */
    protected ProcedureDef_c() { }

    public ProcedureDef_c(TypeSystem ts, Position pos,
            Ref<? extends StructType> container,
			       Flags flags, List<Ref<? extends Type>> formalTypes) {
        super(ts, pos);
	this.container = container;
	this.flags = flags;
	this.formalTypes = TypedList.copyAndCheck(formalTypes, Ref.class, true);
    }
    
    public Ref<? extends StructType> container() {
        return container;
    }

    public Flags flags() {
        return flags;
    }

    public List<Ref<? extends Type>> formalTypes() {
        return Collections.unmodifiableList(formalTypes);
    }


    /**
     * @param container The container to set.
     */
    public void setContainer(Ref<? extends StructType> container) {
        this.container = container;
    }
    
    /**
     * @param flags The flags to set.
     */
    public void setFlags(Flags flags) {
        this.flags = flags;
    }
    
    /**
     * @param formalTypes The formalTypes to set.
     */
    public void setFormalTypes(List<Ref<? extends Type>> formalTypes) {
        this.formalTypes = TypedList.copyAndCheck(formalTypes, Ref.class, true);
    }
    
  
}
