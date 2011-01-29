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

package x10cuda.visit;

import static x10cpp.visit.Emitter.mangled_non_method_name;
import static x10cpp.visit.SharedVarsMethods.CUDA_NATIVE_STRING;
import static x10cpp.visit.SharedVarsMethods.CPP_NATIVE_STRING;
import static x10cpp.visit.SharedVarsMethods.DESERIALIZATION_BUFFER;
import static x10cpp.visit.SharedVarsMethods.DESERIALIZE_METHOD;
import static x10cpp.visit.SharedVarsMethods.SERIALIZATION_BUFFER;
import static x10cpp.visit.SharedVarsMethods.SERIALIZATION_ID_FIELD;
import static x10cpp.visit.SharedVarsMethods.SERIALIZATION_MARKER;
import static x10cpp.visit.SharedVarsMethods.SERIALIZE_BODY_METHOD;
import static x10cpp.visit.SharedVarsMethods.THIS;
import static x10cpp.visit.SharedVarsMethods.SAVED_THIS;
import static x10cpp.visit.SharedVarsMethods.chevrons;
import static x10cpp.visit.SharedVarsMethods.make_ref;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import polyglot.ast.ArrayInit_c;
import polyglot.ast.Assert_c;
import polyglot.ast.Assign_c;
import polyglot.ast.Binary;
import polyglot.ast.Binary_c;
import polyglot.ast.Block;
import polyglot.ast.Block_c;
import polyglot.ast.BooleanLit_c;
import polyglot.ast.Branch_c;
import polyglot.ast.Call;
import polyglot.ast.CanonicalTypeNode;
import polyglot.ast.Case_c;
import polyglot.ast.Catch_c;
import polyglot.ast.CharLit_c;
import polyglot.ast.ClassBody_c;
import polyglot.ast.Conditional_c;
import polyglot.ast.ConstructorDecl_c;
import polyglot.ast.Do_c;
import polyglot.ast.Empty_c;
import polyglot.ast.Eval;
import polyglot.ast.Eval_c;
import polyglot.ast.Expr;
import polyglot.ast.FieldDecl_c;
import polyglot.ast.Field_c;
import polyglot.ast.FloatLit_c;
import polyglot.ast.For;
import polyglot.ast.ForInit;
import polyglot.ast.For_c;
import polyglot.ast.Formal;
import polyglot.ast.Formal_c;
import polyglot.ast.Id_c;
import polyglot.ast.If_c;
import polyglot.ast.Import_c;
import polyglot.ast.Initializer_c;
import polyglot.ast.IntLit;
import polyglot.ast.IntLit_c;
import polyglot.ast.Labeled_c;
import polyglot.ast.Local;
import polyglot.ast.LocalClassDecl_c;
import polyglot.ast.LocalDecl;
import polyglot.ast.LocalDecl_c;
import polyglot.ast.Local_c;
import polyglot.ast.MethodDecl_c;
import polyglot.ast.New_c;
import polyglot.ast.Node;
import polyglot.ast.NullLit_c;
import polyglot.ast.PackageNode_c;
import polyglot.ast.Receiver;
import polyglot.ast.Return_c;
import polyglot.ast.Stmt;
import polyglot.ast.StringLit_c;
import polyglot.ast.SwitchBlock_c;
import polyglot.ast.Switch_c;
import polyglot.ast.Throw_c;
import polyglot.ast.Try;
import polyglot.ast.Try_c;
import polyglot.ast.TypeNode;
import polyglot.ast.Unary_c;
import polyglot.ast.While_c;
import polyglot.frontend.Compiler;
import x10.ast.AssignPropertyCall_c;
import x10.ast.Closure;
import x10.ast.ClosureCall_c;
import x10.ast.Closure_c;
import x10.ast.ForLoop;
import x10.ast.ForLoop_c;
import x10.ast.ParExpr_c;
import x10.ast.PropertyDecl_c;
import x10.ast.StmtSeq;
import x10.ast.SubtypeTest_c;
import x10.ast.Tuple_c;
import x10.ast.TypeDecl_c;
import x10.ast.X10Binary_c;
import x10.ast.X10Call;
import x10.ast.X10Call_c;
import x10.ast.X10CanonicalTypeNode_c;
import x10.ast.X10Cast_c;
import x10.ast.X10ClassDecl;
import x10.ast.X10ClassDecl_c;
import x10.ast.X10Formal;
import x10.ast.X10Instanceof_c;
import x10.ast.X10Loop;
import x10.ast.X10Loop_c;
import x10.ast.X10MethodDecl_c;
import x10.ast.X10New_c;
import x10.ast.X10Special_c;
import x10.ast.X10Unary_c;
import x10.constraint.XEQV;
import x10.constraint.XEquals;
import x10.constraint.XFailure;
import x10.constraint.XFormula;
import x10.constraint.XLit;
import x10.constraint.XLocal;
import x10.constraint.XTerm;
import x10.constraint.XVar;
import x10.extension.X10Ext;
import x10.types.ConstrainedType;
import x10.types.X10ClassDef;
import x10.types.X10ClassType;
import polyglot.types.TypeSystem;
import x10.types.constraints.CConstraint;
import x10cpp.X10CPPCompilerOptions;
import x10cpp.postcompiler.CXXCommandBuilder;
import x10cpp.types.X10CPPContext_c;
import x10cpp.visit.Emitter;
import x10cpp.visit.MessagePassingCodeGenerator;
import x10cpp.visit.SharedVarsMethods;
import x10cpp.visit.X10CPPTranslator;
import x10cuda.ast.CUDAKernel;
import x10cuda.types.CUDAData;
import x10cuda.types.SharedMem;
import x10cuda.types.X10CUDAContext_c;
import polyglot.main.Options;
import polyglot.main.Report;
import polyglot.types.Name;
import polyglot.types.QName;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.Types;
import polyglot.types.VarInstance;
import polyglot.util.ErrorInfo;
import polyglot.util.ErrorQueue;
import polyglot.util.SimpleCodeWriter;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.Translator;
import x10.util.ClassifiedStream;
import x10.util.StreamWrapper;
import x10.visit.ConstantPropagator;

/**
 * Visitor that prettyprints an X10 AST to the CUDA subset of c++.
 * 
 * @author Dave Cunningham
 */

public class CUDACodeGenerator extends MessagePassingCodeGenerator {

	private static final String ANN_KERNEL = "x10.compiler.CUDA";
	private static final String ANN_DIRECT_PARAMS = "x10.compiler.CUDADirectParams";

	public CUDACodeGenerator(StreamWrapper sw, Translator tr) {
		super(sw, tr);
	}

	protected String[] getCurrentNativeStrings() {
		if (!generatingKernel())
			return new String[] { CPP_NATIVE_STRING };
		return new String[] { CUDA_NATIVE_STRING, CPP_NATIVE_STRING };
	}

	private X10CUDAContext_c context() {
		return (X10CUDAContext_c) tr.context();
	}

	private TypeSystem xts() {
		return  tr.typeSystem();
	}

	// defer to CUDAContext.cudaStream()
	private ClassifiedStream cudaStream() {
		return context().cudaStream(sw, tr.job());
	}

	private boolean generatingKernel() {
		return context().generatingKernel();
	}

	private void generatingKernel(boolean v) {
		context().generatingKernel(v);
	}

	// does the block have the annotation that denotes that it should be
	// split-compiled to cuda?
	private boolean blockIsKernel(Node n) {
		return n instanceof CUDAKernel;
	}
	
	private String env = "__env";

	@SuppressWarnings("serial")
	private static class Complaint extends RuntimeException {
	}

	private void complainIfNot(boolean cond, String exp, Node n, boolean except) {
		complainIfNot2(cond, "@CUDA Expected: " + exp, n, except);
	}

	private void complainIfNot2(boolean cond, String exp, Node n, boolean except) {
		if (!cond) {
			tr.job().compiler().errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR,
					exp, n.position());
			if (except)
				throw new Complaint();
		}
	}

	private void complainIfNot(boolean cond, String exp, Node n) {
		complainIfNot(cond, exp, n, true);
	}

	private void complainIfNot2(boolean cond, String exp, Node n) {
		complainIfNot2(cond, exp, n, true);
	}

	private Type arrayCargo(Type typ) {
		if (xts().isArray(typ)) {
			typ = typ.toClass();
			X10ClassType ctyp = (X10ClassType) typ;
			assert ctyp.typeArguments() != null && ctyp.typeArguments().size() == 1; // Array[T]
			return ctyp.typeArguments().get(0);
		}
		if (xts().isRemoteArray(typ)) {
			typ = typ.toClass();
			X10ClassType ctyp = (X10ClassType) typ;
			assert ctyp.typeArguments() != null && ctyp.typeArguments().size() == 1; // RemoteRef[Array[T]]
			Type type2 = ctyp.typeArguments().get(0);
			X10ClassType ctyp2 = (X10ClassType) typ;
			assert ctyp2.typeArguments() != null && ctyp2.typeArguments().size() == 1; // Array[T]
			return ctyp2.typeArguments().get(0);
		}
		return null;

	}

	private boolean isFloatArray(Type typ) {
		Type cargo = arrayCargo(typ);
		return cargo != null && cargo.isFloat();
	}

	private boolean isIntArray(Type typ) {
		Type cargo = arrayCargo(typ);
		return cargo != null && cargo.isInt();
	}

	String prependCUDAType(Type t, String rest) {
		String type = Emitter.translateType(t, true);

		if (isIntArray(t)) {
			type = "x10aux::cuda_array<x10_int> ";
		} else if (isFloatArray(t)) {
			type = "x10aux::cuda_array<x10_float> ";
		} else {
			type = type + " ";
		}

		return type + rest;
	}

	void handleKernel(Stmt b) {
		
		CUDAKernel cuda_kernel = context().cudaKernel();
		//System.out.println("Here is the kernel: "+cuda_kernel);
		
		
		String kernel_name = context().wrappingClosure();
		sw.write("/* block split-compiled to cuda as " + kernel_name + " */ ");

		ClassifiedStream out = cudaStream();

		// environment (passed into kernel via pointer)
		generateStruct(kernel_name, out, context().kernelParams());

		out.forceNewline();

		boolean ptr = !cuda_kernel.directParams;
		// kernel (extern "C" to disable name-mangling which seems to be
		// inconsistent across cuda versions)
		out.write("extern \"C\" __global__ void " + kernel_name + "("
				+ kernel_name + "_env " + (ptr ? "*" : "") + env + ") {");
		out.newline(4);
		out.begin(0);

		if (ptr) {
			for (VarInstance<?> var : context().kernelParams()) {
				String name = var.name().toString();
				if (name.equals(THIS)) {
					name = SAVED_THIS;
				} else {
					name = Emitter.mangled_non_method_name(name);
				}
				out.write("__shared__ " + prependCUDAType(var.type(), name) + ";");
				out.newline();
			}
			out.write("if (threadIdx.x==0) {");
			out.newline(4);
			out.begin(0);
			for (VarInstance<?> var : context().kernelParams()) {
				String name = var.name().toString();
				if (name.equals(THIS)) {
					name = SAVED_THIS;
				} else {
					name = Emitter.mangled_non_method_name(name);
				}
				out.write(name + " = " + env + "->" + name + ";");
				out.newline();
			}
			out.end();
			out.newline();
			out.write("}");
			out.newline();
			out.write("__syncthreads(); // kernel parameters");
			out.newline();
			out.forceNewline();
		}

		sw.pushCurrentStream(out);
		try {
			cuda_kernel.cmem.generateCodeConstantMemory(sw, tr);
		} finally {
			sw.popCurrentStream();
		}

		sw.pushCurrentStream(out);
		try {
			cuda_kernel.shm.generateCodeSharedMem(sw, tr);
		} finally {
			sw.popCurrentStream();
		}

		out.write("__syncthreads(); // initialised shm"); out.newline();        
        out.forceNewline();                

		// body
		sw.pushCurrentStream(out);
        try {
        	super.visitAppropriate(b);
        } finally {
        	sw.popCurrentStream();
        }

		// end
		out.end();
		out.newline();
		out.write("} // " + kernel_name);
		out.newline();

		out.forceNewline();
	}

	private void generateStruct(String kernel_name, SimpleCodeWriter out, ArrayList<VarInstance<?>> vars) {
		out.write("struct " + kernel_name + "_env {");
		out.newline(4);
		out.begin(0);
		// emitter.printDeclarationList(out, context(),
		// context().kernelParams());
		for (VarInstance<?> var : vars) {
			String name = var.name().toString();
			if (name.equals(THIS)) {
				name = SAVED_THIS;
			} else {
				name = Emitter.mangled_non_method_name(name);
			}
			out.write(prependCUDAType(var.type(), name) + ";");
			out.newline();
		}
		out.end();
		out.newline();
		out.write("};");
		out.newline();
	}


	public void visit(Block_c b) {
		super.visit(b);
		try {
			if (blockIsKernel(b)) {
				final CUDAKernel cuda_kernel = (CUDAKernel)b;

				complainIfNot2(!generatingKernel(), "@CUDA kernels may not be nested.", b);
				
				final Block_c[] cell = new Block_c[1];
				cuda_kernel.visit(new NodeVisitor() {
					boolean found = false;
					public Node override(Node parent, Node child) {
						if (child instanceof Block_c) {
							Block_c child_block = (Block_c) child;
							if (child_block.cudaTag() == cuda_kernel.innerStatementTag) {
								cell[0] = child_block;
								found = true;
							}
						}
						if (found) {
							return child;
						}
						return null;
					}
				});

				context().cudaKernel(cuda_kernel);
				context().initKernelParams();
				context().established().cudaKernel(cuda_kernel);
				context().established().initKernelParams();
				
				generatingKernel(true);
				try {
					handleKernel(cell[0]);
				} finally {
					generatingKernel(false);
				}

				
			}
		} catch (Complaint e) {
			// don't bother doing anything more with this kernel,
			// just try and continue with the code after
			// (note that we've already done the regular CPU code)
		}
	}

	public void visit(Closure_c n) {
		context().establishClosure();
		String last = context().wrappingClosure();
		String lastHostClassName = context().wrappingClass();
		X10ClassType hostClassType = (X10ClassType) n.closureDef().typeContainer().get();
		String nextHostClassName = Emitter.translate_mangled_FQN(hostClassType.fullName().toString(), "_");
		String next = getClosureName(nextHostClassName, context().closureId() + 1);
		context().wrappingClosure(next);
		context().wrappingClass(nextHostClassName);
		try {
			super.visit(n);
		} finally {
			context().wrappingClosure(last);
			context().wrappingClass(lastHostClassName);
		}
	}

	protected void generateClosureDeserializationIdDef(ClassifiedStream defn_s, String cnamet, List<Type> freeTypeParams, String hostClassName, Block block, int kind) {
		if (blockIsKernel(block)) {
			
			assert kind==1;

			TypeSystem xts = tr.typeSystem();
			boolean in_template_closure = freeTypeParams.size() > 0;
			if (in_template_closure)
				emitter.printTemplateSignature(freeTypeParams, defn_s);
			defn_s.write("const x10aux::serialization_id_t " + cnamet + "::"
					+ SharedVarsMethods.SERIALIZATION_ID_FIELD + " = ");
			defn_s.newline(4);
			String template = in_template_closure ? "template " : "";
			defn_s.write("x10aux::DeserializationDispatcher::addDeserializer("
					+ cnamet + "::" + template
					+ SharedVarsMethods.DESERIALIZE_METHOD
					+ chevrons("x10::lang::Reference") + ", "+closure_kind_strs[kind]+", " + cnamet
					+ "::" + template
					+ SharedVarsMethods.DESERIALIZE_CUDA_METHOD + ", " + cnamet
					+ "::" + template + SharedVarsMethods.POST_CUDA_METHOD
					+ ", " + "\"" + hostClassName + ".cubin\", \"" + cnamet
					+ "\");");
			defn_s.newline();
			defn_s.forceNewline();
		} else {
			super.generateClosureDeserializationIdDef(defn_s, cnamet,
					freeTypeParams, hostClassName, block, kind);
		}
	}

	protected void generateClosureSerializationFunctions(X10CPPContext_c c, String cnamet, StreamWrapper inc, Block block, List<VarInstance<?>> refs) {

		super.generateClosureSerializationFunctions(c, cnamet, inc, block,refs);

		if (blockIsKernel(block)) {

			CUDAKernel cuda_kernel = ((CUDAKernel)block);			

			ArrayList<VarInstance<?>> env = context().kernelParams();

			if (env == null)
				return;

			generateStruct("__cuda", inc, env);

			inc.write("static void "
						+ SharedVarsMethods.POST_CUDA_METHOD
						+ "("
						+ DESERIALIZATION_BUFFER
						+ " &__buf, x10aux::place __gpu, size_t __blocks, size_t __threads, size_t __shm, size_t argc, char *argv, size_t cmemc, char *cmemv) {");
			inc.newline(4);
			inc.begin(0);

			inc.write("__cuda_env __env;");
			inc.newline();

			if (!cuda_kernel.directParams) {
				inc.write("x10_ulong __remote_env;");
				inc.newline();
				inc.write("::memcpy(&__remote_env, argv, sizeof (void*));");
				inc.newline();
				inc.write("x10aux::remote_free(__gpu, __remote_env);");
				inc.newline();
				// FIXME: any arrays referenced from the env are being leaked
				// here.
				// we need some way to record a copy of the contents of the
				// __env on the host
				// so that we do not have to fetch __remote_env back onto the
				// host
				// then we can free those arrays like in the else branch below
			} else {
				inc.write("::memcpy(&__env, argv, argc);");
				inc.newline();
				for (VarInstance<?> var : env) {
					Type t = var.type();
					String name = var.name().toString();
					if (isIntArray(t) || isFloatArray(t)) {
						if (!xts().isRemoteArray(t)) {
							inc.write("x10aux::remote_free(__gpu, (x10_ulong)(size_t)__env." + name + ".raw);");
						}
					}
					inc.newline();
				}

			}

			inc.end();
			inc.newline();
			inc.write("}");
			inc.newline();

			inc.forceNewline();

			inc.write("static void "+SharedVarsMethods.DESERIALIZE_CUDA_METHOD+"("+DESERIALIZATION_BUFFER+" &__buf, x10aux::place __gpu, size_t &__blocks, size_t &__threads, size_t &__shm, size_t &__argc, char *&__argv, size_t &__cmemc, char *&__cmemv) {");
			inc.newline(4);
			inc.begin(0);

			inc.write(make_ref(cnamet) + " __this = " + cnamet + "::" + DESERIALIZE_METHOD + "<" + cnamet + ">(__buf);");
			inc.newline();

			for (VarInstance<?> var : env) {
				Type t = var.type();
				String name = var.name().toString();
				inc.write(Emitter.translateType(t, true) + " " + name);
				if (cuda_kernel.autoBlocks != null
						&& var == cuda_kernel.autoBlocks.localDef().asInstance()) {
					inc.write(";");
				} else if (cuda_kernel.autoThreads != null
						&& var == cuda_kernel.autoThreads.localDef().asInstance()) {
					inc.write(";");
				} else {
					inc.write(" = __this->" + name + ";");
				}
				inc.newline();
			}

			inc.write("__shm = ");
			inc.begin(0);
			cuda_kernel.shm.generateSize(inc, tr);
			inc.write(";");
			inc.end();
			inc.newline();
			inc.write("x10aux::check_shm_size(__shm);");

			inc.write("__cmemc = ");
			inc.begin(0);
			cuda_kernel.cmem.generateSize(inc, tr);
			inc.write(";");
			inc.end();
			inc.newline();
			inc.write("x10aux::check_cmem_size(__cmemc);");
			
			cuda_kernel.cmem.generateHostCodeConstantMemory(inc, tr);

			// this is probably broken when only one is given.
			if (cuda_kernel.autoBlocks != null
					&& cuda_kernel.autoThreads != null) {
				String bname = cuda_kernel.autoBlocks.name().id().toString();
				String tname = cuda_kernel.autoThreads.name().id().toString();
				inc.write("x10aux::blocks_threads(__gpu, x10aux::DeserializationDispatcher::getMsgType(_serialization_id), __shm, "
								+ bname + ", " + tname + ");");
				inc.newline();
			}
			inc.write("__blocks = (");
			inc.begin(0);
			tr.print(null, cuda_kernel.blocks, inc);
			inc.write(")+1;");
			inc.end();
			inc.newline();

			inc.write("__threads = (");
			inc.begin(0);
			tr.print(null, cuda_kernel.threads, inc);
			inc.write(")+1;");
			inc.end();
			inc.newline();

			inc.write("__cuda_env __env;");
			inc.newline();

			for (VarInstance<?> var : env) {
				Type t = var.type();
				String name = var.name().toString();

				// String addr = "&(*"+name+")[0]"; // old way for rails
				String addr = "&" + name + "->FMGL(raw).raw()[0]";
				// String rr =
				// "x10aux::get_remote_ref_maybe_null("+name+".operator->())";
				// // old object model
				String rr = "&" + name + "->FMGL(rawData).raw()[0]";

				String ts = null;
				if (isIntArray(t)) {
					ts = "x10_int";
				} else if (isFloatArray(t)) {
					ts = "x10_float";
				}

				if (isIntArray(t) || isFloatArray(t)) {
					if (xts().isRemoteArray(t)) {
						inc.write("__env." + name + ".raw = (" + ts + "*)(size_t)" + rr + ";");
						inc.newline();
						inc.write("__env." + name + ".FMGL(size) = " + name + "->FMGL(size);");
						inc.newline();
					} else {
						String len = name + "->FMGL(size)";
						String sz = "sizeof(" + ts + ")*" + len;
						inc.write("__env." + name + ".raw = (" + ts + "*)(size_t)x10aux::remote_alloc(__gpu, " + sz + ");");
						inc.newline();
						inc.write("__env." + name + ".FMGL(size) = " + len + ";");
						inc.newline();
						inc.write("x10aux::cuda_put(__gpu, (x10_ulong) __env." + name + ".raw, " + addr + ", " + sz + ");");
					}
				} else {
					inc.write("__env." + name + " = " + name + ";");
				}
				inc.newline();
			}

			if (env.isEmpty()) {
				inc.write("__argc = 0;");
				inc.end();
				inc.newline();
			} else {
				if (cuda_kernel.directParams) {
					inc.write("memcpy(__argv, &__env, sizeof(__env));");
					inc.newline();
					inc.write("__argc = sizeof(__env);");
					inc.end();
					inc.newline();
				} else {
					inc.write("x10_ulong __remote_env = x10aux::remote_alloc(__gpu, sizeof(__env));");
					inc.newline();
					inc.write("x10aux::cuda_put(__gpu, __remote_env, &__env, sizeof(__env));");
					inc.newline();
					inc.write("::memcpy(__argv, &__remote_env, sizeof (void*));");
					inc.newline();
					inc.write("__argc = sizeof(void*);");
					inc.end();
					inc.newline();
				}
			}
			inc.write("}");
			inc.newline();
			inc.forceNewline();
		}
	}

	public void visit(New_c n) {
		complainIfNot2(!generatingKernel(), "New not allowed in @CUDA code.", n, false);
		super.visit(n);
	}

	@Override
	public void visit(Assert_c n) {
		complainIfNot2(!generatingKernel(),
				"Throwing exceptions not allowed in @CUDA code.", n, false);
		super.visit(n);
	}



	@Override
	public void visit(Catch_c n) {
		complainIfNot2(!generatingKernel(),
				"Catching exceptions not allowed in @CUDA code.", n, false);
		super.visit(n);
	}

	@Override
	public void visit(ClosureCall_c n) {
		complainIfNot2(!generatingKernel(),
				"Closure calls not allowed in @CUDA code.", n, false);
		super.visit(n);
	}

	@Override
	public void visit(Local_c n) {
		CUDAKernel cuda_kernel = context().cudaKernel();
		if (generatingKernel()) {
			ClassifiedStream out = cudaStream();
			Name ln = n.name().id();
			if (ln == cuda_kernel.blocksVar.name().id()) {
				out.write("blockIdx.x");
			} else if (ln == cuda_kernel.threadsVar.name().id()) {
				out.write("threadIdx.x");
			} else if (context().shmIterationVar()!=null && ln == context().shmIterationVar().name().id()) {
				out.write("__i");
			} else if (cuda_kernel.shm.has(ln)) {
				out.write(ln.toString());
			} else if (context().isKernelParam(ln)) {
				// it seems the post-compiler is not good at hoisting these
				// accesses so we do it ourselves
				String literal = constrainedToLiteral(n);
				if (literal!=null) {
					System.out.println("Optimised kernel param: "+n+" --> "+literal);
					out.write(literal);
				} else {
					if (cuda_kernel.directParams) {
						out.write(env + "." + ln);
					} else {
						out.write(ln.toString());
					}
				}
			} else {
				String literal = constrainedToLiteral(n);
				if (literal!=null) {
					System.out.println("Optimised local: "+n+" --> "+literal);
					out.write(literal);
				} else {
					super.visit(n);
				}
			}
		} else {
			// we end up here in the _deserialize_cuda function because
			// generatingKernel() is false
			Name ln = n.name().id();
			if (cuda_kernel == null) {
				// not even in _deserialize_cuda, just arbitrary host code
				super.visit(n);
			} else if (cuda_kernel.autoBlocks != null && ln == cuda_kernel.autoBlocks.name().id()) {
				sw.write(cuda_kernel.autoBlocks.name().id().toString());
			} else if (cuda_kernel.autoThreads != null && ln == cuda_kernel.autoThreads.name().id()) {
				sw.write(cuda_kernel.autoThreads.name().id().toString());
			} else {
				super.visit(n);
			}
		}
	}

	private String constrainedToLiteral(Local_c n) {
		//if (true) return null;
		if (!n.localInstance().def().flags().isFinal()) return null;
		if (!(n.type() instanceof ConstrainedType)) return null;
		ConstrainedType ct = (ConstrainedType) n.type();
		CConstraint cc = ct.getRealXClause();
		XVar local_self = Types.selfVarBinding(cc);
		if (local_self==null) return null;
		if (local_self instanceof XLit) return "/*"+n+":"+n.type()+"*/"+local_self.toString();
		// resolve to another variable, keep going
		CConstraint projected= context().constraintProjection(cc);
		if (! projected.consistent())
			return null;
		
		XVar closed_self = projected.bindingForVar(local_self);
		if (closed_self==null) return null;
		if (closed_self instanceof XLit) return "/*"+n+":"+n.type()+"*/"+closed_self.toString();
		return null;
	}

	@Override
	public void visit(Throw_c n) {
		complainIfNot2(!generatingKernel(),
				"Throwing exceptions not allowed in @CUDA code.", n, false);
		super.visit(n);
	}

	@Override
	public void visit(Try_c n) {
		complainIfNot2(!generatingKernel(),
				"Catching exceptions not allowed in @CUDA code.", n, false);
		super.visit(n);
	}

	@Override
	public void visit(X10ClassDecl_c n) {
		boolean v = context().firstKernel();
		context().firstKernel(true);
		try {
			super.visit(n);
		} finally {
			context().firstKernel(v);
		}
	}

	@Override
	public void visit(X10Instanceof_c n) {
		complainIfNot2(!generatingKernel(),
				"Runtime types not available in @CUDA code.", n, false);
		super.visit(n);
	}

	@Override
	public void visit(X10MethodDecl_c n) {
		//n.prettyPrint(System.out);
		//System.out.println();
		X10MethodDecl_c n2 = (X10MethodDecl_c) n.visit(new ConstantPropagator(tr.job(), tr.typeSystem(), tr.nodeFactory()).context(context()));
		//n2.prettyPrint(System.out);
		//System.out.println();
		super.visit(n2);
	}

	public static boolean postCompile(X10CPPCompilerOptions options,
			Compiler compiler, ErrorQueue eq) {
		if (options.post_compiler != null && !options.output_stdout) {
			Collection<String> compilationUnits = options.compilationUnits();
			String[] nvccCmd = { "nvcc", "--cubin", "-Xptxas", "-v",
					"-I" + options.distPath() + "/include", null };
			for (String f : compilationUnits) {
				if (f.endsWith(".cu")) {
					nvccCmd[5] = f;
					if (!X10CPPTranslator.doPostCompile(options, eq, compilationUnits, nvccCmd, true)) {
						eq.enqueue(ErrorInfo.WARNING, "Found @CUDA annotation, but not compiling for GPU because nvcc could not be run (check your $PATH).");
						return true;
					}
				}
			}

		}

		return true;
	}

} // end of CUDACodeGenerator

// vim:tabstop=4:shiftwidth=4:expandtab
