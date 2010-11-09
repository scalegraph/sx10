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

import x10.compiler.Native;
import x10.compiler.Pinned;
import x10.compiler.Global;
import x10.compiler.Pragma;
import x10.compiler.StackAllocate;
import x10.compiler.TempNoInline_1;

import x10.util.Random;
import x10.util.Stack;
import x10.util.Box;
/**
 * @author tardieu
 */
@Pinned public final class Runtime {

    // Print methods for debugging

    @Native("java", "java.lang.System.err.println(#1)")
    @Native("c++", "x10aux::system_utils::println(x10aux::to_string(#1)->c_str())")
    public native static def println(any:Any) : void;

    @Native("java", "java.lang.System.err.println()")
    @Native("c++", "x10aux::system_utils::println(\"\")")
    public native static def println() : void;

    @Native("java", "java.lang.System.err.printf(#4, #5)")
    @Native("c++", "x10aux::system_utils::printf(#4, #5)")
    public native static def printf[T](fmt:String, t:T) : void;

    // Configuration options

    @Native("java", "x10.runtime.impl.java.Runtime.NO_STEALS")
    @Native("c++", "x10aux::no_steals()")
    public static NO_STEALS = false;

    @Native("java", "x10.runtime.impl.java.Runtime.INIT_THREADS")
    @Native("c++", "x10aux::num_threads()")
    public static INIT_THREADS = 1;

    @Native("java", "x10.runtime.impl.java.Runtime.STATIC_THREADS")
    @Native("c++", "x10aux::static_threads()")
    public static STATIC_THREADS = false;

    // Native runtime interface

    /**
     * Run body at place(id).
     * May be implemented synchronously or asynchronously.
     * Body cannot spawn activities, use clocks, or raise exceptions.
     */
    @Native("java", "x10.runtime.impl.java.Runtime.runClosureAt(#1, #2)")
    @Native("c++", "x10aux::run_closure_at(#1, #2)")
    static def runClosureAt(id:Int, body:()=>void):void { body(); }

    @Native("java", "x10.runtime.impl.java.Runtime.runClosureCopyAt(#1, #2)")
    @Native("c++", "x10aux::run_closure_at(#1, #2)")
    static def runClosureCopyAt(id:Int, body:()=>void):void { body(); }

    @Native("c++", "x10aux::run_async_at(#1, #2, #3)")
    static def runAsyncAt(id:Int, body:()=>void, finishState:FinishState):void {
        val closure = ()=> @x10.compiler.RemoteInvocation {execute(body, finishState);};
        runClosureCopyAt(id, closure);
        dealloc(closure);
    }

    /**
     * Deep copy.
     */
    @Native("java", "x10.runtime.impl.java.Runtime.<#1>deepCopy(#4)")
    @Native("c++", "x10aux::deep_copy<#1 >(#4)")
    static native def deepCopy[T](o:T):T;

    /**
     * Java: run body synchronously at place(id) in the same node as the current place.
     * C++: run body. (no need for a native implementation)
     */
    @Native("java", "x10.runtime.impl.java.Runtime.runAtLocal(#1, #2)")
    @TempNoInline_1
    static def runAtLocal(id:Int, body:()=>void):void { body(); }

    /**
     * Return true if place(id) is in the current node.
     */
    static def isLocal(id:Int):Boolean = id == here.id;

    /**
     * Process one incoming message if any (non-blocking).
     */
    @Native("c++", "x10aux::event_probe()")
    @Native("java","x10.runtime.impl.java.Runtime.eventProbe()")
    static def event_probe():void {}

    /**
     * Register x10rt handlers.
     */
    @Native("c++", "x10aux::DeserializationDispatcher::registerHandlers()")
    static def registerHandlers() {}

    // Accessors for native performance counters

    @Native("c++","x10aux::asyncs_sent")
    static def getAsyncsSent() = 0L;

    @Native("c++","x10aux::asyncs_sent = #1")
    static def setAsyncsSent(v:Long) { }

    @Native("c++","x10aux::asyncs_received")
    static def getAsyncsReceived() = 0L;

    @Native("c++","x10aux::asyncs_received = #1")
    static def setAsyncsReceived(v:Long) { }

    @Native("c++","x10aux::serialized_bytes")
    static def getSerializedBytes() = 0L;

    @Native("c++","x10aux::serialized_bytes = #1")
    static def setSerializedBytes(v:Long) { }

    @Native("c++","x10aux::deserialized_bytes")
    static def getDeserializedBytes() = 0L;

    @Native("c++","x10aux::deserialized_bytes = #1")
    static def setDeserializedBytes(v:Long) { }

    // Methods for explicit memory management

    @Native("c++", "x10::lang::Object::dealloc_object((x10::lang::Object*)#1.operator->())")
    public static def deallocObject (o:Object) { }

    @Native("c++", "x10aux::dealloc(#4.operator->())")
    public static def dealloc[T] (o:()=>T) { }

    @Native("c++", "x10aux::dealloc(#1.operator->())")
    public static def dealloc (o:()=>void) { }


    /**
     * A mortal object is garbage collected when there are no remaining local refs even if remote refs might still exist
     */
    public interface Mortal { }

    @Pinned static class Semaphore {
        private val lock = new Lock();

        private val threads = new Stack[Thread]();

        private var permits:Int;

        def this(n:Int) {
            permits = n;
        }

        private static def min(i:Int, j:Int):Int = i<j ? i : j;

        def release(n:Int):void {
            lock.lock();
            permits += n;
            val m = min(permits, min(n, threads.size()));
            for (var i:Int = 0; i<m; i++) {
                threads.pop().unpark();
            }
            lock.unlock();
        }

        def release():void {
            release(1);
        }

        def reduce(n:Int):void {
            lock.lock();
            permits -= n;
            lock.unlock();
        }

        def acquire():void {
            lock.lock();
            val thread = Thread.currentThread();
            while (permits <= 0) {
                threads.push(thread);
                while (threads.contains(thread)) {
                    lock.unlock();
                    Worker.park();
                    lock.lock();
                }
            }
            --permits;
            lock.unlock();
        }

        def available():Int = permits;
    }

    @Pinned final static class Worker extends Thread {
        // bound on loop iterations to help j9 jit
        private static BOUND = 100;

        // activity (about to be) executed by this worker
        private var activity:Activity = null;

        // pending activities
        private val queue = new Deque();

        // random number generator for this worker
        private val random:Random;

        //Worker Id for CollectingFinish
        val workerId:Int;

        def this(main:()=>Void) {
            super(main, "thread-main");
            workerId = 0;
            random = new Random(0);
        }

        def this(workerId:Int) {
            super(()=>runtime().pool.workers(workerId)(), "thread-" + workerId);
            this.workerId = workerId;
            random = new Random(workerId + (workerId << 8) + (workerId << 16) + (workerId << 24));
        }

        // return size of the deque
        def size():Int = queue.size();

        // return activity executed by this worker
        def activity() = activity;

        // poll activity from the bottom of the deque
        private def poll() = queue.poll() as Activity;

        // steal activity from the top of the deque
        def steal() = queue.steal() as Activity;

        // push activity at the bottom of the deque
        def push(activity:Activity):void = queue.push(activity);

        // run pending activities
        def apply():void {
            val latch = runtime().pool.latch;
            try {
                while (loop(latch, true));
            } catch (t:Throwable) {
                println("Uncaught exception in worker thread");
                t.printStackTrace();
            } finally {
                runtime().pool.release();
            }
        }

        // run activities while waiting on finish
        def join(latch:SimpleLatch):void {
            val tmp = activity; // save current activity
            while (loop(latch, false));
            activity = tmp; // restore current activity
        }

        // inner loop to help j9 jit
        @TempNoInline_1
        private def loop(latch:SimpleLatch, block:Boolean):Boolean {
            @TempNoInline_1
            for (var i:Int = 0; i < BOUND; i++) {
                if (latch()) return false;
                activity = poll();
                if (activity == null) {
                    activity = runtime().pool.scan(random, latch, block);
                    if (activity == null) return false;
                }
                runAtLocal(activity.home, activity.run.());
            }
            return true;
        }

        @TempNoInline_1
        def probe():void {
            @TempNoInline_1
            // process all queued activities
            val tmp = activity; // save current activity
            event_probe();
            while (true) {
                activity = poll();
                if (activity == null) {
                    activity = tmp; // restore current activity
                    return;
                }
                runAtLocal(activity.home, activity.run.());
            }
        }

        // park current worker
        public static def park() {
            if (!STATIC_THREADS) {
                Thread.park();
            } else {
                probe();
            }
        }

        // unpark worker
        public def unpark() {
            if (!STATIC_THREADS) {
                super.unpark();
            }
        }
    }

    @Pinned static class Pool {
        val latch:SimpleLatch;

        private var size:Int; // the number of workers in the pool

        private var spares:Int = 0; // the number of spare workers in the pool

        private var dead:Int = 0;

        private val lock = new Lock();

        private val semaphore = new Semaphore(0);

        // the workers in the pool
        private val workers:Array[Worker]{rail};

        def this(size:Int) {
            this.size = size;
            this.latch = new SimpleLatch();
            val workers = new Array[Worker](MAX_WORKERS);

            // main worker
            workers(0) = worker();

            // other workers
            for (var i:Int = 1; i<size; i++) {
                workers(i) = new Worker(i);
            }
            this.workers = workers;
        }

        def apply():void {
            val s = size;
            for (var i:Int = 1; i<s; i++) {
                workers(i).start();
            }
            workers(0)();
            while (size > dead) Worker.park();
        }

        // notify the pool a worker is about to execute a blocking operation
        def increase():void {
            lock.lock();
            if (spares > 0) {
                // if a spare is available increase parallelism
                spares--;
                lock.unlock();
                semaphore.release();
            } else {
                // allocate and start a new worker
                val i = size++;
                lock.unlock();
                assert (i < MAX_WORKERS);
                if (i >= MAX_WORKERS) {
                    println("TOO MANY THREADS... ABORTING");
                    System.exit(1);
                }
                val worker = new Worker(i);
                workers(i) = worker;
                worker.start();
            }
        }

        // notify the pool a worker resumed execution after a blocking operation
        def decrease(n:Int):void {
            // increase number or spares
            lock.lock();
            spares += n;
            lock.unlock();
            // reduce parallelism
            semaphore.reduce(n);
        }

        // release permit (called by worker upon termination)
        def release() {
            semaphore.release();
            lock.lock();
            dead++;
            if (size == dead) workers(0).unpark();
            lock.unlock();
        }

        // scan workers for activity to steal
        def scan(random:Random, latch:SimpleLatch, block:Boolean):Activity {
            var activity:Activity = null;
            var next:Int = random.nextInt(size);
            for (;;) {
                event_probe();
                if (null != workers(next)) { // avoid race with increase method
                    activity = workers(next).steal();
                }
                if (null != activity || latch()) return activity;
                if (semaphore.available() < 0) {
                    if (block) {
                        semaphore.release();
                        semaphore.acquire();
                    } else {
                        return activity;
                    }
                }
                if (++next == size) next = 0;
            }
        }

        def size() = size;
    }

    // static fields

    static PRINT_STATS = false;

    // runtime instance associated with each place
    public static runtime = PlaceLocalHandle[Runtime]();

    // instance fields

    // per process members
    transient val pool:Pool;

    // per place members
    private transient val monitor:Monitor;
    public transient val finishStates:FinishState.FinishStates;

    // constructor

    private def this(pool:Pool):Runtime {
        this.pool = pool;
        this.monitor = new Monitor();
        this.finishStates = new FinishState.FinishStates();
    }

    /**
     * Return the current worker
     */
    static def worker():Worker = Thread.currentThread() as Worker;

    /**
     * Return the current worker id
     */
    public static def workerId():Int = worker().workerId;

    /**
     * An upper bound on the number of workers
     */
    public static MAX_WORKERS = 1000;

    /**
     * Return the number of workers currently in the pool
     * (can increase, cannot decrease)
     */
    public static def poolSize():Int = runtime().pool.size();

    /**
     * Return the current activity
     */
    static def activity():Activity = worker().activity();

    /**
     * Return the current place
     */
    @Native("c++", "x10::lang::Place_methods::_make(x10aux::here)")
    public static def here():Place = Thread.currentThread().home();

    /**
     * Return the id of the current place
     */
    @Native("c++", "x10aux::here")
    public static def hereInt():int = here().id;

    /**
     * The amount of unscheduled activities currently available to this worker thread.
     * Intended for use in heuristics that control async spawning
     * based on the current amount of surplus work.
     */
    public static def surplusActivityCount():int = worker().size();

    /**
     * Run main activity in a finish
     * @param init Static initializers
     * @param body Main activity
     */
    public static def start(init:()=>void, body:()=>void):void {
        // initialize thread pool for the current process
        val pool = new Pool(INIT_THREADS);

        try {
            // initialize runtime
            for (var i:Int=0; i<Place.MAX_PLACES; i++) {
                if (isLocal(i)) {
                    // we need to instantiate a runtime for each place hosted by the current process
                    // all these runtimes share the same thread pool
                    runAtLocal(i, ()=>runtime.set(new Runtime(pool)));
                }
            }

            // initialize x10rt
            registerHandlers();

            if (hereInt() == 0) {
                val rootFinish = new FinishState.Finish(runtime().pool.latch);
                // in place 0 schedule the execution of the static initializers fby main activity
                execute(new Activity(()=>{finish init(); body();}, rootFinish, true));

                // wait for thread pool to die
                // (happens when main activity terminates)
                pool();

                // root finish has terminated, kill remote processes if any
                if (!isLocal(Place.MAX_PLACES - 1)) {
                    for (var i:Int=1; i<Place.MAX_PLACES; i++) {
                        runClosureAt(i, ()=> @x10.compiler.RemoteInvocation {runtime().pool.latch.release();});
                    }
                }

                // we need to call waitForFinish here to see the exceptions thrown by main if any
                rootFinish.waitForFinish(false);
            } else {
                // wait for thread pool to die
                // (happens when a kill signal is received from place 0)
                pool();
            }
        } finally {
            if (PRINT_STATS) {
                println("ASYNC SENT AT PLACE " + here.id +" = " + getAsyncsSent());
                println("ASYNC RECV AT PLACE " + here.id +" = " + getAsyncsReceived());
            }
        }
    }

    // async at, async, at statement, and at expression implementation
    // at is implemented using async at
    // async at and at must make a copy of the closure parameter (local or remote)
    // async at and at should dealloc the closure parameter
    // async must not copy or dealloc the closure parameter
    
    /**
     * Run async at
     */
    public static def runAsync(place:Place, clocks:Array[Clock]{rail}, body:()=>void):void {
        // Do this before anything else
        val a = activity();
        a.ensureNotInAtomic();
        
        val state = a.finishState();
        val clockPhases = a.clockPhases().make(clocks);
        state.notifySubActivitySpawn(place);
        if (place.id == hereInt()) {
            execute(new Activity(deepCopy(body), state, clockPhases));
        } else {
            val closure = ()=> @x10.compiler.RemoteInvocation { execute(new Activity(body, state, clockPhases)); };
            runClosureCopyAt(place.id, closure);
            dealloc(closure);
        }
        dealloc(body);
    }

    public static def runAsync(place:Place, body:()=>void):void {
        // Do this before anything else
        val a = activity();
        a.ensureNotInAtomic();
        
        val state = a.finishState();
        state.notifySubActivitySpawn(place);
        val ok = a.safe();
        if (place.id == hereInt()) {
            execute(new Activity(deepCopy(body), state, ok));
        } else {
            if (ok) {
                runAsyncAt(place.id, body, state); // optimized case
            } else {
                var closure:()=>void;
                closure = ()=> @x10.compiler.RemoteInvocation { execute(new Activity(body, state, false)); };
                runClosureCopyAt(place.id, closure);
                dealloc(closure);
            }
        }
        dealloc(body);
    }

    /**
     * Run async
     */
    public static def runAsync(clocks:Array[Clock]{rail}, body:()=>void):void {
        // Do this before anything else
        val a = activity();
        a.ensureNotInAtomic();
        
        val state = a.finishState();
        val clockPhases = a.clockPhases().make(clocks);
        state.notifySubActivitySpawn(here);
        execute(new Activity(body, state, clockPhases));
    }

    public static def runAsync(body:()=>void):void {
        // Do this before anything else
        val a = activity();
        a.ensureNotInAtomic();
        
        val state = a.finishState();
        state.notifySubActivitySpawn(here);
        execute(new Activity(body, state, a.safe()));
    }

    /**
     * Run @Uncounted async at
     */
    public static def runUncountedAsync(place:Place, body:()=>void):void {
        // Do this before anything else
        val a = activity();
        a.ensureNotInAtomic();
        
        val ok = a.safe();
        if (place.id == hereInt()) {
            execute(new Activity(deepCopy(body), new FinishState.UncountedFinish(), ok));
        } else {
            val closure = ()=> @x10.compiler.RemoteInvocation { execute(new Activity(body, new FinishState.UncountedFinish(), ok)); };
            runClosureCopyAt(place.id, closure);
            dealloc(closure);
        }
        dealloc(body);
    }

    /**
     * Run @Uncounted async
     */
    public static def runUncountedAsync(body:()=>void):void {
        // Do this before anything else
        val a = activity();
        a.ensureNotInAtomic();
        
        execute(new Activity(body, new FinishState.UncountedFinish(), a.safe()));
    }

    /**
     * a latch with a place for an exception
     */
    static class RemoteControl extends SimpleLatch implements Mortal {
        public def this() { super(); }
        private def this(Any) {
            throw new UnsupportedOperationException("Cannot deserialize "+typeName());
        }
        var e:Throwable = null;
    }

    /**
     * Run at statement
     */
    public static def runAt(place:Place, body:()=>void):void {
        Runtime.ensureNotInAtomic();
        @StackAllocate val me = @StackAllocate new RemoteControl();
        val box = GlobalRef(me);
        async at(place) {
            try {
                body();
                async at(box.home) {
                    val me2 = box();
                    me2.release();
                }
            } catch (e:Throwable) {
                async at(box.home) {
                    val me2 = box();
                    me2.e = e;
                    me2.release();
                }
            }
        }
        if (!NO_STEALS && activity().safe()) worker().join(me);
        me.await();
        dealloc(body);
        if (null != me.e) {
            if (me.e instanceof Error)
                throw me.e as Error;
            if (me.e instanceof RuntimeException)
                throw me.e as RuntimeException;
        }
    }

    /**
     * a latch with a place for an exception and return value
     */
    static class Remote[T] extends RemoteControl {
        public def this() { super(); }
        private def this(Any) {
            throw new UnsupportedOperationException("Cannot deserialize "+typeName());
        }
        var t:Box[T] = null;
    }

    /**
     * Eval at expression
     */
    public static def evalAt[T](place:Place, eval:()=>T):T {
        @StackAllocate val me = @StackAllocate new Remote[T]();
        val box = GlobalRef(me);
        async at(place) {
            try {
                val result = eval();
                async at(box.home) {
                    val me2 = box();
                    me2.t = result;
                    me2.release();
                }
            } catch (e:Throwable) {
                async at(box.home) {
                    val me2 = box();
                    me2.e = e;
                    me2.release();
                }
            }
        }
        if (!NO_STEALS && activity().safe()) worker().join(me);
        me.await();
        dealloc(eval);
        if (null != me.e) {
            if (me.e instanceof Error)
                throw me.e as Error;
            if (me.e instanceof RuntimeException)
                throw me.e as RuntimeException;
        }
        return me.t.value;
    }

    // initialization of static fields in c++ backend

    public static def StaticInitBroadcastDispatcherLock() {
        runtime().monitor.lock();
    }

    public static def StaticInitBroadcastDispatcherAwait() {
        runtime().monitor.await();
    }

    public static def StaticInitBroadcastDispatcherUnlock() {
        runtime().monitor.unlock();
    }

    public static def StaticInitBroadcastDispatcherNotify() {
        runtime().monitor.release();
    }

    // atomic and when

    public static def enterAtomic() {
        runtime().monitor.lock();
        val a = activity();
        if (a != null)
           a.pushAtomic();
    }

    public static def ensureNotInAtomic() {
        val a = activity();
        if (a != null)
           a.ensureNotInAtomic();
    }

    public static def exitAtomic() {
        val a = activity();
        if (a != null)
           a.popAtomic();
        runtime().monitor.release();
    }

    public static def awaitAtomic():void {
        runtime().monitor.await();
    }

    // clocks

    /**
     * Next statement = next on all clocks in parallel.
     */
    @Native("cuda", "__syncthreads()")
    public static def next():void {
        ensureNotInAtomic();
        activity().clockPhases().next();
    }

    /**
     * Resume statement = resume on all clocks in parallel.
     */
    public static def resume():void = activity().clockPhases().resume();

    // finish

    /**
     * Start executing current activity synchronously
     * (i.e. within a finish statement).
     */
    public static def startFinish():FinishState {
        return activity().swapFinish(new FinishState.Finish());
    }

    public static def startFinish(pragma:Int):FinishState {
        val f:FinishState;
        switch (pragma) {
        case Pragma.FINISH_ASYNC:
            f = new FinishState.FinishAsync(); break;
        case Pragma.FINISH_HERE:
            f = new FinishState.FinishHere(); break;
        case Pragma.FINISH_SPMD:
            f = new FinishState.FinishSPMD(); break;
        case Pragma.FINISH_LOCAL:
            f = new FinishState.LocalFinish(); break;
        default: 
            f = new FinishState.Finish();
        }
        return activity().swapFinish(f);
    }

    public static def startLocalFinish():FinishState {
        return activity().swapFinish(new FinishState.LocalFinish());
    }

    public static def startSimpleFinish():FinishState {
        return activity().swapFinish(new FinishState.Finish());
    }

    /**
     * Suspend until all activities spawned during this finish
     * operation have terminated. Throw an exception if any
     * async terminated abruptly. Otherwise continue normally.
     * Should only be called by the thread executing the current activity.
     */
    public static def stopFinish(f:FinishState):void {
        val a = activity();
        val finishState = a.swapFinish(f);
        finishState.waitForFinish(a.safe());
    }

    /**
     * Push the exception thrown while executing s in a finish s,
     * onto the finish state.
     */
    public static def pushException(t:Throwable):void  {
        activity().finishState().pushException(t);
    }
    public static def startCollectingFinish[T](r:Reducible[T]) {
        return activity().swapFinish(new FinishState.CollectingFinish[T](r));
    }

    public static def offer[T](t:T) {
        val state = activity().finishState();
//      Console.OUT.println("Place(" + here.id + ") Runtime.offer: received " + t);
        (state as FinishState.CollectingFinish[T]).accept(t,workerId());
    }

    public static def stopCollectingFinish[T](f:FinishState):T {
        val state = activity().swapFinish(f);
        return (state as FinishState.CollectingFinish[T]).waitForFinishExpr(true);
    }

    // submit an activity to the pool
    static def execute(activity:Activity):void {
        worker().push(activity);
    }

    // submit 
    public static def execute(body:()=>Void, finishState:FinishState):void {
        execute(new Activity(body, finishState));
    }

    public static def probe() {
        worker().probe();
    }

    // notify the pool a worker is about to execute a blocking operation
    static def increaseParallelism():void {
        if (!STATIC_THREADS) {
            runtime().pool.increase();
        }
    }

    // notify the pool a worker resumed execution after a blocking operation
    static def decreaseParallelism(n:Int) {
        if (!STATIC_THREADS) {
            runtime().pool.decrease(n);
        }
    }

    public static def spin() {
        probe();
    }
}

// vim:shiftwidth=4:tabstop=4:expandtab
