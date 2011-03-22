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

package x10.types;

import java.util.ArrayList;
import java.util.List;

import polyglot.ast.Binary;
import polyglot.ast.Call;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.Formal;
import polyglot.ast.Lit;
import polyglot.ast.Local;
import polyglot.ast.Receiver;
import polyglot.ast.Term;
import polyglot.ast.TypeNode;
import polyglot.ast.Unary;
import polyglot.ast.Variable;
import polyglot.ast.Binary.Operator;
import polyglot.types.ClassDef;
import polyglot.types.CodeDef;
import polyglot.types.Context;
import polyglot.types.FieldInstance;
import polyglot.types.Flags;
import polyglot.types.LocalDef;
import polyglot.types.LocalInstance;
import polyglot.types.Name;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.Types;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import x10.ast.Here;
import x10.ast.ParExpr;
import x10.ast.SubtypeTest;
import x10.ast.Tuple;
import x10.ast.X10Cast;
import x10.ast.X10Field_c;
import x10.ast.X10Special;
import x10.ast.HasZeroTest;
import x10.constraint.XEQV;
import x10.constraint.XEquals;
import x10.constraint.XFailure;
import x10.constraint.XLit;
import x10.constraint.XLocal;
import x10.constraint.XUQV;
import x10.constraint.XVar;
import x10.constraint.XTerm;
import x10.constraint.XTerms;
import x10.constraint.XVar;
import x10.errors.Errors;
import x10.errors.Errors.IllegalConstraint;
import x10.types.checker.PlaceChecker;
import x10.types.constraints.CConstraint;
import x10.types.constraints.CConstraint;
import x10.types.constraints.CLocal;
import x10.types.constraints.CTerms;
import x10.types.constraints.SubtypeConstraint;
import x10.types.constraints.TypeConstraint;
import x10.types.constraints.XConstrainedTerm;
import x10.types.matcher.Subst;
import x10.util.Synthesizer;

/**
 * This is the bridge from Expr or TypeNode to a CConstraint. The CConstraint
 * generated may keep a reference to type objects obtained from the Expr or TypeNode.
 *  
 * @author nystrom
 * @author vj
 */
public class XTypeTranslator {
    public static final boolean THIS_VAR = true;

    private final TypeSystem ts;

    public XTypeTranslator(TypeSystem xts) {
        super();
        ts = xts;
    }

    //public static XTerm translate(CConstraint c, Receiver r, TypeSystem xts, Context xc)  {
    //    return xts.xtypeTranslator().translate(c, r, xc);
    //}
    
    /**
     * Translate the given AST term to an XTerm using information in the constraint
     * to resolve self, and using the context to determine AST type information 
     * (e.g. for Specials).
     * @param c --- a constraint used for context, e.g. self information. If term
     * is a this (e.g. this, Foo.this etc), then the constraint's this Var is set
     * on return. 
     * @param term -- the term to be translated
     * @param xc -- the context in which the term is to be translated
     * @return null if the translation is not possible. Caller must always check.
     * FIX: Remove the need for the constraint to be passed into translate.
     * 
     * If tolevel is true, then boolean connectives, && are permitted.
     */
    public XTerm translate(CConstraint c, Receiver term, Context xc)  throws IllegalConstraint {
    	return translate(c, term, xc, false);
    }
   public XTerm translate(CConstraint c, Receiver term, Context xc, boolean tl)  throws IllegalConstraint {
        if (term == null)
            return null;
        if (term instanceof Lit)
            return translate((Lit) term);
        if (term instanceof Here)
            return transHere();
        if (term instanceof Variable)
            return trans(c, (Variable) term, xc, tl);
        if (term instanceof X10Special)
            return trans(c, (X10Special) term, xc, tl);
        if (term instanceof Expr && ts.isUnknown(term.type()))
            return null;
        if (term instanceof Expr) {
            Expr e = (Expr) term;
            if (e.isConstant())
                return XTerms.makeLit(e.constantValue());
        }
        if (term instanceof X10Cast) {
            X10Cast cast = ((X10Cast) term);
            return translate(c, cast.expr().type(cast.type()), xc, tl);
        }
        if (term instanceof Call) {
            return trans(c, (Call) term, xc, tl);
        }
        if (term instanceof Tuple) {
            return trans(c, (Tuple) term, xc, tl);
        }
        if (term instanceof Unary) {
            Unary u = (Unary) term;
            Expr t2 = u.expr();
            Unary.Operator op = u.operator();
            if (op == Unary.POS)
                return translate(c, t2, xc, tl);
            return null; // no other unary operator supported
        }
        if (term instanceof Binary)
            return trans(c, (Binary) term, xc, tl);
        if (term instanceof TypeNode)
            return trans(c, (TypeNode) term);
        if (term instanceof ParExpr)
            return translate(c, ((ParExpr) term).expr(), xc, tl);
        return null;
    }


    /**
     * Return the term var.field, where information about the field is obtained from fi.
     * The returned term carries a reference to fi.def(), and field is fi.name().toString().
     * @param var
     * @param fi
     * @return
     */
    public XVar translate(XVar var, FieldInstance fi, boolean ignore) {
        // Warning -- used to have a string that did not contain container()#.
        return CTerms.makeField(var, fi.def());
    }

    /**
     * Return the term target.field, where information about the field is obtained from fi.
     * The returned term carriers a reference to fi.def(), and field is fi.name().toString().
     * Note that if target is a formula f, then the atom field(f) is returned instead.
     * @param target
     * @param fi
     * @return
     */
    public XTerm translate(XTerm target, FieldInstance fi) {
    	return translate(target, fi, false);
    }
    XTerm translate(XTerm target, FieldInstance fi, boolean ignore) {
        if (fi == null)
            return null;
        try {
            if (fi.flags().isStatic()) {
                Type container = Types.get(fi.def().container());
                container = Types.baseType(container);
                if (container instanceof X10ClassType) {
                    target  = XTerms.makeLit(((X10ClassType) container).fullName());
                }
                else {
                    throw new Errors.CannotTranslateStaticField(container, fi.position());
                }
            }
            XTerm v;
            if (target instanceof XVar) {
                v = CTerms.makeField((XVar) target, fi.def()); // hmm string was fi.name().toString(0 before.
            }
            else {
                // this is odd....?
                // TODO: Determine under what conditions is this path taken.
              
                v = CTerms.makeAtom(fi.def(), target);
            }
            return v;
        } catch (SemanticException z) {
            return null;
        }
    }


    /**
     * Return target.prop, where information about the property is obtained from fi.
     * It must be the case that fi corresponds to a property that takes no arguments. 
     *  Note that if target is a formula f, then the atom prop(f) is returned instead.
     * TODO: Determine why the name cant be just fi.name().toString().
     * @param target
     * @param fi
     * @return
     */
    public XTerm translate(XTerm target, MethodInstance mi) {
        assert mi.flags().isProperty() && mi.formalTypes().size() == 0;
     
        XTerm v;
        if (target instanceof XVar) {
            v = CTerms.makeField((XVar) target, mi.def());
        }
        else {
            // this is odd....?
            // TODO: Determine under what conditions is this path taken.
           // XName field = XTerms.makeName(mi.def(), Types.get(mi.def().container()) + "#" + mi.name().toString() + "()");
            v = CTerms.makeAtom(mi.def(), target);
        }
        return v;
    }

    public static final Object FAKE_KEY = new Object();
    
    /**
     * A fake field is one which exists purely for compilation purposes
     * and has no run-time existence. The main example is "home". It used to exist
     * for all objects, but is now used merely to track the location of the current
     * object statically.
     * @param target
     * @param name
     * @return
     */
    public XTerm translateFakeField(XTerm target, String name)  {
        return XTerms.makeFakeField((XVar) target, Name.make(name));
    }

    /** 
     * Return an XLocal which contains a reference to the type object li.def(),
     * and whose name is li.name().
     * @param li
     * @return
     */
     public CLocal translate(LocalInstance li) {
        return CTerms.makeLocal((X10LocalDef) li.def());
       
    }
     /**
      * Return an XLit representing the literal t.
      * @param t
      * @return
      */
    public XLit translate(Lit t) {
        return XTerms.makeLit(t.constantValue());
    }
    
    /**
     * Translate a type t into an XTerm. A type parameter
     * is translated into an XLocal. Other types are converted
     * into XTypeLit_c.
     * @param t
     * @return
     */
    public XTerm translate(Type t) {
        if (t instanceof ParameterType)
            return translateTypeParam((ParameterType) t);
        //  if (t instanceof X10ClassType)
        //      return transClassType((X10ClassType) t);
        //  if (t instanceof ConstrainedType)
        //      return transConstrainedType((ConstrainedType) t);
        if (t instanceof MacroType) {
            MacroType pt = (MacroType) t;
            return translate(pt.definedType());
        }
        return new XTypeLit(t);
        //  return XTerms.makeLit(t);
    }
    
    public XUQV translateTypeParam(ParameterType t) {
        return XTerms.makeUQV(t.toString()); //XTerms.makeLocal(XTerms.makeName(t));
    }

    
    /**
     * Translate into the literal t.
     * @param t
     * @return
     */
    public static XLit translate(int t) {
        return XTerms.makeLit(t);
    }

    /**
     * Translate into the literal t.
     * @param t
     * @return
     */
    public static XLit translate(boolean t) {
        return XTerms.makeLit(t);
    }

    /**
     * Return the XLit representing null.
     * @return
     */
    public static XLit transNull() {
        return XTerms.makeLit(null);
    }

    /**
     * A subclass of XLit that represents a Type literal.
     * @author vijay
     *
     */
    public final static class XTypeLit extends XLit {
        private static final long serialVersionUID = -1222245257474719757L;

        private XTypeLit(Type l) {
            super(l);
        }

        public Type type() {
            return (Type) val;
        }

        public boolean hasVar(XVar v) {
            return Types.hasVar(type(), v);
        }

        public XTypeLit subst(XTerm y, XVar x, boolean propagate) {
            XTypeLit n = (XTypeLit) super.subst(y, x, propagate);
            Type newVal = n.type();
            try {
                newVal = Subst.subst(type(), y, x);
            } catch (SemanticException e) { }
            if (newVal == n.type())
                return n;
            return new XTypeLit(newVal);
            //if (n == this) n = (XTypeLit) clone();
            //n.val = newVal;
            //return n;
        }
    }

  /*  public CConstraint normalize(CConstraint c, Context xc) {
        CConstraint result = new CConstraint();
        for (XTerm term : c.extConstraints()) {
            try {
                if (term instanceof XEquals) {
                    XEquals xt = (XEquals) term;
                    XTerm right = xt.right();
                    if (right instanceof XEquals) {
                        XEquals xright = (XEquals) right;
                        XTerm t1 = xright.left();
                        XTerm t2 = xright.right();
                        if (c.entails(t1, t2)) {
                            result.addBinding(xt.left(), XTerms.TRUE);
                        } else 
                            if (c.disEntails(t1, t2)) {
                                result.addBinding(xt.left(), XTerms.FALSE);
                            } else
                                result.addBinding(xt.left(), xt.right());
                    }
                } else 
                    result.addTerm(term);
            } catch (XFailure t) {

            }
        }
        return result;
    }
*/
    /**
     * Translate an expression into a CConstraint, throwing SemanticExceptions 
     * if this is not possible.
     * This must be called after type-checking of Expr.
     * @param formals TODO
     * @param term
     * @param xc TODO
     * @param c
     * @return
     * @throws SemanticException
     */
    public CConstraint constraint(List<Formal> ignore, Expr term, Context xc) throws SemanticException {
        CConstraint c = new CConstraint();
        if (term == null)
            return c;

        if (! term.type().isBoolean())
            throw new SemanticException("Cannot build constraint from expression |" 
            		+ term + "| of type " + term.type() + "; not a boolean.", term.position());

        // TODO: handle the formals.
        XTerm t= translate(c, term, xc, true);
        

        if (t == null)
            throw new SemanticException("Cannot build constraint from expression |" + term + "|.", term.position());

        try {
            c.addTerm(t);
        }
        catch (XFailure e) {
            c.setInconsistent();
        }
        return c;
    }

    public TypeConstraint typeConstraint(List<Formal> ignore, Expr term, Context xc) throws SemanticException {
        TypeConstraint c = new TypeConstraint();
        if (term == null)
            return c;

        if (! term.type().isBoolean())
            throw new SemanticException("Cannot build constraint from expression |"
            		+ term + "| of type " + term.type() + "; not a boolean.",
            		term.position());

        // TODO: handle the formals.

        transType(c, term, xc);

        return c;
    }

    

    public static boolean isPureTerm(Term t) {
        boolean result = false;
        if (t instanceof Variable) {
            Variable v = (Variable) t;
            result = v.flags().isFinal();
        }
        return result;
    }

    public XVar translateThisWithoutTypeConstraint() {
        XVar v = CTerms.THIS_THIS; // XTerms.makeLocal(XTerms.makeName("this"));
        return v;
    }

    /*public XLocal translateThis(Type t) throws SemanticException {
        XLocal v = translateThisWithoutTypeConstraint();
        return v;
    }*/


    public CConstraint binaryOp(Binary.Operator op, CConstraint cl, CConstraint cr) {
        return null; // none supported
    }

    public CConstraint unaryOp(Unary.Operator op, CConstraint ca) {
        return null; // none supported
    }
    
    // *********************************************************************************************
    // *********************************** private help routines for translation********************
    private XTerm transHere() {
        return PlaceChecker.here();
    }
    private XLocal trans(Local t) {
        return translate(t.localInstance());
    }

    private XTerm trans(CConstraint c, TypeNode t) {
        return translate(t.type());
    }

    private void transType(TypeConstraint c, Binary t, Context xc) throws SemanticException {
        Expr left = t.left();
        Expr right = t.right();
        XTerm v;

        if (t.operator() == Binary.COND_AND 
                || (t.operator() == Binary.BIT_AND 
                		&& ts.isImplicitCastValid(t.type(), ts.Boolean(), xc))) {
            transType(c, left, xc);
            transType(c, right, xc);
        }
        else {
            throw new SemanticException("Cannot translate " + t 
            		+ " into a type constraint.", t.position());
        }
    }

    private void transType(TypeConstraint c, Expr t, Context xc) throws SemanticException {
        if (t instanceof Binary) {
            transType(c, (Binary) t, xc);
        }
        else if (t instanceof ParExpr) {
            transType(c, ((ParExpr) t).expr(), xc);
        }
        else if (t instanceof SubtypeTest) {
            transType(c, (SubtypeTest) t, xc);
        } else if (t instanceof HasZeroTest) {
            transType(c, (HasZeroTest) t, xc);
        }
        else {
            throw new SemanticException("Cannot translate " + t 
            		+ " into a type constraint.", t.position());
        }
    }


    private void transType(TypeConstraint c, HasZeroTest t, Context xc) throws SemanticException {
        TypeNode left = t.parameter();
        c.addTerm(new SubtypeConstraint(left.type(), null, SubtypeConstraint.Kind.HASZERO));
    }
    private void transType(TypeConstraint c, SubtypeTest t, Context xc) throws SemanticException {
        TypeNode left = t.subtype();
        TypeNode right = t.supertype();
        c.addTerm(new SubtypeConstraint(left.type(), right.type(), t.equals()));
    }

    private XTerm simplify(Binary rb, XTerm v) {
        XTerm result = v;
        Expr r1 = rb.left();
        Expr r2  = rb.right();

        // Determine if their types force them to be equal or disequal.

        CConstraint c1 = Types.xclause(r1.type()).copy();
        XVar x = XTerms.makeUQV();
        c1.addSelfBinding(x);
        CConstraint c2 = Types.xclause(x, r2.type()).copy();
        if (rb.operator()== Binary.EQ) {
            c1.addIn(c2);
            if (! c1.consistent())
                result = XTerms.FALSE;
            if (c1.entails(c2) && c2.entails(c1)) {
                result = XTerms.TRUE;
            }
        }
        return result;
    }

    
    private XTerm trans(CConstraint c, Binary t, Context xc, boolean tl) throws IllegalConstraint {
        Expr left = t.left();
        Expr right = t.right();
        XTerm v = null;
      
        Operator op = t.operator();
        XTerm lt = translate(c, left, xc, op==Binary.COND_AND); // Not top-level, unless op==&&
        XTerm rt = translate(c, right, xc,op==Binary.COND_AND); // Not top-level, unless op==&&
        if (lt == null || rt == null)
            return null;
        if (op == Binary.EQ || op == Binary.NE) {
            if (right instanceof ParExpr) {
                right = ((ParExpr)right).expr();
            }
            if (right instanceof Binary && ((Binary) right).operator() == Binary.EQ) {
                rt = simplify((Binary) right, rt);
            }
            if (left instanceof Binary && ((Binary) right).operator() == Binary.EQ) {
                lt = simplify((Binary) left, lt);
            }

            v = op == Binary.EQ ? XTerms.makeEquals(lt, rt): XTerms.makeDisEquals(lt, rt);
        }
        else if (op == Binary.COND_AND 
                || (op == Binary.BIT_AND && ts.isImplicitCastValid(t.type(), ts.Boolean(), xc))) {
        	if (! tl)
        		throw new IllegalConstraint(t);
        	v = XTerms.makeAnd(lt, rt);
        }
        else if (op == Binary.IN) {
        	if (! tl)
        	 throw new IllegalConstraint(t);
              v = XTerms.makeAtom(t.operator(), lt, rt);
        }
        else  {
            v = XTerms.makeAtom(t.operator(), lt, rt);
            throw new IllegalConstraint(t);
           // return null;
        }
        return v;
    }

    private XTerm trans(CConstraint c, Tuple t, Context xc, boolean tl) throws IllegalConstraint {
        List<XTerm> terms = new ArrayList<XTerm>();
        for (Expr e : t.arguments()) {
            XTerm v = translate(c, e, xc, tl);
            if (v == null)
                return null;
            terms.add(v);
        }
        return XTerms.makeAtom("tuple", terms);
    }

    /**
     * This used to be a key routine that contained special code for handling at constraints.
     * It translates a call t into what the body of the called method would translate to,
     * assuming that the method represents a property.
     * @param c
     * @param t
     * @param xc
     * @return
     */
    private XTerm trans(CConstraint c, Call t, Context xc, boolean tl) throws IllegalConstraint {
        MethodInstance xmi = (MethodInstance) t.methodInstance();
        Flags f = xmi.flags();
        if (f.isProperty()) {
            XTerm r = translate(c, t.target(), xc, tl);
            if (r == null)
                return null;
            // FIXME: should just return the atom, and add atom==body to the real clause of the class
            // FIXME: fold in class's real clause constraints on parameters into real clause of type parameters
            XTerm body = xmi.body();

            if (body == null) {
                // hardwire s.at(t) for an interface
                // return s.home = t is Place ? t : t.home
                // stub out for orthogonal locality
                //body  = PlaceChecker.rewriteAtClause(c, xmi, t, r, xc);
            }
            if (body != null) {
                if (xmi.x10Def().thisVar() != null && t.target() instanceof Expr) {
                    //XName This = XTerms.makeName(new Object(), Types.get(xmi.def().container()) + "#this");
                    //body = body.subst(r, XTerms.makeLocal(This));
                    body = body.subst(r, xmi.x10Def().thisVar());
                }
                for (int i = 0; i < t.arguments().size(); i++) {
                    //XVar x = (XVar) X10TypeMixin.selfVarBinding(xmi.formalTypes().get(i));
                    //XVar x = (XVar) xmi.formalTypes().get(i);
                    XVar x =  CTerms.makeLocal((X10LocalDef) xmi.formalNames().get(i).def());
                    XTerm y = translate(c, t.arguments().get(i), xc, tl);
                    if (y == null)
                        assert y != null : "XTypeTranslator: translation of arg " + i + " of " + t + " yields null (pos=" 
                        + t.position() + ")";
                    body = body.subst(y, x);
                }
                return body;
            }

            if (t.arguments().size() == 0) {
              
                XTerm v;
                if (r instanceof XVar) {
                    v = CTerms.makeField((XVar) r, xmi.def());
                }
                else {
                    v = CTerms.makeAtom(xmi.def(), r);
                }
                return v;
            }
            List<XTerm> terms = new ArrayList<XTerm>();
            terms.add(r);
            for (Expr e : t.arguments()) {
                XTerm v = translate(c, e, xc, tl);
                if (v == null)
                    return null;
                terms.add(v);
            }
            XTerm v = CTerms.makeAtom(xmi.def(), terms);
            return v;
        }
        Type type = t.type();
        return Types.selfVarBinding(type); // maybe null.
    }

    private XTerm trans(CConstraint c, Variable term, Context xc, boolean tl) throws IllegalConstraint {
        if (term instanceof Field)
            return trans(c, (Field) term, xc, tl);
        if (term instanceof X10Special)
            return trans(c, (X10Special) term, xc, tl);
        if (term instanceof Local)
            return trans((Local) term);
        return null;
    }

    private XTerm trans(CConstraint c, X10Special t, Context xc0, boolean tl) {
        Context xc = xc0;
        if (t.kind() == X10Special.SELF) {
            if (c == null) {
                //throw new SemanticException("Cannot refer to self outside a dependent clause.");
                return null;
            }
            XVar v = (XVar) c.self().clone();
            return v;
        }
        else {
            TypeNode tn = t.qualifier();
            if (tn != null) {
                Type q = Types.baseType(tn.type());
                if (q instanceof X10ClassType) {
                    X10ClassType ct = (X10ClassType) q;
                    while (xc != null) {
                        if (xc.inSuperTypeDeclaration()) {
                            if (xc.supertypeDeclarationType() == ct.def())
                                break;
                        }
                        else if (xc.currentClassDef() == ct.def()) {
                            break;
                        }
                        xc = (Context) xc.pop();
                    }
                }
            }
            // why is this code not in X10Context_c.thisVar()?
            XVar thisVar = null;
            for (Context outer = xc; outer != null && thisVar == null; outer = outer.pop()) {
                thisVar = outer.thisVar();
            }
            if (thisVar == null) {
                SemanticException e = new SemanticException("Cannot refer to |this| from the context " + xc);
                return null;
            }
            // vj: Need to set the thisVar for the constraint.
            if (c != null)
                c.setThisVar(thisVar);
            return thisVar;
        }
    }
    private XTerm trans(CConstraint c, Field t, Context xc, boolean tl)  throws IllegalConstraint {
        XTerm receiver = translate(c, t.target(), xc, tl);
        if (receiver == null)
            return null;
        return translate(receiver, t.fieldInstance(), tl);
    }
}
