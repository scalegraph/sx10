package x10.compiler.ws;

public final class AtFrame extends Frame {
    val upRef:GlobalRef[Frame];

    public def this(up:Frame, ff:FinishFrame) {
        super(ff);
        upRef = GlobalRef[Frame](up);
    }

    public def remap():AtFrame = this;

    public def wrapResume(worker:Worker) {
        update(upRef, throwable);
        throwable = null;
    }

    @Inline public static def update(upRef:GlobalRef[Frame], throwable:Throwable) {
        val body = ()=> @x10.compiler.RemoteInvocation {
            val up = (upRef as GlobalRef[Frame]{home==here})();
            up.throwable = throwable;
            Runtime.wsFIFO().push(up);
        };
        Runtime.wsRunAsync(upRef.home.id, body);
    }
}
