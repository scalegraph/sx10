/*
 *
 * (C) Copyright IBM Corporation 2006
 *
 *  This file is part of X10 Language.
 *
 */
package polyglot.ext.x10cpp.visit;

import java.io.File;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import polyglot.ast.Block;
import polyglot.ast.ClassDecl;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.FieldDecl;
import polyglot.ast.MethodDecl;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.SourceCollection;
import polyglot.ast.SourceFile;
import polyglot.ast.Stmt;
import polyglot.ast.TopLevelDecl;

import polyglot.ext.x10.ast.X10ClassDecl;

import polyglot.ext.x10cpp.Configuration;
import polyglot.ext.x10cpp.X10CPPCompilerOptions;
import polyglot.ext.x10cpp.types.X10CPPContext_c;
import polyglot.frontend.Compiler;
import polyglot.frontend.ExtensionInfo;
import polyglot.frontend.Job;
import polyglot.frontend.Source;
import polyglot.frontend.TargetFactory;

import polyglot.main.Options;
import polyglot.main.Report;

import polyglot.types.ClassType;
import polyglot.types.Context;
import polyglot.types.Name;
import polyglot.types.Package;
import polyglot.types.QName;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.ErrorInfo;
import polyglot.util.ErrorQueue;
import polyglot.util.InternalCompilerError;
import polyglot.util.QuotedStringTokenizer;
import polyglot.util.StdErrorQueue;
import polyglot.visit.Translator;
import x10c.util.StreamWrapper;
import x10c.util.WriterStreams;
import static polyglot.ext.x10cpp.visit.SharedVarsMethods.*;

public class X10CPPTranslator extends Translator {
    public static final class DelegateTargetFactory extends TargetFactory {
		protected String outputHeaderExtension;

		public DelegateTargetFactory(File dir, String ext, String hExt, boolean so) {
			super(dir, ext, so);
			if (outputDirectory == null)
				throw new InternalCompilerError("Output directory not set.");
			this.outputHeaderExtension = hExt;
		}

		public static String extractName(Source source) {
			String name = new File(source.name()).getName();
			return name.substring(0, name.lastIndexOf('.'));
		}

		/* (non-Javadoc)
		 * @see polyglot.frontend.TargetFactory#outputFile(java.lang.String, polyglot.frontend.Source)
		 */
		public File outputFile(String packageName, Source source) {
			return outputFile(packageName, extractName(source), source);
		}

		public File outputAsyncFile(String packageName, Source source) {
			return outputAsyncFile(packageName, extractName(source), source);
		}
		public File outputHeaderFile(String packageName, Source source) {
			return outputHeaderFile(packageName, extractName(source), source);
		}

		public String outputName(String packageName, Source source) {
			return outputName(packageName, extractName(source));
		}
		public String outputAsyncName(String packageName, Source source) {
			return outputAsyncName(packageName, extractName(source));
		}
		public String outputHeaderName(String packageName, Source source) {
			return outputHeaderName(packageName, extractName(source));
		}

		/* (non-Javadoc)
		 * @see polyglot.frontend.TargetFactory#outputWriter(java.lang.String, java.lang.String, polyglot.frontend.Source)
		 */
		public Writer outputWriter(String packageName, String className, Source source) throws IOException {
			// TODO Auto-generated method stub
			assert (false);
			return super.outputWriter(QName.make(packageName), Name.make(className), source);
		}

		/* (non-Javadoc)
		 * @see polyglot.frontend.TargetFactory#outputCodeWriter(java.io.File, int)
		 */
		public CodeWriter outputCodeWriter(File f, int width) throws IOException {
			// TODO Auto-generated method stub
			assert (false);
			return super.outputCodeWriter(f, width);
		}

		private String packagePath(String packageName) {
			if (packageName == null || packageName.equals(""))
				return "";
			return packageName.replace('.', '/') + '/';
		}

		private String mangleClassName(String className) {
			return Emitter.mangled_non_method_name(className);
		}

		public String outputName(String packageName, String className) {
			return packagePath(packageName) + mangleClassName(className) + "." + outputExtension;
		}

		/* (non-Javadoc)
		 * @see polyglot.frontend.TargetFactory#outputFile(java.lang.String, java.lang.String, polyglot.frontend.Source)
		 */
		public File outputFile(String packageName, String className, Source source) {
			File outputFile = new File(outputDirectory, outputName(packageName, className));

			if (source != null && outputFile.getPath().equals(source.path()))
				throw new InternalCompilerError("The output file is the same as the source file");

			return outputFile;
		}

		public String outputAsyncName(String packageName, String className) {
			return packagePath(packageName) + className + "." + outputExtension + "_int_";
//			return packagePath(packageName) + className + asyncExtension + "." + "inc";
		}

		public File outputAsyncFile(String packageName, String className, Source source) {
			File outputFile = new File(outputDirectory, outputAsyncName(packageName, className));

			if (source != null && outputFile.getPath().equals(source.path()))
				throw new InternalCompilerError("The Async file is the same as the source file");

			return outputFile;
		}

		public String outputHeaderName(String packageName, String className) {
			return packagePath(packageName) + mangleClassName(className) + "." + outputHeaderExtension;
		}

		public File outputHeaderFile(String packageName, String className, Source source) {
			File outputFile = new File(outputDirectory, outputHeaderName(packageName, className));

			if (source != null && outputFile.getPath().equals(source.path()))
				throw new InternalCompilerError("The header file is the same as the source file");

			return outputFile;
		}

		public String integratedOutputName(String packageName, String className, String ext) {
			return packagePath(packageName) + mangleClassName(className) + "." + ext;
		}

		public File integratedOutputFile(String packageName, String className, Source source, String ext) {
			File outputFile = new File(outputDirectory, 
			                           integratedOutputName(packageName, className, ext));

			if (source != null && outputFile.getPath().equals(source.path()))
				throw new InternalCompilerError("The header file is the same as the source file");

			return outputFile;
		}

		/* (non-Javadoc)
		 * @see polyglot.frontend.TargetFactory#outputWriter(java.io.File)
		 */
		public Writer outputWriter(File outputFile) throws IOException {
			// TODO Auto-generated method stub
			if (Report.should_report(Report.frontend, 2))
				Report.report(2, "Opening " + outputFile + " for output.");

			if (outputStdout)
				return new PrintWriter(System.out);

			if (!outputFile.getParentFile().exists()) {
				File parent = outputFile.getParentFile();
				parent.mkdirs();
			}

			return new FileWriter(outputFile);
		}
	}

	// FIXME: [IP] HACK - override creation of target factory in ExtensionInfo instead
	public X10CPPTranslator(Job job, TypeSystem ts, NodeFactory nf, TargetFactory tf) {
		super(job, ts, nf, createTargetFactory(job));
	}

	private static TargetFactory createTargetFactory(Job job) {
		Options options = job.extensionInfo().getOptions();
		return new DelegateTargetFactory(options.output_directory,
		                                 "cc", "h", options.output_stdout);
	}

	public DelegateTargetFactory targetFactory() {
	    return (DelegateTargetFactory) tf;
	}

	public void print(Node parent, Node n, CodeWriter w_) {
		if (w_ == null)
			return; // FIXME HACK
		StreamWrapper w = (StreamWrapper) w_;
		if (n != null && n.position().line() > 0 &&
				((n instanceof Stmt && !(n instanceof Block)) ||
				 (n instanceof FieldDecl) ||
				 (n instanceof MethodDecl) ||
				 (n instanceof ConstructorDecl) ||
				 (n instanceof ClassDecl)))
		{
			w.forceNewline(0);
			w.write("//#line " + n.position().line() + " \"" + n.position().file() + "\"");
			w.newline();
		}

		// FIXME: [IP] Some nodes have no del() -- warn in that case
		super.print(parent, n, w_);
	}

	/**
	 * Only for the backend -- use with care!
	 * FIXME: HACK!!! HACK!!! HACK!!!
	 */
	void setContext(Context c) {
		context = c;
	}

	/* (non-Javadoc)
	 * @see polyglot.visit.Translator#translateSource(polyglot.ast.SourceFile)
	 */
	protected boolean translateSource(SourceFile sfn) {
		DelegateTargetFactory tf = (DelegateTargetFactory) this.tf;

		int outputWidth = job.compiler().outputWidth();
		Collection outputFiles = job.compiler().outputFiles();

		// Find the public declarations in the file.  We'll use these to
		// derive the names of the target files.  There will be one
		// target file per public declaration.  If there are no public
		// declarations, we'll use the source file name to derive the
		// target file name.
		List exports = exports(sfn);

		try {
			String opfPath;

			String pkg = "";
			if (sfn.package_() != null) {
				Package p = sfn.package_().package_().get();
				pkg = p.fullName().toString();
			}

            // store all explicit imports in the context
            ((X10CPPContext_c)context).pendingImports.addAll(sfn.imports());

			WriterStreams wstreams = null;
			StreamWrapper sw = null;
			// Use the class name to derive a default output file name.
			for (Iterator i = sfn.decls().iterator(); i.hasNext(); ) {
				TopLevelDecl decl = (TopLevelDecl) i.next();
				if (!(decl instanceof X10ClassDecl))
					continue;
				X10ClassDecl cd = (X10ClassDecl) decl;
				String className = cd.classDef().name().toString();
				wstreams = new WriterStreams(className, sfn, pkg, tf, exports, job);
				sw = new StreamWrapper(wstreams, outputWidth);
				opfPath = tf.outputName(pkg, decl.name().toString());
				if (!opfPath.endsWith("$")) outputFiles.add(opfPath);
				translateTopLevelDecl(sw, sfn, decl); 
				if (i.hasNext())
					wstreams.commitStreams();
			}

			Iterator t = job().extensionInfo().scheduler().commandLineJobs().iterator();
			// FIXME: [IP] The following does the same as the prior code below.  Why the change?
			//	while (t.hasNext() && !t.next().equals(job()))
			//		;
			//	if (!t.hasNext())
			boolean filefound = false;
			while (t.hasNext()) {
				if (t.next().equals(job())) {
					filefound = true;
					break;
				}
			}

			wstreams.commitStreams();

			return true;
		}
		catch (IOException e) {
			job.compiler().errorQueue().enqueue(ErrorInfo.IO_ERROR,
					"I/O error while translating: " + e.getMessage());
			return false;
		}
	}

	private void generateClosureSwitch(StreamWrapper w) {
		X10CPPContext_c context = (X10CPPContext_c) this.context();
		Emitter emitter = new Emitter(this);

		w.write("extern \"C\" {"); w.newline(4); w.begin(0);
		w.write("x10aux::AnyClosure *__x10_callback_closureswitch(int id, "+
		        SERIALIZATION_BUFFER+"& s) {");
		w.newline(4); w.begin(0);
		w.write("switch (id) {"); w.newline(4); w.begin(0);
		// iterate through closures
		w.write("default: fprintf(stderr,\"Unrecognised closure id: %d\\n\",id); abort();");
		w.end() ; w.newline();
		w.write("}"); w.end(); w.newline();
		w.write("} // __x10_callback_closureswitch"); w.end(); w.newline();
		w.write("} // extern \"C\""); w.newline();
	}

	private void generateGlobalSwitch(StreamWrapper w) {
		X10CPPContext_c context = (X10CPPContext_c) this.context();
		DelegateTargetFactory tf = (DelegateTargetFactory) this.tf;
		Emitter emitter = new Emitter(this);
		for (Iterator k = context.classesWithArrayCopySwitches.keySet().iterator(); k.hasNext(); ) {
			ClassType ct = (ClassType) k.next();
			if (ct.isNested())
				ct = ct.container().toClass();
			String pkg = "";
			if (ct.package_() != null) {
				pkg = ct.package_().fullName().toString();
			}
			String header = tf.outputHeaderName(pkg, ct.name().toString());
			w.write("#include \"" + header + "\"");
			w.newline();
		}
		for (Iterator k = context.classesWithAsyncSwitches.keySet().iterator(); k.hasNext(); ) {
			ClassType ct = (ClassType) k.next();
			if (ct.isNested())
				ct = ct.container().toClass();
			String pkg = "";
			if (ct.package_() != null) {
				pkg = ct.package_().fullName().toString();
			}
			String header = tf.outputHeaderName(pkg, ct.name().toString());
			w.write("#include \"" + header + "\"");
			w.newline();
		}

/*
		w.write("extern \"C\" {");
		w.newline(4); w.begin(0);
		w.write(VOID_PTR+" "+ARRAY_COPY_SWITCH+"(" + CLOSURE_STRUCT + "* cl, x10_clock_t* clocks, int num_clocks) {");
		w.newline(4); w.begin(0);
		w.write("(void) clocks; (void) num_clocks;");
		w.newline();
		w.write("uint32_t h = cl->handler;");
		w.newline();
		w.write("switch (h) {");
		w.newline(4); w.begin(0);
		// FIXME: Replace with Java 5 loop. 
		for (Iterator k = context.classesWithArrayCopySwitches.keySet().iterator(); k.hasNext(); ) {
			ClassType ct = (ClassType) k.next();
			int[] async_ids = (int[]) context.classesWithArrayCopySwitches.get(ct);
			String className = emitter.translateType(ct);
			for (int i = 0; i < async_ids.length; i++) {
				w.write("case "+async_ids[i]+":");
				w.newline();
			}
			w.newline(4); w.begin(0);
			w.write("return "+className+"::"+ARRAY_COPY_SWITCH+"(cl, clocks, num_clocks);");
			w.end(); w.newline();
		}
		w.end(); w.newline();
		w.write("}");
		w.newline();
		w.write("return NULL;");
		w.end(); w.newline();
		w.write("}"); w.newline();

		w.write(VOID+" "+ASYNC_SWITCH+"(" + CLOSURE_STRUCT + "* cl, x10_clock_t* clocks, int num_clocks) {");
		w.newline(4); w.begin(0);
		w.write("(void) clocks; (void) num_clocks;");
		w.newline();
		w.write("uint32_t h = cl->handler;");
		w.newline();
		w.write("switch (h) {");
		w.newline(4); w.begin(0);
		for (Iterator k = context.classesWithAsyncSwitches.keySet().iterator(); k.hasNext(); ) {
			ClassType ct = (ClassType) k.next();
			int[] async_ids = (int[]) context.classesWithAsyncSwitches.get(ct);
			String className = emitter.translateType(ct);
			for (int i = 0; i < async_ids.length; i++) {
				w.write("case "+async_ids[i]+":");
				w.newline();
			}
			w.newline(4); w.begin(0);
			w.write(className+"::"+ASYNC_SWITCH+"(cl, clocks, num_clocks);");
			w.newline();
			w.write("break;");
			w.end(); w.newline();
		}
		w.end(); w.newline();
		w.write("}");
		w.end(); w.newline();
		w.write("}");
		w.end(); w.newline();
		w.write("}");
*/
		w.newline();
	}

	/* (non-Javadoc)
	 * @see polyglot.visit.Translator#translate(polyglot.ast.Node)
	 */
	public boolean translate(Node ast) {
		if (ast instanceof SourceFile) {
			SourceFile sfn = (SourceFile) ast;
			boolean okay = translateSource(sfn);
			final ExtensionInfo ext = job.extensionInfo();
			final Options options = ext.getOptions();
			final ErrorQueue eq = new StdErrorQueue(System.err,
					options.error_count,
					ext.compilerName());
			if (!okay)
				return false;
			if (!System.getProperty("x10.postcompile", "TRUE").equals("FALSE"))
				return postCompile(options, job.compiler(), eq);
			return true;
		}
		else if (ast instanceof SourceCollection) {
			SourceCollection sc = (SourceCollection) ast;
			// TODO: [IP] separate compilation
//			if (true) throw new InternalCompilerError("Source collections not supported");
			return translateSourceCollection(sc);
		}
		else {
			throw new InternalCompilerError("AST root must be a SourceFile; " +
			                                "found a " + ast.getClass().getName());
		}
	}

	public DelegateTargetFactory getTargetFactory() {
		return (DelegateTargetFactory) this.tf;
	}

    private static class CXXCommandBuilder {
        public static final String DUMMY = "-U___DUMMY___";

        public static final String X10LANG = System.getenv("X10LANG")==null?"../../../x10.runtime.17/src-cpp":System.getenv("X10LANG").replace(File.separatorChar, '/');
        public static final String X10LIB = System.getenv("X10LIB")==null?"../../../pgas/common/work":System.getenv("X10LIB").replace(File.separatorChar, '/');
        public static final String TRANSPORT = System.getenv("X10RT_TRANSPORT")==null?DEFAULT_TRANSPORT:System.getenv("X10RT_TRANSPORT");

        public static final String MANIFEST = "libx10lib17.mft";
        public static final String[] MANIFEST_LOCATIONS = new String[] {
            X10LANG,
            X10LANG+"/lib",
        };
        /** These go before the files */
        public static final String[] preArgs = new String[] {
            "-g",
            "-I"+X10LIB+"/include",
            "-I"+X10LANG,
            "-I"+X10LANG+"/gen", // FIXME: development option
            "-I"+X10LANG+"/include", // dist
            "-I.",
            "-DTRANSPORT="+TRANSPORT,
        };
        /** These go after the files */
        public static final String[] postArgs = new String[] {
            "-L"+X10LIB+"/lib",
            "-L"+X10LANG,
            "-L"+X10LANG+"/lib", // dist
            "-lx10lib17",
            "-lx10rt17",
            "-lupcrts_"+TRANSPORT,
            "-ldl",
            "-lm",
            "-lpthread",
        };
        public static final String BDWGCROOT = System.getenv("BDWGCROOT")==null?X10LANG+"/bdwgc/install":System.getenv("BDWGCROOT").replace(File.separatorChar, '/');
        /** These go before the files if gcEnabled is true */
        public static final String[] preArgsGC = new String[] {
            "-I"+BDWGCROOT+"/include",
            "-DX10_USE_BDWGC",
        };
        /** These go after the files if gcEnabled is true */
        public static final String[] postArgsGC = new String[] {
            BDWGCROOT+"/lib/libgc.a",
        };

        private final X10CPPCompilerOptions options;

        public CXXCommandBuilder(Options options) {
            assert (options != null);
            assert (options.post_compiler != null);
            this.options = (X10CPPCompilerOptions) options;
        }

        /** Is GC enabled on this platform? */
        protected boolean gcEnabled() { return false; }

        protected String defaultPostCompiler() { return "g++"; }

        /** Add the arguments that go before the output files */
        protected void addPreArgs(ArrayList<String> cxxCmd) {
            for (int i = 0; i < preArgs.length; i++) {
                cxxCmd.add(preArgs[i]);
            }
            if (!Configuration.DISABLE_GC && gcEnabled()) {
                for (int i = 0; i < preArgsGC.length; i++) {
                    cxxCmd.add(preArgsGC[i]);
                }
            }
        }

        /** Add the arguments that go after the output files */
        protected void addPostArgs(ArrayList<String> cxxCmd) {
            if (!Configuration.DISABLE_GC && gcEnabled()) {
                for (int i = 0; i < postArgsGC.length; i++) {
                    cxxCmd.add(postArgsGC[i]);
                }
            }
            for (int i = 0; i < postArgs.length; i++) {
                cxxCmd.add(postArgs[i]);
            }
        }

        protected void addExecutablePath(ArrayList<String> cxxCmd) {
            File exe = null;
            if (options.executable_path != null)
                exe = new File(options.executable_path);
            else if (Configuration.MAIN_CLASS != null)
                exe = new File(options.output_directory, Configuration.MAIN_CLASS);
            else
                return;
            cxxCmd.add("-o");
            cxxCmd.add(exe.getAbsolutePath().replace(File.separatorChar,'/'));
        }

        /** Construct the C++ compilation command */
        public final String[] buildCXXCommandLine(Collection<String> outputFiles) {
            String post_compiler = options.post_compiler;
            if (post_compiler.contains("javac"))
                post_compiler = defaultPostCompiler();

            QuotedStringTokenizer st = new QuotedStringTokenizer(post_compiler);
            int pc_size = st.countTokens();
            ArrayList<String> cxxCmd = new ArrayList<String>();
            String token = "";
            for (int i = 0; i < pc_size; i++) {
                token = st.nextToken();
                // The first '#' marks the place where the filenames go
                if (token.equals("#") || token.equals("%")) {
                    break;
                }
                cxxCmd.add(token);
            }

            boolean skipArgs = token.equals("%");
            if (!skipArgs) {
                addPreArgs(cxxCmd);
                addExecutablePath(cxxCmd);
            }

            HashSet<String> exclude = new HashSet<String>();
            try {
                String manifest = Configuration.MANIFEST;
                if (manifest == null) {
                    for (int i = 0; i < MANIFEST_LOCATIONS.length; i++) {
                        File x10lang_m = new File(MANIFEST_LOCATIONS[i]+"/"+MANIFEST);
                        if (!x10lang_m.exists())
                            continue;
                        manifest = x10lang_m.getPath();
                    }
                }
                if (manifest != null) {
                    FileReader fr = new FileReader(manifest);
                    BufferedReader br = new BufferedReader(fr);
                    String file = "";
                    while ((file = br.readLine()) != null)
                        exclude.add(file);
                }
            } catch (IOException e) { }

            Iterator iter = outputFiles.iterator();
            for (; iter.hasNext(); ) {
                String file = (String) iter.next();
                file = file.replace(File.separatorChar,'/');
                if (exclude.contains(file))
                    continue;
                cxxCmd.add(file);
            }

            while (st.hasMoreTokens()) {
                token = st.nextToken();
                // The second '#' delimits the libraries that have to go at the end
                if (token.equals("#") || token.equals("%")) {
                    break;
                }
                cxxCmd.add(token);
            }

            boolean skipLibs = token.equals("%");
            if (!skipLibs) {
                addPostArgs(cxxCmd);
            }

            while (st.hasMoreTokens()) {
                cxxCmd.add(st.nextToken());
            }

            return cxxCmd.toArray(new String[cxxCmd.size()]);
        }
    }

    private static class Cygwin_CXXCommandBuilder extends CXXCommandBuilder {
        /** These go before the files */
        public static final String[] preArgsCygwin = new String[] {
            "-msse2",
            "-mfpmath=sse",
        };
        /** These go after the files */
        public static final String[] postArgsCygwin = new String[] {
        };

        public Cygwin_CXXCommandBuilder(Options options) {
            super(options);
            assert (PLATFORM.startsWith("win32"));
        }

        protected void addPreArgs(ArrayList<String> cxxCmd) {
            super.addPreArgs(cxxCmd);
            for (int i = 0; i < preArgsCygwin.length; i++) {
                cxxCmd.add(preArgsCygwin[i]);
            }
        }

        protected void addPostArgs(ArrayList<String> cxxCmd) {
            super.addPostArgs(cxxCmd);
            for (int i = 0; i < postArgsCygwin.length; i++) {
                cxxCmd.add(postArgsCygwin[i]);
            }
        }
    }

    private static class Linux_CXXCommandBuilder extends CXXCommandBuilder {
        /** These go before the files */
        public static final String[] preArgsLinux = new String[] {
            "-pthread",
            "-msse2",
            "-mfpmath=sse",
        };
        /** These go after the files */
        public static final String[] postArgsLinux = new String[] {
            "-Wl,-export-dynamic",
            "-lrt",
        };

        public Linux_CXXCommandBuilder(Options options) {
            super(options);
            assert (PLATFORM.startsWith("linux"));
        }

        /** Disable for now.  TODO: enable */
        protected boolean gcEnabled() { return false; }

        protected void addPreArgs(ArrayList<String> cxxCmd) {
            super.addPreArgs(cxxCmd);
            for (int i = 0; i < preArgsLinux.length; i++) {
                cxxCmd.add(preArgsLinux[i]);
            }
        }

        protected void addPostArgs(ArrayList<String> cxxCmd) {
            super.addPostArgs(cxxCmd);
            for (int i = 0; i < postArgsLinux.length; i++) {
                cxxCmd.add(postArgsLinux[i]);
            }
        }
    }

    private static class AIX_CXXCommandBuilder extends CXXCommandBuilder {
        public static final boolean USE_XLC = System.getenv("USE_XLC")!=null;
        //"mpCC_r -q64 -qrtti=all -qarch=pwr5 -O3 -qtune=pwr5 -qhot -qinline"
        //"mpCC_r -q64 -qrtti=all"
        /** These go before the files */
        public static final String[] preArgsAIX = new String[] {
            USE_XLC ? "-q64" : "-maix64", // Assume 64-bit
            USE_XLC ? "-qrtti=all" : DUMMY,
            //USE_XLC ? DUMMY : "-pipe", // TODO: is this needed?
        };
        /** These go after the files */
        public static final String[] postArgsAIX = new String[] {
            USE_XLC ? "-bbigtoc" : "-Wl,-bbigtoc",
            USE_XLC ? DUMMY : "-Wl,-binitfini:poe_remote_main",
            USE_XLC ? DUMMY : "-L/usr/lpp/ppe.poe/lib",
            USE_XLC ? DUMMY : "-lmpi_r",
            USE_XLC ? DUMMY : "-lvtd_r",
            USE_XLC ? DUMMY : "-llapi_r",
        };

        public AIX_CXXCommandBuilder(Options options) {
            super(options);
            assert (PLATFORM.startsWith("aix"));
        }

        protected boolean gcEnabled() { return false; }

        protected String defaultPostCompiler() {
            if (USE_XLC)
                return "mpCC_r";
            return "g++";
        }

        protected void addPreArgs(ArrayList<String> cxxCmd) {
            super.addPreArgs(cxxCmd);
            for (int i = 0; i < preArgsAIX.length; i++) {
                cxxCmd.add(preArgsAIX[i]);
            }
        }

        protected void addPostArgs(ArrayList<String> cxxCmd) {
            super.addPostArgs(cxxCmd);
            for (int i = 0; i < postArgsAIX.length; i++) {
                cxxCmd.add(postArgsAIX[i]);
            }
        }
    }

    public static final String PLATFORM = System.getenv("X10_PLATFORM")==null?"unknown":System.getenv("X10_PLATFORM");
    public static final String DEFAULT_TRANSPORT = PLATFORM.startsWith("aix")?"lapi":"sockets";

    private static CXXCommandBuilder getCXXCommandBuilder(Options options, ErrorQueue eq) {
        if (PLATFORM.startsWith("win32"))
            return new Cygwin_CXXCommandBuilder(options);
        if (PLATFORM.startsWith("linux"))
            return new Linux_CXXCommandBuilder(options);
        if (PLATFORM.startsWith("aix"))
            return new AIX_CXXCommandBuilder(options);
        eq.enqueue(ErrorInfo.WARNING,
                "Unknown platform '"+PLATFORM+"'; using the default post-compiler (g++)");
        return new CXXCommandBuilder(options);
    }

    /**
	 * The post-compiler option has the following structure:
	 * "[pre-command with options (usually g++)] [(#|%) [post-options (usually extra files)] [(#|%) [library options]]]".
	 * Using '%' instead of '#' to delimit a section will cause the default values in that section to be omitted.
	 */
	public static boolean postCompile(Options options, Compiler compiler, ErrorQueue eq) {
		if (eq.hasErrors())
			return false;

		if (options.post_compiler != null && !options.output_stdout) {
            Collection<String> outputFiles = compiler.outputFiles();
            String[] cxxCmd = getCXXCommandBuilder(options, eq).buildCXXCommandLine(outputFiles);

			if (Report.should_report("postcompile", 1)) {
				StringBuffer cmdStr = new StringBuffer();
				for (int i = 0; i < cxxCmd.length; i++)
					cmdStr.append(cxxCmd[i]+" ");
				Report.report(1, "Executing post-compiler " + cmdStr);
			}

			try {
                Runtime runtime = Runtime.getRuntime();
				Process proc = runtime.exec(cxxCmd, null, options.output_directory);

				InputStreamReader err = new InputStreamReader(proc.getErrorStream());

				try {
					char[] c = new char[72];
					int len;
					StringBuffer sb = new StringBuffer();
					while((len = err.read(c)) > 0) {
						sb.append(String.valueOf(c, 0, len));
					}

					if (sb.length() != 0) {
						eq.enqueue(ErrorInfo.POST_COMPILER_ERROR, sb.toString());
					}
				}
				finally {
					err.close();
				}

				proc.waitFor();

				if (!options.keep_output_files) {
					String[] rmCmd = new String[1+outputFiles.size()];
					rmCmd[0] = "rm";
					Iterator iter = outputFiles.iterator();
					for (int i = 1; iter.hasNext(); i++)
						rmCmd[i] = (String) iter.next();
					runtime.exec(rmCmd);
				}

				if (proc.exitValue() > 0) {
					eq.enqueue(ErrorInfo.POST_COMPILER_ERROR,
							"Non-zero return code: " + proc.exitValue());
					return false;
				}
			}
			catch(Exception e) {
				eq.enqueue(ErrorInfo.POST_COMPILER_ERROR, e.getMessage());
				return false;
			}
			// FIXME: [IP] HACK: Prevent the java post-compiler from running
			options.post_compiler = null;
		}
		return true;
	}

	private boolean translateSourceCollection(SourceCollection sc) {
		boolean okay = true;

		for (Iterator i = sc.sources().iterator(); i.hasNext(); ) {
			SourceFile sfn = (SourceFile) i.next();
			okay &= translateSource(sfn);
		}

		if (true)
			throw new InternalCompilerError("Don't yet know how to translate source collections");
		return okay;
	}
}
