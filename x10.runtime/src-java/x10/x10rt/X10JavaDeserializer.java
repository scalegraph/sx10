/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2011.
 */

package x10.x10rt;

import sun.misc.Unsafe;
import x10.core.GlobalRef;
import x10.core.IndexedMemoryChunk;
import x10.core.X10Throwable;
import x10.io.SerialData;
import x10.rtt.NamedType;
import x10.rtt.RuntimeType;
import x10.runtime.impl.java.Runtime;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class X10JavaDeserializer {

    // When a Object is deserialized record its position
    private List<Object> objectList;
    private DataInputStream in;
    private int counter = 0;
    private static Unsafe unsafe = getUnsafe();
    private static final String CONSTRUCTOR_METHOD_NAME_FOR_REFLECTION = "$init_for_reflection";

    public X10JavaDeserializer(DataInputStream in) {
        this.in = in;
        objectList = new ArrayList<Object>();
    }

    public int record_reference(Object obj) {
        if (Runtime.TRACE_SER) {
            System.out.println("\tRecording reference of type " + obj.getClass() + " at " + counter + "  (absolute) in map");
        }
        objectList.add(counter, obj);
        counter++;
        return counter - 1;
    }

    public void update_reference(int pos, Object obj) {
        objectList.set(pos, obj);
    }


    public Object getObjectAtPosition(int pos) {
        Object o = objectList.get(pos);
        if (Runtime.TRACE_SER) {
            System.out.println("\t\tRetrieving repeated reference  of type " + o.getClass() + " at " + pos + "  (absolute) in map");
        }
        return o;
    }

    public Object readRef() throws IOException {
        int serializationID = readInt();
        if (serializationID == DeserializationDispatcher.refValue) {
            return getObjectAtPosition(readInt());
        }
        if (serializationID == DeserializationDispatcher.javaClassID) {
            return deserializeRefUsingReflection(serializationID);
        }
        return DeserializationDispatcher.getInstanceForId(serializationID, this);
    }

    public <T> void readArray(T[] array) throws IOException {
        int length = array.length;
        for (int i = 0; i < length; i++) {
            array[i] = (T) readRef();
        }
    }

    public int readInt() throws IOException {
        int v = in.readInt();
        if (Runtime.TRACE_SER) {
            System.out.println("Deserializing a int: " + v);
        }
        return v;
    }

    public int[] readIntArray() throws IOException {
        int length = in.readInt();
        int[] v = new int[length];
        for (int i = 0; i < length; i++) {
            v[i] = in.readInt();
        }
        return v;
    }

    public boolean readBoolean() throws IOException {
        boolean v = in.readBoolean();
        if (Runtime.TRACE_SER) {
            System.out.println("Deserializing a boolean: " + v);
        }
        return v;
    }

    public boolean[] readBooleanArray() throws IOException {
        int length = in.readInt();
        boolean[] v = new boolean[length];
        for (int i = 0; i < length; i++) {
            v[i] = in.readBoolean();
        }
        return v;
    }

    public char readChar() throws IOException {
        char v = in.readChar();
        if (Runtime.TRACE_SER) {
            System.out.println("Deserializing a char: " + v);
        }
        return v;
    }

    public char[] readCharArray() throws IOException {
        int length = in.readInt();
        char[] v = new char[length];
        for (int i = 0; i < length; i++) {
            v[i] = in.readChar();
        }
        return v;
    }

    public byte readByte() throws IOException {
        byte v = in.readByte();
        if (Runtime.TRACE_SER) {
            System.out.println("Deserializing a byte: " + v);
        }
        return v;
    }

    public byte[] readByteArray() throws IOException {
        int length = in.readInt();
        byte[] v = new byte[length];
        _readByteArray(length, v);
        return v;
    }

    public void _readByteArray(int length, byte[] v) throws IOException {
        int read = 0;
        while (read < length) {
                read += in.read(v, read, length-read);
        }
    }

    public short readShort() throws IOException {
        short v = in.readShort();
        if (Runtime.TRACE_SER) {
            System.out.println("Deserializing a short: " + v);
        }
        return v;
    }

    public short[] readShortArray() throws IOException {
        int length = in.readInt();
        short[] v = new short[length];
        for (int i = 0; i < length; i++) {
            v[i] = in.readShort();
        }
        return v;
    }

    public long readLong() throws IOException {
        long v = in.readLong();
        if (Runtime.TRACE_SER) {
            System.out.println("Deserializing a long: " + v);
        }
        return v;
    }

    public long[] readLongArray() throws IOException {
        int length = in.readInt();
        long[] v = new long[length];
        for (int i = 0; i < length; i++) {
            v[i] = in.readLong();
        }
        return v;
    }

    public double readDouble() throws IOException {
        double v = in.readDouble();
        if (Runtime.TRACE_SER) {
            System.out.println("Deserializing a double: " + v);
        }
        return v;
    }

    public double[] readDoubleArray() throws IOException {
        int length = in.readInt();
        double[] v = new double[length];
        for (int i = 0; i < length; i++) {
            v[i] = in.readDouble();
        }
        return v;
    }

    public float readFloat() throws IOException {
        float v = in.readFloat();
        if (Runtime.TRACE_SER) {
            System.out.println("Deserializing a float: " + v);
        }
        return v;
    }

    public float[] readFloatArray() throws IOException {
        int length = in.readInt();
        float[] v = new float[length];
        for (int i = 0; i < length; i++) {
            v[i] = in.readFloat();
        }
        return v;
    }

    public String readString() throws IOException {
        int classID = in.readInt();
        if (classID == DeserializationDispatcher.refValue) {
            return (String) getObjectAtPosition(readInt());
        } else if (classID == DeserializationDispatcher.NULL_ID) {
            if (Runtime.TRACE_SER) {
                System.out.println("Deserializing a null reference");
            }
            return null;
        }
        String str = readStringValue();
        if (Runtime.TRACE_SER) {
            System.out.println("Deserializing a String: " + str);
        }
        record_reference(str);
        return str;
    }

    public String readStringValue() throws IOException {
        int length = readInt();
        byte[] bytes = new byte[length];
        _readByteArray(length, bytes);
        return new String(bytes);
    }

    public String[] readStringArray() throws IOException {
        int length = in.readInt();
        String[] v = new String[length];
        for (int i = 0; i < length; i++) {
            v[i] = readString();
        }
        return v;
    }

    public Object readRefUsingReflection() throws IOException {
        int serializationID = readInt();
        if (serializationID == DeserializationDispatcher.refValue) {
            return getObjectAtPosition(readInt());
        } else if (serializationID == DeserializationDispatcher.NULL_ID) {
            if (Runtime.TRACE_SER) {
                System.out.println("Deserialized a null reference");
            }
            return null;
        } else if (serializationID <= 8) {
            return DeserializationDispatcher.deserializePrimitive(serializationID, this);
        }

        if (Runtime.TRACE_SER) {
            System.out.println("Deserializing non-null value with id " + serializationID);
        }
        return deserializeRefUsingReflection(serializationID);
    }

    private Object deserializeRefUsingReflection(int serializationID) throws IOException {
        try {
            Class<?> clazz = DeserializationDispatcher.getClassForID(serializationID, this);
            Object o = unsafe.allocateInstance(clazz);
            int i = record_reference(o);
            Class<?> superclass = clazz.getSuperclass();

            if ("x10.rtt.FloatType".equals(clazz.getName()) || "x10.rtt.IntType".equals(clazz.getName())
                    || "x10.rtt.DoubleType".equals(clazz.getName())
                    || "x10.rtt.LongType".equals(clazz.getName())
                    || "x10.rtt.BooleanType".equals(clazz.getName())
                    || "x10.rtt.StringType".equals(clazz.getName())
                    || "x10.rtt.CharType".equals(clazz.getName())
                    || "x10.rtt.ByteType".equals(clazz.getName())
                    || "x10.rtt.ShortType".equals(clazz.getName())
                    || "x10.rtt.ObjectType".equals(clazz.getName())
                    || "x10.rtt.UByteType".equals(clazz.getName())
                    || "x10.rtt.UIntType".equals(clazz.getName())
                    || "x10.rtt.ULongType".equals(clazz.getName())
                    || "x10.rtt.UShortType".equals(clazz.getName())) {
                readInt();
                // These classes dont implement the serialization/deserialization routines, hence we deserialize the superclass
                return deserializeClassUsingReflection(superclass, o, i);
            }
            return deserializeClassUsingReflection(clazz, o, i);
        } catch (SecurityException e) {
            // This should never happen
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T deserializeClassUsingReflection(Class<?> clazz, T obj, int i) throws IOException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

        // We need to handle these classes in a special way cause there implementation of serialization/deserialization is
        // not straight forward. Hence we just call into the custom serialization of these classes.
        if ("java.lang.String".equals(clazz.getName())) {
            obj = (T) readStringValue();
            return obj;
        } else if ("x10.rtt.NamedType".equals(clazz.getName())) {
            NamedType.$_deserialize_body((NamedType) obj, this);
            return obj;
        } else if ("x10.rtt.RuntimeType".equals(clazz.getName())) {
            X10JavaSerializable x10JavaSerializable = RuntimeType.$_deserialize_body((RuntimeType) obj, this);
            if (obj != x10JavaSerializable) {
                update_reference(i, x10JavaSerializable);
                obj = (T) x10JavaSerializable;
            }
            return obj;
        } else if ("x10.core.IndexedMemoryChunk".equals(clazz.getName())) {
            IndexedMemoryChunk imc = (IndexedMemoryChunk)obj;
            ((IndexedMemoryChunk) obj)._deSerialize_body(imc, this);
            return (T) imc;
        } else if ("x10.core.IndexedMemoryChunk$$Closure$0".equals(clazz.getName())) {
            return (T) IndexedMemoryChunk.$Closure$0.$_deserialize_body((IndexedMemoryChunk.$Closure$0) obj, this);
        } else if ("x10.core.IndexedMemoryChunk$$Closure$1".equals(clazz.getName())) {
            return (T) IndexedMemoryChunk.$Closure$1.$_deserialize_body((IndexedMemoryChunk.$Closure$1) obj, this);
        } else if (GlobalRef.class.getName().equals(clazz.getName())) {
            return (T) GlobalRef.$_deserialize_body((GlobalRef)obj, this);
        } else if (X10Throwable.class.getName().equals(clazz.getName())) {
            return (T) X10Throwable.$_deserialize_body((X10Throwable)obj, this);
        }

        Class[] interfaces = clazz.getInterfaces();
        boolean isCustomSerializable = false;
        for (Class aInterface : interfaces) {
            if ("x10.io.CustomSerialization".equals(aInterface.getName())) {
                isCustomSerializable = true;
                break;
            }
        }

        Class<?> superclass = clazz.getSuperclass();
        if (!isCustomSerializable && !("java.lang.Object".equals(superclass.getName()) || "x10.core.Ref".equals(superclass.getName()) || "x10.core.Struct".equals(superclass.getName()))) {
            // We need to deserialize the super class first
            obj = deserializeClassUsingReflection(superclass, obj, i);
        }

        // We need to sort the fields first. Cause the order here could depend on the JVM.
        Field[] declaredFields = clazz.getDeclaredFields();
        Set<Field> fields = new TreeSet<Field>(new FieldComparator());
        for (Field field : declaredFields) {
            if (field.isSynthetic())
                continue;
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
                continue;
            }
            fields.add(field);
        }

        for (Field field : fields) {
            field.setAccessible(true);
            Class<?> type = field.getType();
            if (type.isPrimitive()) {
                readPrimitiveUsingReflection(field, obj);
            } else if (type.isArray()) {
                field.set(obj, readArrayUsingReflection(type.getComponentType()));
            } else if ("java.lang.String".equals(type.getName())) {
                field.set(obj, readStringUsingReflection());
            } else {
                Object value = readRefUsingReflection();
                field.set(obj, value);
            }
        }

        if (isCustomSerializable) {
                SerialData serialData = (SerialData) readRefUsingReflection();

               // We cant use the same method name in all classes cause it creates and endless loop cause whn super.init is called it calls back to this method
                Method makeMethod = clazz.getMethod(clazz.getName().replace(".", "$") + CONSTRUCTOR_METHOD_NAME_FOR_REFLECTION, SerialData.class);
                makeMethod.setAccessible(true);
                makeMethod.invoke(obj, serialData);
            }
        return obj;
    }

    private <T> void readPrimitiveUsingReflection(Field field, T obj) throws IOException, IllegalAccessException {
        Class<?> type = field.getType();
        if ("int".equals(type.getName())) {
            field.setInt(obj, readInt());
        } else if ("double".equals(type.getName())) {
            field.setDouble(obj, readDouble());
        } else if ("float".equals(type.getName())) {
            field.setFloat(obj, readFloat());
        } else if ("boolean".equals(type.getName())) {
            field.setBoolean(obj, readBoolean());
        } else if ("byte".equals(type.getName())) {
            field.setByte(obj, readByte());
        } else if ("short".equals(type.getName())) {
            field.setShort(obj, readShort());
        } else if ("long".equals(type.getName())) {
            field.setLong(obj, readLong());
        } else if ("char".equals(type.getName())) {
            field.setChar(obj, readChar());
        }
    }

    private Object readArrayUsingReflection(Class<?> componentType) throws IOException, IllegalAccessException {
        if (componentType.isPrimitive()) {
            if ("int".equals(componentType.getName())) {
                return readIntArray();
            } else if ("double".equals(componentType.getName())) {
                return readDoubleArray();
            } else if ("float".equals(componentType.getName())) {
                return readFloatArray();
            } else if ("boolean".equals(componentType.getName())) {
                return readBooleanArray();
            } else if ("byte".equals(componentType.getName())) {
                return readByteArray();
            } else if ("short".equals(componentType.getName())) {
                return readShortArray();
            } else if ("long".equals(componentType.getName())) {
                return readLongArray();
            } else if ("char".equals(componentType.getName())) {
                return readCharArray();
            }
        } else if ("java.lang.String".equals(componentType.getName())) {
            return readStringArray();
        } else if (componentType.isArray()) {
            int length = readInt();
            Object o = Array.newInstance(componentType, length);
            for (int i = 0; i < length; i++) {
                 Array.set(o, i, readArrayUsingReflection(componentType));
            }
            return o;
        } else {
            int length = readInt();
            Object o = Array.newInstance(componentType, length);
            for (int i = 0; i < length; i++) {
                Array.set(o, i, readRefUsingReflection());
            }
            return o;
        }
        return null;
    }

    private String readStringUsingReflection() throws IOException {
           return readString();
    }

    public static Unsafe getUnsafe() {
        Unsafe unsafe = null;
        try {
            Class uc = Unsafe.class;
            Field[] fields = uc.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].getName().equals("theUnsafe")) {
                    fields[i].setAccessible(true);
                    unsafe = (Unsafe) fields[i].get(uc);
                    break;
                }
            }
        } catch (Exception ignore) {
        }
        return unsafe;
    }

}
