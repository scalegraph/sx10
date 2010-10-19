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
import java.util.List;

import polyglot.ast.Block;
import polyglot.ast.Conditional;
import polyglot.ast.Empty;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.FloatLit;
import polyglot.ast.If;
import polyglot.ast.IntLit;
import polyglot.ast.Lit;
import polyglot.ast.LocalDecl;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Stmt;
import polyglot.frontend.Job;
import polyglot.types.LocalDef;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import x10.ast.X10NodeFactory;
import x10.constraint.XFailure;
import x10.constraint.XLit;
import x10.constraint.XLocal;
import x10.constraint.XVar;
import x10.constraint.XTerm;
import x10.constraint.XTerms;
import x10.types.X10TypeMixin;
import x10.types.checker.Converter;
import x10.types.constraints.CConstraint;
import x10.types.constraints.Constraints;

/**
 * Very simple constant propagation pass. 
 * <p> If an expr has a constant value, replace it with the value. 
 * 
 * <p> Replace branches on constants with the consequent or alternative as appropriate. 
 * 
 * <p> TODO: Handle constant rails and conversions.
 * <p> TODO: Propagate through rails A(0) = v; ... A(0) --> v. TODO: Dead code
 * elimination. visitor.
 * 
 * FIXME: [IP] propagate closure literals
 * 
 * @author nystrom
 */
public class ConstantPropagator extends ContextVisitor {
    public ConstantPropagator(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
    }
    
    @Override
    protected Node leaveCall(Node parent, Node old, Node n, NodeVisitor v) {
        Position pos = n.position();

        if (n instanceof Expr || n instanceof Stmt) {
        }
        else {
            return n;
        }
        
        if (n instanceof LocalDecl) {
            LocalDecl d = (LocalDecl) n;
            if (d.flags().flags().isFinal() && d.init() != null && isConstant(d.init())) {
                d.localDef().setConstantValue(constantValue(d.init()));
                return nf.Empty(d.position());
            }
        }

        if (n instanceof Lit) {
            return n;
        }

        if (n instanceof Conditional) {
            Conditional c = (Conditional) n;
            Expr cond = c.cond();
            if (isConstant(cond)) {
                boolean b = (boolean) (Boolean) constantValue(cond);
                if (b)
                    return c.consequent();
                else
                    return c.alternative();
            }
        }

        if (n instanceof If) {
            If c = (If) n;
            Expr cond = c.cond();
            if (isConstant(cond)) {
                Object o = constantValue(cond);
                if (o instanceof Boolean) {
                    boolean b = (boolean) (Boolean) o;
                    if (b)
                        return c.consequent();
                    else
                        return c.alternative() != null ? c.alternative() : nf.Empty(pos);
                }
            }
        }

        if (n instanceof Expr) {
            Expr e = (Expr) n;
            if (isConstant(e)) {
                Object o = constantValue(e);
                Expr result = toExpr(o, e.position());
                if (result != null)
                    return result;
            }
        }
        
        if (n instanceof Block) {
            Block b = (Block) n;
            List<Stmt> ss = new ArrayList<Stmt>();
            for (Stmt s : b.statements()) {
                if (s instanceof Empty) {
                }
                else {
                    ss.add(s);
                }
            }
            if (ss.size() != b.statements().size())
                return b.statements(ss);
        }

        return n;
    }

    public static Object constantValue(Expr e) {
        if (e.isConstant())
            return e.constantValue();
        
        if (e.type().isNull())
            return null;

        if (e instanceof Field) {
        	Field f = (Field) e;
        	if (f.target() instanceof Expr) {
        		Expr target = (Expr) f.target();
        		Type t = target.type();
        		CConstraint c = X10TypeMixin.xclause(t);
        		if (c != null) {
        			XTerm val = c.bindingForSelfField(f);
        			if (val instanceof XLit) {
        				XLit l = (XLit) val;
        				return l.val();
        			}
        		}
        	}
        }

        Type t = e.type();
        CConstraint c = X10TypeMixin.xclause(t);
        if (c != null) {
            XVar r = c.self();
            if (r instanceof XLit) {
                XLit l = (XLit) r;
                return l.val();
            }
        }
        return null;
    }

    public static boolean isConstant(Expr e) {
        if (e.isConstant())
            return true;

        if (e.type().isNull())
            return true;
        
        if (e instanceof Field) {
            Field f = (Field) e;
            if (f.target() instanceof Expr) {
                Expr target = (Expr) f.target();
                Type t = target.type();
                CConstraint c = X10TypeMixin.xclause(t);
                if (c != null) {
                	XTerm val = c.bindingForSelfField(f);
                	if (val instanceof XLit) {
                		return true;
                	}
                }
            }
        }

        Type t = e.type();
        CConstraint c = X10TypeMixin.xclause(t);
        if (c != null) {
            XVar r = c.self();
            if (r instanceof XLit) {
                XLit l = (XLit) r;
                return true;
            }
        }
        return false;
    }

    public Expr toExpr(Object o, Position pos) {
        X10NodeFactory nf = (X10NodeFactory) this.nf;

        Expr e = null;
        if (o == null) {
            e = nf.NullLit(pos);
        } else
        if (o instanceof Integer) {
            e = nf.IntLit(pos, IntLit.INT, (long) (int) (Integer) o);
        } else
        if (o instanceof Long) {
            e = nf.IntLit(pos, IntLit.LONG, (long) (Long) o);
        } else
        if (o instanceof Float) {
            e = nf.FloatLit(pos, FloatLit.FLOAT, (double) (float) (Float) o);
        } else
        if (o instanceof Double) {
            e = nf.FloatLit(pos, FloatLit.DOUBLE, (double) (Double) o);
        } else
        if (o instanceof Character) {
            e = nf.CharLit(pos, (char) (Character) o);
        } else
        if (o instanceof Boolean) {
            e = nf.BooleanLit(pos, (boolean) (Boolean) o);
        } else
        if (o instanceof String) {
            e = nf.StringLit(pos, (String) o);
        } else
        if (o instanceof Object[]) {
            Object[] a = (Object[]) o;
            List<Expr> args = new ArrayList<Expr>(a.length);
            for (Object ai : args) {
                Expr ei = toExpr(ai, pos);
                if (ei == null)
                    return null;
                args.add(ei);
            }
            e = nf.Tuple(pos, args);
        }
        try {
            if (e != null) e = Converter.check(e, this);
        } catch (SemanticException cause) {
            throw new InternalCompilerError("Unexpected exception when typechecking "+e, e.position(), cause);
        }
        return e;
    }

}
