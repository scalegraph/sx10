package x10.compiler.ws;

import x10.compiler.Header;
import x10.compiler.Inline;

public abstract class AsyncFrame extends Frame {
    // constructor
    @Header public def this(up:Frame) {
        super(up);
    }

    // copy constructor
    public def this(Int, o:AsyncFrame) {
        super(upcast[FinishFrame,Frame](cast[Frame,FinishFrame](o.up).redirect));
    }

    abstract public def move(ff:FinishFrame):void;

    @Inline public final def poll(worker:Worker) {
        if (null == worker.deque.poll()) {
            worker.lock.lock();
            worker.lock.unlock();
            val ff = cast[Frame,FinishFrame](up).redirect;
            if (!eq(up, ff)) move(ff);
            worker.unroll(upcast[FinishFrame,Frame](ff));
            throw Stolen.STOLEN;
        }
        return;
    }
}
