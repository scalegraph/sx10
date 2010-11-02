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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import polyglot.ast.Binary;
import polyglot.ast.Call;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.IntLit;
import polyglot.ast.Local;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.TypeNode;
import polyglot.ast.Unary;
import polyglot.ast.Unary_c;
import polyglot.ast.Variable;
import polyglot.types.ClassDef;
import polyglot.types.ClassType;
import polyglot.types.Flags;
import polyglot.types.MethodDef;
import polyglot.types.MethodInstance;
import polyglot.types.Name;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.Types;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import x10.errors.Errors;
import x10.types.X10MethodInstance;
import x10.types.X10TypeMixin;
import x10.types.X10TypeSystem;
import x10.types.X10TypeSystem_c;
import x10.types.checker.Checker;
import x10.types.checker.Converter;
import x10.types.checker.PlaceChecker;
import x10.visit.X10TypeChecker;

/**
 * An immutable representation of a unary operation op Expr.
 * Overridden from Java to allow unary negation of points.
 *
 * @author igor Feb 15, 2006
 */
public class X10Unary_c extends Unary_c {

    /**
     * @param pos
     * @param op
     * @param expr
     */
    public X10Unary_c(Position pos, Operator op, Expr expr) {
        super(pos, op, expr);
    }

    // TODO: take care of constant points.
    public Object constantValue() {
        return super.constantValue();
    }

    public static Binary.Operator getBinaryOp(Unary.Operator op) {
        if (op == PRE_INC || op == POST_INC) {
            return Binary.ADD;
        } else if (op == PRE_DEC || op == POST_DEC) {
            return Binary.SUB;
        }
        return null;
    }

    /**
     * Type check a unary expression. Must take care of various cases because
     * of operators on regions, distributions, points, places and arrays.
     * An alternative implementation strategy is to resolve each into a method
     * call.
     */
    public Node typeCheck(ContextVisitor tc) {
        X10TypeSystem ts = (X10TypeSystem) tc.typeSystem();
        NodeFactory nf = (NodeFactory) tc.nodeFactory();
        Unary.Operator op = this.operator();

        if (op == NEG && expr instanceof IntLit) {
            IntLit.Kind kind = 
                ((IntLit) expr).kind();
            if (kind == IntLit.INT || kind == X10IntLit_c.UINT)
                kind = IntLit.INT;
            else
                kind = IntLit.LONG;
            IntLit lit = nf.IntLit(position(), kind, -((IntLit) expr).longValue());
            try {
                return Converter.check(lit, tc);
            } catch (SemanticException e) {
                throw new InternalCompilerError("Unexpected exception when typechecking "+lit, lit.position(), e);
            }
        }

        Type t = expr.type();

        if (op == POST_INC || op == POST_DEC || op == PRE_INC || op == PRE_DEC) {
            // Compute the type and the expected type
            Type et = t;
            if (expr instanceof Variable) {
                Variable v = (Variable) expr;
                if (v.flags().isFinal()) {
                    Errors.issue(tc.job(),
                            new SemanticException("Cannot apply " + op + " to a final variable.", position()));
                }
            }
            else {
                Expr target = null;
                List<TypeNode> typeArgs = null;
                List<Expr> args = null;

                // Handle a(i)++ and a.apply(i)++
                if (expr instanceof ClosureCall) {
                    ClosureCall e = (ClosureCall) expr;
                    target = e.target();
                    typeArgs = e.typeArguments();
                    args = e.arguments();
                }
                else if (expr instanceof X10Call) {
                    X10Call e = (X10Call) expr;
                    if (!(e.target() instanceof Expr) || e.name().id() != ClosureCall.APPLY) {
                        Errors.issue(tc.job(),
                                new SemanticException("Cannot apply " + op + " to an arbitrary method call.", position()));
                        t = ts.unknownType(position());
                        et = null;
                    } else {
                        target = (Expr) e.target();
                        typeArgs = e.typeArguments();
                        args = e.arguments();
                    }
                } else {
                    Errors.issue(tc.job(),
                            new SemanticException("Cannot apply " + op + " to an arbitrary expression.", position()));
                    t = ts.unknownType(position());
                    et = null;
                }

                if (target != null) {
                    List<Type> tArgs = new ArrayList<Type>();
                    for (TypeNode tn : typeArgs) {
                        tArgs.add(tn.type());
                    }
                    List<Type> actualTypes = new ArrayList<Type>();
                    // value goes before args
                    actualTypes.add(t);
                    for (Expr a : args) {
                        actualTypes.add(a.type());
                    }
                    X10MethodInstance mi = Checker.findAppropriateMethod(tc, target.type(), SettableAssign.SET, tArgs, actualTypes);
                    if (mi.error() != null) {
                        Errors.issue(tc.job(), new SemanticException("Unable to perform operation", mi.error()), this);
                    }
                    // Make sure we don't coerce here.
                    List<Type> fTypes = mi.formalTypes();
                    for (int i = 0; i < actualTypes.size(); i++) {
                        if (!ts.isSubtype(actualTypes.get(i), fTypes.get(i), tc.context()))
                            Errors.issue(tc.job(),
                                    new SemanticException("No "+SettableAssign.SET+" method found in " + target.type(), position()));
                    }
                    t = mi.returnType();
                    et = fTypes.get(0);
                }
            }

            if (et != null) {
                // Check that there's a binary operator with the right return type
                IntLit lit = nf.IntLit(position(), IntLit.INT, 1);
                try {
                    lit = Converter.check(lit, tc);
                } catch (SemanticException e) {
                    throw new InternalCompilerError("Unexpected exception when typechecking "+lit, lit.position(), e);
                }
                Binary.Operator binaryOp = getBinaryOp(op);
                Call c = X10Binary_c.desugarBinaryOp(nf.Binary(position(), expr, binaryOp, lit), tc);
                if (c == null) {
                    Errors.issue(tc.job(),
                            new SemanticException("No binary operator " + binaryOp + " found in type " + t, expr.position()));
                } else {
                    X10MethodInstance mi = (X10MethodInstance) c.methodInstance();
                    if (mi.error() != null) {
                        Errors.issue(tc.job(), new SemanticException("Unable to perform operation", mi.error()), this);
                    }
                    Type resultType = mi.returnType();
                    if (!ts.isSubtype(resultType, et, tc.context())) {
                        Errors.issue(tc.job(),
                                new SemanticException("Incompatible return type of binary operator "+binaryOp+" found:\n\t operator return type: " + resultType + "\n\t expression type: "+et, expr.position()));
                    }
                }
            }

            return this.type(t);
        }

        Call c = desugarUnaryOp(this, tc);
        if (c != null) {
            X10MethodInstance mi = (X10MethodInstance) c.methodInstance();
            if (mi.error() != null) {
                Errors.issue(tc.job(), mi.error(), this);
            }
            // rebuild the unary using the call's arguments.  We'll actually use the call node after desugaring.
            Type resultType = c.type();
            resultType = ts.performUnaryOperation(resultType, t, op);
            if (mi.flags().isStatic()) {
                return this.expr(c.arguments().get(0)).type(resultType);
            }
            else {
                return this.expr((Expr) c.target()).type(resultType);
            }
        }

        if (!ts.hasUnknown(t)) {
            Errors.issue(tc.job(),
                    new SemanticException("No operation " + op + " found for operand " + t + ".", position()));
        }

        return this.type(ts.unknownType(position()));
    }

    public static X10Call_c desugarUnaryOp(Unary n, ContextVisitor tc) {
        Expr left = n.expr();
        Unary.Operator op = n.operator();
        Position pos = n.position();

        Type l = left.type();

        NodeFactory nf = (NodeFactory) tc.nodeFactory();
        Name methodName = unaryMethodName(op);

        if (methodName == null) return null;

        // TODO: byte+byte should convert both bytes to int and search int
        // For now, we have to define byte+byte in byte.x10.

        X10Call_c virtual_left = null;
        X10Call_c static_left = null;

        if (methodName != null) {
            // Check if there is a method with the appropriate name and type with the operand as receiver.   
            X10Call_c n2 = (X10Call_c) nf.X10Call(pos, left, nf.Id(pos, methodName), Collections.<TypeNode>emptyList(), Collections.<Expr>emptyList());
            n2 = X10Binary_c.typeCheckCall(tc, n2);
            X10MethodInstance mi2 = (X10MethodInstance) n2.methodInstance();
            if (mi2.error() == null && !mi2.def().flags().isStatic())
                virtual_left = n2;
        }

        if (methodName != null) {
            // Check if there is a static method of the left type with the appropriate name and type.   
            X10Call_c n4 = (X10Call_c) nf.X10Call(pos, nf.CanonicalTypeNode(pos, Types.ref(l)), nf.Id(pos, methodName), Collections.<TypeNode>emptyList(), Collections.singletonList(left));
            n4 = X10Binary_c.typeCheckCall(tc, n4);
            X10MethodInstance mi4 = (X10MethodInstance) n4.methodInstance();
            if (mi4.error() == null && mi4.def().flags().isStatic())
                static_left = n4;
        }

        List<X10Call_c> defs = new ArrayList<X10Call_c>();
        if (virtual_left != null) defs.add(virtual_left);
        if (static_left != null) defs.add(static_left);

        if (defs.size() == 0) return null;

        X10TypeSystem_c xts = (X10TypeSystem_c) tc.typeSystem();

        List<X10Call_c> best = new ArrayList<X10Call_c>();
        X10Binary_c.Conversion bestConversion = X10Binary_c.Conversion.UNKNOWN;

        for (int i = 0; i < defs.size(); i++) {
            X10Call_c n1 = defs.get(i);

            // Check if n needs a conversion
            Expr[] actuals = new Expr[] {
                n1.arguments().size() != 1 ? (Expr) n1.target() : n1.arguments().get(0)
            };
            Expr[] original = new Expr[] { left };
            X10Binary_c.Conversion conversion = X10Binary_c.conversionNeeded(actuals, original);

            if (bestConversion.harder(conversion)) {
                best.clear();
                best.add(n1);
                bestConversion = conversion;
            }
            else if (conversion.harder(bestConversion)) {
                // best is still the best
            }
            else {  // all other things being equal
                MethodDef md = n1.methodInstance().def();
                Type td = Types.get(md.container());
                ClassDef cd = X10Binary_c.def(td);

                for (X10Call_c c : best) {
                    MethodDef bestmd = c.methodInstance().def();
                    assert (bestmd != md) : pos.toString();
                    if (bestmd == md) continue;  // same method by a different path (shouldn't happen for unary)

                    Type besttd = Types.get(bestmd.container());
                    if (xts.isUnknown(besttd) || xts.isUnknown(td)) {
                        best.add(n1);
                        continue;
                    }

                    ClassDef bestcd = X10Binary_c.def(besttd);
                    assert (bestcd != null && cd != null);

                    if (xts.descendsFrom(cd, bestcd)) {
                        best.clear();
                        best.add(n1);
                        bestConversion = conversion;
                    }
                    else if (xts.descendsFrom(bestcd, cd)) {
                        // best is still the best
                    }
                    else {
                        best.add(n1);
                    }
                }
            }
        }
        assert (best.size() != 0);

        X10Call_c result = best.get(0);
        if (best.size() > 1) {
            List<MethodInstance> bestmis = new ArrayList<MethodInstance>();
            Type rt = null;
            boolean rtset = false;
            ClassType ct = null;
            boolean ctset = false;
            // See if all matches have the same container and return type, and save that to avoid losing information.
            for (X10Call_c c : best) {
                MethodInstance xmi = c.methodInstance();
                bestmis.add(xmi);
                if (!rtset) {
                    rt = xmi.returnType();
                    rtset = true;
                } else if (rt != null && !xts.typeEquals(rt, xmi.returnType(), tc.context())) {
                    if (xts.typeBaseEquals(rt, xmi.returnType(), tc.context())) {
                        rt = X10TypeMixin.baseType(rt);
                    } else {
                        rt = null;
                    }
                }
                if (!ctset) {
                    ct = xmi.container().toClass();
                    ctset = true;
                } else if (ct != null && !xts.typeEquals(ct, xmi.container(), tc.context())) {
                    if (xts.typeBaseEquals(ct, xmi.container(), tc.context())) {
                        ct = X10TypeMixin.baseType(ct).toClass();
                    } else {
                        ct = null;
                    }
                }
            }
            if (ct == null) ct = l.toClass();
            SemanticException error = new Errors.AmbiguousOperator(op, bestmis, pos);
            X10MethodInstance mi = xts.createFakeMethod(ct, Flags.PUBLIC.Static(), methodName, Collections.<Type>emptyList(), Collections.singletonList(l), error);
            if (rt != null) mi = mi.returnType(rt);
            result = (X10Call_c) nf.X10Call(pos, nf.CanonicalTypeNode(pos, Types.ref(ct)),
                    nf.Id(pos, methodName), Collections.<TypeNode>emptyList(),
                    Collections.singletonList(left)).methodInstance(mi).type(mi.returnType());
        }
        try {
            result = (X10Call_c) PlaceChecker.makeReceiverLocalIfNecessary(result, tc);
        } catch (SemanticException e) {
            X10MethodInstance mi = (X10MethodInstance) result.methodInstance();
            if (mi.error() == null)
                result = (X10Call_c) result.methodInstance(mi.error(e));
        }
        if (n.isConstant())
            result = result.constantValue(n.constantValue());
        return result;
    }

    public static Name unaryMethodName(Unary.Operator op) {
        Map<Unary.Operator,String> methodNameMap = new HashMap<Unary.Operator, String>();
        methodNameMap.put(NEG, "operator-");
        methodNameMap.put(POS, "operator+");
        methodNameMap.put(NOT, "operator!");
        methodNameMap.put(BIT_NOT, "operator~");

        String methodName = methodNameMap.get(op);
        if (methodName == null)
            return null;
        return Name.make(methodName);
    }
}

