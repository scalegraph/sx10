/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2010.
 */

package x10.parser;

import java.util.ArrayList;
import java.util.List;

import polyglot.ast.AmbExpr;
import polyglot.ast.CanonicalTypeNode_c;
import polyglot.ast.FlagsNode;
import polyglot.ast.Formal;
import polyglot.ast.Formal_c;
import polyglot.ast.Id;
import polyglot.ast.NodeFactory;
import polyglot.parse.ParsedName;
import polyglot.parse.VarDeclarator;
import polyglot.types.Flags;
import polyglot.types.TypeSystem;
import polyglot.util.Position;
import x10.parser.X10Parser.JPGPosition;

/**
 * @author vj Jan 23, 2005
 * @author igor Jan 13, 2006
 * TODO: actually use this class instead of the Object[] in the parser
 */
public class X10VarDeclarator extends VarDeclarator {
	private final List<Formal> vars;
	public FlagsNode flags;

	public X10VarDeclarator(JPGPosition pos, Id name) {
		this(pos, name, null);
	}

	public X10VarDeclarator(JPGPosition pos, List<ParsedName> paramList) {
		//this(pos, x10.visit.X10PrettyPrinterVisitor.getId(), paramList);
		// TODO: use the below instead
		this(pos, null, paramList);
	}

	public X10VarDeclarator(JPGPosition pos, Id name, List<ParsedName> paramList) {
		super(pos, name);
		if (paramList != null) {
			this.vars = new ArrayList<Formal>(paramList.size());
			for (ParsedName ni : paramList) {
				TypeSystem ts = ni.ts;
				NodeFactory nf = ni.nf;
				this.vars.add(nf.Formal(ni.pos, flags == null ? nf.FlagsNode(ni.pos, Flags.NONE) : flags, nf.CanonicalTypeNode(ni.pos, ts.Int()), ni.name));
			}
	
		} else {
			vars = null;
		}
	}

	public void setFlag(FlagsNode flags) {
		this.flags = flags;
	}

    public Position position() {
        return pos;
    }
    
    public void position(Position pos) {
        this.pos = pos;
    }
    
	public boolean hasExplodedVars() {
		return vars != null && ! vars.isEmpty();
	}

	public List<Formal> names() {
		return vars;
	}
}

