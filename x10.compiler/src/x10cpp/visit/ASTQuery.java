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

package x10cpp.visit;

import static x10cpp.visit.SharedVarsMethods.CPP_NATIVE_STRING;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import polyglot.ast.Binary;
import polyglot.ast.Block_c;
import polyglot.ast.BooleanLit;
import polyglot.ast.Call_c;
import polyglot.ast.Cast;
import polyglot.ast.Conditional;
import polyglot.ast.Eval;
import polyglot.ast.Eval_c;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.FieldAssign;
import polyglot.ast.Field_c;
import polyglot.ast.FloatLit;
import polyglot.ast.Formal;
import polyglot.ast.IntLit;
import polyglot.ast.Local;
import polyglot.ast.MethodDecl;
import polyglot.ast.Node;
import polyglot.ast.Receiver;
import polyglot.ast.Return;
import polyglot.ast.Stmt;
import polyglot.ast.TypeNode;
import polyglot.ast.Unary;
import polyglot.types.ClassType;
import polyglot.types.Name;
import polyglot.types.NoClassException;
import polyglot.types.QName;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.ErrorInfo;
import polyglot.util.InternalCompilerError;
import polyglot.visit.InnerClassRemover;
import polyglot.visit.Translator;
import x10.ast.Async_c;
import x10.ast.Closure;
import x10.ast.ClosureCall;
import x10.ast.ParExpr;
import x10.ast.X10CanonicalTypeNode_c;
import x10.extension.X10Ext;
import x10.types.X10ClassDef;
import x10.types.X10ClassType;
import x10.types.X10MethodDef;
import x10.types.X10MethodInstance;
import polyglot.types.TypeSystem;
import x10.types.X10TypeSystem_c;
import x10.util.HierarchyUtils;
import x10cpp.Configuration;
import x10cpp.types.X10CPPContext_c;

/**
 * Stateless Queries of various kinds against the AST. Extracted from X10PrettyPrinterVisitor.
 * 
 * @author vj
 *
 */

public class ASTQuery {

	private final Translator tr;
	public ASTQuery(Translator tr) {
		this.tr = tr;
	}

    boolean isSyntheticField(String name) {
		if (name.startsWith("jlc$")) return true;
		return false;
	}

    public boolean isSyntheticOuterAccessor(Stmt n) {
        if (n instanceof Eval && ((Eval)n).expr() instanceof FieldAssign) {
            FieldAssign init = (FieldAssign) ((Eval)n).expr();
            Field_c f = (Field_c)init.left();
            return f.fieldInstance().name().equals(InnerClassRemover.OUTER_FIELD_NAME);
        }
        return false;
    }

    public boolean isMainMethod(X10MethodDef md) {
        return HierarchyUtils.isMainMethod(md, tr.context());
    }

    boolean hasAnnotation(Node dec, String name) {
        return hasAnnotation((TypeSystem) tr.typeSystem(), dec, name);
    }

	public static boolean hasAnnotation(TypeSystem ts, Node dec, String name) {
		try {
			if (annotationNamed(ts, dec, name) != null)
				return true;
		} catch (NoClassException e) {
			if (!e.getClassName().equals(name))
				throw new InternalCompilerError("Something went terribly wrong", e);
		} catch (SemanticException e) {
			throw new InternalCompilerError("Something is terribly wrong", e);
		}
		return false;
	}

	public static X10ClassType annotationNamed(TypeSystem ts, Node o, String name) throws SemanticException {
		// Nate's code. This one.
		if (o.ext() instanceof X10Ext) {
			X10Ext ext = (X10Ext) o.ext();
			X10ClassType baseType = (X10ClassType) ts.systemResolver().find(QName.make(name));
			List<X10ClassType> ats = ext.annotationMatching(baseType);
			if (ats.size() > 1) {
				throw new SemanticException("Expression has more than one " + name + " annotation.", o.position());
			}
			if (!ats.isEmpty()) {
				X10ClassType at = ats.get(0);
				return at;
			}
		}
		return null;
	}

	boolean isClockMaker(Call_c n) {
		if (n.isTargetImplicit())
			return false;
		Receiver target = n.target();
		if (!(target instanceof Field_c))
			return false;
		Field_c f = (Field_c) target;
		if (!(f.target() instanceof X10CanonicalTypeNode_c))
			return false;
		X10CanonicalTypeNode_c t = (X10CanonicalTypeNode_c) f.target();
		TypeSystem xts = (TypeSystem) tr.typeSystem();
		if (!xts.isClock(t.type()) || !f.name().equals("factory"))
			return false;
		if (n.arguments().size() != 0)
			return false;
		if (!n.name().equals("clock"))
			return false;
		return true;
	}

	boolean isBlockDistMaker(Call_c n) {
		if (n.isTargetImplicit())
			return false;
		Receiver target = n.target();
		if (!(target instanceof Field_c))
			return false;
		Field_c f = (Field_c) target;
		if (!(f.target() instanceof X10CanonicalTypeNode_c))
			return false;
		X10CanonicalTypeNode_c t = (X10CanonicalTypeNode_c) f.target();
		X10TypeSystem_c xts = (X10TypeSystem_c) tr.typeSystem();
		if (!xts.isDistribution(t.type()) || !f.name().equals("factory"))
			return false;
		// TODO: detect other distribution constructors
		if (n.arguments().size() != 1)
			return false;
		if (!n.name().equals("block"))
			return false;
		return true;
	}

	boolean isPointMaker(Call_c n) {
		if (n.isTargetImplicit())
			return false;
		Receiver target = n.target();
		if (!(target instanceof Field_c))
			return false;
		Field_c f = (Field_c) target;
		if (!(f.target() instanceof X10CanonicalTypeNode_c))
			return false;
		X10CanonicalTypeNode_c t = (X10CanonicalTypeNode_c) f.target();
		TypeSystem xts = (TypeSystem) tr.typeSystem();
		if (!xts.isPoint(t.type()) || !f.name().equals("factory"))
			return false;
		if (n.arguments().size() < 1)
			return false;
		if (!n.name().equals("point"))
			return false;
		return true;
	}

	static final ArrayList<X10MethodInstance> knownAsyncArrayCopyMethods = new ArrayList<X10MethodInstance>();

	/* -- SPMD compilation --
	boolean isAsyncArrayCopy(Call_c n) {
		X10TypeSystem_c ts = (X10TypeSystem_c) tr.typeSystem();
		if (knownAsyncArrayCopyMethods.size() == 0) {
			X10CPPContext_c context = (X10CPPContext_c) tr.context();
			try {
				Type x_l_Runtime = (Type) ts.Runtime();
				Type Int = ts.Int();
				Type[] OA_I_P_I_I_B = { ts.array(ts.Object()), Int, ts.Place(), Int, Int, ts.Boolean() };
				knownAsyncArrayCopyMethods.add(ts.findMethod(x_l_Runtime, ts.MethodMatcher(x_l_Runtime, Name.make("asyncDoubleArrayCopy"), Arrays.asList(OA_I_P_I_I_B)), context.currentClassDef()));
			} catch (SemanticException e) { assert (false); }
		}
		if (!(n.target() instanceof X10CanonicalTypeNode_c))
			return false;
		X10CanonicalTypeNode_c target = (X10CanonicalTypeNode_c) n.target();
		if (!ts.typeEquals(target.type(), ts.Runtime()))
			return false;
		if (!knownAsyncArrayCopyMethods.contains(n.methodInstance()))
			return false;
		return true;
	}
	*/


	static final ArrayList<X10MethodInstance> knownArrayCopyMethods = new ArrayList<X10MethodInstance>();

	boolean isAsyncArrayCopy(Async_c n) {
		TypeSystem ts = (TypeSystem) tr.typeSystem();
		X10CPPContext_c context = (X10CPPContext_c) tr.context();
		if (knownArrayCopyMethods.size() == 0) {
			try {
				Type x_l_Runtime = (Type) ts.Runtime();
				Type array = ts.Array();
				Type Int = ts.Int();
				Type[] A_I_A_I_I = { array, Int, array, Int, Int };
				knownArrayCopyMethods.add(ts.findMethod(x_l_Runtime, ts.MethodMatcher(x_l_Runtime, Name.make("arrayCopy"), Arrays.asList(A_I_A_I_I), context)));
				// TODO
//				Type[] A_A = { array, array };
//				knownArrayCopyMethods.add(ts.findMethod(x_l_Runtime, "arrayCopy", Arrays.asList(A_A), context.currentClass()));
				// TODO
//				ReferenceType x_l_region = ts.region();
//				Type[] A_R_A_R = { array, x_l_region, array, x_l_region };
//				knownArrayCopyMethods.add(ts.findMethod(x_l_Runtime, "arrayCopy", Arrays.asList(A_R_A_R), context.currentClass()));
			} catch (SemanticException e) { assert (false); }
		}
		List<Expr> clocks = n.clocks();
		if (clocks != null && clocks.size() > 1)
			return false;
		Stmt body = n.body();
		if (body instanceof Block_c) {
			Block_c block = (Block_c) body;
			if (block.statements().size() != 1)
				return false;
			body = (Stmt) block.statements().get(0);
		}
		if (!(body instanceof Eval_c) || !(((Eval_c) body).expr() instanceof Call_c))
			return false;
		Call_c call = (Call_c) ((Eval_c) body).expr();
		if (!(call.target() instanceof X10CanonicalTypeNode_c))
			return false;
		X10CanonicalTypeNode_c target = (X10CanonicalTypeNode_c) call.target();
		if (!ts.typeEquals(target.type(), ts.Runtime(), context))
			return false;
		if (!knownArrayCopyMethods.contains(call.methodInstance()))
			return false;
		return true;
	}

	static int getConstructorId(X10CPPContext_c c) {
		return c.closureId();
	}

    /*
	static boolean outerClosure(X10CPPContext_c c) {
		return c.closures.nesting == 0;
	}
    */
	static ClassType getOuterClass(X10CPPContext_c c) {
		ClassType currentClass = c.currentClass();
		while (currentClass.isNested()) {
			currentClass = (ClassType) currentClass.container();
		}
		return currentClass;
	}


	public static String getCppRep(X10ClassDef def) {
		return getCppRepParam(def, 1);
	}
	public static String getCppBoxRep(X10ClassDef def) {
		return getCppRepParam(def, 2);
	}
	public static String getCppRTTRep(X10ClassDef def) {
		return getCppRepParam(def, 3);
	}
	public static String getCppRepParam(X10ClassDef def, int i) {
		try {
			TypeSystem xts = (TypeSystem) def.typeSystem();
			Type rep = (Type) xts.systemResolver().find(QName.make("x10.compiler.NativeRep"));
			List<Type> as = def.annotationsMatching(rep);
			for (Type at : as) {
				assertNumberOfInitializers(at, 4);
				String lang = getStringPropertyInit(at, 0);
				if (lang != null && lang.equals(CPP_NATIVE_STRING)) {
					return getStringPropertyInit(at, i);
				}
			}
		}
		catch (SemanticException e) {}
		return null;
	}

	public static void assertNumberOfInitializers(Type at, int len) {
	    at = Types.baseType(at);
	    if (at instanceof X10ClassType) {
	        X10ClassType act = (X10ClassType) at;
	        assert len == act.propertyInitializers().size();
	    }
	}

	public static String getStringPropertyInit(Type at, int index) {
	    Object v = getPropertyInit(at, index);
	    if (v instanceof String)
	        return (String) v;
	    else if (v != null)
	        return v.toString();
	    return null;
	}

	public static Object getPropertyInit(Type at, int index) {
	    at = Types.baseType(at);
	    if (at instanceof X10ClassType) {
	        X10ClassType act = (X10ClassType) at;
	        if (index < act.propertyInitializers().size()) {
	            Expr e = act.propertyInitializer(index);
	            if (e.isConstant()) {
	                return e.constantValue();
	            }
	        }
	    }
	    return null;
	}

	/**
	 * Returns true if e is a constant expression.
	 */
	boolean isConstantExpression(Expr e) {
	    if (!e.isConstant())
	        return false;
	    if (e instanceof BooleanLit)
	        return true;
	    if (e instanceof IntLit)
	        return true;
	    if (e instanceof FloatLit)
	        return true;
	    if (e instanceof Cast)
	        return isConstantExpression(((Cast) e).expr());
	    if (e instanceof ParExpr)
	        return isConstantExpression(((ParExpr) e).expr());
	    if (e instanceof Unary)
	        return isConstantExpression(((Unary) e).expr());
	    if (e instanceof Binary)
	        return isConstantExpression(((Binary) e).left()) &&
	               isConstantExpression(((Binary) e).right());
	    if (e instanceof Conditional)
	        return isConstantExpression(((Conditional) e).cond()) &&
	               isConstantExpression(((Conditional) e).consequent()) &&
	               isConstantExpression(((Conditional) e).alternative());
	    if (e instanceof Closure) {
	        Closure c = (Closure) e;
	        List<Stmt> ss = c.body().statements();
            if (ss.size() != 1)
	            return false;
	        if (!(ss.get(0) instanceof Return))
	            return false;
	        return isConstantExpression(((Return) ss.get(0)).expr());
	    }
	    if (e instanceof ClosureCall) {
	        ClosureCall cc = (ClosureCall) e;
	        List<Expr> as = ((ClosureCall) e).arguments();
	        for (Expr a : as) {
	            if (!isConstantExpression(a))
	                return false;
            }
	        return isConstantExpression(cc.target());
	    }
	    return false;
	}

	/**
	 * Returns true if e can be evaluated more than once with the same result.
	 */
	boolean isIdempotent(Expr e) {
	    if (e instanceof Local)
	        return true;
	    if (e instanceof ParExpr)
	        return isIdempotent(((ParExpr) e).expr());
	    if (e instanceof Field) {
	        Receiver target = ((Field) e).target();
	        if (!(target instanceof Expr))
	            return (target instanceof TypeNode);
	        return isIdempotent((Expr) target);
	    }
	    if (e instanceof Cast)
	        return isIdempotent(((Cast) e).expr());
	    return isConstantExpression(e);
	}
}
