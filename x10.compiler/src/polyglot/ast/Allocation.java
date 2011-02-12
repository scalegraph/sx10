/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2010.
 */
package polyglot.ast;

import java.util.List;

import polyglot.ast.Expr;
import polyglot.ast.TypeNode;

/**
 * @author Bowen Alpern
 *
 */
public interface Allocation extends Expr {
    
    /**
     * @return the type arguments for the allocation
     */
    List<TypeNode> typeArguments();

    /**
     * @param typeArgs the type arguments for the allocation
     * @return a copy of the allocation with it type arguments set
     */
    Allocation typeArguments(List<TypeNode> typeArgs);

}
