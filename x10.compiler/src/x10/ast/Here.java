/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2014.
 */

package x10.ast;

import polyglot.ast.Expr;
import x10.types.constraints.XConstrainedTerm;

/** The node constructed for the X10 place designator here.
 *
 */
public interface Here extends Expr {
    XConstrainedTerm placeTerm();
    Here_c placeTerm(XConstrainedTerm pt);
}

