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

package x10.compiler;

import x10.lang.annotations.*;

/**
 * This annotation is used to allow the programmer
 * to direct the compiler to never inline
 * the annotated method</p>
 *
 * This annotation is processed by the X10 compiler's
 * common optimizer.
 */
public interface NoInline extends MethodAnnotation, StatementAnnotation, ExpressionAnnotation { }
