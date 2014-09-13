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

import java.util.List;

import polyglot.ast.Id;
import polyglot.ast.Term;
import polyglot.types.Ref;
import polyglot.types.Type;
import x10.types.ParameterType;

public interface TypeParamNode extends Term {
	Id name();
	TypeParamNode name(Id id);

	ParameterType type();
	TypeParamNode type(ParameterType type);
	public ParameterType.Variance variance();
	public TypeParamNode variance(ParameterType.Variance variance);
	
	List<Type> upperBounds();
}
