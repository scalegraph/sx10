/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.lex;

import polyglot.util.Position;

/** A token class for identifiers. */
public class Identifier extends Token {
    protected String identifier;
  public Identifier(Position position, String identifier, int sym)
  {
	super(position, sym);
	this.identifier=identifier;
  }

  public String getIdentifier() { return identifier; }

  public String toString() { return "identifier \"" + identifier + "\""; }
}
