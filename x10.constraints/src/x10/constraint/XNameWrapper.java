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

package x10.constraint;

/**
 * A wrapper for a named entity of type T that holds the entity itself,
 * and the name. 
 * 
 * <p> The name can be explicitly supplied by the client. If it is not
 * supplied, it is obtained from the entity via <code>toString()</code>. 
 * 
 * <p> Because <code>XName</code>'s are used by the constraint framework to identify
 * entities, either instances of the type <code>T</code> must be canonicalized, or 
 * <code>T</code> must implement <code>equals()</code> properly.
 * 
 * @param <T> the type of entity being wrapped
 * @author njnystrom
 */
public class XNameWrapper<T> implements XName {
	T v;
	String s;

	public XNameWrapper(T v) {
		this(v, v.toString());
	}
	
	public XNameWrapper(T v, String s) {
		this.v = v;
		this.s = s;
	}

	public T val() {
		return v;
	}
	
	public String toString() {
		return s;
	}

	public int hashCode() {
		return v.hashCode();
	}

	public boolean equals(Object o) {
		return o instanceof XNameWrapper<?> && v.equals(((XNameWrapper<?>) o).v);
	}
}
