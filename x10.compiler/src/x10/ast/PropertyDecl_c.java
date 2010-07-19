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

package x10.ast;

import java.util.Collections;
import java.util.List;

import polyglot.ast.Block;
import polyglot.ast.ClassBody;
import polyglot.ast.Expr;
import polyglot.ast.FlagsNode;
import polyglot.ast.Formal;
import polyglot.ast.Id;
import polyglot.ast.MethodDecl;
import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.ast.TypeNode;
import polyglot.types.Flags;
import polyglot.types.SemanticException;
import polyglot.util.Position;
import polyglot.visit.TypeBuilder;
import x10.types.*;

public class PropertyDecl_c extends X10FieldDecl_c  implements PropertyDecl {
    public PropertyDecl_c(Position pos, FlagsNode flags, TypeNode type,
            Id name, X10NodeFactory nf) {
        this(pos, flags, type, name, null, nf);
    }
    
    public PropertyDecl_c(Position pos, FlagsNode flags, TypeNode type,
            Id name, Expr init, X10NodeFactory nf) {
        super(pos, flags, type, name, init);
    }

    @Override
    public Node buildTypesOverride(TypeBuilder tb) throws SemanticException {
        assert tb.currentClass() != null;
	PropertyDecl_c n = (PropertyDecl_c) super.buildTypesOverride(tb);
        X10FieldDef fi = (X10FieldDef) n.fieldDef();
        fi.setProperty();
        
        // Set the property flag.
        fi.setFlags(X10Flags.toX10Flags(flags.flags()).Property());
        return n.flags(flags.flags(fi.flags()));
    }
    
   /**
    * Return body, augmented with properties and their getters. Used for classes. May be called during
    * initial AST construction phase.
    * 
    * @param properties -- properties declared with the class or interface.
    * @param body   -- the body of the class or interface
    * @return body, with properties and getters added.
    */
    public static ClassBody addPropertyGetters(List<PropertyDecl> properties, ClassBody body,
    		X10NodeFactory nf) {
	if (properties != null) {
	    for (PropertyDecl p : properties) {
		body = body.addMember(((PropertyDecl_c) p).getter(nf));
	    }
	}
    	return body;
    }
    
    /**
     * Return body, augmented with getters. Used for interfaces. May be called during
     * initial AST construction phase.
     * 
     * @param properties -- properties declared with the class or interface.
     * @param body   -- the body of the class or interface
     * @return body, with properties and getters added.
     */
    public static ClassBody addAbstractGetters(List<PropertyDecl> properties, ClassBody body, X10NodeFactory nf) {
        if (properties != null) {
            for (PropertyDecl p : properties) {
		body = body.addMember(((PropertyDecl_c) p).abstractGetter(nf));
	    }
	}
        return body;
    }

    private Position getCompilerGenPos() { return X10NodeFactory_c.compilerGenerated(position()); }
    protected MethodDecl getter(X10NodeFactory nf) {
        X10TypeSystem ts = (X10TypeSystem) nf.extensionInfo().typeSystem();
        Position pos = getCompilerGenPos();
        Flags flags = X10Flags.PROPERTY.Public().Final();
        List<Formal> formals = Collections.EMPTY_LIST;
        List<TypeNode> throwTypes = Collections.EMPTY_LIST;
        Expr e = nf.Field(pos, nf.This(pos), name);
        //Report.report(1, "PropertyDecl_c: GOLDEN e=|" + e + " " + e.getClass());
        
        Stmt s = nf.Return(pos, e);
        Block body = nf.Block(pos, s);
        
        MethodDecl getter = nf.MethodDecl(pos, nf.FlagsNode(pos, flags), type, name, formals, throwTypes, body);
        return getter;
    }
    
    /**
     * For Interfaces with properties, an abstract method signature for each property 
     * is generated in the interface body.
     * Any class implementing the interface has to have the same property 
     * <RAJ> 
     */
    protected MethodDecl abstractGetter(X10NodeFactory nf) {
      MethodDecl abstractGetter = nf.MethodDecl(getCompilerGenPos(), nf.FlagsNode(getCompilerGenPos(), X10Flags.PROPERTY.Public().Abstract()), type, name, 
                              Collections.EMPTY_LIST, Collections.EMPTY_LIST, null);
      return abstractGetter;
    }
}
