package x10.compiler.ws;

import x10.compiler.Abort;
import x10.compiler.Header;
import x10.compiler.Ifdef;
import x10.compiler.Inline;
import x10.compiler.NoInline;
import x10.compiler.NoReturn;
import x10.compiler.Uninitialized;

import x10.util.Stack;

abstract public class FinishFrame extends Frame {
    @Uninitialized public var asyncs:Int;
    @Uninitialized transient public var stack:Stack[Throwable];

//    @Ifdef("__CPP__")
    @Uninitialized public var redirect:FinishFrame;

    @Header public def this(up:Frame) {
        super(up);
        this.stack = null;
        @Ifdef("__CPP__") {
            this.redirect = null;
        }
    }

    @Ifdef("__CPP__")
    public def this(Int, o:FinishFrame) {
        super(o.up.realloc());
        this.asyncs = 1;
        this.stack = null;
    }

    @Ifdef("__CPP__")
    public abstract def remap():FinishFrame;

    @Ifdef("__CPP__")
    public def realloc() {
        if (null != redirect) return redirect;
        val tmp = remap();
        tmp.redirect = tmp;
        redirect = tmp;
        return tmp;
    }

    public def wrapBack(worker:Worker, frame:Frame) {
        if (null != frame.throwable) {
            Runtime.atomicMonitor.lock();
            caught(frame.throwable);
            Runtime.atomicMonitor.unlock();
        }
    }

    public def wrapResume(worker:Worker) {
        var n:Int;
        Runtime.atomicMonitor.lock(); n = --asyncs; Runtime.atomicMonitor.unlock();
        if (0 != n) throw Abort.ABORT;
        throwable = MultipleExceptions.make(stack);
    }

    @Inline public final def append(s:Stack[Throwable]) {
        if (null != s) {
            Runtime.atomicMonitor.lock();
            if (null == stack) stack = new Stack[Throwable]();
            while (!s.isEmpty()) stack.push(s.pop());
            Runtime.atomicMonitor.unlock();
        }
    }

    @Inline public final def append(ff:FinishFrame) {
        append(ff.stack);
    }

    @NoInline public final def caught(t:Throwable) {
        if (t == Abort.ABORT) throw t;
        if (null == stack) stack = new Stack[Throwable]();
        stack.push(t);
    }

    @Inline public final def rethrow() {
        if (null != stack) rethrowSlow();
    }

    @NoInline @NoReturn public final def rethrowSlow() {
        throw new MultipleExceptions(stack);
    }

    @Inline public final def check() {
        if (null != stack) {
            while (!stack.isEmpty()) {
                Runtime.pushException(stack.pop());
            }
        }
    }
}
