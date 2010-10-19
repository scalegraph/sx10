/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.visit;

import java.io.*;

import polyglot.ast.Node;
import polyglot.frontend.Compiler;
import polyglot.util.CodeWriter;


/** Visitor which dumps the AST to a file. */
public class DumpAst extends NodeVisitor
{
    protected PrintWriter fw;
    protected CodeWriter w;

    /** @deprecated Use the other constructor. */
    public DumpAst(String name, int width) throws IOException {
        this.fw = new PrintWriter(new FileWriter(name));
        this.w = Compiler.createCodeWriter(fw, width);
    }

    public DumpAst(CodeWriter w) {
        this.w = w;
    }

    /** 
     * Visit each node before traversal of children. Call <code>dump</code> for
     * that node. Then we begin a new <code>CodeWriter</code> block and traverse
     * the children.
     */
    public NodeVisitor enter(Node n) {
        w.write("(");
        n.dump(w);
        w.allowBreak(4);
        w.begin(0);
        return this;
    }

    /**
     * This method is called only after normal traversal of the children. Thus
     * we must end the <code>CodeWriter</code> block that was begun in 
     * <code>enter</code>.
     */
    public Node leave(Node old, Node n, NodeVisitor v) {
        w.end();
        w.write(")");
        w.allowBreak(0);
        return n;
    }

    public void finish() {
        try {
            w.flush();

            if (fw != null) {
                fw.flush();
                fw.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
