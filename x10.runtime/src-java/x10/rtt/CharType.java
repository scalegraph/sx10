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


public class CharType extends RuntimeType<x10.core.Char> {

    private static final long serialVersionUID = 1L;

    // make sure deserialized RTT object is not duplicated
    private Object readResolve() throws java.io.ObjectStreamException {
        return Types.CHAR;
    }

    public CharType() {
        super(x10.core.Char.class,
              new Type[] {
                  new ParameterizedType(Types.COMPARABLE, UnresolvedType.THIS),
                  new ParameterizedType(x10.util.Ordered.$RTT, UnresolvedType.THIS),
                  Types.STRUCT
              });
    }
    
    @Override
    public String typeName() {
        return "x10.lang.Char";
    }

    // for shortcut 
    @Override
    public boolean instanceOf(Object o) {
        return o instanceof x10.core.Char;
    }

    @Override
    public Object makeArray(int length) {
        return new char[length];
    }
    
    @Override
    public Object makeArray(Object... elem) {
        char[] arr = new char[elem.length];
        for (int i = 0; i < elem.length; i++) {
            arr[i] = ((Character)elem[i]).charValue();
        }
        return arr;
    }
    
    @Override
    public x10.core.Char getArray(Object array, int i) {
        return x10.core.Char.$box(((char[]) array)[i]);
    }
    
//    @Override
//    public Character setArray(Object array, int i, x10.core.Char v) {
//        ((char[]) array)[i] = x10.core.Char.$unbox(v);
//        return v;
//    }
    @Override
    public void setArray(Object array, int i, x10.core.Char v) {
        ((char[]) array)[i] = x10.core.Char.$unbox(v);
    }
    
    @Override
    public int arrayLength(Object array) {
    	return ((char[]) array).length;
    }

}
