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

package x10.visit;

import java.util.Iterator;
import java.util.List;

import polyglot.ast.ClassDecl;
import polyglot.ast.Expr;
import polyglot.ast.FieldDecl;
import polyglot.ast.Import;
import polyglot.ast.MethodDecl;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.PackageNode;
import polyglot.ast.SourceFile;
import polyglot.ast.Stmt;
import polyglot.ast.TypeNode;
import polyglot.frontend.Job;
import polyglot.types.ClassType;
import polyglot.types.QName;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import x10.ast.AnnotationNode;
import x10.extension.X10Ext;
import x10.types.X10ClassType;

public class AnnotationChecker extends ContextVisitor {
	public AnnotationChecker(Job job, TypeSystem ts, NodeFactory nf) {
		super(job, ts, nf);
	}
	
	public Node leaveCall(Node parent, Node old, Node n, NodeVisitor v) throws SemanticException {

		if (! (n.ext() instanceof X10Ext)) {
			return n;
		}
		
		init();

		List<AnnotationNode> annotations = ((X10Ext) n.ext()).annotations();
		
		for (Iterator<AnnotationNode> i = annotations.iterator(); i.hasNext(); ) {
			AnnotationNode a = i.next(); 
			X10ClassType at = a.annotationInterface();
			if (n instanceof TypeNode && ! at.isSubtype(TA, context)) {
				throw new SemanticException("Annotations on types must implement " + TA, n.position());
			}
			if (n instanceof Expr && ! at.isSubtype(EA, context)) {
				throw new SemanticException("Annotations on expressions must implement " + EA, n.position());
			}
			if (n instanceof Stmt && ! at.isSubtype(SA, context)) {
				throw new SemanticException("Annotations on statements must implement " + SA, n.position());
			}
			if (n instanceof MethodDecl && ! at.isSubtype(MA, context)) {
				throw new SemanticException("Annotations on method declarations must implement " + MA, n.position());
			}
			if (n instanceof FieldDecl && ! at.isSubtype(FA, context)) {
				throw new SemanticException("Annotations on field declarations must implement " + FA, n.position());
			}
			if (n instanceof ClassDecl && ! at.isSubtype(CA, context)) {
				throw new SemanticException("Annotations on class declarations must implement " + CA, n.position());
			}
			if (n instanceof PackageNode && parent instanceof SourceFile && ! at.isSubtype(PA, context)) {
				throw new SemanticException("Annotations on package declarations must implement " + PA, n.position());
			}
			if (n instanceof Import && ! at.isSubtype(IA, context)) {
				throw new SemanticException("Annotations on imports must implement " + IA, n.position());
			}
			if (! at.isSubtype(A, context)) {
				throw new SemanticException("Annotations must implement " + A, n.position());
			}
		}
		
		return n;
	}
	
	ClassType A, TA, EA, SA, MA, FA, CA, IA, PA;
	
	public void init() throws SemanticException {
		if (A != null) return;
		TA = (ClassType) ts.systemResolver().find(QName.make("x10.lang.annotations.TypeAnnotation"));
		EA = (ClassType) ts.systemResolver().find(QName.make("x10.lang.annotations.ExpressionAnnotation"));
		SA = (ClassType) ts.systemResolver().find(QName.make("x10.lang.annotations.StatementAnnotation"));
		MA = (ClassType) ts.systemResolver().find(QName.make("x10.lang.annotations.MethodAnnotation"));
		FA = (ClassType) ts.systemResolver().find(QName.make("x10.lang.annotations.FieldAnnotation"));
		CA = (ClassType) ts.systemResolver().find(QName.make("x10.lang.annotations.ClassAnnotation"));
		IA = (ClassType) ts.systemResolver().find(QName.make("x10.lang.annotations.ImportAnnotation"));
		PA = (ClassType) ts.systemResolver().find(QName.make("x10.lang.annotations.PackageAnnotation"));
		A  = (ClassType) ts.systemResolver().find(QName.make("x10.lang.annotations.Annotation"));
	}
}
