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

package x10cuda.types;

import java.util.ArrayList;
import java.util.Iterator;

import polyglot.ast.Block;
import polyglot.ast.Expr;
import polyglot.ast.LocalDecl;
import polyglot.ast.Return;
import polyglot.ast.Stmt;
import polyglot.types.Name;
import polyglot.types.Type;
import polyglot.visit.Translator;
import x10.ast.Closure;
import polyglot.types.TypeSystem;
import x10.util.ClassifiedStream;
import x10.util.StreamWrapper;
import x10.visit.Inliner.InliningRewriter;

public class SharedMem {
    
    ArrayList<Decl> decls = new ArrayList<Decl>();
    
    private abstract static class Decl {
        public final LocalDecl ast;
        public Decl (LocalDecl ast) { this.ast = ast; }
        abstract public String generateDef(StreamWrapper out, String offset, Translator tr);
        abstract public String generateInit(StreamWrapper out, String offset, Translator tr);
        abstract public void generateSize(StreamWrapper inc, Translator tr);
		abstract public void generateCMemPop(StreamWrapper out, Translator tr);
    }
    
    private static class Array extends Decl {
        public final Expr numElements;
        public final Expr init;
        public final String elementType;
        public Array (LocalDecl ast, Expr numElements, Expr init, String elementType) {
            super(ast);
            this.numElements = numElements;
            this.init = init;
            this.elementType = elementType;
        }
        public String generateDef(StreamWrapper out, String raw, Translator tr) {
            String name = ast.name().id().toString();
            out.write("x10aux::cuda_array<"+elementType+"> "+name+" = { ");
            if (numElements!=null) {
	            tr.print(null, numElements, out);
            } else {
            	tr.print(null, init, out);
            	out.write(".size");
            }
            out.write(", ("+elementType+"*) "+raw+" };"); out.newline();
            return "&"+ast.name().id()+".raw["+ast.name().id()+".size]";
        }
        public String generateInit(StreamWrapper out, String offset, Translator tr) {
            out.write("{"); out.newline(4); out.begin(0);
            out.write("x10_int __len = ");
            if (numElements!=null) {
	            tr.print(null, numElements, out);
            } else {
            	tr.print(null, init, out);
            	out.write(".size");
            }
            out.write(";"); out.newline();
            
            out.write("for (int __i=0 ; __i<__len ; ++__i) {"); out.newline(4); out.begin(0);
            // TODO: assumes Array initialised with another Array -- closure version also possible
            if (numElements == null) {
            	out.write("const "+elementType+" &__v = ");
                tr.print(null, init, out);
            	out.write(".raw[__i];");
            } else {
            	if (init instanceof Closure) {
            		Closure lit = (Closure) init;
            		// Use the InlininingRewriter to get rid of the early returns.
            		// Then strip off the final return and assign __v instead.
            		try {
	            		((X10CUDAContext_c) tr.context()).shmIterationVar(lit.formals().get(0).name().id());
	            		Closure init_c_norm = (Closure) lit.visit(new InliningRewriter(lit, tr.job(), tr.typeSystem(), tr.nodeFactory(), tr.context()));
	            		Block b = init_c_norm.body();
	            		for (int i=0; i<b.statements().size()-1 ; ++i ) {
		            		tr.print(null, b.statements().get(i), out);
	            		}
	            		Stmt return_stmt = b.statements().get(b.statements().size()-1);
	            		Return return_ = (Return)return_stmt;
	                	out.write(elementType+" __v = ");
	            		tr.print(null, return_.expr(), out);
	                	out.write(";");
            		} finally {
	            		((X10CUDAContext_c) tr.context()).shmIterationVar(null);
            		}
            	} else {
                	out.write("const "+elementType+" &__v = ");
            		tr.print(null, init, out);
                	out.write(";");
            	}
            }
    		out.newline();
            out.write(ast.name().id()+".raw[__i] = __v;");
            out.newline();
            
            out.end(); out.newline();
            out.write("}");
            out.end(); out.newline();
            out.write("}");
            return "&"+ast.name().id()+".raw["+ast.name().id()+".size]";
        }
        public void generateSize(StreamWrapper inc, Translator tr) {
            if (numElements!=null) {
	            tr.print(null, numElements, inc);
            } else {
            	tr.print(null, init, inc);
            	inc.write("->FMGL(rawLength)");
            }
            // FIXME: x10_float is baked in here
            inc.write("*sizeof("+elementType+")");

        }
		@Override
		public void generateCMemPop(StreamWrapper out, Translator tr) {
			// TODO Auto-generated method stub
			out.write("pop.populateArr<"+elementType+", x10aux::ref<x10::array::Array<"+elementType+"> > >(");
            tr.print(null, init, out);
			out.write(");");
		}
    }
    private static class Var extends Decl {
        public Var (LocalDecl ast) { super(ast); }
        public String generateDef(StreamWrapper out, String offset, Translator tr) {
            // TODO: not implemented
            assert false: "not implemented";
    		return "";
        }
        public String generateInit(StreamWrapper out, String offset, Translator tr) {
            // TODO: not implemented
            assert false: "not implemented";
        	return "";
        }
        public void generateSize(StreamWrapper inc, Translator tr) {
            // TODO: not implemented
            assert false: "not implemented";
        }
		@Override
		public void generateCMemPop(StreamWrapper out, Translator tr) {
			// TODO Auto-generated method stub
			
		}
    }
    
    public void addArrayInitClosure(LocalDecl ast, Expr numElements, Expr init, String type) {
        decls.add(new Array(ast, numElements, init, type));
    }

    public void addArrayInitArray(LocalDecl ast, Expr init, String type) {
        decls.add(new Array(ast, null, init, type));
    }

    public void addVar(LocalDecl ast) {
        decls.add(new Var(ast));
    }
    
    public boolean has(Name n) {
        for (Decl d : decls) {
            if (d.ast.name().id() == n) {
                return true;
            }
        }
        return false;
    }

    public void generateCodeSharedMem(StreamWrapper out, Translator tr) {
        out.write("// shm");
        out.newline();

        if (decls.size()==0) return;

        String raw = "__shm";
        for (SharedMem.Decl d : decls) {
            d.generateDef(out, raw, tr);
            out.write("if (threadIdx.x == 0) {"); out.newline(4); out.begin(0);
            raw = d.generateInit(out, raw, tr);
            out.end(); out.newline();
            out.write("}"); out.newline();
        }
    }

    public void generateCodeConstantMemory(StreamWrapper out, Translator tr) {
    	out.write("// cmem");
        out.newline();

        String raw = "&__cmem[0]";
        for (SharedMem.Decl d : decls) {
            raw = d.generateDef(out, raw, tr);
        }
    }

    public void generateHostCodeConstantMemory(StreamWrapper out, Translator tr) {
        out.write("// cmem");        
        out.newline();

        if (decls.size()==0) return;

		out.write("x10aux::CMemPopulator pop(__cmemv);");

        for (SharedMem.Decl d : decls) {
            d.generateCMemPop(out, tr);
        }
    }


    public void generateSize(StreamWrapper inc, Translator tr) {
        // TODO Auto-generated method stub
        String prefix = "";
        for (SharedMem.Decl d : decls) {
            inc.write(prefix);
            d.generateSize(inc, tr);
            prefix = " + ";
        }
        if (prefix.equals("")) inc.write("0");
    }
}
