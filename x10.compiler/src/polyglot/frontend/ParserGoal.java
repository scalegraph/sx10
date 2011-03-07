/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * Copyright (c) 2006 IBM Corporation
 * 
 */

package polyglot.frontend;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;

import polyglot.ast.ClassMember;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.SourceFile;
import polyglot.ast.TopLevelDecl;
import polyglot.ast.TypeNode;
import polyglot.main.Reporter;
import polyglot.types.Flags;
import polyglot.util.ErrorInfo;
import polyglot.util.ErrorQueue;
import polyglot.util.Position;

/**
 * A pass which runs a parser.  After parsing it stores the AST in the Job.
 * so it can be accessed by later passes.
 */
public class ParserGoal extends SourceGoal_c
{
    private static final long serialVersionUID = -2901265337615479927L;

    protected Compiler compiler;

    public ParserGoal(Compiler compiler, Job job) {
        super("Parser", job);
	this.compiler = compiler;
    }

    protected SourceFile createDummyAST() {
	NodeFactory nf = job().extensionInfo().nodeFactory();
	String fName = job.source().name();
	Position pos = new Position(job.source().name(), fName).markCompilerGenerated();
	String name = fName.substring(fName.lastIndexOf(File.separatorChar)+1, fName.lastIndexOf('.'));
	TopLevelDecl decl = nf.ClassDecl(pos, nf.FlagsNode(pos, Flags.PUBLIC),
	                                 nf.Id(pos, name), null, Collections.<TypeNode>emptyList(),
	                                 nf.ClassBody(pos, Collections.<ClassMember>emptyList()));
	SourceFile ast = nf.SourceFile(pos, Collections.singletonList(decl)).source(job.source());
	return ast;
    }

    /**
     * Do not fail on parse errors, but create a dummy AST containing a single class
     * with the expected name, to allow proceeding with compilation.
     */
    public boolean runTask() {
	ErrorQueue eq = compiler.errorQueue();
        
	FileSource source = (FileSource) job().source();

	Node ast = null;
	Reporter reporter = scheduler.extensionInfo().getOptions().reporter;
	try {
	    if (reporter.should_report("parser", 1))
	        reporter.report(1, "" + source);
	    
	    Reader reader = source.open();
            
            Parser p = job().extensionInfo().parser(reader, source, eq);

	    if (reporter.should_report(Reporter.frontend, 2))
	        reporter.report(2, "Using parser " + p);
	    ast = p.parse();

	    source.close();
	}
	catch (IOException e) {
	    eq.enqueue(ErrorInfo.IO_ERROR, e.getMessage(),
                new Position(job().source().path(),
                             job().source().name(), 1, 1, 1, 1));

	}

	if (ast == null) {
	    ast = createDummyAST();
	}

	job().ast(ast);
	return true;
    }

    public String toString() {
	return super.toString() + "(" + job().source() + ")";
    }
}
