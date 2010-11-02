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

package x10.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import polyglot.ast.Binary;
import polyglot.ast.Call;
import polyglot.ast.Expr;
import polyglot.ast.Formal;
import polyglot.ast.Loop;
import polyglot.ast.Loop_c;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Stmt;
import polyglot.ast.Term;
import polyglot.types.ClassDef;
import polyglot.types.Context;
import polyglot.types.Flags;
import polyglot.types.LazyRef;
import polyglot.types.LocalDef;
import polyglot.types.Name;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.Types;
import polyglot.types.UnknownType;
import polyglot.util.ErrorInfo;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.TypeBuilder;
import polyglot.visit.TypeCheckPreparer;
import polyglot.visit.TypeChecker;
import x10.constraint.XFailure;
import x10.constraint.XName;
import x10.constraint.XVar;
import x10.constraint.XTerm;
import x10.constraint.XTerms;
import x10.constraint.XVar;
import x10.errors.Errors;
import x10.types.X10ClassType;
import polyglot.types.Context;
import x10.types.X10FieldInstance;
import x10.types.X10LocalDef;
import x10.types.X10MethodInstance;

import x10.types.X10TypeEnv;
import x10.types.X10TypeEnv_c;
import x10.types.X10TypeMixin;
import polyglot.types.TypeSystem;
import x10.types.checker.Converter;
import x10.types.constraints.CConstraint;
import x10.types.matcher.Subst;
import x10.util.Synthesizer;
import x10.visit.X10TypeChecker;

/**
 * Captures the commonality of for, foreach and ateach loops in X10.
 * TODO:
 * (1) formal must be a variable whose type will be that of the region underlying domain.
 * (2) domain must be an array, distribution or region. If it is an array or distribution a,
 *     the system must behave as if the user typed a.region.
 * (3) Perhaps we can allow continue statements within a for loop, but not within
 *     a foreach or an ateach.
 *
 * COMMENTS / TODO (added by Christian)
 * (1) for now, continue/break should work as usual
 *      for foreach;
 * (2) ateach is broken in many respects, including break/continue,
 *      see comments in ateach.xcd
 * (3) this AST node does not seem to support the 'correct' syntax for
 *      multi-dimensional arrays (ateach(i,j:D) { S }).  But that's
 *      probably ok, for now the XCD files expect to see
 *      ateach(i:D) { S } and type 'i' as 'int[]' for multi-dimensional
 *      arrays, and as 'int' for single-dimensional arrays.
 *
 * @author vj Dec 9, 2004
 */
public abstract class X10Loop_c extends Loop_c implements X10Loop {
	protected Formal formal;
	protected Expr domain;
	protected Stmt body;
	protected List<Stmt> locals;

	protected LoopKind loopKind;

	/**
	 * @param pos
	 */
	protected X10Loop_c(Position pos) {
		super(pos);
	}

	protected X10Loop_c(Position pos, Formal formal, Expr domain, Stmt body) {
		super(pos);
		this.formal = formal;
		this.domain = domain;
		this.body = body;
	}

	/** Reconstruct the expression. */
	protected X10Loop_c reconstruct(Formal formal, Expr domain, Stmt body) {
		if (formal != this.formal || domain != this.domain || body != this.body) {
			X10Loop_c n = (X10Loop_c) copy();
			n.formal = formal;
			n.domain = domain;
			n.body = body;
			return n;
		}
		return this;
	}
	

	
	public Node typeCheckOverride(Node parent, ContextVisitor tc) {
	    TypeChecker tc1 = (TypeChecker) tc.enter(parent, this);
	    
	    Expr domain = (Expr) this.visitChild(this.domain, tc1);
	    Type domainType = domain.type();
	    
	    Formal formal = (Formal) this.visitChild(this.formal, tc1);
	    
	    NodeFactory nf = (NodeFactory) tc.nodeFactory();
	    TypeSystem ts = (TypeSystem) tc.typeSystem();
	    
//	    if (ts.isPoint(formal.type().type())) {
//	        X10Type point = (X10Type) formal.type().type();
//	        formal = setClauses(formal, domain, nf);
//	    }
	    
	    return tc.visitEdgeNoOverride(parent, this.domain(domain).formal(formal));
	}

	/** Type check the statement. */
	public Node typeCheck(ContextVisitor tc) {
		X10Loop_c n = (X10Loop_c) typeCheckNode(tc);
		return n;
	}
	
	
	public Node typeCheckNode(ContextVisitor tc) {
		NodeFactory nf = tc.nodeFactory();
		TypeSystem ts = (TypeSystem) tc.typeSystem();
		Type domainType = domainTypeRef.get();
		if (domainType == null ) {
			// aha, in this case the type inferencer did not run, since an explicit type was givem.
			domainType = domain.type();
		}
		Type formalType = formal.declType();
		Type Iterable = ts.Iterable(formalType);
		assert domainType != null 
		: "formal=" + formal + " domain = " + domain + " position = " + position();
		if (ts.isSubtype(domainType, Iterable, tc.context())) {
		//	if (X10TypeMixin.areConsistent(formalType, domainType)
		    return this;
		}

//		// Check if there is a method with the appropriate name and type with the left 
		// operand as receiver.   
//		X10MethodInstance mi = ts.findMethod(domainType, ts.MethodMatcher(domainType, Name.make("iterator"), Collections.EMPTY_LIST), tc.context().currentClassDef());
//		Type rt = mi.returnType();
//		if (! mi.flags().isStatic() && ts.isSubtype(rt, Iterator))
//		    return this;

		if (ts.isSubtype(formalType, ts.Point(), tc.context())) {
		    try {
		        Expr newDomain = Converter.attemptCoercion(tc, domain, ts.Region());
		        if (newDomain != null && newDomain != domain)
		            return this.domain(newDomain).del().typeCheck(tc);
		    }
		    catch (SemanticException e) {
		    }
		    try {
		        Expr newDomain = Converter.attemptCoercion(tc, domain, ts.Dist());
		        if (newDomain != null && newDomain != domain)
		            return this.domain(newDomain).del().typeCheck(tc);
		    }
		    catch (SemanticException e) {
		    }
		}
		
		try {
			// The expected type is Iterable[Foo]. The constraints on domainType dont matter
			// for this failure, so strip them.
		    throw new SemanticException("Loop domain is not of expected type." 
		    		+ "\n\t Expected type: Iterable[" + formalType + "]" 
		    		+ "\n\t Actual type: " + X10TypeMixin.baseType(domainType), position());
		}
		catch (SemanticException e) {
		    tc.errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR, "ERROR: " + e.getMessage(), position());
		    return this;
		}
	}

	/* (non-Javadoc)
	 * @see polyglot.ast.Term#entry()
	 */
	public Term firstChild() {
		return formal;
	}

	/* (non-Javadoc)
	 * @see polyglot.ast.Term#acceptCFG(polyglot.visit.CFGBuilder, java.util.List)
	 */
	@Override
	public <S> List<S> acceptCFG(CFGBuilder v, List<S> succs) {
		v.visitCFG(formal, domain, ENTRY);
		v.visitCFG(domain, body, ENTRY);
		v.visitCFG(body, this, EXIT);
		return succs;
	}

	@Override
	public Context enterScope(Context c) {
		return c.pushBlock();
	}

	/** Visit the children of the expression. */
	@Override
	public Node visitChildren(NodeVisitor v) {
		Formal formal = (Formal) visitChild(this.formal, v);
		Expr domain = (Expr) visitChild(this.domain, v);
		Stmt body = (Stmt) visitChild(this.body, v);
		return reconstruct(formal, domain, body);
	}

	/* (non-Javadoc)
	 * @see x10.ast.X10Loop#body()
	 */
	public Stmt body() {
		return this.body;
	}

	/* (non-Javadoc)
	 * @see x10.ast.X10Loop#formal()
	 */
	public Formal formal() {
		return this.formal;
	}

	/* (non-Javadoc)
	 * @see x10.ast.X10Loop#domain()
	 */
	public Expr domain() {
		return this.domain;
	}

	/* (non-Javadoc)
	 * @see x10.ast.X10Loop#locals()
	 */
	public List<Stmt> locals() {
		return this.locals == null ? Collections.<Stmt>emptyList() : this.locals;
	}

	/* (non-Javadoc)
	 * @see x10.ast.X10Loop#body(polyglot.ast.Stmt)
	 */
	public X10Loop body(Stmt body) {
		X10Loop_c n = (X10Loop_c) copy();
		n.body = body;
		return n;
	}

	/* (non-Javadoc)
	 * @see x10.ast.X10Loop#formal(polyglot.ast.Formal)
	 */
	public X10Loop formal(Formal formal) {
		X10Loop_c n = (X10Loop_c) copy();
		n.formal = formal;
		return n;
	}

	/* (non-Javadoc)
	 * @see x10.ast.X10Loop#domain(polyglot.ast.Expr)
	 */
	public X10Loop domain(Expr domain) {
		X10Loop_c n = (X10Loop_c) copy();
		n.domain = domain;
		return n;
	}

	/* (non-Javadoc)
	 * @see x10.ast.X10Loop#locals(java.util.List)
	 */
	public X10Loop locals(List<Stmt> locals) {
		X10Loop_c n = (X10Loop_c) copy();
		n.locals = locals;
		return n;
	}

	public Term continueTarget() {
		return formal;
	}

	public Expr cond() { return null; }
	
	/*
	 * Type inference works as follows.
	 * 
	 * We first seek to infer the type of the index expression i in the for loop for (i in D) S
	 * (if this type is not specified explicitly).
	 * 
	 * There are two source of information about index type. First, the index expression itself.
	 * If it is of the form (k1,..., kn) we should infer that the type is Point(n). 
	 * 
	 * Second, we look to find if the type DT of the domain expression D is of the form Iterable[T].
	 * In this case, the inferred type of i is T. 
	 * 
	 * The subtlety here is that T may have a reference to Foo@this; this needs to be replaced with
	 * a logical variable corresponding to D. However, D may not have a self var binding. So we have
	 * to create such a variable. 
	 * 
	 * Examples that should work:
	 * 	
	def g(g:Dist(1)): Dist(1) = g;
	def m(gridDist:Dist(1)) {
		for ((i) in g(gridDist) ) ;
		for ((i) in 0..9) ;
		for ((i) in gridDist | here);
		//for ((i,j) in 0..9) ;
	}
	 * 
	 */
	
	LazyRef<Type> domainTypeRef = Types.lazyRef(null);
	public Type domainType() {
		Type domainType = domainTypeRef.get();
		if (domainType == null ) {
			// aha, in this case the type inferencer did not run, since an explicit type was givem.
			domainType = domain.type();
		}
		return domainType;
	}

	@Override
	public Node setResolverOverride(final Node parent, final TypeCheckPreparer v) {
		final Expr domain = this.domain;
		final X10Loop loop = this;

		final Formal f = this.formal;
		X10LocalDef li = (X10LocalDef) f.localDef();
		if (f.type() instanceof UnknownTypeNode) {
			UnknownTypeNode tn = (UnknownTypeNode) f.type();

			NodeVisitor childv = v.enter(parent, loop);
			childv = childv.enter(loop, domain);

			if (childv instanceof TypeCheckPreparer) {
				final TypeCheckPreparer tcp = (TypeCheckPreparer) childv;
				final LazyRef<Type> r = (LazyRef<Type>) tn.typeRef();
				TypeChecker tc = new X10TypeChecker(v.job(), v.typeSystem(), v.nodeFactory(), 
						v.getMemo());
				tc = (TypeChecker) tc.context(tcp.context().freeze());

				// Create a ref and goal for computing the domain type.
				final LazyRef<Type> domainTypeRef = this.domainTypeRef;
				domainTypeRef.setResolver(new TypeCheckExprGoal(loop, domain, tc, domainTypeRef));

				final NodeFactory nf = (NodeFactory) v.nodeFactory();
				final TypeSystem ts = (TypeSystem) v.typeSystem();
				final ClassDef curr = v.context().currentClassDef();

				// Now, infer index Type.
				r.setResolver(new Runnable() { 
					Type getIndexType(Type domainType) {
						Type base = X10TypeMixin.baseType(domainType);

						if (base instanceof X10ClassType) {
							if (ts.hasSameClassDef(base, ts.Iterable())) {
								return X10TypeMixin.getParameterType(base, 0);
							}
							else {
								Type sup = ts.superClass(domainType);
								if (sup != null) {
									Type t = getIndexType(sup);
									if (t != null) return t;
								}
								for (Type ti : ts.interfaces(domainType)) {
									Type t = getIndexType(ti);
									if (t != null) {
										return t;
									}
								}
							}
						}
						return null;
					}

					public void run() {
						
						Type indexType = null;
						int length = ((X10Formal) f).vars().size();
						if (length > 0) {
							indexType = ts.Point();

							// Add a self.rank=n clause, if the formal
							// has n components.
							XVar self = X10TypeMixin.xclause(indexType).self();
							Synthesizer synth = new Synthesizer(nf, ts);
							XTerm v = synth.makePointRankTerm((XVar) self);
							XTerm rank = XTerms.makeLit(new Integer(length));
							try {
								indexType = X10TypeMixin.addBinding(indexType, v, rank);
							} catch (XFailure z) {
								throw new InternalCompilerError("Unexpected error: " + z);
							}
							r.update(indexType);
							return;
						}
						
						Type domainType = domainTypeRef.get();
						indexType = getIndexType(domainType);    
						if (indexType == null) 
						    return;
						Type base = X10TypeMixin.baseType(domainType);
						

						XVar selfValue = X10TypeMixin.selfVarBinding(domainType);
						boolean generated = false;
						if (selfValue == null) {
							selfValue = XTerms.makeUQV();
							generated = true;
						}
						XVar thisVar = base instanceof X10ClassType ? 
								((X10ClassType) base).x10Def().thisVar() :null;

						// Now the problem is that indexType may 
						// have a non-null thisVar (e.g. Foo#this). We need to 
						// replace thisVar with var. 
						if (thisVar != null)
							try {
								
								indexType = Subst.subst(indexType, selfValue, thisVar);
								if (generated) {
									CConstraint c = X10TypeMixin.xclause(domainType);
									c=c.substitute(selfValue, c.self());
									indexType = X10TypeMixin.addConstraint(indexType, c);
									indexType = Subst.subst(indexType, XTerms.makeEQV(), selfValue);
								}
								
							}
						catch (XFailure z) {
							
						}
						catch (SemanticException e) {
						}
						r.update(indexType);
					}
				});
			}
		}

		return super.setResolverOverride(parent, v);
	}

	public Node buildTypes(TypeBuilder tb) throws SemanticException {
		X10Loop n = (X10Loop) super.buildTypes(tb);
		
		// Set the final flag on all formals introduced in the loop.
		Formal f = (Formal) n.formal().visit(new NodeVisitor() {
			public Node leave(Node old, Node n, NodeVisitor v) {
				if (n instanceof Formal) {
					Formal f = (Formal) n;
					LocalDef li = f.localDef();
					Flags flags = f.flags().flags();
					flags = flags.Final();
					li.setFlags(flags);
					return f.flags(f.flags().flags(flags)).localDef(li);
				}
				return n;
			}
		});
		return n.formal(f);
	}
}

