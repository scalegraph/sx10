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

package x10.regionarray;

class ValRow extends Row {

    private val row:Rail[Int];

    public def this(row:Rail[Int]) {
        super(row.size as Int);
        this.row = row;
    }

    public def this(cols:Int, init:(Int)=>Int) {
        super(cols);
        row = new Rail[Int](cols, (i:Long)=>init(i as Int));
    }
    
    public operator this(i:Int) = row(i);
    
    public operator this(i:Int)=(v:Int):Int {
        throw new IllegalOperationException("ValRow.set");
    }
}
