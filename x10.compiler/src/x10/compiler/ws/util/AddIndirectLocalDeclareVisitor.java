package x10.compiler.ws.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import polyglot.ast.Block;
import polyglot.ast.Expr;
import polyglot.ast.Local;
import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.visit.NodeVisitor;
import x10.ast.X10NodeFactory;

/**
 * @author Haichuan
 * For inner class frames, each frame should have indirect references
 * to access upper frames' fields(transformed from locals).
 * 
 * And these indirect references should be added as local declarations in front of
 * each fast/slow/back method, which is the goal of this visitor.
 * 
 * It process the code block, and add the local declares
 *
 */
public class AddIndirectLocalDeclareVisitor extends NodeVisitor{
    X10NodeFactory xnf;
    Block targetBlock;
    Map<Expr, Stmt>refToDeclMap;
    
    public AddIndirectLocalDeclareVisitor(X10NodeFactory xnf, Map<Expr, Stmt>refToDeclMap){
        this.xnf = xnf;
        this.refToDeclMap = refToDeclMap;
    }
    
    public Node visitEdge(Node parent, Node child) {
        //in the first step, we need decide the target block
        if(child instanceof Block && targetBlock == null){
            targetBlock = (Block) child; //the outer block
        }
        return super.visitEdge(parent, child);
    }

    public Node leave(Node old, Node n, NodeVisitor v) {
        if(old == targetBlock && refToDeclMap != null){ //only process the target block
            
            Block block = (Block)n;
            
            //first detect all locals;
            LocalExprFinder lef = new LocalExprFinder();
            block.visit(lef);//no replacement, just detect
            
            HashSet<Stmt> localDecls = new HashSet<Stmt>(); //we need use set to only add one time
            for(Local local : lef.getLocalList()){
                Stmt s = refToDeclMap.get(local);
                if(s != null){
                    localDecls.add(s);
                }
            }
            //finally, add all these into the block
            if(localDecls.size() > 0){
                ArrayList<Stmt> nStmts = new ArrayList<Stmt>();
                nStmts.addAll(localDecls);
                nStmts.addAll(block.statements());
                block = xnf.Block(block.position(), nStmts);
            }
            return block;
        }
        return n;
    }
    
    /**
     * @author Haichuan
     * Find out all local var usages, and stored them into localList;
     */
    static class LocalExprFinder extends NodeVisitor {
        private ArrayList<Local> localList;
        public LocalExprFinder(){
            localList = new ArrayList<Local>();
        }
        

        protected List<Local> getLocalList() {
            return localList;
        }

        public Node leave(Node parent, Node old, Node n, NodeVisitor v) {
            if (n instanceof Local) {
                localList.add((Local) n);
            }
            return n;
        }
    }
}




