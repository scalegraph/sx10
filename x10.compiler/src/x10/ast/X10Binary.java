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

import polyglot.ast.Binary;
import polyglot.ast.Expr;
import x10.types.MethodInstance;


/**
 * An immutable representation of a binary operation.
 * @author vj
 */
public interface X10Binary extends Binary {
    boolean invert();
    X10Binary invert(boolean invert);
    MethodInstance methodInstance();
    X10Binary methodInstance(MethodInstance mi);
}
