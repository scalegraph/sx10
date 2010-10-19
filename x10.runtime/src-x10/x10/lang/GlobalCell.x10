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

package x10.lang;

public class GlobalCell[T] {
	
	protected val root : GlobalRef[Cell[T]];
    def this(v:T) {
    	root = GlobalRef[Cell[T]](new Cell[T](v));
    }
   

    /**
     * Return a string representation of the GlobalCell.
     * 
     */
    public def toString()  = root.toString();


    /**
     * Return the value stored in the Cell.
     * Will work even if the Cell reference is remote.
     *
     * @return the current value stored in the Cell.
     */
    public def apply() =  at (root) root().value;

    /**
     * Set the value stored in the Cell to the new value.
     * Will work even if the Cell reference is remote.
     *
     * @param x the new value
     */
    public def apply(x:T) { at(root) root().value = x; }

    /**
     * Set the value stored in the Cell to the new value.
     * Will work even if the Cell reference is remote.
     *
     * @param x the new value
     * @return the new value stored in the Cell.
     */
    public def set(x:T) { 
    	at(root) { 
    		root().value = x; 
        }
    	return x;
   }

    /**
     * Create a new Cell with the given value stored in it.
     *
     * @param T the value type of the Cell
     * @param x the given value
     * @return a new Cell with the given value stored in it.
     */
    public static def make[T](x:T)= (new GlobalCell[T](new Cell[T](x)));


    /**
     * Return the value stored in the given GlobalCell.
     * Will work even if the Cell reference is remote.
     *
     * @param T the value type of the Cell
     * @param x the given Cell
     * @return the value stored in the given Cell.
     */
    public static operator[T](x:GlobalCell[T]) = x();

}

// vim:tabstop=4:shiftwidth=4:expandtab
