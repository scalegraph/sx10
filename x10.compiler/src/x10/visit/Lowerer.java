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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import polyglot.ast.Assign;
import polyglot.ast.Block;
import polyglot.ast.Call;
import polyglot.ast.CanonicalTypeNode;
import polyglot.ast.Catch;
import polyglot.ast.Eval;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.FieldAssign;
import polyglot.ast.FloatLit;
import polyglot.ast.Formal;
import polyglot.ast.Id;
import polyglot.ast.IntLit;
import polyglot.ast.IntLit_c;
import polyglot.ast.Local;
import polyglot.ast.LocalAssign;
import polyglot.ast.LocalDecl;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Return;
import polyglot.ast.Stmt;
import polyglot.ast.Throw;
import polyglot.ast.Try;
import polyglot.ast.TypeNode;
import polyglot.ast.Unary;
import polyglot.frontend.Job;
import polyglot.main.Reporter;
import polyglot.types.ClassType;
import polyglot.types.CodeInstance;
import polyglot.types.Context;
import polyglot.types.FieldInstance;
import polyglot.types.Flags;
import polyglot.types.LocalDef;
import polyglot.types.Name;
import polyglot.types.QName;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.types.VarDef;
import polyglot.types.VarInstance;
import polyglot.util.CollectionUtil; import x10.util.CollectionFactory;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import x10.Configuration;
import x10.ast.AnnotationNode;
import x10.ast.Async;
import x10.ast.AtEach;
import x10.ast.AtExpr;
import x10.ast.AtStmt;
import x10.ast.Atomic;
import x10.ast.Closure;
import x10.ast.DepParameterExpr;
import x10.ast.Finish;
import x10.ast.FinishExpr;
import x10.ast.Here;
import x10.ast.Next;
import x10.ast.Offer;
import x10.ast.Resume;
import x10.ast.SettableAssign;
import x10.ast.Tuple;
import x10.ast.When;
import x10.ast.X10Binary_c;
import x10.ast.X10Call;
import x10.ast.X10CanonicalTypeNode;
import x10.ast.X10Cast;
import x10.ast.X10Formal;
import x10.ast.X10Instanceof;
import x10.ast.X10New;
import x10.ast.X10Special;
import x10.ast.X10Unary_c;
import x10.constraint.XFailure;
import x10.constraint.XVar;
import x10.emitter.Emitter;
import x10.extension.X10Ext;
import x10.extension.X10Ext_c;
import x10.types.AsyncInstance;
import x10.types.AtInstance;
import x10.types.ClosureDef;
import x10.types.ConstrainedType;
import x10.types.X10ClassType;
import x10.types.X10ConstructorInstance;
import x10.types.MethodInstance;
import x10.types.X10ParsedClassType;

import x10.types.checker.Converter;
import x10.types.checker.PlaceChecker;
import x10.types.constraints.CConstraint;
import x10.types.constraints.XConstrainedTerm;
import x10.util.ClosureSynthesizer;
import x10.util.Synthesizer;
import x10.util.synthesizer.InstanceCallSynth;
import x10.visit.Desugarer.Substitution;

/**
 * Visitor to desugar the AST before code generation.
 * 
 * NOTE: all the nodes created in the Desugarer must have the appropriate type information.
 * The NodeFactory methods do not fill in the type information.  Use the helper methods available
 * in the Desugarer to create expressions, or see how the type information is filled in for other
 * types of nodes elsewhere in the Desugarer.  TODO: factor out the helper methods into the
 * {@link Synthesizer}.
 */
public class Lowerer extends ContextVisitor {
    private final Synthesizer synth;
    public Lowerer(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
        synth = new Synthesizer(nf, ts);
    }

    private static int count;
    //Collecting Finish Use: store reducer
    private static Stack<FinishExpr> reducerS = new Stack<FinishExpr>();
    private static Stack<Local> clockStack = new Stack<Local>();
    private static int flag = 0;

    private static Name getTmp() {
        return Name.make("__lowerer__var__" + (count++) + "__");
    }

    private static final Name RUN_AT = Name.make("runAt");
    private static final Name EVAL_AT = Name.make("evalAt");
    private static final Name RUN_ASYNC = Name.make("runAsync");
    private static final Name RUN_UNCOUNTED_ASYNC = Name.make("runUncountedAsync");
    private static final Name HOME = Name.make("home");
    private static final Name HERE_INT = Name.make("hereInt");
    private static final Name NEXT = Name.make("next");
    private static final Name RESUME = Name.make("resume");
    private static final Name DROP = Name.make("drop");
    private static final Name MAKE = Name.make("make");
    
    private static final Name AWAIT_ATOMIC = Name.make("awaitAtomic");
    private static final Name ENTER_ATOMIC = Name.make("enterAtomic");
    private static final Name ENSURE_NOT_IN_ATOMIC = Name.make("ensureNotInAtomic");
    private static final Name EXIT_ATOMIC = Name.make("exitAtomic");
    
    private static final Name START_FINISH = Name.make("startFinish");
    private static final Name PUSH_EXCEPTION = Name.make("pushException");
    private static final Name STOP_FINISH = Name.make("stopFinish");
    private static final Name PLACES = Name.make("places");
    private static final Name RESTRICTION = Name.make("restriction");
    private static final Name CONVERT = Converter.operator_as;
    private static final Name CONVERT_IMPLICITLY = Converter.implicit_operator_as;
    private static final Name DIST = Name.make("dist");
    
    private static final Name XOR = Name.make("xor");
    private static final Name FENCE = Name.make("fence");
    private static final QName IMMEDIATE = QName.make("x10.compiler.Immediate");
    private static final QName PRAGMA = QName.make("x10.compiler.Pragma");
    private static final QName REF = QName.make("x10.compiler.Ref");
    private static final QName UNCOUNTED = QName.make("x10.compiler.Uncounted");
    private static final QName REMOTE_OPERATION = QName.make("x10.compiler.RemoteOperation");
    private static final QName ASYNC_CLOSURE = QName.make("x10.compiler.AsyncClosure");
    
    private static final Name START_COLLECTING_FINISH = Name.make("startCollectingFinish");
    private static final Name STOP_COLLECTING_FINISH = Name.make("stopCollectingFinish");
    private static final Name OFFER = Name.make("makeOffer");  
    
    //added for scalable finish
    private static final Name START_LOCAL_FINISH = Name.make("startLocalFinish");
    private static final Name START_SIMPLE_FINISH = Name.make("startSimpleFinish");
    
    public Node override(Node parent, Node n) { 
    	if (n instanceof Finish) {
    		Finish finish = (Finish) n;
    		if (! finish.clocked())
    			// Follow normal procedure
    			return null;
    		// Translate clocked finish S ==> 
    		// var clock_??1:Clock=null; 
    		// finish 
    		// try { 
    		//   val clock_??2 = Clock.make(); 
    		//   clock_??1=clock_??2; 
    		//   S; //--> nested clocked async T ==> async clocked(clock_??2) T
    		//  } finally {
    		//    clock_???.drop();
    		//  }
    		// TODO: Simplify this to finish { val clock?? = Clock.make(); try { S} finally{ clock??.drop();}}
    		Context xc = context();
    		Position pos = finish.position();
    		Name name = xc.makeFreshName("clock");
    		Flags flags = Flags.FINAL;
    		Type type = ts.Clock();
    		
    		final Name varName = xc.getNewVarName();
			final LocalDef li = ts.localDef(pos, flags, Types.ref(type), varName);
			final Id varId = nf.Id(pos, varName);
			final Local ldRef = (Local) nf.Local(pos, varId).localInstance(li.asInstance()).type(type);
			clockStack.push(ldRef);
			
			final Name outerVarName = xc.getNewVarName();
			final LocalDef outerLi = ts.localDef(pos, flags, Types.ref(type), outerVarName);
			final Id outerVarId = nf.Id(pos, outerVarName);
			final Local outerLdRef = (Local) nf.Local(pos, outerVarId).localInstance(outerLi.asInstance()).type(type);

			try {
				Expr clock = synth.makeStaticCall(pos, type, MAKE, type, xc);
				final TypeNode tn = nf.CanonicalTypeNode(pos, type);
				Expr nullLit = nf.NullLit(pos).type(type);
				final LocalDecl outerLd = nf.LocalDecl(pos, nf.FlagsNode(pos, Flags.NONE), tn, outerVarId, nullLit).localDef(outerLi);
				
				Block block = synth.toBlock(finish.body());
				final LocalDecl ld = nf.LocalDecl(pos, nf.FlagsNode(pos, flags), tn, varId, clock).localDef(li);
				Stmt assign = nf.Eval(pos, synth.makeAssign(pos, outerLdRef, Assign.ASSIGN, ldRef, xc));
				block = block.prepend(assign);
				block = block.prepend(ld);
				Block drop = nf.Block(pos,nf.Eval(pos, new InstanceCallSynth(nf, xc, pos, outerLdRef, DROP).genExpr()));
				Stmt stm1 = nf.Try(pos, block, Collections.<Catch>emptyList(), drop);
				Node result = visitEdgeNoOverride(parent, nf.Block(pos, outerLd, nf.Finish(pos, stm1, false)));
				return result;
			} catch (SemanticException z) {
				return null;
			}
    	}
    	// handle async at(p) S and treat it as the old async(p) S.
    	if (n instanceof Async) {
    		Async async = (Async) n;
    		Stmt body = async.body();
    		AtStmt atStm = toAtStmt(body);
    		if (atStm==null)
    			return null;
    		Expr place = atStm.place(); 
    		if (ts.hasSameClassDef(Types.baseType(place.type()), ts.GlobalRef())) {
    			try {
    				place = synth.makeFieldAccess(async.position(),place, ts.homeName(), context());
    			} catch (SemanticException e) {
    			}
    		}
    		List<Expr> clocks = async.clocks();
    		place = (Expr) visitEdgeNoOverride(atStm, place);
    		body = (Stmt) visitEdgeNoOverride(atStm, atStm.body());
    		if (clocks != null && ! clocks.isEmpty()) {
    			List<Expr> nclocks = new ArrayList<Expr>();
    			for (Expr c : clocks) {
    				nclocks.add((Expr) visitEdgeNoOverride(async, c));
    			}
    			clocks =nclocks;
    		}
    		try {
    			return visitAsyncPlace(async, place, body);
    		} catch (SemanticException z) {
    			return null;
    		}
    	}
        if (n instanceof Eval) {
            try {
                Stmt s = visitEval((Eval) n);
                flag = 1;
                return visitEdgeNoOverride(parent, s);
            }
            catch (SemanticException e) {
                return null;
            }
        }

        return null;
    }

    //Collecting Finish Use : store reducer when enter finishR
    @Override
    protected NodeVisitor enterCall(Node parent, Node n) {
        if (n instanceof LocalDecl){
            LocalDecl f = (LocalDecl) n;
            if (f.init() instanceof FinishExpr) {
                reducerS.push((FinishExpr) f.init());
            }
        }
        if (n instanceof Eval) {
            if (((Eval) n).expr() instanceof Assign) {
                Assign f = (Assign) ((Eval)n).expr();
                Expr right = f.right();
                if (right instanceof FinishExpr) {
                    reducerS.push((FinishExpr) f.right());
                }
            }
        }
        if (n instanceof Return) {
            Return f = (Return) n;
            if (f.expr() instanceof FinishExpr) {
                reducerS.push((FinishExpr) f.expr());
            }
        }

        return this;
    }

    public Node leaveCall(Node old, Node n, NodeVisitor v) throws SemanticException {
        if (n instanceof Async)
            return visitAsync(old, (Async) n);
        if (n instanceof AtStmt)
            return visitAtStmt((AtStmt) n);
        if (n instanceof AtExpr)
            return visitAtExpr((AtExpr) n);
        if (n instanceof Here)
            return visitHere((Here) n);
        if (n instanceof Next)
            return visitNext((Next) n);
        if (n instanceof Atomic)
            return visitAtomic((Atomic) n);
        if (n instanceof When)
            return visitWhen((When) n);
        if (n instanceof Finish)
            return visitFinish((Finish) n);
        if (n instanceof Offer)
            return visitOffer((Offer) n);
        if (n instanceof Return)
            return visitReturn((Return) n);
        if (n instanceof AtEach)
            return visitAtEach((AtEach) n);
        if (n instanceof Eval)
            return visitEval((Eval) n);
        if (n instanceof LocalDecl)
            return visitLocalDecl((LocalDecl) n);
        if (n instanceof Resume)
            return visitResume((Resume) n);
        return n;
    }

    private Expr visitAtExpr(AtExpr e) throws SemanticException {
        return visitRemoteClosure(e, EVAL_AT, e.place());
    }

    Expr getPlace(Position pos, Expr place) throws SemanticException{
    	if (! ts.isImplicitCastValid(place.type(), ts.Place(), context())) {
            	place = synth.makeInstanceCall(pos, place, ts.homeName(),
            			Collections.<TypeNode>emptyList(),
            			Collections.<Expr>emptyList(),
            			ts.Place(),
            			Collections.<Type>emptyList(),
            			context());
            }
    	return place;
    }

    private Expr visitRemoteClosure(Closure c, Name implName, Expr place) throws SemanticException {
        Position pos = c.position();
        place = getPlace(pos, place);
        List<TypeNode> typeArgs = Arrays.asList(new TypeNode[] { c.returnType() });
        Position bPos = c.body().position();
        ClosureDef cDef = c.closureDef().position(bPos);
        Expr closure = nf.Closure(c, bPos)
            .closureDef(cDef)
        	.type(ClosureSynthesizer.closureAnonymousClassDef( ts, cDef).asType());
        List<Expr> args = new ArrayList<Expr>(Arrays.asList(new Expr[] { place, closure }));
        List<Type> mArgs = new ArrayList<Type>(Arrays.asList(new Type[] {
            ts.Place(), cDef.asType()
        }));
       // List<Type> tArgs = Arrays.asList(new Type[] { fDef.returnType().get() });

        Expr result = synth.makeStaticCall(pos, ts.Runtime(), implName,
        		typeArgs, args, c.type(), context());
        return result;
    }

    private static CodeInstance<?> findEnclosingCode(CodeInstance<?> ci) {
        if (ci instanceof AsyncInstance) {
            return findEnclosingCode(((AsyncInstance) ci).methodContainer());
        } else if (ci instanceof AtInstance) {
            return findEnclosingCode(((AtInstance) ci).methodContainer());
        }
        return ci;
    }

    private Stmt atStmt(Position pos, Stmt body, Expr place,
            List<VarInstance<? extends VarDef>> env) throws SemanticException {
        place = getPlace(pos, place);
        Closure closure = synth.makeClosure(body.position(), ts.Void(), synth.toBlock(body), context());
        closure.closureDef().setCapturedEnvironment(env);
        CodeInstance<?> mi = findEnclosingCode(Types.get(closure.closureDef().methodContainer()));
        closure.closureDef().setMethodContainer(Types.ref(mi));
        Stmt result = nf.Eval(pos,
        		synth.makeStaticCall(pos, ts.Runtime(), RUN_AT,
        				Arrays.asList(new Expr[] { place, closure }), ts.Void(),
        				context()));
        return result;
    }

    private Stmt visitAtStmt(AtStmt a) throws SemanticException {
        Position pos = a.position();
        return atStmt(pos, a.body(), a.place(), a.atDef().capturedEnvironment());
    }

    private AtStmt toAtStmt(Stmt body) {
    	if ((body instanceof AtStmt)) {
    		return (AtStmt) body;
    	}
    	if (body instanceof Block) {
    		Block block = (Block) body;
    		if (block.statements().size()==1) {
    			body = block.statements().get(0);
    			if ((body instanceof AtStmt)) {
    				return (AtStmt) body;
    			}
    		}
    	}
    	return null;
    }
    
    private List<Expr> clocks(boolean clocked, List<Expr> clocks) {
    	if (! clocked)
    		return clocks;
    	if (clocks == null)
    		clocks = new ArrayList<Expr>();
    	clocks.add(clockStack.peek());
    	return clocks;
    }
    // Begin asyncs
    // rewrite @Uncounted async S, with special translation for @Uncounted async at (p) S.
    private Stmt visitAsync(Node old, Async a) throws SemanticException {
    	List<Expr> clocks = clocks(a.clocked(), a.clocks());
        Position pos = a.position();
        X10Ext ext = (X10Ext) a.ext();
        List<X10ClassType> refs = Emitter.annotationsNamed(ts, a, REF);
        List<VarInstance<? extends VarDef>> env = a.asyncDef().capturedEnvironment();
        if (a.clocked()) {
            env = new ArrayList<VarInstance<? extends VarDef>>(env);
            env.add(clockStack.peek().localInstance());
        }
        if (isUncountedAsync(ts, a)) {
        	if (old instanceof Async)
            	 return uncountedAsync(pos, a.body(), env);
        }
        if (old instanceof Async)
            return async(pos, a.body(), clocks, refs, env);
        Stmt specializedAsync = specializeAsync(a, null, a.body());
        if (specializedAsync != null)
            return specializedAsync;
        return async(pos, a.body(), clocks, refs, env);
    }
    // Begin asyncs
    // rewrite @Uncounted async S, with special translation for @Uncounted async at (p) S.
    private Stmt visitAsyncPlace(Async a, Expr place, Stmt body) throws SemanticException {
    	List<Expr> clocks = clocks(a.clocked(), a.clocks());
        Position pos = a.position();
        List<X10ClassType> refs = Emitter.annotationsNamed(ts, a, REF);
        List<VarInstance<? extends VarDef>> env = a.asyncDef().capturedEnvironment();
        if (a.clocked()) {
            env = new ArrayList<VarInstance<? extends VarDef>>(env);
            env.add(clockStack.peek().localInstance());
        }
        if (isUncountedAsync(ts, a)) {
            return uncountedAsync(pos, body, place, env);
        }
        Stmt specializedAsync = specializeAsync(a, place, body);
        if (specializedAsync != null)
            return specializedAsync;
        return async(pos, body, clocks, place, refs, env);
    }

    // TODO: add more rules from SPMDcppCodeGenerator
    private boolean isGloballyAvailable(Expr e) {
        if (e instanceof Local)
            return true;
        return false;
    }

    public static boolean isUncountedAsync(TypeSystem ts, Async a) {
        return Emitter.hasAnnotation(ts, a, UNCOUNTED);
    }

    /**
     * Recognize the following pattern:
     * <pre>
     * @Immediate async at(p) {
     *     r(i) ^= v;
     * }
     * </pre>
     * where <tt>p: Place</tt>, <tt>r: Rail[T]!p</tt>, <tt>i:Int</tt>, and <tt>v:T</tt>,
     * and compile it into an optimized remote operation.
     * @param a the async statement
     * @return an invocation of the remote operation, or null if no match
     * @throws SemanticException
     * TODO: move into a separate pass!
     */
    private Stmt specializeAsync(Async a, Expr p, Stmt body) throws SemanticException {
        if (!Emitter.hasAnnotation(ts, a, IMMEDIATE))
            return null;
        if (a.clocks().size() != 0)
            return null;
        
        if (body instanceof Block) {
            List<Stmt> stmts = ((Block) body).statements();
            if (stmts.size() != 1)
                return null;
            body = stmts.get(0);
        }
        if (!(body instanceof Eval))
            return null;
        Expr e = ((Eval) body).expr();
        if (!(e instanceof SettableAssign))
            return null;
        SettableAssign sa = (SettableAssign) e;
        if (sa.operator() != Assign.BIT_XOR_ASSIGN)
            return null;
        List<Expr> is = sa.index();
        if (is.size() != 1)
            return null;
        Expr i = is.get(0);
        if (p instanceof X10New) {
            // TODO: make sure we calling the place constructor
            // TODO: decide between rail and place-local handle
            X10New n = (X10New) p;
            Expr q =  n.arguments().get(0);
            Expr r = sa.array();
            Expr v = sa.right();
            if (/*!isGloballyAvailable(r) || */!isGloballyAvailable(i) || !isGloballyAvailable(v))
                return null;
    /*        List<Type> ta = ((X10ClassType) X10TypeMixin.baseType(r.type())).typeArguments();
            if (!v.type().isLong() || !ts.isRailOf(r.type(), ts.Long()))
                return null;
            if (!PlaceChecker.isAtPlace(r, p, xContext()))
                return null;
    */
            ClassType RemoteOperation = (ClassType) ts.typeForName(REMOTE_OPERATION);
            Position pos = a.position();
            List<Expr> args = new ArrayList<Expr>();
            Expr p1 = (Expr) leaveCall(null, q, this);
            args.add(p1);
            args.add((Expr) leaveCall(null, r, this));
            args.add((Expr) leaveCall(null, i, this));
            args.add((Expr) leaveCall(null, v, this));
            Stmt alt = nf.Eval(pos, synth.makeStaticCall(pos, RemoteOperation, XOR, args, ts.Void(), context()));
            Expr cond = nf.Binary(pos, q, X10Binary_c.EQ, call(pos, HERE_INT, ts.Int())).type(ts.Boolean());
            Stmt cns = a.body();
            return nf.If(pos, cond, cns, alt);
        } else {
            Expr r = sa.array();
            Expr v = sa.right();
            if (/*!isGloballyAvailable(r) || */!isGloballyAvailable(i) || !isGloballyAvailable(v))
                return null;
    /*        List<Type> ta = ((X10ClassType) X10TypeMixin.baseType(r.type())).typeArguments();
            if (!v.type().isLong() || !ts.isRailOf(r.type(), ts.Long()))
                return null;
            if (!PlaceChecker.isAtPlace(r, p, xContext()))
                return null;
    */
            ClassType RemoteOperation = (ClassType) ts.typeForName(REMOTE_OPERATION);
            Position pos = a.position();
            List<Expr> args = new ArrayList<Expr>();
            Expr p1 = (Expr) leaveCall(null, p, this);
            args.add(p1);
            args.add((Expr) leaveCall(null, r, this));
            args.add((Expr) leaveCall(null, i, this));
            args.add((Expr) leaveCall(null, v, this));
            Stmt alt = nf.Eval(pos, synth.makeStaticCall(pos, RemoteOperation, XOR, args, ts.Void(), context()));
            Expr cond = nf.Binary(pos, p, X10Binary_c.EQ, call(pos, HOME, ts.Place())).type(ts.Boolean());
            Stmt cns = a.body();
            return nf.If(pos, cond, cns, alt);
        }
    }

    private Stmt async(Position pos, Stmt body, List<Expr> clocks, Expr place, List<X10ClassType> annotations,
            List<VarInstance<? extends VarDef>> env) throws SemanticException {
        if (ts.isImplicitCastValid(place.type(), ts.GlobalRef(), context())) {
            place = synth.makeFieldAccess(pos,place, ts.homeName(), context());
        }
        if (clocks.size() == 0)
        	return async(pos, body, place, annotations, env);
        Type clockRailType = Types.makeArrayRailOf(ts.Clock(), pos);
        Tuple clockRail = (Tuple) nf.Tuple(pos, clocks).type(clockRailType);

        return makeAsyncBody(pos, new ArrayList<Expr>(Arrays.asList(new Expr[] { place, clockRail })),
                             new ArrayList<Type>(Arrays.asList(new Type[] { ts.Place(), clockRailType})),
                             body, annotations, env);
    }

    private Stmt async(Position pos, Stmt body, Expr place, List<X10ClassType> annotations,
            List<VarInstance<? extends VarDef>> env) throws SemanticException {
        List<Expr> l = new ArrayList<Expr>(1);
        l.add(place);
        List<Type> t = new ArrayList<Type>(1);
        t.add(ts.Place());
        return makeAsyncBody(pos, l, t, body, annotations, env);
    }

    private Stmt async(Position pos, Stmt body, List<Expr> clocks, List<X10ClassType> annotations,
            List<VarInstance<? extends VarDef>> env) throws SemanticException {
        if (clocks.size() == 0)
        	return async(pos, body, annotations, env);
        Type clockRailType = Types.makeArrayRailOf(ts.Clock(), pos);
        Tuple clockRail = (Tuple) nf.Tuple(pos, clocks).type(clockRailType);
        return makeAsyncBody(pos, new ArrayList<Expr>(Arrays.asList(new Expr[] { clockRail })),
                             new ArrayList<Type>(Arrays.asList(new Type[] { clockRailType})), body,
                             annotations, env);
    }

    private Stmt async(Position pos, Stmt body, List<X10ClassType> annotations,
            List<VarInstance<? extends VarDef>> env) throws SemanticException {
        return makeAsyncBody(pos, new LinkedList<Expr>(),
                new LinkedList<Type>(), body, annotations, env);
    }

    private Stmt makeAsyncBody(Position pos, List<Expr> exprs, List<Type> types,
            Stmt body, List<X10ClassType> annotations,
            List<VarInstance<? extends VarDef>> env) throws SemanticException {
    	if (annotations == null)
    		annotations = new ArrayList<X10ClassType>(1);
    	annotations.add((X10ClassType) ts.systemResolver().findOne(ASYNC_CLOSURE));
        Closure closure = synth.makeClosure(body.position(), ts.Void(),
                synth.toBlock(body), context(), annotations);
        closure.closureDef().setCapturedEnvironment(env);
        CodeInstance<?> mi = findEnclosingCode(Types.get(closure.closureDef().methodContainer()));
        closure.closureDef().setMethodContainer(Types.ref(mi));
        exprs.add(closure);
        types.add(closure.closureDef().asType());
        Stmt result = nf.Eval(pos,
                synth.makeStaticCall(pos, ts.Runtime(), RUN_ASYNC, exprs,
                        ts.Void(), types, context()));
        return result;
    }

    private Stmt uncountedAsync(Position pos, Stmt body, Expr place,
            List<VarInstance<? extends VarDef>> env) throws SemanticException {
        List<Expr> l = new ArrayList<Expr>(1);
        l.add(place);
        List<Type> t = new ArrayList<Type>(1);
        t.add(ts.Place());
        return makeUncountedAsyncBody(pos, l, t, body, env);
    }

    private Stmt uncountedAsync(Position pos, Stmt body,
            List<VarInstance<? extends VarDef>> env) throws SemanticException {
        return makeUncountedAsyncBody(pos, new LinkedList<Expr>(),
                new LinkedList<Type>(), body, env);
    }

    private Stmt makeUncountedAsyncBody(Position pos, List<Expr> exprs, List<Type> types, Stmt body,
            List<VarInstance<? extends VarDef>> env) throws SemanticException {
        Closure closure = synth.makeClosure(body.position(), ts.Void(), synth.toBlock(body), context());
        CodeInstance<?> mi = findEnclosingCode(Types.get(closure.closureDef().methodContainer()));
        closure.closureDef().setMethodContainer(Types.ref(mi));
        closure.closureDef().setCapturedEnvironment(env);
        exprs.add(closure);
        types.add(closure.closureDef().asType());
        Stmt result = nf.Eval(pos,
                synth.makeStaticCall(pos, ts.Runtime(), RUN_UNCOUNTED_ASYNC, exprs,
                        ts.Void(), types, context()));
        return result;
    }
    // end Async


    // here -> Runtime.home()
    private Expr visitHere(Here h) throws SemanticException {
        Position pos = h.position();
        return call(pos, HOME, ts.Place());
    }

    // next; -> Runtime.next();
    private Stmt visitNext(Next n) throws SemanticException {
        Position pos = n.position();
        return nf.Eval(pos, call(pos, NEXT, ts.Void()));
    }
    
    // next; -> Runtime.next();
    private Stmt visitResume(Resume n) throws SemanticException {
        Position pos = n.position();
        return nf.Eval(pos, call(pos, RESUME, ts.Void()));
    }

    // atomic S; -> try { Runtime.enterAtomic(); S } finally { Runtime.exitAtomic(); }
    private Stmt visitAtomic(Atomic a) throws SemanticException {
        Position pos = a.position();
        Block tryBlock = nf.Block(pos, nf.Eval(pos, call(pos, ENTER_ATOMIC, ts.Void())), a.body());
        Block finallyBlock = nf.Block(pos, nf.Eval(pos, call(pos, EXIT_ATOMIC, ts.Void())));
        return nf.Try(pos, tryBlock, Collections.<Catch>emptyList(), finallyBlock);
    }

    private Stmt wrap(Position pos, Stmt s) {
        return s.reachable() ? nf.Block(pos, s, nf.Break(pos)) : s;
    }

    protected Expr getLiteral(Position pos, Type type, boolean val) {
        type = Types.baseType(type);
        if (ts.isBoolean(type)) {
            Type t = ts.Boolean();
            try {
                t = Types.addSelfBinding(t, val ? ts.TRUE() : ts.FALSE());
            } catch (XFailure e) { }
            return nf.BooleanLit(pos, val).type(t);
        } else
            throw new InternalCompilerError(pos, "Unknown literal type: "+type);
    }

    // when(E1) S1 or(E2) S2...; ->
    //    Runtime.ensureNotInAtomic();
    //    try { Runtime.enterAtomic();
    //          while (true) { if (E1) { S1; break; } if (E2) { S2; break; } ... Runtime.awaitAtomic(); }
    //    finally { Runtime.exitAtomic(); }
    private Stmt visitWhen(When w) throws SemanticException {
        Position pos = w.position();
        Block body = nf.Block(pos, nf.If(pos, w.expr(), wrap(pos, w.stmt())));
        for(int i=0; i<w.stmts().size(); i++) {
            body = body.append(nf.If(pos, (Expr) w.exprs().get(i), wrap(pos, (Stmt) w.stmts().get(i))));
        }
        body = body.append(nf.Eval(pos, call(pos, AWAIT_ATOMIC, ts.Void())));
        Block tryBlock = nf.Block(pos, 
        		nf.Eval(pos, call(pos, ENTER_ATOMIC, ts.Void())),
        		nf.While(pos, getLiteral(pos, ts.Boolean(), true), body));
        Block finallyBlock = nf.Block(pos, nf.Eval(pos, call(pos, EXIT_ATOMIC, ts.Void())));
        return nf.Block(pos, 
        		nf.Eval(pos, call(pos, ENSURE_NOT_IN_ATOMIC, ts.Void())),
        		nf.Try(pos, 
        				tryBlock, Collections.<Catch>emptyList(), 
        				finallyBlock));
    }

    protected Expr call(Position pos, Name name, Type returnType) throws SemanticException {
    	return synth.makeStaticCall(pos, ts.Runtime(), name, returnType, context());
    }

    protected Expr call(Position pos, Name name, Expr arg, Type returnType) throws SemanticException {
        return synth.makeStaticCall(pos, ts.Runtime(), name, Collections.singletonList(arg), returnType, context());
    }

    /**
     * Recognize the following pattern:
     * <pre>
     * @Immediate finish S;
     * </pre>
     * where <tt>S</tt> is any statement,
     * and compile it into S followed by an optimized remote fence operation.
     * @param f the finish statement
     * @return a block consisting of S followed by the invocation of a remote fence operation, or null if no match
     * @throws SemanticException
     * TODO: move into a separate pass!
     */
    private Stmt specializeFinish(Finish f) throws SemanticException {
        if (!Emitter.hasAnnotation(ts, f, IMMEDIATE))
            return null;
        Position pos = f.position();
        ClassType target = (ClassType) ts.typeForName(REMOTE_OPERATION);
        List<Expr> args = new ArrayList<Expr>();
        return nf.Block(pos, f.body(), nf.Eval(pos, synth.makeStaticCall(pos, target, FENCE, args, ts.Void(), context())));
    }
    
    private int getPatternFromAnnotation(AnnotationNode a){
    	Ref<? extends Type> r = a.annotationType().typeRef();
		X10ParsedClassType xpct = (X10ParsedClassType) r.getCached();
		List<Expr> allProperties = xpct.propertyInitializers();
		Expr pattern = allProperties.get(3);
		if (pattern instanceof IntLit_c) {
			return (int) ((IntLit_c) pattern).value();
		}
		return 0;
    }
    /**
     * Recognize the following pattern:
     * @FinishAsync(,,,"local") which means all asyncs in this finish are in the same place as finish
     * @param f
     * @return a method call expression that invokes "startLocalFinish"
     * @throws SemanticException
     */
    private Expr specializedFinish2(Finish f) throws SemanticException {
        Position pos = f.position();
    	int p=0;
        Type annotation = ts.systemResolver().findOne(QName.make("x10.compiler.FinishAsync"));
        if (!((X10Ext) f.ext()).annotationMatching(annotation).isEmpty()) {
        	List<AnnotationNode> allannots = ((X10Ext)(f.ext())).annotations();
        	AnnotationNode a = null;
        	int p1 = 0;
        	int p2 = 0;
        	if(allannots.size()>0){
				if (allannots.size() > 1) {
					boolean isConsistent = true;
					for(int i=0;i<allannots.size()-1;i++){
						p1 = getPatternFromAnnotation(allannots.get(i));
						p2 = getPatternFromAnnotation(allannots.get(i+1));
						if(p1 != p2){
							isConsistent = false;
							break;
						}
					}
					if(!isConsistent){
						reporter.report(0,"WARNING:compiler inferes different annotations from what the programer sets in "+job.source().name());
						if(reporter.should_report("verbose", 1)){
							reporter.report(5,"\tcompiler inferes "+p1);
							reporter.report(5,"\tprogrammer annotates "+p2);
						}
					}
				}
				a = allannots.get(allannots.size()-1);
				if(reporter.should_report("", 1)) 
					reporter.report(1,a.toString());
				p = getPatternFromAnnotation(a);
				
        	}else{
        		reporter.report(0,"annotation is not correct "+ allannots.size());
        	}
        }
        Type atype = ts.systemResolver().findOne(PRAGMA);
        List<X10ClassType> atypes  = ((X10Ext) f.ext()).annotationMatching(atype);
        if (!atypes.isEmpty()) {
            return call(pos, START_FINISH, atypes.get(0).propertyInitializer(0), ts.FinishState());
        }
        
        switch(p){
        case 1:return call(pos, START_LOCAL_FINISH, ts.FinishState());
        case 2:return call(pos, START_SIMPLE_FINISH, ts.FinishState());
        //TODO:more patterns can be filled here
        default:return call(pos, START_FINISH, ts.FinishState());
        }
    }

    // finish S; ->
    //    {
    //    Runtime.ensureNotInAtomic();
    //    val fresh = Runtime.startFinish();
    //    try { S; }
    //    catch (t:Throwable) { Runtime.pushException(t); throw new RuntimeException(); }
    //    finally { Runtime.stopFinish(fresh); }
    //    }
    private Stmt visitFinish(Finish f) throws SemanticException {
        Position pos = f.position();
        Name tmp = getTmp();

        Stmt specializedFinish = specializeFinish(f);
        if (specializedFinish != null)
            return specializedFinish;

        // TODO: merge with the call() function
        MethodInstance mi = ts.findMethod(ts.Runtime(),
                ts.MethodMatcher(ts.Runtime(), PUSH_EXCEPTION, Collections.singletonList(ts.Throwable()), context()));
        LocalDef lDef = ts.localDef(pos, ts.NoFlags(), Types.ref(ts.Throwable()), tmp);
        Formal formal = nf.Formal(pos, nf.FlagsNode(pos, ts.NoFlags()),
                nf.CanonicalTypeNode(pos, ts.Throwable()), nf.Id(pos, tmp)).localDef(lDef);
        Expr local = nf.Local(pos, nf.Id(pos, tmp)).localInstance(lDef.asInstance()).type(ts.Throwable());
        Expr call = nf.X10Call(pos, nf.CanonicalTypeNode(pos, ts.Runtime()),
                nf.Id(pos, PUSH_EXCEPTION), Collections.<TypeNode>emptyList(),
                Collections.singletonList(local)).methodInstance(mi).type(ts.Void());
        Throw thr = throwRuntimeException(pos);
        Expr startCall = specializedFinish2(f);

        Context xc = context();
        final Name varName = xc.getNewVarName();
        final Type type = ts.FinishState();
        final LocalDef li = ts.localDef(pos, ts.Final(), Types.ref(type), varName);
        final Id varId = nf.Id(pos, varName);
        final LocalDecl ld = nf.LocalDecl(pos, nf.FlagsNode(pos, ts.Final()), nf.CanonicalTypeNode(pos, type), varId, startCall).localDef(li).type(nf.CanonicalTypeNode(pos, type));
        final Local ldRef = (Local) nf.Local(pos, varId).localInstance(li.asInstance()).type(type);

        Block tryBlock = nf.Block(pos, f.body());
        Catch catchBlock = nf.Catch(pos, formal, nf.Block(pos, nf.Eval(pos, call), thr));
        Block finallyBlock = nf.Block(pos, nf.Eval(pos, call(pos, STOP_FINISH, ldRef, ts.Void())));

        Try tcfBlock = nf.Try(pos, tryBlock, Collections.singletonList(catchBlock), finallyBlock);

        // propagate async initialization info to backend
        X10Ext_c ext = (X10Ext_c) f.ext();
        if (ext.initVals != null) {
            tcfBlock = (Try)((X10Ext_c)tcfBlock.ext()).asyncInitVal(ext.initVals);
        }

        return nf.Block(pos,
        		nf.Eval(pos, call(pos, ENSURE_NOT_IN_ATOMIC, ts.Void())),
        		ld,
        		tcfBlock);
    }

    // Generates a throw of a new RuntimeException().
    private Throw throwRuntimeException(Position pos) throws SemanticException {
        Type re = ts.RuntimeException();
        X10ConstructorInstance ci = ts.findConstructor(re, ts.ConstructorMatcher(re, Collections.<Type>emptyList(), context()));
        Expr newRE = nf.New(pos, nf.CanonicalTypeNode(pos, re), Collections.<Expr>emptyList()).constructorInstance(ci).type(re);
        return nf.Throw(pos, newRE);
    }

    // x = finish (R) S; ->
    //    {
    //    val fresh = Runtime.startCollectingFinish(R);
    //    try { S; }
    //    catch (t:Throwable) { Runtime.pushException(t); throw new RuntimeException(); }
    //    finally { x = Runtime.stopCollectingFinish(fresh); }
    //    }
    private Stmt visitFinishExpr(Assign n, LocalDecl l, Return r) throws SemanticException {
    	FinishExpr f = null;
        if ((l==null) && (n!=null)&& (r == null)) {
                f = (FinishExpr) n.right();
        }
        if ((n==null) && (l!=null)&& (r==null)) {
                f = (FinishExpr) l.init();
        }
        if ((n==null) && (l==null)&& (r!=null)) {
                f = (FinishExpr) r.expr();
        }
    	
        Position pos = f.position();
        Expr reducer = f.reducer();
        
        // Begin Try Block Code
        Type reducerType = reducer.type();
        if (reducerType instanceof ConstrainedType) {
    		ConstrainedType ct = (ConstrainedType) reducerType;
    		reducerType = Types.baseType(Types.get(ct.baseType()));
        }

        // reducerType is "Reducible[T]", and reducerTarget is "T"
        // Parse out T
        Type reducerTarget = Types.reducerType(reducerType);
        assert reducerTarget!=null;
        
        Call myCall = synth.makeStaticCall(pos, ts.Runtime(), START_COLLECTING_FINISH, Collections.<TypeNode>singletonList(nf.CanonicalTypeNode(pos, reducerTarget)), Collections.singletonList(reducer), ts.Void(), Collections.singletonList(reducerType), context());

        Context xc = context();
        final Name varName = xc.getNewVarName();
        final Type type = ts.FinishState();
        final LocalDef li = ts.localDef(pos, ts.Final(), Types.ref(type), varName);
        final Id varId = nf.Id(pos, varName);
        final LocalDecl s1 = nf.LocalDecl(pos, nf.FlagsNode(pos, ts.Final()), nf.CanonicalTypeNode(pos, type), varId, myCall).localDef(li).type(nf.CanonicalTypeNode(pos, type));
        final Local ldRef = (Local) nf.Local(pos, varId).localInstance(li.asInstance()).type(type);

        Block tryBlock = nf.Block(pos,f.body());

        // Begin catch block
        Name tmp2 = getTmp();
        MethodInstance mi = ts.findMethod(ts.Runtime(),
                ts.MethodMatcher(ts.Runtime(), PUSH_EXCEPTION, Collections.singletonList(ts.Throwable()), context()));
        LocalDef lDef = ts.localDef(pos, ts.NoFlags(), Types.ref(ts.Throwable()), tmp2);
        Formal formal = nf.Formal(pos, nf.FlagsNode(pos, ts.NoFlags()),
                nf.CanonicalTypeNode(pos, ts.Throwable()), nf.Id(pos, tmp2)).localDef(lDef);
        Expr local = nf.Local(pos, nf.Id(pos, tmp2)).localInstance(lDef.asInstance()).type(ts.Throwable());
        Expr call = nf.X10Call(pos, nf.CanonicalTypeNode(pos, ts.Runtime()),
                nf.Id(pos, PUSH_EXCEPTION), Collections.<TypeNode>emptyList(),
                Collections.singletonList(local)).methodInstance(mi).type(ts.Void());
        Throw thr = throwRuntimeException(pos);
        Catch catchBlock = nf.Catch(pos, formal, nf.Block(pos, nf.Eval(pos, call), thr));
        
        // Begin finally block
        Stmt returnS = null;
        Call staticCall = synth.makeStaticCall(pos, ts.Runtime(), STOP_COLLECTING_FINISH, Collections.<TypeNode>singletonList(nf.CanonicalTypeNode(pos, reducerTarget)), Collections.<Expr>singletonList(ldRef), reducerTarget, Collections.<Type>singletonList(type), context());
        if ((l==null) && (n!=null)&& (r==null)) {
        	Expr left = n.left().type(reducerTarget);
            Expr b = synth.makeAssign(pos, left, Assign.ASSIGN, staticCall, xc);
            returnS = nf.Eval(pos, b);
        }
        if ((n==null) && (l!=null) && (r==null)) {
            Expr local2 = nf.Local(pos, l.name()).localInstance(l.localDef().asInstance()).type(reducerTarget);
         	Expr b = synth.makeAssign(pos, local2, Assign.ASSIGN, staticCall, xc);
            returnS = nf.Eval(pos, b);
        }
        if ((n==null) && (l==null) && (r!=null)) {
            returnS = nf.X10Return(pos, staticCall, true);
        }
        
        Block finalBlock = nf.Block(pos, returnS);
        if(reducerS.size()>0) reducerS.pop();
        return nf.Block(pos, s1, nf.Try(pos, tryBlock, Collections.singletonList(catchBlock), finalBlock));
    }

    //  offer e ->
    //  x10.lang.Runtime.offer(e);      
    private Stmt visitOffer(Offer n) throws SemanticException {		
    	Position pos = n.position();
    	Expr offerTarget = n.expr();
        Type expectType = null;
        if(reducerS.size()>0) {
            FinishExpr f = reducerS.peek();
            Expr reducer = f.reducer();
            Type reducerType = reducer.type();
            if (reducerType instanceof ConstrainedType) {
                ConstrainedType ct = (ConstrainedType) reducerType;
                reducerType = Types.baseType(Types.get(ct.baseType()));
            }
            X10ParsedClassType reducerTypeWithGenericType = null;
            Type thisType = reducerType;
            while(thisType != null) {
            //First check the reducerType itself is a reducible or not;
            //If not, it should be a class that implements reducible            
            if(ts.isReducible(((ClassType)thisType).def().asType())){
                //generic type case
                reducerTypeWithGenericType = (X10ParsedClassType) thisType;
                break;
            }
            else{ 
                //implement interface case
                for (Type t : ts.interfaces(thisType)) {
                    ClassType baseType = ((X10ParsedClassType)t).def().asType();
                    if(ts.isReducible(baseType)){
                        reducerTypeWithGenericType = (X10ParsedClassType) t;
                        break;
                    }
                }
            }
            thisType = ts.superClass(thisType);
            }
            
            assert(reducerTypeWithGenericType != null);
            //because Reducible type only has one argument, we could take it directly
            expectType = reducerTypeWithGenericType.typeArguments().get(0);
        }
        else {
            expectType = offerTarget.type();
        }
   	 
        CanonicalTypeNode CCE = nf.CanonicalTypeNode(pos, expectType);
        TypeNode reducerA = (TypeNode) CCE;
        Expr newOfferTarget = nf.X10Cast(pos, reducerA, offerTarget,Converter.ConversionType.CHECKED).type(reducerA.type());

    	Call call = synth.makeStaticCall(pos, ts.Runtime(), OFFER, Collections.singletonList(offerTarget), ts.Void(), Collections.singletonList(expectType),  context());
    	
    	Stmt offercall = nf.Eval(pos, call);     	
    	return offercall;		 
    }

    //handle finishR in return stmt:
    private Stmt visitReturn(Return n) throws SemanticException {
        if (n.expr() instanceof FinishExpr) {
            Stmt returnS = visitFinishExpr(null,null,n);
            return returnS;
        }

        return n;
    }

    private Stmt visitLocalDecl(LocalDecl n) throws SemanticException {
        if (n.init() instanceof FinishExpr) {
            Position pos = n.position();
            ArrayList<Stmt> sList = new ArrayList<Stmt>();
            sList.add(n.init(null));                      
            Stmt s = visitFinishExpr(null, n,null);
            sList.add(s);
            return nf.StmtSeq(pos, sList);
        }
      	return n;
    }

    // ateach (p in D) S; ->
    //    { Runtime.ensureNotInAtomic(); val d = D.dist; for (p in d.places()) async (p) for (pt in d|here) async S; }
    private Stmt visitAtEach(AtEach a) throws SemanticException {
        Position pos = a.position();
        Position bpos = a.body().position();
        Name tmp = getTmp();

        Expr domain = a.domain();
        Type dType = domain.type();
        if (ts.isX10DistArray(dType)) {
            FieldInstance fDist = dType.toClass().fieldNamed(DIST);
            dType = fDist.type();
            domain = nf.Field(pos, domain, nf.Id(pos, DIST)).fieldInstance(fDist).type(dType);
        }
        LocalDef lDef = ts.localDef(pos, ts.Final(), Types.ref(dType), tmp);
        LocalDecl local = nf.LocalDecl(pos, nf.FlagsNode(pos, ts.Final()),
                nf.CanonicalTypeNode(pos, dType), nf.Id(pos, tmp), domain).localDef(lDef);
        X10Formal formal = (X10Formal) a.formal();
        Type fType = formal.type().type();
        assert (ts.isPoint(fType));
        assert (ts.isDistribution(dType));
        // Have to desugar some newly-created nodes
        Type pType = ts.Place();
        MethodInstance rmi = ts.findMethod(dType,
                ts.MethodMatcher(dType, RESTRICTION, Collections.singletonList(pType), context()));
        Expr here = visitHere(nf.Here(bpos));
        Expr dAtPlace = nf.Call(bpos,
                nf.Local(pos, nf.Id(pos, tmp)).localInstance(lDef.asInstance()).type(dType),
                nf.Id(bpos, RESTRICTION),
                here).methodInstance(rmi).type(rmi.returnType());
        Expr here1 = visitHere(nf.Here(bpos));
        List<VarInstance<? extends VarDef>> env = a.atDef().capturedEnvironment();
        Stmt body = async(a.body().position(), a.body(), a.clocks(), here1, null, env);
        Stmt inner = nf.ForLoop(pos, formal, dAtPlace, body).locals(formal.explode(this));
        MethodInstance pmi = ts.findMethod(dType,
                ts.MethodMatcher(dType, PLACES, Collections.<Type>emptyList(), context()));
        Expr places = nf.Call(bpos,
                nf.Local(pos, nf.Id(pos, tmp)).localInstance(lDef.asInstance()).type(dType),
                nf.Id(bpos, PLACES)).methodInstance(pmi).type(pmi.returnType());
        Name pTmp = getTmp();
        LocalDef pDef = ts.localDef(pos, ts.Final(), Types.ref(pType), pTmp);
        Formal pFormal = nf.Formal(pos, nf.FlagsNode(pos, ts.Final()),
                nf.CanonicalTypeNode(pos, pType), nf.Id(pos, pTmp)).localDef(pDef);
        List<VarInstance<? extends VarDef>> env1 = new ArrayList<VarInstance<? extends VarDef>>(env);
        env1.remove(formal.localDef().asInstance());
        for (int i = 0; i < formal.localInstances().length; i++) {
            env1.remove(formal.localInstances()[i].asInstance());
        }
        env1.add(lDef.asInstance());
        Stmt body1 = async(bpos, inner, a.clocks(),
                nf.Local(bpos, nf.Id(bpos, pTmp)).localInstance(pDef.asInstance()).type(pType),
                null, env1);
        return nf.Block(pos, 
        		nf.Eval(pos, call(pos, ENSURE_NOT_IN_ATOMIC, ts.Void())),
        		local, 
        		nf.ForLoop(pos, pFormal, places, body1));
    }

    private Stmt visitEval(Eval n) throws SemanticException {
        Position pos = n.position();
        if ((n.expr() instanceof Assign)&&(flag==1)) {
            Assign f = (Assign) n.expr();
            Expr right = f.right();
            if (right instanceof FinishExpr)
                return visitFinishExpr(f, null,null);
        }
        return n;
    }
}
