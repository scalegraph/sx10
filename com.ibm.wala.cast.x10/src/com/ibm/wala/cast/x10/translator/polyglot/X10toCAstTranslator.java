/*
 * Created on Sep 8, 2005
 */
package com.ibm.wala.cast.x10.translator.polyglot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import polyglot.ast.Assign;
import polyglot.ast.Call;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.Expr;
import polyglot.ast.Formal;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.ProcedureDecl;
import polyglot.ast.Stmt;
import polyglot.types.ClassType;
import polyglot.types.CodeInstance;
import polyglot.types.LocalDef;
import polyglot.types.MethodInstance;
import polyglot.types.Named;
import polyglot.types.QName;
import polyglot.types.StructType;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import x10.ast.AssignPropertyCall;
import x10.ast.Async;
import x10.ast.AtEach;
import x10.ast.AtStmt;
import x10.ast.Atomic;
import x10.ast.Clocked;
import x10.ast.Closure;
import x10.ast.ClosureCall;
import x10.ast.Finish;
import x10.ast.ForLoop;
import x10.ast.Future;
import x10.ast.Here;
import x10.ast.LocalTypeDef;
import x10.ast.Next;
import x10.ast.ParExpr;
import x10.ast.Range;
import x10.ast.Region;
import x10.ast.SettableAssign;
import x10.ast.Tuple;
import x10.ast.TypeDecl;
import x10.ast.When;
import x10.ast.When_c;
import x10.ast.X10Formal;
import x10.ast.X10Loop;
import x10.types.FunctionType;
import x10.types.ParametrizedType;
import polyglot.types.TypeSystem;

import com.ibm.wala.cast.java.translator.polyglot.PolyglotIdentityMapper;
import com.ibm.wala.cast.java.translator.polyglot.PolyglotJava2CAstTranslator;
import com.ibm.wala.cast.java.translator.polyglot.PolyglotTypeDictionary;
import com.ibm.wala.cast.java.translator.polyglot.TranslatingVisitor;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstNodeTypeMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.CAstTypeDictionary;
import com.ibm.wala.cast.tree.impl.CAstSymbolImpl;
import com.ibm.wala.cast.x10.translator.X10CAstEntity;
import com.ibm.wala.cast.x10.translator.X10CastNode;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.IteratorPlusOne;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

public class X10toCAstTranslator extends PolyglotJava2CAstTranslator {
    public interface ParametricType extends CAstType.Class {
        List<CAstType> getParameters();
    }

    public class PolyglotJavaParametricType extends PolyglotJavaType implements ParametricType {
        private final List<Type> fPolyglotTypeParameters;
        private List<CAstType> fTypeParameters = null;

        public PolyglotJavaParametricType(ClassType baseType, List<Type> typeParams, CAstTypeDictionary dict, TypeSystem system) {
          super(baseType, dict, system);
          fPolyglotTypeParameters= typeParams;
        }

        public List<CAstType> getParameters() {
          if (fTypeParameters == null) {
            buildTypeParameters();
          }
          return fTypeParameters;
        }

        private void buildTypeParameters() {
          for(Type typeParam: fPolyglotTypeParameters) {
            fTypeParameters.add(fDict.getCAstTypeFor(typeParam));
          }
        }

        @Override
        public String toString() {
          StringBuilder sb= new StringBuilder();
          sb.append(fType.fullName());
          sb.append("<");
          int idx= 0;
          for(Type typeParam: fPolyglotTypeParameters) {
            if (idx++ > 0) { sb.append(", "); }
            sb.append(typeParam.toString());
          }
          sb.append(">");
          return sb.toString();
        }
    }

    public X10toCAstTranslator(ClassLoaderReference clr, NodeFactory nf, TypeSystem ts, PolyglotIdentityMapper fMapper, Boolean replicateForDoLoops) {
	super(clr, nf, ts, fMapper, replicateForDoLoops);
    }

    protected TranslatingVisitor createTranslator() {
        return new X10TranslatingVisitorImpl();
    }

    protected PolyglotTypeDictionary createTypeDict() {
	return new X10TypeDictionary(fTypeSystem, this);
    }

    protected CAstEntity walkAsyncEntity(final Node rootNode, final Node bodyNode, final WalkContext context) {
      Map<CAstNode,CAstEntity> childEntities= new HashMap<CAstNode,CAstEntity>();
      final CodeBodyContext asyncContext= new CodeBodyContext(context, childEntities);
      final CAstNode bodyAST= walkNodes(bodyNode, asyncContext);

      return new AsyncEntity(childEntities, rootNode, asyncContext, bodyAST);
    }

    protected CAstEntity walkClosureEntity(final Closure rootNode, final Node bodyNode, final WalkContext context) {
      Map<CAstNode,CAstEntity> childEntities = new HashMap<CAstNode,CAstEntity>();
      final MethodContext closureContext = new MethodContext(rootNode.closureDef().asInstance(), childEntities, context);
      final CAstNode bodyAST = walkNodes(bodyNode, closureContext);

      final List<Formal> formals = rootNode.formals();
      final Collection<String> argNames = new ArrayList<String>();
      final Collection<CAstNode> bodyNodes = new ArrayList<CAstNode>();
      
      // closure object is the receiver
      argNames.add("$this");
      
      for (final Formal formal : formals) {
        argNames.add(formal.name().toString());
        if (formal instanceof X10Formal) {
          final X10Formal x10Formal = (X10Formal) formal;
          if (x10Formal.hasExplodedVars()) {
            int i = 0;
            for (final Formal explodedVarFormal : x10Formal.vars()) {
              bodyNodes.add(createExplodedVarInitialization(context, formal, explodedVarFormal, i++));
            }
          }
        }
      }
      final CAstNode newBodyAst;
      if (bodyNodes.isEmpty()) {
        newBodyAst = bodyAST;
      } else {
        // We want to access directly to the block statement.
        bodyNodes.add(bodyAST.getChild(0).getChild(0));
        newBodyAst = makeNode(context, fFactory, bodyNode, CAstNode.LOCAL_SCOPE, 
                              makeNode(context, fFactory, bodyNode, CAstNode.BLOCK_STMT,
                                       bodyNodes.toArray(new CAstNode[bodyNodes.size()])));
      }

      return new ClosureBodyEntity(childEntities, rootNode, closureContext, newBodyAst, context.getEnclosingType(), 
                                   argNames.toArray(new String[argNames.size()]),
                                   bodyNodes.toArray(new CAstNode[bodyNodes.size()]));
    }
    
    private CAstNode createExplodedVarInitialization(final WalkContext context, final Formal parameter,
                                                     final Formal explodedVar, final int index) {
      final CAstNode recvNode = makeNode(context, parameter.position(), CAstNode.VAR,
                                         fFactory.makeConstant(parameter.name().toString()));
     
      final TypeReference typeRef = fIdentityMapper.getTypeRef(parameter.type().type());
      final MethodReference methodRef = MethodReference.findOrCreate(typeRef, Atom.findOrCreateAsciiAtom("apply"), 
                                                                     Descriptor.findOrCreateUTF8("(I)Lx10/lang/Int;"));
      final CallSiteReference csRef = CallSiteReference.make(0, methodRef, IInvokeInstruction.Dispatch.VIRTUAL);
      return makeNode(context, parameter.position(), CAstNode.DECL_STMT,
              fFactory.makeConstant(new CAstSymbolImpl(explodedVar.name().toString(), false)),
              makeNode(context, fFactory, parameter, CAstNode.CALL, 
                       new CAstNode[] { recvNode, fFactory.makeConstant(csRef), 
                                        fFactory.makeConstant(index)}));
    }

    private final class AsyncBodyType implements CAstType.Method {
	private final Node fNode;

	private final Type declaringType;

	private AsyncBodyType(Node node, Type declaringType) {
	    super();
	    fNode= node;
	    this.declaringType = declaringType;
	}

	public CAstType getReturnType() {
	    return getTypeDict().getCAstTypeFor(
		    (fNode instanceof Future) ?
			    ((Future) fNode).type() : fTypeSystem.Void());
	}

	public List getArgumentTypes() {
	    return Collections.EMPTY_LIST;
	}

	public Collection getExceptionTypes() {
	    // TODO should figure out what exceptions can really be thrown by the body
	    return Collections.EMPTY_LIST;
	}

	public int getArgumentCount() {
	    return 0;
	}

	public String getName() {
	    return "<activity>";
	}

	public Collection getSupertypes() {
	    return Collections.singleton(getTypeDict().getCAstTypeFor(fTypeSystem.Object()));
	}

	public CAstType getDeclaringType() {
	    return getTypeDict().getCAstTypeFor(declaringType);
	}
    }

    final class AsyncEntity extends CodeBodyEntity {
	
    private final CodeBodyContext fContext;

	private final CAstNode fBodyast;

	private final CAstSourcePositionMap.Position fPosition;

	private final AsyncBodyType fBodyType;

	private AsyncEntity(Map<CAstNode,CAstEntity> entities, Node node, CodeBodyContext context, CAstNode bodyast) {
	    super(entities);
	    fPosition= makePosition(node.position());
	    fContext= context;
	    fBodyast= bodyast;
	    fBodyType= new AsyncBodyType(node, fContext.getEnclosingType());
	}

	public CAstSourcePositionMap.Position getPosition() {
	  return fPosition;
	}

	public int getKind() {
	    return X10CAstEntity.ASYNC_BODY;
	}

	public String getName() {
	    return "<activity " + fPosition.getURL() + ":" + fPosition.getFirstLine() + ":" + fPosition.getFirstCol() + ">";
	}

	public String[] getArgumentNames() {
	    return new String[0];
	}

	public CAstNode[] getArgumentDefaults() {
	  return new CAstNode[0];
	}

	public int getArgumentCount() {
	    return 0;
	}

	public CAstNode getAST() {
	    return fBodyast;
	}

	public CAstControlFlowMap getControlFlow() {
	    return fContext.cfg();
	}

	public CAstSourcePositionMap getSourceMap() {
	    return fContext.pos();
	}

	public CAstNodeTypeMap getNodeTypeMap() {
	    return fContext.getNodeTypeMap();
	}

	public Collection getQualifiers() {
	    return Collections.EMPTY_LIST;
	}

	public CAstType getType() {
	    return fBodyType;
	}
	public String toString() {
	    return getName();
	}
    }

    class X10TranslatingVisitorImpl extends JavaTranslatingVisitorImpl implements X10TranslatorVisitor {
        @Override
        public CAstNode visit(ConstructorDecl cd, MethodContext mc) {
            if (cd.body() == null) { // PORT1.7 Body can be null
              return makeNode(mc, fFactory, cd, CAstNode.BLOCK_STMT, new CAstNode[0]);
            }
            return super.visit(cd, mc);
        }

        public CAstNode visit(Async a, WalkContext context) {
	    CAstEntity bodyEntity= walkAsyncEntity(a, a.body(), context);
	    List clocks = a.clocks();

	    CAstNode args[] = new CAstNode[ clocks.size()+1 ];
//	    args[0] = walkNodes(a.place(), context);
	    for(int i = 0; i < clocks.size(); i++) {
	    	args[i] = walkNodes((Node)clocks.get(i), context);
	    }

	    // FUNCTION_EXPR will translate to a type wrapping the single method with the given body
	    args[args.length-1] = makeNode(context, a.body(), CAstNode.FUNCTION_EXPR, fFactory.makeConstant(bodyEntity));

	    CAstNode asyncNode= makeNode(context, a, X10CastNode.ASYNC_INVOKE, args);
	    context.addScopedEntity(asyncNode, bodyEntity);
	    return asyncNode;
	}

	public CAstNode visit(Finish f, WalkContext context) {
	    return makeNode(context, f, CAstNode.UNWIND,
		    makeNode(context, f, CAstNode.BLOCK_STMT, 
			    makeNode(context, X10CastNode.FINISH_ENTER, f.body().position().startOf()),
			    walkNodes(f.body(), context)),
		    makeNode(context, X10CastNode.FINISH_EXIT, f.body().position().endOf()));
	}

	private CAstNode walkRegionIterator(X10Loop loop, final CAstNode bodyNode, WalkContext context) {
	    return walkRegionIterator(loop.formal(), bodyNode, walkNodes(loop.domain(), context), loop.position(), context);
	}

	private CAstNode walkRegionIterator(Formal formal, final CAstNode bodyNode, CAstNode domainNode, polyglot.util.Position bodyPos, WalkContext wc) {
	    X10Formal x10Formal= (X10Formal) formal;
	    LocalDef[] vars = x10Formal.localInstances();
	    CAstNode[] bodyStmts = new CAstNode[vars.length + 1]; // var decls + bodyNode

	    for (int i = 0; i < vars.length; i++)
		bodyStmts[i]= makeNode(wc, vars[i].position(), CAstNode.DECL_STMT,
		  fFactory.makeConstant(new CAstSymbolImpl(vars[i].name().toString(), vars[i].flags().isFinal())),
		  makeNode(wc, vars[i].position(), CAstNode.ARRAY_REF, 
		          makeNode(wc, vars[i].position(), CAstNode.VAR, fFactory.makeConstant(formal.name().id().toString())),
		          fFactory.makeConstant(TypeReference.Int),
		          fFactory.makeConstant(i)));
	    bodyStmts[vars.length] = bodyNode;
	    
	    return makeNode(wc, bodyPos, CAstNode.LOCAL_SCOPE,
		makeNode(wc, bodyPos, CAstNode.BLOCK_STMT,
		    makeNode(wc, formal, CAstNode.DECL_STMT, fFactory.makeConstant(new CAstSymbolImpl("iter tmp", false)),
			    makeNode(wc, formal, X10CastNode.REGION_ITER_INIT, domainNode)),
		    makeNode(wc, bodyPos, CAstNode.LOOP,
			makeNode(wc, formal, X10CastNode.REGION_ITER_HASNEXT,
				makeNode(wc, formal, CAstNode.VAR, fFactory.makeConstant("iter tmp"))),
			makeNode(wc, bodyPos, CAstNode.BLOCK_STMT,
			    makeNode(wc, formal, CAstNode.DECL_STMT, walkNodes(formal, wc),
				makeNode(wc, formal, X10CastNode.REGION_ITER_NEXT, makeNode(wc, formal, CAstNode.VAR, fFactory.makeConstant("iter tmp")))),
			    bodyStmts))));
	}

	public CAstNode visit(AtEach a, WalkContext wc) {
	    CAstEntity bodyEntity= walkAsyncEntity(a, a.body(), wc);

	    Expr domain= a.domain();
	    Type type= domain.type();
	    CAstNode dist;

	    if (type.isArray())
		dist= makeNode(wc, domain, X10CastNode.ARRAY_DISTRIBUTION, walkNodes(domain, wc));
	    else
		dist= walkNodes(domain, wc);

	    List clocks = a.clocks();
	    
	    CAstNode args[] = new CAstNode[ clocks.size() + 2 ];
	    
	    args[0] = makeNode(wc, a.domain(), X10CastNode.PLACE_OF_POINT, makeNode(wc, a.domain(), CAstNode.VAR, fFactory.makeConstant("dist temp")), walkNodes(a.formal(), wc));

	    for(int i = 0; i < clocks.size(); i++) {
	    	args[i+1] = walkNodes((Node)clocks.get(i), wc);
	    }

	    // FUNCTION_EXPR will translate to a type wrapping the single method with the given body
	    args[ args.length - 1] = makeNode(wc, a.body(), CAstNode.FUNCTION_EXPR, fFactory.makeConstant(bodyEntity));

	    final CAstNode bodyNode= makeNode(wc, a, X10CastNode.ASYNC_INVOKE, args);

	    wc.addScopedEntity(bodyNode, bodyEntity);
	    return makeNode(wc, a, CAstNode.LOCAL_SCOPE,
		makeNode(wc, a, CAstNode.BLOCK_STMT,
			makeNode(wc, a.domain(), CAstNode.DECL_STMT, 
			  fFactory.makeConstant(new CAstSymbolImpl("dist temp", true)),
			  dist),
			walkRegionIterator(a.formal(), bodyNode, fFactory.makeNode(CAstNode.VAR, fFactory.makeConstant("dist temp")), a.body().position(), wc)));
	}

	public CAstNode visit(Future f, WalkContext wc) {
	    CAstEntity bodyEntity= walkAsyncEntity(f, f.body(), wc);
	    CAstNode bodyNode= makeNode(wc, f, X10CastNode.ASYNC_INVOKE,
		    walkNodes(f.place(), wc),
		    // FUNCTION_EXPR will translate to a type wrapping the single method with the given body
		    makeNode(wc, f.body(), CAstNode.FUNCTION_EXPR, fFactory.makeConstant(bodyEntity)));

	    wc.addScopedEntity(bodyNode, bodyEntity);
	    return bodyNode;
	}

	public CAstNode visit(Tuple t, WalkContext wc) {
	    CAstNode[] children = new CAstNode[t.arguments().size()+1];
            // N.B.: The type of the result will be Rail[T], where T is the LUB of all the child expr types.
            TypeReference tupleTypeRef = fIdentityMapper.getTypeRef(t.type());
            int idx= 0;

            children[idx++] = fFactory.makeConstant(tupleTypeRef);
	    for(Expr child: t.arguments()) {
	        children[idx++] = walkNodes(child, wc);
	    }
	    return makeNode(wc, fFactory, t, X10CastNode.TUPLE_EXPR, children);
	}

	public CAstNode visit(SettableAssign n, WalkContext wc) {
	    Expr array = n.array();
	    List<Expr> indices = n.index();
	    Assign.Operator op = n.operator();
	    Expr rhs = n.right();
	    CAstNode lhsCAstNode = visitArrayAccess(n, array, indices, wc);

	    return processAssign(n, lhsCAstNode, op, rhs, wc);
	}

	public CAstNode visitArrayAccess(Expr access, Expr array, List<Expr> indices, WalkContext wc) {
            TypeReference eltTypeRef = fIdentityMapper.getTypeRef(array.type());
            CAstNode[] children= new CAstNode[indices.size()+2];

            hookUpNPETargets(access, wc); // as for base class visit(ArrayAccess), right???

            int idx= 0;
            children[idx++]= walkNodes(array, wc);
            children[idx++]= fFactory.makeConstant(eltTypeRef);
            for(Expr index: indices) {
                children[idx++]= walkNodes(index, wc);
            }
            return makeNode(wc, fFactory, access, isIndexedByPoint(indices) ? X10CastNode.ARRAY_REF_BY_POINT : CAstNode.ARRAY_REF, children);
        }

        public CAstNode visitArrayAccess1D(Expr access, Expr array, Expr index, WalkContext wc) {
            TypeReference eltTypeRef = fIdentityMapper.getTypeRef(array.type());
            CAstNode[] children= new CAstNode[3];

            int idx= 0;
            children[idx++]= walkNodes(array, wc);
            children[idx++]= fFactory.makeConstant(eltTypeRef);
            children[idx++]= walkNodes(index, wc);

            return makeNode(wc, fFactory, array, isIndexedByPoint(index) ? X10CastNode.ARRAY_REF_BY_POINT : CAstNode.ARRAY_REF, children);
        }

        private boolean isIndexedByPoint(Expr index) {
            return index.type().isSubtype(((TypeSystem) fTypeSystem).Point(), fTypeSystem.emptyContext());
        }

        private CAstNode visitArrayAssign(Expr assign, Expr array, List<Expr> indices, WalkContext wc) {
            throw new UnsupportedOperationException();
        }
        public CAstNode visit(AssignPropertyCall a, WalkContext wc) {
            // TODO process the assignments
            return makeNode(wc, fFactory, null, CAstNode.EMPTY);
        }
        
        public CAstNode visit(Call c, WalkContext wc) {
	    MethodInstance methodInstance= c.methodInstance();
	    StructType methodOwner= methodInstance.container();

	    //PORT1.7 Array accesses are now represented as ordinary method calls
	    if (methodOwner instanceof ClassType) {
	        ClassType classType = (ClassType) methodOwner;
	        String className = classType.fullName().toString();
	        final QName settableName = QName.make("x10.lang.Settable");
            boolean isSettable = false;
            for (final Type type : classType.interfaces()) {
              if (type instanceof Named) {
                if (((Named) type).fullName().equals(settableName)) {
                  isSettable = true;
                  break;
                }
              }
            }

	        if (className.equals("x10.lang.Array") || isSettable) {
	            if (c.name().id().toString().equals("apply")) {
	                Expr array = (Expr) c.target();
	                List<Expr> indices = c.arguments();

	                return visitArrayAccess(c, array, indices, wc);
	            } else if (c.name().id().toString().equals("set")) {
                        Expr array = (Expr) c.target();
                        List<Expr> indices = c.arguments();

                        return visitArrayAssign(c, array, indices, wc);
	            }
	        }
	    } else if (methodOwner instanceof ParametrizedType) {
	        ParametrizedType parType = (ParametrizedType) methodOwner;
	        String baseName = parType.fullName().toString();

	        if (baseName.equals("x10.lang.Future")) {
	            List<Type> typeParms= parType.typeParameters();
	            Type retType= typeParms.get(0);
	            TypeReference typeRef= TypeReference.findOrCreate(fClassLoaderRef, fIdentityMapper.typeToTypeID(retType));

	            return makeNode(wc, c, X10CastNode.FORCE, walkNodes(c.target(), wc), fFactory.makeConstant(typeRef));
	        }
	    }
	    return super.visit(c, wc);
	}

	public CAstNode visit(Region r, WalkContext context) {
	    // NOOP for now; Region nodes don't actually get generated by the front-end; what's
	    // generated by the parser is a call to a region factory factory method.
	    return null;
	}

	public CAstNode visit(Range r, WalkContext context) {
	    // TODO Auto-generated method stub
	    return null;
	}

	public CAstNode visit(Here h, WalkContext context) {
	    return makeNode(context, h, X10CastNode.HERE);
	}
	
	public CAstNode visit(LocalTypeDef l, WalkContext context) {
	  return fFactory.makeNode(CAstNode.EMPTY);
	}

	public CAstNode visit(Next n, WalkContext context) {
	    return makeNode(context, n, X10CastNode.NEXT);
	}

	public CAstNode visit(When w, WalkContext wc) {
	    When_c when= (When_c) w;
//          List/*<When.Branch>*/ branches= when.branches();
	    List/*<Expr>*/ exprs= when.exprs();
	    List/*<Stmt>*/ stmts= when.stmts();
	    // In the fullness of time, some analyses may want to have "when" constructs
	    // clearly marked in a more declarative fashion, but for now, this has the
	    // advantage of making the operational semantics clear, with minimal extra
	    // machinery.
            Assertions.productionAssertion(exprs.size() == stmts.size());
	    CAstNode[] whenClauses= new CAstNode[exprs.size()+1];

	    
	    CAstNode whenExit= makeNode(wc, w.position().endOf(), CAstNode.LABEL_STMT,
		    fFactory.makeConstant("when exit"),
		    makeNode(wc, CAstNode.EMPTY, w.position().endOf()));

	    wc.cfg().map(whenExit, whenExit);

	    int idx= 0;
            Iterator stmtIter= IteratorPlusOne.make(stmts.iterator(), when.stmt());
	    for(Iterator exprIter= IteratorPlusOne.make(exprs.iterator(), when.expr()); exprIter.hasNext(); idx++) {
//		Branch b= (Branch) iter.next();
                Expr expr= (Expr) exprIter.next();
                Stmt stmt= (Stmt) stmtIter.next();

		CAstNode whenBreak= makeNode(wc, expr, CAstNode.GOTO);

		whenClauses[idx]= makeNode(wc, expr, CAstNode.IF_STMT,
			walkNodes(expr, wc),
			makeNode(wc, stmt, CAstNode.BLOCK_STMT,
				walkNodes(stmt, wc),
				whenBreak));
		wc.cfg().map(whenBreak, whenBreak);
		wc.cfg().add(whenBreak, whenExit, null);
	    }
	    return makeNode(wc, w, CAstNode.BLOCK_STMT,
		    makeNode(wc, w, CAstNode.LOOP,
			    fFactory.makeConstant(true),
			    wrapBodyInAtomic(makeNode(wc, w, CAstNode.BLOCK_STMT, whenClauses), w, wc)),
	            whenExit);

	    // Alternative, quasi-declarative representation:
//	    CAstNode[] branchNodes= new CAstNode[branches.size()*2];
//
//	    int idx= 0;
//	    for(Iterator iter= branches.iterator(); iter.hasNext(); idx += 2) {
//		Branch b= (Branch) iter.next();
//
//		branchNodes[idx]= walkNodes(b.expr(), context);
//		branchNodes[idx+1]= walkNodes(b.stmt(), context);
//	    }
//	    return fFactory.makeNode(X10CastNode.WHEN, branchNodes);
	}

	public CAstNode visit(X10Formal f, WalkContext context) {
	    return fFactory.makeConstant(new CAstSymbolImpl(f.name().id().toString(), true));
	}

	public CAstNode visit(Clocked c, WalkContext context) {
		Assertions.UNREACHABLE();
	    return null;
	}

	public CAstNode visit(Atomic a, WalkContext context) {
	    final CAstNode bodyNode= walkNodes(a.body(), context);
	    return wrapBodyInAtomic(bodyNode, a, context);
	}

	private CAstNode wrapBodyInAtomic(final CAstNode bodyNode, Node n, WalkContext wc) {
	    return makeNode(wc, n, CAstNode.UNWIND,
		    makeNode(wc, n, CAstNode.BLOCK_STMT, 
			    makeNode(wc, X10CastNode.ATOMIC_ENTER, n.position().startOf()),
			    bodyNode),
		    makeNode(wc, X10CastNode.ATOMIC_EXIT, n.position().startOf()));
	}

	private boolean isIndexedByPoint(List<Expr> indices) {
	    if (indices.size() > 1) return false;
	    return isIndexedByPoint(indices.get(0));
	}

//        private int tempCtr= 0;
//
//	public CAstNode visit(ArrayConstructor ac, WalkContext wc) {
//	    Expr dist= ac.distribution();
//	    Expr init= ac.initializer();
//	    Type arrayType= ac.type();
//	    // TODO Filter arrayType so that e.g. x10.lang.IntReferenceArray becomes int[] so
//	    // that WALA doesn't complain that an array type doesn't seem to be an array type.
//	   /* if (arrayType instanceof X10ParsedClassType_c) {
//	    	X10ParsedClassType_c t = ((X10ParsedClassType_c)arrayType);
//	    	List<Type> ps = t.typeParameters();
//	    	if (ps.size() == 1) {
//	    		arrayType = ps.get(0).arrayOf();
//	    	}
//	    }
//	    // ugly hackish attempt to find array type.  unlikely to be right all the time
//	    */
//	    TypeReference arrayTypeRef= fIdentityMapper.getTypeRef(arrayType);
//	    Type baseType= ac.arrayBaseType().type();
//	    TypeReference baseTypeRef= fIdentityMapper.getTypeRef(baseType);
//
//	    if (init instanceof Closure) {
//		Closure closure= (Closure) init;
//		Formal formal1= (Formal) closure.formals().get(0); // The closure for an array ctor init always has a single argument
//		// Turn this construct into an array allocation followed by a region
//		// iteration whose body calls the initializer and assigns the result
//		// to the corresponding array slot.
//		//
//		// BLOCK_EXPR [
//		//     ASSIGN [ arrayTmp, NEW [ type, dist.region ] ],
//		//     for(point p: dist.region) {
//		//         ASSIGN [ ARRAY_REF [ arrayTmp, p ], CALL [ closure, p ] ]
//		//     }
//		//     tmp
//		// ]
//		//
//		CAstNode closureNode= walkNodes(closure, wc);
//                String arrayTempName= "array temp" + tempCtr;
//                String distTempName= "dist temp" + tempCtr++;
//		CAstSymbol arrayTemp= new AstTranslator.InternalCAstSymbol(arrayTempName, true);
//		CAstSymbol distTemp= new AstTranslator.InternalCAstSymbol(distTempName, true);
//		CAstNode distDeclNode=
//			makeNode(wc, dist, CAstNode.DECL_STMT,
//					fFactory.makeConstant(distTemp),
//					walkNodes(dist, wc));
//		CAstNode arrayNewNode= 
//			makeNode(wc, ac, CAstNode.DECL_STMT,
//				fFactory.makeConstant(arrayTemp),
//				makeNode(wc, ac, CAstNode.NEW,
//					fFactory.makeConstant(arrayTypeRef),
//					makeNode(wc, fFactory, dist, CAstNode.VAR, fFactory.makeConstant(distTempName))));
//		int dummyPC = 0; // Just wrap the kind of call; the "rear end" won't care about anything else...
//		MethodReference closureRef= createMethodRefForClosure(closure);
//		CallSiteReference closureCallSiteRef= CallSiteReference.make(dummyPC, closureRef, IInvokeInstruction.Dispatch.VIRTUAL);
//		CAstNode arrayElemInit= makeNode(wc, closure, CAstNode.BLOCK_EXPR,
//			makeNode(wc, formal1, CAstNode.ASSIGN,
//				makeNode(wc, closure, X10CastNode.ARRAY_REF_BY_POINT,
//					makeNode(wc, closure, CAstNode.VAR, fFactory.makeConstant(arrayTempName)),
//					fFactory.makeConstant(baseTypeRef),
//					makeNode(wc, fFactory, formal1, CAstNode.VAR, fFactory.makeConstant(formal1.name()))),
//				makeNode(wc, closure, CAstNode.CALL,
//					closureNode,
//					fFactory.makeConstant(closureCallSiteRef),
//					makeNode(wc, fFactory, formal1, CAstNode.VAR, fFactory.makeConstant(formal1.name())))));
//
//		CAstNode loopBody=
//			walkRegionIterator(formal1, arrayElemInit,
//					makeNode(wc, dist, CAstNode.VAR, fFactory.makeConstant(distTempName)), closure.position(), wc);
//
//		return makeNode(wc, closure, CAstNode.BLOCK_EXPR, // NEED CAstNode.LOCAL_SCOPE or make "array temp" names unique
//			distDeclNode,
//			arrayNewNode,
//			loopBody,
//			makeNode(wc, fFactory, formal1, CAstNode.VAR, fFactory.makeConstant(arrayTempName)));
//	    } else if (init instanceof ArrayInit) {
//		ArrayInit arrayInit= (ArrayInit) init;
//		// ARRAY_NEW [ type, dist.region, walkNodes(init, wc) ]
//		CAstNode[] eltNodes= new CAstNode[arrayInit.elements().size()+1];
//		int idx= 0;
//		eltNodes[idx++]= makeNode(wc, ac, CAstNode.NEW,
//			fFactory.makeConstant(arrayTypeRef),
//			walkNodes(dist, wc));
//		for(Iterator iter= arrayInit.elements().iterator(); iter.hasNext(); ) {
//		    Expr elem= (Expr) iter.next();
//		    eltNodes[idx++]= walkNodes(elem, wc);
//		}
//		return makeNode(wc, init, CAstNode.ARRAY_LITERAL, eltNodes);
//	    } else if (init == null) {
//		return makeNode(wc, ac, CAstNode.NEW,
//			fFactory.makeConstant(arrayTypeRef),
//			walkNodes(dist, wc));
//	    } else {
//		Assertions.UNREACHABLE("ArrayConstructor has non-closure, non-ArrayInit initializer of type " + init.getClass());
//		return null;
//	    }
//	}
//
	private String castNameForType(Type type) {
	    return getTypeDict().getCAstTypeFor(type).getName();
	}
/*
	private MethodReference createMethodRefForClosure(Closure closure) {
	    List formals= closure.formals();
	    TypeName[] argTypes= new TypeName[formals.size()];
	    for(int i= 0; i < argTypes.length; i++) {
		Formal f= (Formal) formals.get(i);
		argTypes[i]= TypeName.findOrCreate(castNameForType(f.type().type()));
	    }
	    TypeName retType= TypeName.findOrCreate(castNameForType(closure.returnType().type()));
	    MethodReference closureRef= MethodReference.findOrCreate(
		    TypeReference.findOrCreate(fClassLoaderRef, "Lclosure" + new PolyglotSourcePosition(closure.position())),
		    new Selector(Atom.findOrCreateAsciiAtom("invoke"), Descriptor.findOrCreate(argTypes, retType)));
	    return closureRef;
	}
	*/

/*	
	public CAstNode visit(GenParameterExpr gpe, WalkContext context) {
	    // TODO Auto-generated method stub
	    return null;
	}
*/
	
	public CAstNode visit(ForLoop f, WalkContext context) {
		Node breakTarget = makeBreakTarget(f);
		Node continueTarget = makeContinueTarget(f);
		String loopLabel = (String) context.getLabelMap().get(f);
		WalkContext lc = new LoopContext(context, loopLabel, breakTarget, continueTarget);

	    return makeNode(context, f, CAstNode.BLOCK_STMT,
	    		walkNodes(breakTarget, context),
	    		walkRegionIterator(f, 
	    				makeNode(context, f, CAstNode.BLOCK_STMT,
	    						walkNodes(f.body(), lc),
	    						walkNodes(continueTarget, lc)),
	    				lc));
	}

	public CAstNode visit(Closure closure, WalkContext wc) {
	    CAstEntity bodyEntity= walkClosureEntity(closure, closure.body(), wc);
	    CAstNode closureNode= makeNode(wc, closure.body(), CAstNode.FUNCTION_EXPR, fFactory.makeConstant(bodyEntity));

	    wc.addScopedEntity(closureNode, bodyEntity);
	    return closureNode;
	}

	public CAstNode visit(ClosureCall closureCall, WalkContext wc) {
        MethodInstance instance = closureCall.closureInstance();
        MethodReference methodRef = fIdentityMapper.getMethodRef(instance);

        int dummyPC = 0;
        CallSiteReference callSiteRef = CallSiteReference.make(dummyPC, methodRef, IInvokeInstruction.Dispatch.VIRTUAL);

        CAstNode[] children = new CAstNode[2 + instance.formalTypes().size()];
        int i = 0;
        children[i++] = walkNodes(closureCall.target(), wc);
        children[i++] = fFactory.makeConstant(callSiteRef);

        for (final Expr arg : closureCall.arguments()) {
            children[i++] = walkNodes(arg, wc);
        }

        CAstNode result = makeNode(wc, fFactory, closureCall, CAstNode.CALL, children);
        wc.cfg().map(closureCall, result);
        return result;
	}

	public CAstNode visit(ParExpr pe, WalkContext wc) {
	    return walkNodes(pe.expr(), wc);
	}
    
    public CAstNode visit(final AtStmt atStmt, final WalkContext context) {
      return makeNode(context, atStmt, CAstNode.UNWIND,
                      makeNode(context, atStmt, CAstNode.BLOCK_STMT, 
                               makeNode(context, X10CastNode.AT_STMT_ENTER, atStmt.position().startOf()),
                               walkNodes(atStmt.body(), context)),
                               makeNode(context, X10CastNode.AT_STMT_EXIT, atStmt.position().endOf()));
    }
    
    }
    
    final class ClosureBodyEntity extends CodeBodyEntity {
	private final CodeBodyContext fContext;

	private final CAstNode fBodyAst;

	private final CAstSourcePositionMap.Position fPosition;

	private final ClosureBodyType fBodyType;

	private final CAstType fEnclosingType;

    private final String[] fArgumentNames;
    
    private final CAstNode[] fArgDefaults;

	public ClosureBodyEntity(final Map<CAstNode,CAstEntity> entities, final Closure node, final CodeBodyContext context,
	                         final CAstNode bodyAst, final Type enclosingType, final String[] argumentNames,
	                         final CAstNode[] argDefaults) {
	    super(entities);
	    fContext= context;
	    fBodyAst= bodyAst;
	    fPosition= makePosition(node.position());
	    fEnclosingType= getTypeDict().getCAstTypeFor(enclosingType);
	    fBodyType= new ClosureBodyType(node.closureDef().asType(), fEnclosingType);
	    this.fArgumentNames = argumentNames;
	    this.fArgDefaults = argDefaults;
	}
	
	public CAstNode getAST() {
	    return fBodyAst;
	}

	public int getArgumentCount() {
	    return fArgumentNames.length;
	}

	public CAstNode[] getArgumentDefaults() {
	    return fArgDefaults;
	}

	public String[] getArgumentNames() {
	     return fArgumentNames;
	}

	public CAstControlFlowMap getControlFlow() {
	    return fContext.cfg();
	}

	public int getKind() {
	    return X10CAstEntity.CLOSURE_BODY;
	}

	public String getName() {
	    return "apply";
	}

	public CAstNodeTypeMap getNodeTypeMap() {
	    return fContext.getNodeTypeMap();
	}

	public Position getPosition() {
	    return fPosition;
	}

	public Collection getQualifiers() {
	    return Collections.EMPTY_LIST;
	}

	public CAstSourcePositionMap getSourceMap() {
	    return fContext.pos();
	}

	public CAstType getType() {
	    return fBodyType;
	}
    }

    private final class ClosureBodyType implements CAstType.Method {
	private final FunctionType closureType;

	private List<CAstType> argTypes;

	private List<CAstType> excTypes;

	private CAstType returnType;

	private CAstType enclosingType;

	public ClosureBodyType(FunctionType cType, CAstType enclosingType) {
	    closureType= cType;
	    this.enclosingType= enclosingType;
	}

	public int getArgumentCount() {
	    return getArgumentTypes().size();
	}

	private List<CAstType> mapTypes(List<Type> types) {
	    List<CAstType> castTypes= new ArrayList<CAstType>();
	    for(Iterator iter= types.iterator(); iter.hasNext(); ) {
		Type type= (Type) iter.next();
		CAstType castType= getTypeDict().getCAstTypeFor(type);
		castTypes.add(castType);
	    }
	    return castTypes;
	}

	public List getArgumentTypes() {
	    if (argTypes == null) {
	    	argTypes= mapTypes(closureType.argumentTypes());
	    }
	    return argTypes;
	}

	public Collection getExceptionTypes() {
	    return Collections.EMPTY_LIST;
/* there are not checked exceptions in X10
	    if (excTypes == null) {
		excTypes= mapTypes(closureType.throwTypes());
	    }
	    return excTypes;
*/
	}

	public CAstType getReturnType() {
	    if (returnType == null) {
		returnType= getTypeDict().getCAstTypeFor(closureType.returnType());
	    }
	    return returnType;
	}

	public String getName() {
	    return ""; // Closures have no names
	}

	public Collection getSupertypes() {
	    return Collections.EMPTY_LIST;
	}

	public CAstType getDeclaringType() {
	    return enclosingType;
	}
    }

    protected CAstNode walkNodes(Node n, WalkContext context) {
      if (n == null) return fFactory.makeNode(CAstNode.EMPTY);
      return X10ASTTraverser.visit(n, (X10TranslatorVisitor) getTranslator(), context);
    }
      
    protected CAstEntity walkEntity(final Node rootNode, final WalkContext context) {
      if (rootNode instanceof TypeDecl) {
        return new TypeDeclarationCAstEntity(makePosition(rootNode.position()), (TypeDecl) rootNode);
      } else  if (rootNode instanceof ProcedureDecl) {
        // We need to have a specialization for the case of exploded vars definition and initialization.
        final ProcedureDecl pd = (ProcedureDecl) rootNode;
        final Map<CAstNode, CAstEntity> memberEntities = new LinkedHashMap<CAstNode, CAstEntity>();
        final MethodContext mc = new MethodContext(pd.procedureInstance().asInstance(), memberEntities, context);

        CAstNode procedureAST = null;

        if (! pd.flags().flags().isAbstract()) {
          procedureAST = walkNodes(pd, mc);
        }

        final List<Formal> formals = pd.formals();
        final String[] argNames;
        int i = 0;
        if (! pd.flags().flags().isStatic()) {
          argNames = new String[formals.size() + 1];
          argNames[i++] = "this";
        } else {
          argNames = new String[formals.size()];
        }
        final Collection<CAstNode> bodyNodes = new ArrayList<CAstNode>();
        for (final Formal formal : formals) {
          argNames[i++] = formal.name().toString();
          if (formal instanceof X10Formal) {
            final X10Formal x10Formal = (X10Formal) formal;
            if (x10Formal.hasExplodedVars()) {
              int j = 0;
              for (final Formal explodedVarFormal : x10Formal.vars()) {
                bodyNodes.add(createExplodedVarInitialization(mc, formal, explodedVarFormal, j++));
              }
            }
          }
        }
        
        final CAstNode newBodyAst;
        if (bodyNodes.isEmpty()) {
          newBodyAst = procedureAST;
        } else {
          // We want to access directly to the block statement.
          bodyNodes.add(procedureAST.getChild(0).getChild(0));
          newBodyAst = makeNode(mc, fFactory, pd, CAstNode.LOCAL_SCOPE, 
                                makeNode(mc, fFactory, pd, CAstNode.BLOCK_STMT,
                                         bodyNodes.toArray(new CAstNode[bodyNodes.size()])));
        }

        return new ProcedureEntity(newBodyAst, fTypeSystem, pd.procedureInstance().asInstance(), argNames, memberEntities, mc);
      } else {
        return super.walkEntity(rootNode, context);
      }
    }
    
    final class TypeDeclarationCAstEntity implements CAstEntity {

      TypeDeclarationCAstEntity(final Position position, final TypeDecl typeDecl) {
        this.fPosition = position;
        this.fTypeDecl = typeDecl;
      }

      public CAstNode getAST() {
        // No AST node
        return null;
      }

      public Map<CAstNode, Collection<CAstEntity>> getAllScopedEntities() {
        return Collections.emptyMap();
      }

      public int getArgumentCount() {
        return 0;
      }

      public CAstNode[] getArgumentDefaults() {
        return new CAstNode[0];
      }

      public String[] getArgumentNames() {
        return new String[0];
      }

      public CAstControlFlowMap getControlFlow() {
        // No Control Flow Map
        return null;
      }

      public int getKind() {
        return CAstEntity.TYPE_ENTITY;
      }

      public String getName() {
        return this.fTypeDecl.name().toString();
      }

      public CAstNodeTypeMap getNodeTypeMap() {
        return null;
      }

      public Position getPosition() {
        return this.fPosition;
      }

      public Collection getQualifiers() {
        return mapFlagsToQualifiers(this.fTypeDecl.typeDef().flags());
      }

      public Iterator getScopedEntities(CAstNode construct) {
        return null;
      }

      public String getSignature() {
        final StringBuilder sb = new StringBuilder();
        sb.append('L').append(getName().replace('.', '/')).append(';');
        return sb.toString();
      }

      public CAstSourcePositionMap getSourceMap() {
        // No source map
        return null;
      }

      public CAstType getType() {
        return new CAstType() {

          public String getName() {
            return fTypeDecl.name().toString();
          }

          public Collection getSupertypes() {
            return Collections.EMPTY_LIST;
          }
          
        };
      }
      
      private final Position fPosition;
      
      private final TypeDecl fTypeDecl;
      
    }
}
