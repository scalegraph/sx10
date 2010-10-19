package x10.util.synthesizer;

import java.util.ArrayList;
import java.util.List;

import polyglot.ast.Expr;
import polyglot.ast.Stmt;
import polyglot.types.Name;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.Position;
import x10.ast.Closure;
import x10.ast.Tuple;
import x10.ast.X10NodeFactory;
import x10.types.X10Context;

/**
 * Some codes based on desugar
 */
public class AsyncSynth extends AbstractStateSynth implements IStmtSynth {

    Stmt body;
    List<Expr> clocks;
    Expr place;
    
    public AsyncSynth(X10NodeFactory xnf, X10Context xct, Position pos,
                      Stmt body, List<Expr> clocks, Expr place) {
        super(xnf, xct, pos);
        this.body = body;
        this.clocks = clocks;
        this.place = place;
    }
    
    public AsyncSynth(X10NodeFactory xnf, X10Context xct, Position pos,
                      Stmt body, Expr place) {
        super(xnf, xct, pos);
        this.body = body;
        this.clocks = new ArrayList<Expr>();
        this.place = place;
    }
    
    public AsyncSynth(X10NodeFactory xnf, X10Context xct, Position pos,
                      Stmt body, List<Expr> clocks) {
        super(xnf, xct, pos);
        this.body = body;
        this.clocks = clocks;
    }
    
    public AsyncSynth(X10NodeFactory xnf, X10Context xct, Position pos,
                      Stmt body) {
        super(xnf, xct, pos);
        this.body = body;
        this.clocks = new ArrayList<Expr>();
    }
    

    public Stmt genStmt() throws SemanticException {
        
        //different situations
        List<Expr> exprs = new ArrayList<Expr>();
        List<Type> types = new ArrayList<Type>();
        
        
        if(place == null){
            if(clocks.size() > 0){
                Type clockRailType = xts.ValRail(xts.Clock());
                Tuple clockRail = (Tuple) xnf.Tuple(pos, clocks).type(clockRailType);
                exprs.add(clockRail);
                types.add(clockRailType);
            }
        }
        else{ //place != null;
            //process places
            if (xts.isImplicitCastValid(place.type(), xts.Object(), xct)) {
                place = synth.makeFieldAccess(pos,place, xts.homeName(), xct);
            }
            exprs.add(place);
            types.add(xts.Place());
            
            if(clocks.size() > 0){
                Type clockRailType = xts.ValRail(xts.Clock());
                Tuple clockRail = (Tuple) xnf.Tuple(pos, clocks).type(clockRailType);
                exprs.add(clockRail);
                types.add(clockRailType);
            }
        }

        System.out.println(xct.currentCode());
        Closure closure = synth.makeClosure(body.position(), xts.Void(), synth.toBlock(body), xct);
        exprs.add(closure);
        types.add(closure.closureDef().asType());
        Stmt result = xnf.Eval(pos, synth.makeStaticCall(pos, xts.Runtime(), Name.make("runAsync"), exprs, xts.Void(),
                                                         types, xct));
        
        return result;
    }

}
