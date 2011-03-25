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

package x10.rtt;


public class BooleanType extends RuntimeType<Boolean> {

	private static final long serialVersionUID = 1L;

    public BooleanType() {
        super(boolean.class,
              new Type[] {
                  new ParameterizedType(Types.COMPARABLE, UnresolvedType.THIS),
                  Types.STRUCT
              });
    }
    
    @Override
    public String typeName() {
        return "x10.lang.Boolean";
    }

    @Override
    public boolean instanceof$(Object o) {
        return o instanceof java.lang.Boolean;
    }

    @Override
    public Object makeArray(int length) {
        return new boolean[length];
    }
    
    @Override
    public Object makeArray(Object... elem) {
        boolean[] arr = new boolean[elem.length];
        for (int i = 0; i < elem.length; i++) {
            arr[i] = ((Boolean)elem[i]).booleanValue();
        }
        return arr;
    }
    
    @Override
    public Boolean getArray(Object array, int i) {
        return ((boolean[]) array)[i];
    }
    
    @Override
    public Boolean setArray(Object array, int i, Boolean v) {
        return ((boolean[]) array)[i] = v;
    }
    
    @Override
    public int arrayLength(Object array) {
    	return ((boolean[]) array).length;
    }
    
}
