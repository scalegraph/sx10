/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

import polyglot.util.CodeWriter;
import polyglot.util.Position;

/**
 * An integer literal: longs, ints, shorts, bytes, and chars.
 */
public abstract class NumLit_c extends Lit_c implements NumLit
{
    protected long value;

    public NumLit_c(Position pos, long value) {
	super(pos);
	this.value = value;
    }

    /** Get the value of the expression. */
    public long longValue() {
	return this.value;
    }

    public void dump(CodeWriter w) {
        super.dump(w);

	w.allowBreak(4, " ");
	w.begin(0);
	w.write("(value " + value + ")");
	w.end();
    }
}
