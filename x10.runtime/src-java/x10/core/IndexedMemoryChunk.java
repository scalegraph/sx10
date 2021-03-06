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

package x10.core;

import java.io.IOException;
import java.util.Arrays;

import x10.core.fun.VoidFun_0_0;
import x10.lang.Place;
import x10.rtt.NamedType;
import x10.rtt.RuntimeType;
import x10.rtt.Type;
import x10.rtt.Types;
import x10.serialization.SerializationConstants;
import x10.serialization.X10JavaDeserializer;
import x10.serialization.X10JavaSerializable;
import x10.serialization.X10JavaSerializer;

public final class IndexedMemoryChunk<T> extends x10.core.Struct implements X10JavaSerializable {

    private static final long serialVersionUID = 1L;

    public Type<T> T;
    public int length;
    public Object value;

    // constructor just for allocation
    public IndexedMemoryChunk(java.lang.System[] $dummy) {
        // call default constructor instead of "constructor just for allocation" for x10.core.Struct
//        super($dummy);
    }

    public final IndexedMemoryChunk<T> x10$util$IndexedMemoryChunk$$init$S(Type<T> T, int length, Object value) {
        this.T = T;
        this.length = length;
        this.value = value;
        return this;
    }

    public IndexedMemoryChunk(Type<T> T, int length, Object value) {
        this.T = T;
        this.length = length;
        this.value = value;
    }

    public final IndexedMemoryChunk<T> x10$util$IndexedMemoryChunk$$init$S(Type<T> T) {
        this.x10$util$IndexedMemoryChunk$$init$S(T, 0, null);
        return this;
    }

    public IndexedMemoryChunk(Type<T> T) {
        this(T, 0, null);
    }

    // zero value constructor
    public IndexedMemoryChunk(Type<T> T, java.lang.System $dummy) {
        this(T);
    }

    private IndexedMemoryChunk(Type<T> T, int length, boolean zeroed) {
        this(T, length, T.makeArray(length));
        if (zeroed) {
            if (!Types.hasNaturalZero(T)) {
                Object zeroValue = Types.zeroValue(T);
                java.util.Arrays.fill((Object[]) value, zeroValue);
            }
        }
    }

    public static <T> IndexedMemoryChunk<T> allocate(Type/*<T>*/ T, long length, boolean zeroed) {
        if (length > Integer.MAX_VALUE) {
            // TODO
            throw new java.lang.OutOfMemoryError("Array length must be shorter than 2^31");
        }
        return new IndexedMemoryChunk<T>(T, (int) length, zeroed);
    }

    public static <T> IndexedMemoryChunk<T> allocate(Type/*<T>*/ T, int length, boolean zeroed) {
        return new IndexedMemoryChunk<T>(T, length, zeroed);
    }

    @Override
    public java.lang.String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("IndexedMemoryChunk(");
        int sz = Math.min(length, 10);
        for (int i = 0; i < sz; i++) {
            if (i > 0)
                sb.append(",");
            sb.append($apply$G(i));
        }
        if (sz < length)
            sb.append("...(omitted " + (length - sz) + " elements)");
        sb.append(")");
        return sb.toString();
    }

    public T $apply$G(int i) {
        return T.getArray(value, i);
    }

    public void $set(int i, T v) {
        T.setArray(value, i, v);
    }

    public void set_unsafe(T v, int i) {
        $set(i, v);
    }

    public void clear(int start, int numElems) {
        if (numElems <= 0)
            return;
        if (value instanceof boolean[]) {
            Arrays.fill(getBooleanArray(), start, start + numElems, false);
        } else if (value instanceof byte[]) {
            Arrays.fill(getByteArray(), start, start + numElems, (byte) 0);
        } else if (value instanceof char[]) {
            Arrays.fill(getCharArray(), start, start + numElems, (char) 0);
        } else if (value instanceof short[]) {
            Arrays.fill(getShortArray(), start, start + numElems, (short) 0);
        } else if (value instanceof int[]) {
            Arrays.fill(getIntArray(), start, start + numElems, 0);
        } else if (value instanceof float[]) {
            Arrays.fill(getFloatArray(), start, start + numElems, 0.0F);
        } else if (value instanceof long[]) {
            Arrays.fill(getLongArray(), start, start + numElems, 0L);
        } else if (value instanceof double[]) {
            Arrays.fill(getDoubleArray(), start, start + numElems, 0.0);
        } else {
            Object zeroValue = Types.zeroValue(T);
            Arrays.fill(getObjectArray(), start, start + numElems, zeroValue);
        }
    }

    public void deallocate() {
        value = null;
        length = 0;
    }

    public static <T> void asyncCopy(IndexedMemoryChunk<T> src, final int srcIndex, final RemoteIndexedMemoryChunk<T> dst, final int dstIndex, final int numElems) {
        // synchronous version for the same place
        if (dst.home.id == x10.lang.Runtime.home().id) {
            System.arraycopy(src.value, srcIndex, dst.$apply$G().value, dstIndex, numElems);
            return;
        }

        // extra copy here simplifies logic and allows us to do this entirely at the Java level.
        // We'll eventually need to optimize this by writing custom native/JNI code instead of treating
        // it as just another async to execute remotely.
        final Object dataToCopy;
        if (numElems == src.length) {
            dataToCopy = src.getBackingArray();
        } else {
            dataToCopy = allocate(src.T, numElems, false).getBackingArray();
            System.arraycopy(src.value, srcIndex, dataToCopy, 0, numElems);
        }

        VoidFun_0_0 copyBody = new $Closure$0(dataToCopy, dst.id, dstIndex, numElems);

        x10.lang.Runtime.runAsync(dst.home, copyBody, null);
    }

    // static nested class version of copyBody
    public static class $Closure$0 extends x10.core.Ref implements VoidFun_0_0 {
        private static final long serialVersionUID = 1L;
        public Object srcData;
        public int dstId;
        public int dstIndex;
        public int numElems;

        // Just for allocation
        $Closure$0() {
        }
        $Closure$0(Object srcData, int dstId, int dstIndex, int numElems) {
            this.srcData = srcData;
            this.dstId = dstId;
            this.dstIndex = dstIndex;
            this.numElems = numElems;
        }
        public void $apply() {
            Object dstData = RemoteIndexedMemoryChunk.getValue(dstId);
            System.arraycopy(srcData, 0, dstData, dstIndex, numElems);
        }
        public static final RuntimeType<$Closure$0> $RTT = x10.rtt.StaticVoidFunType.<$Closure$0> make($Closure$0.class, new Type[] { VoidFun_0_0.$RTT });
        public RuntimeType<$Closure$0> $getRTT() {
            return $RTT;
        }
        public Type<?> $getParam(int i) {
            return null;
        }

        //TODO Keith This is not compatible with C++ at the moment cause the java backend does not implement send_put
        public void $_serialize(X10JavaSerializer $serializer) throws IOException {
            $serializer.write(this.numElems);
            if (this.numElems > 0) {
                Class<?> componentType = this.srcData.getClass().getComponentType();
                if (componentType.isPrimitive()) {
                    $serializer.write(SerializationConstants.JAVA_OBJECT_STREAM_ID);
                    $serializer.writeUsingObjectOutputStream(this.srcData);
                } else if (componentType.equals(java.lang.String.class)) {
                    $serializer.write(SerializationConstants.STRING_ID);
                    $serializer.write((java.lang.String[]) this.srcData);
                } else if (this.srcData instanceof X10JavaSerializable[]) {
                    $serializer.write((X10JavaSerializable[]) this.srcData);
                } else {
                    $serializer.write((Object[]) this.srcData);
                }
            }
            $serializer.write(this.dstId);
            $serializer.write(this.dstIndex);
        }

        public static X10JavaSerializable $_deserializer(X10JavaDeserializer $deserializer) throws IOException {
            $Closure$0 $_obj = new $Closure$0();
            $deserializer.record_reference($_obj);
            return $_deserialize_body($_obj, $deserializer);
        }

        public static X10JavaSerializable $_deserialize_body($Closure$0 $_obj, X10JavaDeserializer $deserializer) throws IOException {
            $_obj.numElems = $deserializer.readInt();
            if ($_obj.numElems > 0) {
                short serializationID = $deserializer.readShort();
                if (serializationID == SerializationConstants.JAVA_OBJECT_STREAM_ID) {
                    $_obj.srcData = $deserializer.readUsingObjectInputStream();
                } else if (serializationID == SerializationConstants.STRING_ID) {
                    $_obj.srcData = $deserializer.readStringArray();
                } else {
                    $_obj.srcData = $deserializer.readRef();
                }
            }
            $_obj.dstId = $deserializer.readInt();
            $_obj.dstIndex = $deserializer.readInt();
            return $_obj;
        }
    }

    public static <T> void asyncCopy(IndexedMemoryChunk<T> src, int srcIndex, RemoteIndexedMemoryChunk<T> dst, int dstIndex, int numElems, VoidFun_0_0 notifier) {
        // synchronous version for the same place
        if (dst.home.id == x10.lang.Runtime.home().id) {
            System.arraycopy(src.value, srcIndex, dst.$apply$G().value, dstIndex, numElems);
            notifier.$apply();
            return;
        }

        throw new java.lang.UnsupportedOperationException("asyncCopy with notifier not implemented for multivm");
        // notifier.$apply();
    }

    public static <T> void asyncCopy(final RemoteIndexedMemoryChunk<T> src, final int srcIndex, IndexedMemoryChunk<T> dst, final int dstIndex, final int numElems) {
        // synchronous version for the same place
        if (src.home.id == x10.lang.Runtime.home().id) {
            System.arraycopy(src.$apply$G().value, srcIndex, dst.value, dstIndex, numElems);
            return;
        }

        // A really bad implementation!  Leaks dst!!  Non-optimized copies! Extra distributed async/finish traffic!
        final RemoteIndexedMemoryChunk<T> dstWrapper = RemoteIndexedMemoryChunk.wrap(dst);

        VoidFun_0_0 copyBody1 = new $Closure$1<T>(src, srcIndex, dstWrapper, dstIndex, numElems);

        x10.lang.Runtime.runAsync(src.home, copyBody1, null);
    }

    // static nested class version of copyBody1
    public static class $Closure$1<T> extends x10.core.Ref implements VoidFun_0_0 {
        private static final long serialVersionUID = 1L;
        public Type<T> srcT;
        public int srcId;
        public int srcLength;
        public int srcIndex;
        public int dstWrapperId;
        public Place dstWrapperHome;
        public int dstIndex;
        public int numElems;

        //Just for allocation
        $Closure$1() {
        }
        $Closure$1(RemoteIndexedMemoryChunk<T> src, int srcIndex, RemoteIndexedMemoryChunk<T> dstWrapper, int dstIndex, int numElems) {
            this.srcT = src.T;
            this.srcId = src.id;
            this.srcLength = src.length;
            this.srcIndex = srcIndex;
            this.dstWrapperId = dstWrapper.id;
            this.dstWrapperHome = dstWrapper.home;
            this.dstIndex = dstIndex;
            this.numElems = numElems;
        }
        public void $apply() {
            // This body runs at src's home.  It accesses the data for src and then does
            // another async back to dstWrapper's home to transfer the data.
            Object srcData = RemoteIndexedMemoryChunk.getValue(srcId);

            // extra copy here simplifies logic and allows us to do this entirely at the Java level.
            // We'll eventually need to optimize this by writing custom native/JNI code instead of treating
            // it as just another async to execute remotely.
            final Object dataToCopy;
            if (numElems == srcLength) {
                dataToCopy = srcData;
            } else {
                dataToCopy = allocate(srcT, numElems, false).getBackingArray();
                System.arraycopy(srcData, srcIndex, dataToCopy, 0, numElems);
            }

            // N.B. copyBody2 is same as copyBody 
            VoidFun_0_0 copyBody2 = new $Closure$0(dataToCopy, dstWrapperId, dstIndex, numElems);

            x10.lang.Runtime.runAsync(dstWrapperHome, copyBody2, null);
        }
        public static final RuntimeType<$Closure$1<?>> $RTT = x10.rtt.StaticVoidFunType.<$Closure$1<?>> make($Closure$1.class, new Type[] { VoidFun_0_0.$RTT });
        public RuntimeType<$Closure$1<?>> $getRTT() {
            return $RTT;
        }
        public Type<?> $getParam(int i) {
            return i == 0 ? srcT : null;
        }

        //TODO Keith This is not compatible with C++ at the moment cause the java backend does not implement send_put
        public void $_serialize(X10JavaSerializer $serializer) throws IOException {
            $serializer.write(this.srcT);
            $serializer.write(this.srcId);
            $serializer.write(this.srcLength);
            $serializer.write(this.srcIndex);
            $serializer.write(this.dstWrapperId);
            $serializer.write(this.dstWrapperHome);
            $serializer.write(this.dstIndex);
            $serializer.write(this.numElems);
        }

        public static X10JavaSerializable $_deserializer(X10JavaDeserializer $deserializer) throws IOException {
            $Closure$1 $_obj = new $Closure$1();
            $deserializer.record_reference($_obj);
            return $_deserialize_body($_obj, $deserializer);
        }

        public static X10JavaSerializable $_deserialize_body($Closure$1 $_obj, X10JavaDeserializer $deserializer) throws IOException {
            $_obj.srcT = $deserializer.readRef();
            $_obj.srcId = $deserializer.readInt();
            $_obj.srcLength = $deserializer.readInt();
            $_obj.srcIndex = $deserializer.readInt();
            $_obj.dstWrapperId = $deserializer.readInt();
            $_obj.dstWrapperHome = $deserializer.readRef();
            $_obj.dstIndex = $deserializer.readInt();
            $_obj.numElems = $deserializer.readInt();
            return $_obj;
        }
    }

    public static <T> void asyncCopy(RemoteIndexedMemoryChunk<T> src, int srcIndex, IndexedMemoryChunk<T> dst, int dstIndex, int numElems, VoidFun_0_0 notifier) {
        // synchronous version for the same place
        if (src.home.id == x10.lang.Runtime.home().id) {
            System.arraycopy(src.$apply$G().value, srcIndex, dst.value, dstIndex, numElems);
            notifier.$apply();
            return;
        }

        throw new java.lang.UnsupportedOperationException("asyncCopy with notifier not implemented for multivm");
        // notifier.$apply();
    }

    public static <T> void copy(IndexedMemoryChunk<T> src, int srcIndex, IndexedMemoryChunk<T> dst, int dstIndex, int numElems) {
        System.arraycopy(src.value, srcIndex, dst.value, dstIndex, numElems);
    }

    public boolean _struct_equals$O(Object o) {
        return o != null && this.value == ((IndexedMemoryChunk<?>) o).value;
    }

    // TODO implement remote operations
    public RemoteIndexedMemoryChunk<T> getCongruentSibling(x10.lang.Place p) {
        throw new java.lang.UnsupportedOperationException("Remote operations are not implemented.");
    }

    public static final RuntimeType<IndexedMemoryChunk<?>> $RTT = NamedType.<IndexedMemoryChunk<?>> make("x10.util.IndexedMemoryChunk", IndexedMemoryChunk.class, RuntimeType.INVARIANTS(1), new Type[] { Types.STRUCT });
    public RuntimeType<IndexedMemoryChunk<?>> $getRTT() {
        return $RTT;
    }
    public Type<?> $getParam(int i) {
        return i == 0 ? T : null;
    }


    // Methods to get the backing array.   May be called by generated code.
    public Object getBackingArray() {
        return value;
    }

    public boolean[] getBooleanArray() {
        return (boolean[]) value;
    }
    public byte[] getByteArray() {
        return (byte[]) value;
    }
    public short[] getShortArray() {
        return (short[]) value;
    }
    public char[] getCharArray() {
        return (char[]) value;
    }
    public int[] getIntArray() {
        return (int[]) value;
    }
    public long[] getLongArray() {
        return (long[]) value;
    }
    public float[] getFloatArray() {
        return (float[]) value;
    }
    public double[] getDoubleArray() {
        return (double[]) value;
    }
    public Object[] getObjectArray() {
        return (Object[]) value;
    }

    public void $_serialize(X10JavaSerializer $serializer) throws IOException {
        $serializer.write(T);
        $serializer.write(length);

        // If the T is a java primitive type, we use default java serialization here
        // cause its much faster than writing a single element at a time
        if (Types.isPrimitiveType(T)) {
            $serializer.writeUsingObjectOutputStream(value);
        } else if (Types.isStringType(T)) {
            java.lang.String[] castValue = (java.lang.String[]) value;
            for (java.lang.String v : castValue) {
                $serializer.write(v);
            }
        } else {
            Object[] castValue = (Object[]) value;
            for (Object v : castValue) {
                if (v instanceof X10JavaSerializable) {
                    $serializer.write((X10JavaSerializable) v);
                } else {
                    $serializer.write(v);
                }
            }
        }
    }

    public static X10JavaSerializable $_deserializer(X10JavaDeserializer $deserializer) throws IOException {
        IndexedMemoryChunk $_obj = new IndexedMemoryChunk((java.lang.System[]) null);
        $deserializer.record_reference($_obj);
        return $_deserialize_body($_obj, $deserializer);
    }

    public static X10JavaSerializable $_deserialize_body(IndexedMemoryChunk $_obj, X10JavaDeserializer $deserializer) throws IOException {
        $_obj.T = $deserializer.readRef();
        $_obj.length = $deserializer.readInt();

        // If the T is a java primitive type, we use default java serialization here
        // cause its much faster than reading a single element at a time
        if (Types.isPrimitiveType($_obj.T)) {
            $_obj.value = $deserializer.readUsingObjectInputStream();
        } else if (Types.isStringType($_obj.T)) {
            java.lang.String[] values = (java.lang.String[]) $_obj.T.makeArray($_obj.length);
            for (int i = 0; i < $_obj.length; i++) {
                values[i] = $deserializer.readString();
            }
            $_obj.value = values;
        } else {
            Object[] values = (Object[]) $_obj.T.makeArray($_obj.length);
            for (int i = 0; i < $_obj.length; i++) {
                values[i] = $deserializer.readRef();
            }
            $_obj.value = values;
        }
        return $_obj;
    }

}
