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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import x10.lang.Place;
import x10.lang.Runtime.Mortal;
import x10.rtt.Type;
import x10.x10rt.X10JavaDeserializer;
import x10.x10rt.X10JavaSerializable;
import x10.x10rt.X10JavaSerializer;

public final class GlobalRef<T> extends x10.core.Struct implements
        Externalizable, X10JavaSerializable {
	
	private static final int _serialization_id = x10.x10rt.DeserializationDispatcher.addDispatcher(GlobalRef.class.getName(), "x10.lang.GlobalRef");

    public static final x10.rtt.RuntimeType<GlobalRef<?>> $RTT = new x10.rtt.NamedType<GlobalRef<?>>(
        "x10.lang.GlobalRef",
        GlobalRef.class,
        new x10.rtt.RuntimeType.Variance[] { x10.rtt.RuntimeType.Variance.INVARIANT },
        new x10.rtt.Type[] { x10.rtt.Types.STRUCT }
    );

    @Override
    public x10.rtt.RuntimeType<GlobalRef<?>> $getRTT() {
        return $RTT;
    }

    @Override
    public x10.rtt.Type<?> $getParam(int i) {
        if (i == 0)
            return T;
        return null;
    }

    // a singleton which represents null value
    private static final Object $null = new Object() {
        @Override
        public java.lang.String toString() {
            return "<null>";
        }
    };

    private static final <T> T encodeNull(T t) {
        if (t == null)
            t = (T) $null;
        return t;
    }

    private static final <T> T decodeNull(T t) {
        if (t == $null)
            t = null;
        return t;
    }

//    private static class WeakGlobalRefEntry extends WeakReference {
//        //TODO: Make WeakGlobalRefEntry as a variation of GlobalRefEntry
//        long id;
//        final int hashCode;
//
//        public WeakGlobalRefEntry(long id, Object referent,
//                ReferenceQueue<WeakGlobalRefEntry> referenceQueue) {
//            super(referent, referenceQueue);
//            this.id = id;
//            hashCode = System.identityHashCode(referent);
//        }
//
//        @Override
//        public int hashCode() {
//            return hashCode;
//        }
//    }

    private static class GlobalRefEntry extends WeakReference<Object> {
        final long id;
        private final Object strongRef; // strong reference, used for non-Mortal object
        private final int hashCode;

        GlobalRefEntry(long id, Object obj, ReferenceQueue<Object> refQ, boolean isStrong) {
            super(obj, refQ);
            //System.out.println("GlobalRefEntry: id=" + id + " obj=" + obj + " isStrong=" + isStrong);
            assert(obj != null); // null should be replaced in the caller
                                 // if null is allowed for obj, we cannot distinguish 
                                 // the situation that weak reference is removed
            strongRef = isStrong ? obj : null; // prohibit the collection of the obj
            this.id = id;
            hashCode = System.identityHashCode(obj);
        }
        
        @Override
        public int hashCode() {
            return hashCode;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof GlobalRefEntry)) return false;
            GlobalRefEntry ge = (GlobalRefEntry)obj;
            if (hashCode != ge.hashCode) return false;
            Object t = get();
            if (t == null) return false; // weak reference is removed
            return t == ge.get();
        }
    }

    private static final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<Object>();

//    private static final GlobalRefEntry $nullEntry = new GlobalRefEntry($null);
//
//    private static final GlobalRefEntry wrapObject(Object t) {
//        if (t == $null)
//            return $nullEntry;
//        return new GlobalRefEntry(t);
//    }

    private static AtomicLong lastId = new AtomicLong(0L);
    private static ConcurrentHashMap<java.lang.Long, GlobalRefEntry> id2Object = new ConcurrentHashMap<java.lang.Long, GlobalRefEntry>();
    private static ConcurrentHashMap<GlobalRefEntry, java.lang.Long> object2Id = new ConcurrentHashMap<GlobalRefEntry, java.lang.Long>();
//    private static WeakHashMap<GlobalRefEntry, java.lang.Long> mortal2Id = new WeakHashMap<GlobalRefEntry, java.lang.Long>();

    private x10.rtt.Type<?> T;
    public x10.lang.Place home;
    private long id; // place local id of referenced object
    transient Object t;
    
    // constructor just for allocation
    public GlobalRef(java.lang.System[] $dummy) {
        super($dummy);
    }

    public GlobalRef() {
        T = null;
        home = null;
        id = 0L;
        t = null;
    }

    @Override
    public GlobalRef<T> $init() {
        T = null;
        home = null;
        id = 0L;
        t = null;
        return this;
    }

    public GlobalRef<T> $init(final x10.rtt.Type<?> T, T t, java.lang.Class<?> dummy$0) {
        this.T = T;
        this.home = x10.lang.Runtime.home();
        this.t = t;
        return this;
    }

    public GlobalRef(final x10.rtt.Type<?> T, T t, java.lang.Class<?> dummy$0) {
        this.T = T;
        this.home = x10.lang.Runtime.home();
        this.t = t;
    }

    // zero value constructor
    public GlobalRef(final x10.rtt.Type<?> T, java.lang.System $dummy) {
    	this(T, null, (java.lang.Class<?>) null);
    }

    private static void removeUnusedGlobalRefEntries() {
        GlobalRefEntry ge = null;
        while ((ge = (GlobalRefEntry)referenceQueue.poll()) != null) {
            assert(ge.strongRef==null && ge.id!=0L && ge.get()==null);
            id2Object.remove(ge.id);
            object2Id.remove(ge);
            // ge will be collected by the next GC
        }
    }

    private void globalize() {
        if (isGlobalized()) return; // allready allocated
        removeUnusedGlobalRefEntries(); // clean up garbage entries
        
        assert (T != null);
        assert (home != null);
        
        Object obj = encodeNull(t); // null cannot be passed to GlobalRefEntry
        java.lang.Long tmpId = lastId.incrementAndGet(); //TODO: check wraparound
        GlobalRefEntry ge = new GlobalRefEntry(tmpId, obj, referenceQueue, !(obj instanceof Mortal));
        id2Object.put(tmpId, ge); // set id first
        java.lang.Long existingId = object2Id.putIfAbsent(ge, tmpId);
        if (existingId != null) {
            this.id = existingId;
            id2Object.remove(tmpId);
            // ge will be collected by the next GC
        } else {
            this.id = tmpId;
        }
    }

    private boolean isGlobalized() {
        return id != 0L;
    }

    final public T $apply$G() {
        return (T) t;
    }

    final public x10.lang.Place home() {
        return this.home;
    }

    @Override
    final public java.lang.String toString() {
        globalize(); // necessary to decide the id for this object
        return "GlobalRef(" + this.home + "," + this.id + ")";
    }

    @Override
    final public int hashCode() {
        globalize(); // necessary to decide the id for this object
        return (this.home.hashCode() << 18) + (int) this.id;
    }

    @Override
    final public boolean equals(java.lang.Object other) {
        if (!(other instanceof GlobalRef<?>))
            return false;

        GlobalRef<?> otherGref = (GlobalRef<?>) other;

        return this._struct_equals$O(otherGref);
    }

    final public boolean equals(x10.core.GlobalRef<T> other) {
        return this._struct_equals(other);
    }

    final public boolean _struct_equals$O(java.lang.Object other) {
        if (!x10.core.GlobalRef.$RTT.instanceOf(other, T)) {
            return false;
        }
        return this._struct_equals((x10.core.GlobalRef<T>) other);
    }

    final public boolean _struct_equals(x10.core.GlobalRef<T> other) {
        // if both GlobalRefs are local (home should be here)
        if (!other.isGlobalized() && !isGlobalized()) 
            return (t == other.t); // use "==" rather than "equals"
        // if homes are different
        if (!x10.rtt.Equality.equalsequals(this.home, other.home))
            return false;
        // if homes are same
        globalize(); other.globalize(); // ensure both GlobalRefs have ids
        return x10.rtt.Equality.equalsequals(this.id, other.id);
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        globalize();
        out.writeObject(T);
        out.writeObject(home);
        out.writeLong(id);
    }

    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {

        T = (x10.rtt.Type<?>) in.readObject();
        home = (x10.lang.Place) in.readObject();
        id = in.readLong();

        if (home.id == x10.lang.Runtime.home().id) {
            GlobalRefEntry ge = id2Object.get(id);
            if (ge == null)
                throw new IllegalStateException("No GlobalRefEntry for id=" + id);
            Object obj = ge.get();
            if (obj == null)
                throw new IllegalStateException("No GlobalRef'ed object for id=" + id);
            t = decodeNull(obj);
        } else {
            t = null;
        }

        //System.out.println("GlobalRef is deserialized. home="
        //        + x10.lang.Runtime.home().id + ", ref.home=" + id + ", tgt="
        //        + t);
    }

	public void $_serialize(X10JavaSerializer serializer) throws IOException {
        globalize();
        serializer.write(T);
        serializer.write(home);
        serializer.write(id);
	}

	public static X10JavaSerializable $_deserializer(X10JavaDeserializer deserializer) throws java.io.IOException {
       GlobalRef gr = new GlobalRef();
        deserializer.record_reference(gr);
        return $_deserialize_body(gr, deserializer);
	}

	public int $_get_serialization_id() {
		return _serialization_id;
	}

    public static X10JavaSerializable $_deserialize_body(GlobalRef gr, X10JavaDeserializer deserializer) throws IOException {
        x10.rtt.Type<?> T = (Type<?>) deserializer.readRef();
        Place home = (Place) deserializer.readRef();
        long id = deserializer.readLong();
        gr.home = home;
        gr.id = id;
        gr.T = T;
        if (gr.home.id == x10.lang.Runtime.home().id) {
            GlobalRefEntry ge = GlobalRef.id2Object.get(id);
            if (ge == null)
                throw new IllegalStateException("No GlobalRefEntry for id=" + id);
            Object obj = ge.get();
            if (obj == null)
                throw new IllegalStateException("No GlobalRef'ed object for id=" + id);
            gr.t = decodeNull(obj);
        } else {
            gr.t = null;
        }
        return gr;
    }

    public static class LocalEval extends x10.core.Ref {

	private static final long serialVersionUID = 1L;
    private static final int _serialization_id = x10.x10rt.DeserializationDispatcher.addDispatcher(LocalEval.class.getName());
	public static final x10.rtt.RuntimeType<LocalEval> $RTT = new x10.rtt.NamedType<LocalEval>("x10.lang.GlobalRef.LocalEval", LocalEval.class, new x10.rtt.Type[] {x10.rtt.Types.OBJECT});
	public x10.rtt.RuntimeType<?> $getRTT() {return $RTT;}
    
	// constructor just for allocation
	public LocalEval(final java.lang.System[] $dummy) { super($dummy);}
	public LocalEval $init() {return this;}
	// creation method for java code
	public static LocalEval $make(){return new LocalEval((java.lang.System[])null).$init();}
        

	public static <$T, $U> $U evalAtHome(x10.rtt.Type $T, x10.rtt.Type $U, x10.core.GlobalRef<$T> ref, x10.core.fun.Fun_0_1<$T,$U> eval) {
	    if (x10.rtt.Equality.equalsequals(x10.lang.Runtime.home(),ref.home)) {
		return eval.$apply(ref.$apply$G(),$T);
	    } else {
		return x10.lang.Runtime.<$U>evalAt_1_$_x10$lang$Runtime_T_$$G($U, ref.home, new $Closure$Eval<$T, $U>($T, $U, ref, eval, (java.lang.Class<?>) null));
	    }
	}
        
        
	public static <$T> $T getLocalOrCopy(x10.rtt.Type $T, x10.core.GlobalRef<$T> ref) {
	    if (x10.rtt.Equality.equalsequals(x10.lang.Runtime.home(),ref.home)) {
		return ref.$apply$G();
	    } else {
		return x10.lang.Runtime.<$T>evalAt_1_$_x10$lang$Runtime_T_$$G($T, ref.home, new $Closure$Apply<$T>($T, ref, (java.lang.Class<?>) null));
	    }
	}

    public void $_serialize(X10JavaSerializer serializer) throws IOException {
	}

	public static X10JavaSerializable $_deserializer(X10JavaDeserializer deserializer) throws java.io.IOException {
       LocalEval obj = new LocalEval((System []) null);
        deserializer.record_reference(obj);
        return $_deserialize_body(obj, deserializer);
	}

	public int $_get_serialization_id() {
		return _serialization_id;
	}

    public static X10JavaSerializable $_deserialize_body(LocalEval obj, X10JavaDeserializer deserializer) throws IOException {
           return obj;
    }


	public static class $Closure$Eval<$T, $U> extends x10.core.Ref implements x10.core.fun.Fun_0_0 {
	    private static final long serialVersionUID = 1L;
        private static final int _serialization_id = x10.x10rt.DeserializationDispatcher.addDispatcher($Closure$Eval.class.getName());
	    public static final x10.rtt.RuntimeType<$Closure$Eval> $RTT =
		new x10.rtt.StaticFunType<$Closure$Eval>($Closure$Eval.class, 
							 new x10.rtt.RuntimeType.Variance[] {x10.rtt.RuntimeType.Variance.INVARIANT, x10.rtt.RuntimeType.Variance.INVARIANT},
							 new x10.rtt.Type[] {new x10.rtt.ParameterizedType(x10.core.fun.Fun_0_0.$RTT, x10.rtt.UnresolvedType.PARAM(1)), x10.rtt.Types.OBJECT});
	    public x10.rtt.RuntimeType<?> $getRTT() {return $RTT;}
	    public x10.rtt.Type<?> $getParam(int i) {if (i ==0)return $T;if (i ==1)return $U;return null;}

	    // constructor just for allocation
	    public $Closure$Eval(final java.lang.System[] $dummy) { super($dummy);}
	    public $Closure$Eval(x10.rtt.Type $T, x10.rtt.Type $U, x10.core.GlobalRef<$T> ref, x10.core.fun.Fun_0_1<$T,$U> eval, java.lang.Class<?> $dummy0) {
		this.$T = $T;
		this.$U = $U;
		this.ref = ref;
		this.eval = eval;
	    }
	    // creation method for java code
	    public static <$T, $U> $Closure$Eval $make(x10.rtt.Type $T, x10.rtt.Type $U, x10.core.GlobalRef<$T> ref, x10.core.fun.Fun_0_1<$T,$U> eval, java.lang.Class<?> $dummy0){
		return new $Closure$Eval($T, $U, ref, eval, (java.lang.Class<?>) null);
	    }

	    private x10.rtt.Type $T;
	    private x10.rtt.Type $U;

	    public x10.core.GlobalRef<$T> ref;
	    public x10.core.fun.Fun_0_1<$T,$U> eval;
                
	    public $U $apply$G() {
		return this.eval.$apply(this.ref.$apply$G(),$T);
	    }

        public void $_serialize(X10JavaSerializer serializer) throws IOException {
            serializer.write($T);
            serializer.write($U);
            serializer.write(ref);
            serializer.write(eval);
        }

        public static X10JavaSerializable $_deserializer(X10JavaDeserializer deserializer) throws java.io.IOException {
            $Closure$Eval obj = new $Closure$Eval((System[]) null);
            deserializer.record_reference(obj);
            return $_deserialize_body(obj, deserializer);
        }

        public int $_get_serialization_id() {
            return _serialization_id;
        }

        public static X10JavaSerializable $_deserialize_body($Closure$Eval obj, X10JavaDeserializer deserializer) throws IOException {
            x10.rtt.Type $T = (Type) deserializer.readRef();
            obj.$T = $T;
            x10.rtt.Type $U = (Type) deserializer.readRef();
            obj.$U = $U;
            GlobalRef ref = (GlobalRef) deserializer.readRef();
            obj.ref = ref;
            x10.core.fun.Fun_0_1 eval = ( x10.core.fun.Fun_0_1)deserializer.readRef();
            obj.eval = eval;
            return obj;
        }
	}

            
	public static class $Closure$Apply<$T> extends x10.core.Ref implements x10.core.fun.Fun_0_0 {
	    private static final long serialVersionUID = 1L;
        private static final int _serialization_id = x10.x10rt.DeserializationDispatcher.addDispatcher($Closure$Apply.class.getName());
	    public static final x10.rtt.RuntimeType<$Closure$Apply> $RTT =
		new x10.rtt.StaticFunType<$Closure$Apply>($Closure$Apply.class, 
							  new x10.rtt.RuntimeType.Variance[] {x10.rtt.RuntimeType.Variance.INVARIANT},
							  new x10.rtt.Type[] {new x10.rtt.ParameterizedType(x10.core.fun.Fun_0_0.$RTT, x10.rtt.UnresolvedType.PARAM(0)), x10.rtt.Types.OBJECT});
	    public x10.rtt.RuntimeType<?> $getRTT() {return $RTT;}
	    public x10.rtt.Type<?> $getParam(int i) {if (i ==0)return $T;return null;}

	    // constructor just for allocation
	    public $Closure$Apply(final java.lang.System[] $dummy) { super($dummy);}
	    public $Closure$Apply(x10.rtt.Type $T, x10.core.GlobalRef<$T> ref, java.lang.Class<?> $dummy0) {
		this.$T = $T;
		this.ref = ref;
	    }
	    // creation method for java code
	    public static <$T> $Closure$Apply $make(x10.rtt.Type $T, x10.core.GlobalRef<$T> ref, java.lang.Class<?> $dummy0) {
		return new $Closure$Apply($T, ref,(java.lang.Class<?>) null);
	    }
	
	    private x10.rtt.Type $T;

	    public x10.core.GlobalRef<$T> ref;

	    public $T $apply$G() {
		return this.ref.$apply$G();
	    }

        public void $_serialize(X10JavaSerializer serializer) throws IOException {
            serializer.write($T);
            serializer.write(ref);
        }

        public static X10JavaSerializable $_deserializer(X10JavaDeserializer deserializer) throws java.io.IOException {
            $Closure$Apply obj = new $Closure$Apply((System[]) null);
            deserializer.record_reference(obj);
            return $_deserialize_body(obj, deserializer);
        }

        public int $_get_serialization_id() {
            return _serialization_id;
        }

        public static X10JavaSerializable $_deserialize_body($Closure$Apply obj, X10JavaDeserializer deserializer) throws IOException {
            x10.rtt.Type $T = (Type) deserializer.readRef();
            obj.$T = $T;
            GlobalRef ref = (GlobalRef) deserializer.readRef();
            obj.ref = ref;
            return obj;
        }
	}
    }
}
