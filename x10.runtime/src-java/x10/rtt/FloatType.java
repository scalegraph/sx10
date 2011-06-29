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


import x10.x10rt.X10JavaDeserializer;
import x10.x10rt.X10JavaSerializable;
import x10.x10rt.X10JavaSerializer;

import java.io.IOException;

public class FloatType extends RuntimeType<x10.core.Float> implements X10JavaSerializable{

	private static final long serialVersionUID = 1L;
    private static final int _serialization_id = x10.x10rt.DeserializationDispatcher.addDispatcher(FloatType.class.getName());

    // make sure deserialized RTT object is not duplicated
    private Object readResolve() throws java.io.ObjectStreamException {
        return Types.FLOAT;
    }

    public FloatType() {
        super(x10.core.Float.class,
              new Type[] {
                  new ParameterizedType(Types.COMPARABLE, UnresolvedType.THIS),
                  new ParameterizedType(x10.lang.Arithmetic.$RTT, UnresolvedType.THIS),
                  new ParameterizedType(x10.util.Ordered.$RTT, UnresolvedType.THIS),
                  Types.STRUCT
              });
    }
    
    @Override
    public String typeName() {
        return "x10.lang.Float";
    }

    // for shortcut 
    @Override
    public boolean instanceOf(Object o) {
        return o instanceof x10.core.Float;
    }

    @Override
    public Object makeArray(int length) {
        return new float[length];
    }
    
    @Override
    public Object makeArray(Object... elem) {
        float[] arr = new float[elem.length];
        for (int i = 0; i < elem.length; i++) {
            arr[i] = ((Number)elem[i]).floatValue();
        }
        return arr;
    }
    
    @Override
    public x10.core.Float getArray(Object array, int i) {
        return x10.core.Float.$box(((float[]) array)[i]);
    }
    
    @Override
    public void setArray(Object array, int i, x10.core.Float v) {
        ((float[]) array)[i] = x10.core.Float.$unbox(v);
    }
    
    @Override
    public int arrayLength(Object array) {
    	return ((float[]) array).length;
    }

    public void _serialize(X10JavaSerializer serializer) throws IOException {
    }

    public int _get_serialization_id() {
        return _serialization_id;
    }

    public static X10JavaSerializable _deserializer(X10JavaDeserializer deserializer) throws IOException {
		return _deserialize_body(null, deserializer);
	}

    public static X10JavaSerializable _deserialize_body(FloatType t, X10JavaDeserializer deserializer) throws IOException {
        FloatType floatType = (FloatType) Types.FLOAT;
        deserializer.record_reference(floatType);
        return floatType;
    }
}
