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

import x10.compiler.RemoteInvocation;

import x10.util.HashMap;
import x10.util.Pair;
import x10.util.Stack;
import x10.util.concurrent.atomic.AtomicInteger;

interface FinishState {
    def notifySubActivitySpawn(place:Place):void;
    def notifyActivityCreation():void;
    def notifyActivityTermination():void;
    def pushException(t:Throwable):void;
    def waitForFinish(safe:Boolean):void;

    static class LocalFinish implements FinishState {
        private var count:Int = 1;
        public def notifySubActivitySpawn(place:Place) {
            assert place.id == Runtime.hereInt();
            atomic count++;
        }
        public def notifyActivityCreation() {}
        public def notifyActivityTermination() {
            atomic count--;
        }
        public def pushException(t:Throwable) {} //TODO
        public def waitForFinish(safe:Boolean) {
            when (count == 0);
        }
    }

    static class UncountedFinish implements FinishState {
        public def notifySubActivitySpawn(place:Place) {}
        public def notifyActivityCreation() {}
        public def notifyActivityTermination() {}
        public def pushException(t:Throwable) {
            Runtime.println("Uncaught exception in uncounted activity");
            t.printStackTrace();
        }
        public def waitForFinish(safe:Boolean) { assert false; }
    }

    static class FinishStates implements (GlobalRef[FinishState],()=>FinishState)=>FinishState {
        // maintain a mapping from finish refs to local finish objects
        private val map = new HashMap[GlobalRef[FinishState],FinishState]();
        private val lock = new Lock();

        public def apply(root:GlobalRef[FinishState], factory:()=>FinishState):FinishState{
            lock.lock();
            var f:FinishState = map.getOrElse(root, null);
            if (null != f) {
                lock.unlock();
                return f;
            }
            f = factory();
            map.put(root, f);
            lock.unlock();
            return f;
        }

        public def remove(root:GlobalRef[FinishState]) {
            lock.lock();
            map.remove(root);
            lock.unlock();
        }
    }

    static class Finish(root:GlobalRef[FinishState]) implements FinishState,x10.io.CustomSerialization {
        protected transient var me:FinishState; // local finish object
        protected def this(root:RootFinish) {
            property(root.ref());
            me = root;
        }
        def this(latch:Latch) {
            this(new RootFinish(latch));
        }
        def this() {
            this(new Latch());
        }
        protected def this(root:GlobalRef[FinishState]) {
            property(root);
            me = null;
        }
        private def this(any:Any) { // deserialization constructor
            val root = any as GlobalRef[FinishState];
            property(root);
            if (root.home.id == Runtime.hereInt()) {
                me = (root as GlobalRef[FinishState]{home==here})();
            } else {
                me = Runtime.runtime().finishStates(root, ()=>new RemoteFinish(root));
            }
        }
        public def serialize():Any = root;
        public def notifySubActivitySpawn(place:Place) { me.notifySubActivitySpawn(place); }
        public def notifyActivityCreation() { me.notifyActivityCreation(); }
        public def notifyActivityTermination() { me.notifyActivityTermination(); }
        public def pushException(t:Throwable) { me.pushException(t); }
        public def waitForFinish(safe:Boolean) { me.waitForFinish(safe); }
    }

    static class RootFinish implements FinishState {
        private val root = GlobalRef[FinishState](this);
        protected val latch:Latch;
        protected var exceptions:Stack[Throwable]; // lazily initialized
        protected val counts:Rail[Int] = Rail.make[Int](Place.MAX_PLACES, 0);
        protected val seen:Rail[Boolean] = Rail.make[Boolean](Place.MAX_PLACES, false);
        protected val lock:Lock = new Lock();
        def this(latch:Latch) {
            this.latch = latch;
            counts(Runtime.hereInt()) = 1;
        }
        def ref() = root;
        public def notifySubActivitySpawn(place:Place):void {
            lock.lock();
            counts(place.parent().id)++;
            lock.unlock();
        }
        public def notifyActivityCreation():void {}
        public def notifyActivityTermination():void {
            lock.lock();
            counts(Runtime.hereInt())--;
            for(var i:Int=0; i<Place.MAX_PLACES; i++) {
                if (counts(i) != 0) {
                    lock.unlock();
                    return;
                }
            }
            latch.release();
            lock.unlock();
        }
        public def process(t:Throwable):void {
            if (null == exceptions) exceptions = new Stack[Throwable]();
            exceptions.push(t);
        }
        public def pushException(t:Throwable):void {
            lock.lock();
            process(t);
            lock.unlock();
        }
        public def waitForFinish(safe:Boolean):void {
            if (!Runtime.NO_STEALS && safe) Runtime.worker().join(latch);
            latch.await();
            val root = this.root;
            val closure = ()=>@RemoteInvocation { Runtime.runtime().finishStates.remove(root); };
            seen(Runtime.hereInt()) = false;
            for(var i:Int=0; i<Place.MAX_PLACES; i++) {
                if (seen(i)) Runtime.runClosureAt(i, closure);
            }
            Runtime.dealloc(closure);
            val t = MultipleExceptions.make(exceptions);
            if (null != t) throw t;
        }

        protected def process(rail:Rail[Int]) {
            var b:Boolean = true;
            for(var i:Int=0; i<Place.MAX_PLACES; i++) {
                counts(i) += rail(i);
                seen(i) |= counts(i) != 0;
                if (counts(i) != 0) b = false;
            }
            if (b) latch.release();
        }

        def notify(rail:Rail[Int]):void {
            lock.lock();
            process(rail);
            lock.unlock();
        }

        protected def process(rail:Rail[Pair[Int,Int]]):void {
            for(var i:Int=0; i<rail.length; i++) {
                counts(rail(i).first) += rail(i).second;
                seen(rail(i).first) = true;
            }
            for(var i:Int=0; i<Place.MAX_PLACES; i++) {
                if (counts(i) != 0) {
                    return;
                }
            }
            latch.release();
        }

        def notify(rail:Rail[Pair[Int,Int]]):void {
            lock.lock();
            process(rail);
            lock.unlock();
        }

        def notify(rail:Rail[Int], t:Throwable):void {
            lock.lock();
            process(t);
            process(rail);
            lock.unlock();
        }

        def notify(rail:Rail[Pair[Int,Int]], t:Throwable):void {
            lock.lock();
            process(t);
            process(rail);
            lock.unlock();
        }
    }

    static class RemoteFinish implements FinishState {
        protected var exceptions:Stack[Throwable];
        protected val lock = new Lock();
        protected val counts = Rail.make[Int](Place.MAX_PLACES, 0);
        protected val places = Rail.make[Int](Place.MAX_PLACES, Runtime.hereInt());
        protected var length:Int = 1;
        protected var count:AtomicInteger = new AtomicInteger(0);
        protected var root:GlobalRef[FinishState];
        def this(root:GlobalRef[FinishState]) {
            this.root = root;
        }
        public def notifyActivityCreation():void {
            count.getAndIncrement();
        }
        public def notifySubActivitySpawn(place:Place):void {
            lock.lock();
            if (counts(place.id)++ == 0 && Runtime.hereInt() != place.id) {
                places(length++) = place.id;
            }
            lock.unlock();
        }
        public def pushException(t:Throwable):void {
            lock.lock();
            if (null == exceptions) exceptions = new Stack[Throwable]();
            exceptions.push(t);
            lock.unlock();
        }
        public def waitForFinish(safe:Boolean) { assert false; }
        public def notifyActivityTermination():void {
            lock.lock();
            counts(Runtime.hereInt())--;
            if (count.decrementAndGet() > 0) {
                lock.unlock();
                return;
            }
            val t = MultipleExceptions.make(exceptions);
            val root = this.root;
            val closure:()=>void;
            if (2*length > Place.MAX_PLACES) {
                val message = Rail.make[Int](counts.length, 0, counts);
                if (null != t) {
                    closure = ()=>@RemoteInvocation { deref(root).notify(message, t); };
                } else {
                    closure = ()=>@RemoteInvocation { deref(root).notify(message); };
                }
            } else {
                val message = Rail.make[Pair[Int,Int]](length, (i:Int)=>Pair[Int,Int](places(i), counts(places(i))));
                if (null != t) {
                    closure = ()=>@RemoteInvocation { deref(root).notify(message, t); };
                } else {
                    closure = ()=>@RemoteInvocation { deref(root).notify(message); };
                }
            }
            counts.reset(0);
            length = 1;
            exceptions = null;
            lock.unlock();
            Runtime.runClosureAt(root.home.id, closure);
            Runtime.dealloc(closure);
        }
        static def deref(root:GlobalRef[FinishState]) = (root as GlobalRef[FinishState]{home==here})() as RootFinish;
    }

    static class StatefulReducer[T] {
        val reducer:Reducible[T];
        var result:T;
        val MAX = 1000;
        var resultRail:Rail[T];
        var workerFlag:Rail[Boolean] = Rail.make[Boolean](MAX, false);
        def this(r:Reducible[T]) {
            reducer = r;
            val zero = reducer.zero();
            result = zero;
            resultRail = Rail.make[T](MAX, zero);
        }
        def accept(t:T) {
            result = reducer(result, t);
        }
        def accept(t:T, id:Int) {
            if ((id >= 0) && (id < MAX)) {
                resultRail(id) = reducer(resultRail(id), t);
                workerFlag(id) = true;
            }
        }
        def placeMerge() {
            for(var i:Int=0; i<MAX; i++) {
                if (workerFlag(i)) {
                    result = reducer(result, resultRail(i));
                    resultRail(i) = reducer.zero();
                }
            }
        }
        def result() = result;
        def reset() {
            result = reducer.zero();
        }
    }

    static interface CollectingFinishState[T] extends FinishState {
        def accept(t:T, id:Int):void;
    }

    static class CollectingFinish[T] extends Finish implements CollectingFinishState[T],x10.io.CustomSerialization {
        val reducer:Reducible[T];
        def this(reducer:Reducible[T]) {
            super(new RootCollectingFinish(reducer));
            this.reducer = reducer;
        }
        private def this(any:Any) { // deserialization constructor
            super((any as Pair[GlobalRef[FinishState],Reducible[T]]).first);
            reducer = (any as Pair[GlobalRef[FinishState],Reducible[T]]).second;
            if (root.home.id == Runtime.hereInt()) {
                me = (root as GlobalRef[FinishState]{home==here})();
            } else {
                me = Runtime.runtime().finishStates(root, ()=>new RemoteCollectingFinish[T](root, reducer));
            }
        }
        public def serialize():Any = Pair[GlobalRef[FinishState],Reducible[T]](root, reducer);
        public def accept(t:T, id:Int) { (me as CollectingFinishState[T]).accept(t, id); }
        public def waitForFinishExpr(safe:Boolean) = (me as RootCollectingFinish[T]).waitForFinishExpr(safe);
    }

    static class RootCollectingFinish[T] extends RootFinish implements CollectingFinishState[T] {
        val sr:StatefulReducer[T];
        def this(reducer:Reducible[T]) {
           super(new Latch());
           sr = new StatefulReducer[T](reducer);
        }
        public def accept(t:T, id:Int) {
           sr.accept(t, id);
        }
        def notifyValue(rail:Rail[Int], v:T):void {
            lock.lock();
            sr.accept(v);
            process(rail);
            lock.unlock();
        }
        def notifyValue(rail:Rail[Pair[Int,Int]], v:T):void {
            lock.lock();
            sr.accept(v);
            process(rail);
            lock.unlock();
        }
        final public def waitForFinishExpr(safe:Boolean):T {
            waitForFinish(safe);
            sr.placeMerge();
            val result = sr.result();
            sr.reset();
            return result;
        }
    }

    static class RemoteCollectingFinish[T] extends RemoteFinish implements CollectingFinishState[T] {
        val sr:StatefulReducer[T];
        def this(root:GlobalRef[FinishState], reducer:Reducible[T]) {
            super(root);
            sr = new StatefulReducer[T](reducer);
        }
        public def accept(t:T, id:Int) {
            sr.accept(t, id);
        }
        public def notifyActivityTermination():void {
            lock.lock();
            counts(Runtime.hereInt())--;
            if (count.decrementAndGet() > 0) {
                lock.unlock();
                return;
            }
            val t = MultipleExceptions.make(exceptions);
            val root = this.root;
            val closure:()=>void;
            sr.placeMerge();
            val result = sr.result();
            sr.reset();
            if (2*length > Place.MAX_PLACES) {
                val message = Rail.make[Int](counts.length, 0, counts);
                if (null != t) {
                    closure = ()=>@RemoteInvocation { deref[T](root).notify(message, t); };
                } else {
                    closure = ()=>@RemoteInvocation { deref[T](root).notifyValue(message, result); };
                }
            } else {
                val message = Rail.make[Pair[Int,Int]](length, (i:Int)=>Pair[Int,Int](places(i), counts(places(i))));
                if (null != t) {
                    closure = ()=>@RemoteInvocation { deref[T](root).notify(message, t); };
                } else {
                    closure = ()=>@RemoteInvocation { deref[T](root).notifyValue(message, result); };
                }
            }
            counts.reset(0);
            length = 1;
            exceptions = null;
            lock.unlock();
            Runtime.runClosureAt(root.home.id, closure);
            Runtime.dealloc(closure);
        }
        static def deref[T](root:GlobalRef[FinishState]) = (root as GlobalRef[FinishState]{home==here})() as RootCollectingFinish[T];
    }
}

// vim:shiftwidth=4:tabstop=4:expandtab
