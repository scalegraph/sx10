/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.*;

import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;

/**
 * A <code>Call</code> is an immutable representation of a Java
 * method call.  It consists of a method name and a list of arguments.
 * It may also have either a Type upon which the method is being
 * called or an expression upon which the method is being called.
 */
public class Call_c extends Expr_c implements Call
{
  protected Receiver target;
  protected Id name;
  protected List<Expr> arguments;
  protected MethodInstance mi;
  protected boolean targetImplicit;

  public Call_c(Position pos, Receiver target, Id name,
                List<Expr> arguments) {
    super(pos);
    assert(name != null && arguments != null); // target may be null
    this.target = target;
    this.name = name;
    this.arguments = TypedList.copyAndCheck(arguments, Expr.class, true);
    this.targetImplicit = (target == null);
  }

  /** Get the precedence of the call. */
  public Precedence precedence() {
    return Precedence.LITERAL;
  }

  /** Get the target object or type of the call. */
  public Receiver target() {
    return this.target;
  }

  /** Set the target object or type of the call. */
  public Call target(Receiver target) {
    Call_c n = (Call_c) copy();
    n.target = target;
    return n;
  }
  
  /** Get the name of the call. */
  public Id name() {
      return this.name;
  }
  
  /** Set the name of the call. */
  public Call name(Id name) {
      Call_c n = (Call_c) copy();
      n.name = name;
      return n;
  }

  public MethodInstance procedureInstance() {
      return methodInstance();
  }

  /** Get the method instance of the call. */
  public MethodInstance methodInstance() {
    return this.mi;
  }

  public Call procedureInstance(ProcedureInstance<? extends ProcedureDef> pi) {
      return methodInstance((MethodInstance) pi);
  }

  /** Set the method instance of the call. */
  public Call methodInstance(MethodInstance mi) {
    if (mi == this.mi) return this;
    Call_c n = (Call_c) copy();
    n.mi = mi;
    return n;
  }

  public boolean isTargetImplicit() {
      return this.targetImplicit;
  }

  public Call targetImplicit(boolean targetImplicit) {
      if (targetImplicit == this.targetImplicit) {
          return this;
      }
      
      Call_c n = (Call_c) copy();
      n.targetImplicit = targetImplicit;
      return n;
  }

  /** Get the actual arguments of the call. */
  public List<Expr> arguments() {
    return this.arguments;
  }

  /** Set the actual arguments of the call. */
  public Call arguments(List<Expr> arguments) {
    Call_c n = (Call_c) copy();
    n.arguments = TypedList.copyAndCheck(arguments, Expr.class, true);
    return n;
  }

  /** Reconstruct the call. */
  protected Call_c reconstruct(Receiver target, Id name, List<Expr> arguments) {
    if (target != this.target || name != this.name || ! CollectionUtil.allEqual(arguments,
                                                         this.arguments)) {
      Call_c n = (Call_c) copy();
      
      // If the target changes, assume we want it to be an explicit target.
      n.targetImplicit = n.targetImplicit && target == n.target;
      
      n.target = target;
      n.name = name;
      n.arguments = TypedList.copyAndCheck(arguments, Expr.class, true);
      return n;
    }

    return this;
  }

  /** Visit the children of the call. */
  public Node visitChildren(NodeVisitor v) {
      Receiver target = (Receiver) visitChild(this.target, v);
      Id name = (Id) visitChild(this.name, v);
      List<Expr> arguments = visitList(this.arguments, v);
      return reconstruct(target, name, arguments);
  }

  public Node buildTypes(TypeBuilder tb) throws SemanticException {
    Call_c n = (Call_c) super.buildTypes(tb);

    TypeSystem ts = tb.typeSystem();

    MethodInstance mi = ts.createMethodInstance(position(), new ErrorRef_c<MethodDef>(ts, position(), "Cannot get MethodDef before type-checking method invocation."));
    return n.methodInstance(mi);
  }
  
  /**
     * Typecheck the Call when the target is null. This method finds
     * an appropriate target, and then type checks accordingly.
     * 
     * @param argTypes list of <code>Type</code>s of the arguments
     */
    protected Node typeCheckNullTarget(ContextVisitor tc, List<Type> argTypes) throws SemanticException {
        TypeSystem ts = tc.typeSystem();
        NodeFactory nf = tc.nodeFactory();
        Context c = tc.context();

        // the target is null, and thus implicit
        // let's find the target, using the context, and
        // set the target appropriately, and then type check
        // the result
        MethodInstance mi = c.findMethod(ts.MethodMatcher(null, name.id(), argTypes, c));
        
        Receiver r;
        if (mi.flags().isStatic()) {
            Type container = findContainer(ts, mi);            
            r = nf.CanonicalTypeNode(position().startOf(), container).typeRef(Types.ref(container));
        } else {
            // The method is non-static, so we must prepend with "this", but we
            // need to determine if the "this" should be qualified.  Get the
            // enclosing class which brought the method into scope.  This is
            // different from mi.container().  mi.container() returns a super type
            // of the class we want.
            ClassType scope = c.findMethodScope(name.id());

            if (! ts.typeEquals(scope, c.currentClass(), c)) {
                r = (Special) nf.This(position().startOf(),
                            nf.CanonicalTypeNode(position().startOf(), scope)).del().typeCheck(tc);
            }
            else {
                r = (Special) nf.This(position().startOf()).del().typeCheck(tc);
            }
        }

        // we call computeTypes on the reciever too.
        Call_c call = (Call_c) this.targetImplicit(true).target(r);       
        call = (Call_c)call.methodInstance(mi).type(mi.returnType());
//        call = (Call_c) call.methodInstance(mi);
        return call;
    }

    /**
     * Used to find the missing static target of a static method call.
     * Should return the container of the method instance. 
     * 
     */
    protected Type findContainer(TypeSystem ts, MethodInstance mi) {
        return mi.container();
    }

    /** Type check the call. */
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();

        Context c = tc.context();

        List<Type> argTypes = new ArrayList<Type>(this.arguments.size());

        for (Iterator<Expr> i = this.arguments.iterator(); i.hasNext(); ) {
            Expr e = (Expr) i.next();
            argTypes.add(e.type());
        }

        if (this.target == null) {
            return this.typeCheckNullTarget(tc, argTypes);
        }
        
        Type targetType = target.type();
        MethodInstance mi = ts.findMethod(targetType, 
                                          ts.MethodMatcher(targetType, this.name.id(), argTypes, c));
        
        /* This call is in a static context if and only if
         * the target (possibly implicit) is a type node.
         */
        boolean staticContext = (this.target instanceof TypeNode);

        if (staticContext && !mi.flags().isStatic()) {
            throw new SemanticException("Cannot call non-static method " + name.id() + " of " + target.type() + " in static context.", position());
        }

        // If the target is super, but the method is abstract, then complain.
        if (this.target instanceof Special && 
            ((Special)this.target).kind() == Special.SUPER &&
            mi.flags().isAbstract()) {
                throw new SemanticException("Cannot call an abstract method of the super class", this.position());            
        }

        Call_c call = (Call_c)this.methodInstance(mi).type(mi.returnType());

        // If we found a method, the call must type check, so no need to check
        // the arguments here.
        call.checkConsistency(c);

        return call;
    }

  public Type childExpectedType(Expr child, AscriptionVisitor av)
  {
      if (child == target) {
          return mi.container();
      }

      Iterator<Expr> i = this.arguments.iterator();
      Iterator<Type> j = mi.formalTypes().iterator();

      while (i.hasNext() && j.hasNext()) {
          Expr e = (Expr) i.next();
          Type t = (Type) j.next();

          if (e == child) {
              return t;
          }
      }

      return child.type();
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(targetImplicit ? "" : target.toString() + ".");
    sb.append(name);
    sb.append("(");

    int count = 0;

    for (Iterator<Expr> i = arguments.iterator(); i.hasNext(); ) {
        if (count++ > 2) {
            sb.append("...");
            break;
        }

        Expr n = (Expr) i.next();
        sb.append(n.toString());

        if (i.hasNext()) {
            sb.append(", ");
        }
    }

    sb.append(")");
    return sb.toString();
  }

  /** Write the expression to an output file. */
  public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
    w.begin(0);
    if (!targetImplicit) {
        if (target instanceof Expr) {
          printSubExpr((Expr) target, w, tr);
        }
        else if (target != null) {
          print(target, w, tr);
        }
	w.write(".");
	w.allowBreak(2, 3, "", 0);
    }

    w.write(name + "(");
    if (arguments.size() > 0) {
	w.allowBreak(2, 2, "", 0); // miser mode
	w.begin(0);
		    
	for (Iterator<Expr> i = arguments.iterator(); i.hasNext(); ) {
	    Expr e = i.next();
	    print(e, w, tr);

	    if (i.hasNext()) {
		w.write(",");
		w.allowBreak(0, " ");
	    }
	}

	w.end();
    }
    w.write(")");
    w.end();
  }

  /** Dumps the AST. */
  public void dump(CodeWriter w) {
    super.dump(w);

    w.allowBreak(4, " ");
    w.begin(0);
    w.write("(targetImplicit " + targetImplicit + ")");
    w.end();

    if ( mi != null ) {
      w.allowBreak(4, " ");
      w.begin(0);
      w.write("(instance " + mi + ")");
      w.end();
    }

    w.allowBreak(4, " ");
    w.begin(0);
    w.write("(name " + name + ")");
    w.end();

    w.allowBreak(4, " ");
    w.begin(0);
    w.write("(arguments " + arguments + ")");
    w.end();
  }

  public Term firstChild() {
      if (target instanceof Term) {
          return (Term) target;
      }
      return listChild(arguments, null);
  }

  public <S> List<S> acceptCFG(CFGBuilder v, List<S> succs) {
      if (target instanceof Term) {
          Term t = (Term) target;
          
          if (!arguments.isEmpty()) {
              v.visitCFG(t, listChild(arguments, null), ENTRY);
              v.visitCFGList(arguments, this, EXIT);
          } else {
              v.visitCFG(t, this, EXIT);
          }
      }

      return succs;
  }

  /** Check exceptions thrown by the call. */
  public Node exceptionCheck(ExceptionChecker ec) throws SemanticException {
    if (mi == null) {
      throw new InternalCompilerError(position(),
                                      "Null method instance after type "
                                      + "check.");
    }

    return super.exceptionCheck(ec);
  }

  
  // check that the implicit target setting is correct.
  protected void checkConsistency(Context c) throws SemanticException {
      if (targetImplicit) {
          // the target is implicit. Check that the
          // method found in the target type is the
          // same as the method found in the context.
          
          // as exception will be thrown if no appropriate method
          // exists. 
          MethodInstance ctxtMI = c.findMethod(c.typeSystem().MethodMatcher(null, name.id(), mi.formalTypes(), c));
          
          // cannot perform this check due to the context's findMethod returning a 
          // different method instance than the typeSystem in some situations
//          if (!c.typeSystem().equals(ctxtMI, mi)) {
//              throw new InternalCompilerError("Method call " + this + " has an " +
//                   "implicit target, but the name " + name + " resolves to " +
//                   ctxtMI + " in " + ctxtMI.container() + " instead of " + mi+ " in " + mi.container(), position());
//          }
      }      
  }
  
}
