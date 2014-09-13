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

package x10.util;

/**
 * This struct allows treating a pair of values as a single value, for example when returning from a method.
 */
public struct Pair[T,U] {
    public val first:T;
    public val second:U;

    public def this(first:T, second:U):Pair[T,U] {
        this.first = first;
        this.second = second;
    }

    public def toString():String {
        return "(" + first + ", " + second + ")";
    }

}
