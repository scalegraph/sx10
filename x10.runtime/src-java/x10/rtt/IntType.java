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


public class IntType extends RuntimeType<Integer> {
    public IntType() {
        super(int.class,
            new Type[] {
                new ParameterizedType(Types.COMPARABLE, new UnresolvedType(-1))
            });
    }
    
    @Override
    public String typeName() {
        return "x10.lang.Int";
    }

    @Override
    public boolean instanceof$(Object o) {
        return o instanceof java.lang.Integer;
    }
    
    @Override
    public Object makeArray(int length) {
        return new int[length];
    }
    
    @Override
    public Object makeArray(Object... elem) {
        int[] arr = new int[elem.length];
        for (int i = 0; i < elem.length; i++) {
            arr[i] = ((Number)elem[i]).intValue();
        }
        return arr;
    }
    
    @Override
    public Integer getArray(Object array, int i) {
        return ((int[]) array)[i];
    }
    
    @Override
    public Integer setArray(Object array, int i, Integer v) {
        return ((int[]) array)[i] = v;
    }
    
    @Override
    public int arrayLength(Object array) {
    	return ((int[]) array).length;
    }
    
}
