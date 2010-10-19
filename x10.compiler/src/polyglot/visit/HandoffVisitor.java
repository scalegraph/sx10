/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.visit;

import polyglot.ast.*;
import polyglot.frontend.ExtensionInfo;
import polyglot.frontend.Job;

/**
 * This visitor adds jobs for <code>SourceFile</code>s in the AST to the
 * schedule of another extension.
 */
public class HandoffVisitor extends NodeVisitor
{
    protected ExtensionInfo ext;

    public HandoffVisitor(ExtensionInfo ext) {
        this.ext = ext;
    }

    public Node override(Node n) {
        if (n instanceof SourceFile || n instanceof SourceCollection) {
            return null;
        }
        return n;
    }

    public Node leave(Node old, Node n, NodeVisitor v) {
        if (n instanceof SourceFile) {
            SourceFile sf = (SourceFile) n;
            Job job = ext.scheduler().addJob(sf.source(), sf);
            ext.scheduler().addDependenciesForJob(job, true);
        }
        return n;
    }
}
