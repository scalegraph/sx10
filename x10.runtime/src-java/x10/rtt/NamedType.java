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

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import x10.serialization.X10JavaDeserializer;
import x10.serialization.X10JavaSerializable;
import x10.serialization.X10JavaSerializer;

public class NamedType<T> extends RuntimeType<T> implements X10JavaSerializable {

    private static final long serialVersionUID = 1L;
    
    public String typeName;

    // Just for allocation
    public NamedType() {
        super();
    }
    
    // N.B. this is also used to implement readResolve() in place for Types.COMPARABLE
    protected NamedType(String typeName, Class<?> javaClass, Variance[] variances, Type<?>[] parents) {
        super(javaClass, variances, parents);
        this.typeName = typeName;
    }

    private static final boolean useCache = true;
    private static final ConcurrentHashMap<Class<?>, NamedType<?>> typeCache = new ConcurrentHashMap<Class<?>, NamedType<?>>();
    public static <T> NamedType/*<T>*/ make(String typeName, Class<?> javaClass) {
        if (useCache) {
            NamedType<?> type = typeCache.get(javaClass);
            if (type == null) {
                NamedType<?> type0 = new NamedType<T>(typeName, javaClass, null, null);
                type = typeCache.putIfAbsent(javaClass, type0);
                if (type == null) type = type0;
            }
            return (NamedType<T>) type;
        } else {
            return new NamedType<T>(typeName, javaClass, null, null);
        }
    }

    public static <T> NamedType/*<T>*/ make(String typeName, Class<?> javaClass, Variance[] variances) {
        if (useCache) {
            NamedType<?> type = typeCache.get(javaClass);
            if (type == null) {
                NamedType<?> type0 = new NamedType<T>(typeName, javaClass, variances, null);
                type = typeCache.putIfAbsent(javaClass, type0);
                if (type == null) type = type0;
            }
            return (NamedType<T>) type;
        } else {
            return new NamedType<T>(typeName, javaClass, variances, null);
        }
    }

    public static <T> NamedType/*<T>*/ make(String typeName, Class<?> javaClass, Type<?>[] parents) {
        if (useCache) {
            NamedType<?> type = typeCache.get(javaClass);
            if (type == null) {
                NamedType<?> type0 = new NamedType<T>(typeName, javaClass, null, parents);
                type = typeCache.putIfAbsent(javaClass, type0);
                if (type == null) type = type0;
            }
            return (NamedType<T>) type;
        } else {
            return new NamedType<T>(typeName, javaClass, null, parents);
        }
    }
    
    public static <T> NamedType/*<T>*/ make(String typeName, Class<?> javaClass, Variance[] variances, Type<?>[] parents) {
        if (useCache) {
            NamedType<?> type = typeCache.get(javaClass);
            if (type == null) {
                NamedType<?> type0 = new NamedType<T>(typeName, javaClass, variances, parents);
                type = typeCache.putIfAbsent(javaClass, type0);
                if (type == null) type = type0;
            }
            return (NamedType<T>) type;
        } else {
            return new NamedType<T>(typeName, javaClass, variances, parents);
        }
    }

    @Override
    public String typeName() {
        return typeName;
    }

    @Override
    public void $_serialize(X10JavaSerializer serializer) throws IOException {
        super.$_serialize(serializer);
        serializer.write(typeName);
    }

    public static X10JavaSerializable $_deserializer(X10JavaDeserializer deserializer) throws IOException {
        NamedType namedType = new NamedType();
        deserializer.record_reference(namedType);
        return $_deserialize_body(namedType, deserializer);
    }

    public static X10JavaSerializable $_deserialize_body(NamedType nt, X10JavaDeserializer deserializer) throws IOException {
        RuntimeType.$_deserialize_body(nt, deserializer);
        nt.typeName = deserializer.readString();
        return nt;
    }
}
