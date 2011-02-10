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
 * Annotation to mark methods and fields as having a particular native implementation.
 * lang is the name of the language, typically "java" or "c++".
 * code is the code to insert for a call to the method or an access to the field.
 *
 * For "java" annotations:
 *
 * Given a method with signature:
 *     def m[X, Y](x, y);
 * and a call
 *     o.m[A, B](a, b);
 * #0 = o
 * #1 = A
 * #2 = boxed representation of A
 * #3 = run-time Type object for A
 * #4 = B
 * #5 = boxed representation of B
 * #6 = run-time Type object for B
 * #7 = a
 * #8 = b
 *
 * For "c++" annotations:
 *
 * As for "java" except boxed and run-time representations of type vars should not be used.  Also there is also the capability to refer to type params and method params by name:
 * #this = o
 * #X = A
 * #Y = B
 * #x = a
 * #y = b
 */
public interface Native(lang: String, code: String) extends StatementAnnotation, MethodAnnotation, FieldAnnotation { }
