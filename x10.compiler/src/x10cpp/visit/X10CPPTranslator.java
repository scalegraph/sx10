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

import java.io.File;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import polyglot.ast.Assert;
import polyglot.ast.Block;
import polyglot.ast.Branch;
import polyglot.ast.Catch;
import polyglot.ast.ClassDecl;
import polyglot.ast.ClassMember;
import polyglot.ast.CompoundStmt;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.Eval;
import polyglot.ast.FieldDecl;
import polyglot.ast.For;
import polyglot.ast.Formal;
import polyglot.ast.If;
import polyglot.ast.LocalDecl;
import polyglot.ast.MethodDecl;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.ProcedureDecl;
import polyglot.ast.Return;
import polyglot.ast.SourceCollection;
import polyglot.ast.SourceFile;
import polyglot.ast.Stmt;
import polyglot.ast.SwitchBlock;
import polyglot.ast.TopLevelDecl;
import polyglot.ast.Try;


import polyglot.frontend.Compiler;
import polyglot.frontend.ExtensionInfo;
import polyglot.frontend.Job;
import polyglot.frontend.Source;
import polyglot.frontend.TargetFactory;

import polyglot.main.Options;
import polyglot.main.Report;

import polyglot.types.ClassType;
import polyglot.types.Context;
import polyglot.types.MemberDef;
import polyglot.types.MethodDef;
import polyglot.types.Package;
import polyglot.types.QName;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.CodeWriter;
import polyglot.util.ErrorInfo;
import polyglot.util.ErrorQueue;
import polyglot.util.InternalCompilerError;
import polyglot.util.SimpleCodeWriter;
import polyglot.util.StdErrorQueue;
import polyglot.util.StringUtil;
import polyglot.visit.Translator;
import x10.ast.ForLoop;
import x10.ast.X10ClassDecl;
import x10.extension.X10Ext;
import x10.types.X10ClassDef;
import x10.types.X10ClassType;
import x10.types.X10TypeSystem_c;
import x10.util.ClassifiedStream;
import x10.util.StreamWrapper;
import x10.util.WriterStreams;
import x10.visit.StaticNestedClassRemover;
import x10cpp.Configuration;
import x10cpp.X10CPPCompilerOptions;
import x10cpp.X10CPPJobExt;
import x10cpp.debug.LineNumberMap;
import x10cpp.postcompiler.AIX_CXXCommandBuilder;
import x10cpp.postcompiler.CXXCommandBuilder;
import x10cpp.postcompiler.Cygwin_CXXCommandBuilder;
import x10cpp.postcompiler.Linux_CXXCommandBuilder;
import x10cpp.postcompiler.SunOS_CXXCommandBuilder;
import x10cpp.types.X10CPPContext_c;
import static x10cpp.visit.ASTQuery.getCppRep;
import static x10cpp.visit.ASTQuery.getStringPropertyInit;
import static x10cpp.visit.SharedVarsMethods.*;

public class X10CPPTranslator extends Translator {
	
	public X10CPPTranslator(Job job, TypeSystem ts, NodeFactory nf, TargetFactory tf) {
		super(job, ts, nf, tf);
	}
    
	/** Return the dir where classes in the given package will be compiled.  Does not include output directory prefix.
	 * Accepts null input.  Note that if appending more paths to this directory, it is necessary to use '/' as a
	 * separator regardless of the platform. */
	public static String packagePath (String pkg) {
		return (pkg==null ? "" : pkg.replace('.', '/') + '/');
	}
	
	/** Return the filename of the c++ file for the given class.  Does not include output directory prefix.
	 * If no package then give null.  Note that if further processing the returned value, it is necessary to use '/' as a
	 * separator regardless of the platform. */
	public static String outputFileName (String pkg, String c, String ext) {
		return packagePath(pkg) + Emitter.mangled_non_method_name(c) + "." + ext;
	}
	
	/** Return the c++ file for the given class. */
	public static File outputFile (Options opts, String pkg, String c, String ext) {
		return new File(opts.output_directory, outputFileName(pkg, c, ext));
	}
    
	private static int adjustSLNForNode(int outputLine, Node n) {
	    // FIXME: Debugger HACK: adjust for loops
	    if (n instanceof For || n instanceof ForLoop)
	        return outputLine + 1;
	    return outputLine;
	}

	private static int adjustELNForNode(int outputLine, Node n) {
	    if (n instanceof For || n instanceof ForLoop)
	        return outputLine - 2;
	    if (n instanceof Return && ((Return) n).expr() == null)
	        return outputLine;
	    if (n instanceof Eval || n instanceof Branch || n instanceof Try)
	        return outputLine;
	    if (n instanceof Block)
	        return outputLine;
	    return outputLine - 1;
	}

	private static final String FILE_TO_LINE_NUMBER_MAP = "FileToLineNumberMap";

	public void print(Node parent, Node n, CodeWriter w_) {
		if (w_ == null)
			return; // FIXME HACK
		StreamWrapper w = (StreamWrapper) w_;
		assert (n != null);
		final int line = n.position().line();
		final int column = n.position().column();
		final String file = n.position().file();
		if (line > 0 &&
				((n instanceof Stmt && !(n instanceof Block) && !(n instanceof Catch)) ||
				 (n instanceof FieldDecl) ||
				 (n instanceof MethodDecl) ||
				 (n instanceof ConstructorDecl) ||
				 (n instanceof ClassDecl)))
		{
			w.forceNewline(0);
			w.write("//#line " + line + " \"" + file + "\": "+n.getClass().getName());
			w.newline();
		}
		
		if (x10.Configuration.DEBUG && n instanceof Stmt && !(n instanceof Assert) && !(n instanceof Block) && !(n instanceof Catch) && !(parent instanceof If))
		{
			w.write("_X10_STATEMENT_HOOK()");
			if (!(parent instanceof For))
				w.write("; ");
			else
				w.write(", ");
		}
		
		final int startLine = w.currentStream().getStreamLineNumber(); // for debug info

		// FIXME: [IP] Some nodes have no del() -- warn in that case
		super.print(parent, n, w_);

		final int endLine = w.currentStream().getStreamLineNumber(); // for debug info

		if (x10.Configuration.DEBUG && line > 0 &&
		    ((n instanceof Stmt && !(n instanceof SwitchBlock) && !(n instanceof Catch)) ||
		     (n instanceof ClassMember)))
		{
		    final String cppFile = w.getStreamName(w.currentStream().ext);
		    String key = w.getStreamName(StreamWrapper.CC);
		    X10CPPContext_c c = (X10CPPContext_c) context;
		    HashMap<String, LineNumberMap> fileToLineNumberMap =
		        c.<HashMap<String, LineNumberMap>>findData(FILE_TO_LINE_NUMBER_MAP);
		    if (fileToLineNumberMap != null) {
		        final LineNumberMap lineNumberMap = fileToLineNumberMap.get(key);
		        // [DC] avoid NPE when writing to .cu files
		        if (lineNumberMap != null) {
		            final MemberDef def =
		                (n instanceof Block) ?
		                    (parent instanceof MethodDecl) ? ((MethodDecl) parent).methodDef() :
		                    (parent instanceof ConstructorDecl) ? ((ConstructorDecl) parent).constructorDef()
		                    : null
		                : null;
		            final int lastX10Line = parent.position().endLine();
		            if (n instanceof Stmt) {
		                final int adjustedStartLine = adjustSLNForNode(startLine, n);
		                final int adjustedEndLine = adjustELNForNode(endLine, n);
		                final int fixedEndLine = adjustedEndLine < adjustedStartLine ? adjustedStartLine : adjustedEndLine;
		                w.currentStream().registerCommitListener(new ClassifiedStream.CommitListener() {
		                    public void run(ClassifiedStream s) {
		                        int cppStartLine = s.getStartLineOffset()+adjustedStartLine;
		                        int cppEndLine = s.getStartLineOffset()+fixedEndLine;
//		                        System.out.println("Adding line number entry: "+cppFile+":"+cppStartLine+"-"+cppEndLine+"->"+file+":"+line);
		                        lineNumberMap.put(cppFile, cppStartLine, cppEndLine, file, line, column);
		                        if (def != null) {
		                            lineNumberMap.addMethodMapping(def, cppFile, cppStartLine, cppEndLine, lastX10Line);
		                        }
		                    }
		                });
		            }
		            if (n instanceof FieldDecl && !c.inTemplate()) // the c.inTemplate() skips mappings for templates, which don't have a fixed size.
		            	lineNumberMap.addClassMemberVariable(((FieldDecl)n).name().toString(), ((FieldDecl)n).type().toString(), Emitter.mangled_non_method_name(context.currentClass().toString()));
		            else if (n instanceof LocalDecl && !((LocalDecl)n).position().isCompilerGenerated())
		            	lineNumberMap.addLocalVariableMapping(((LocalDecl)n).name().toString(), ((LocalDecl)n).type().toString(), line, lastX10Line, file, false);
		            else if (def != null)
		            {
		            	// include method arguments in the local variable tables
		            	List<Formal> args = ((ProcedureDecl)parent).formals();
		            	for (int i=0; i<args.size(); i++)
		            		lineNumberMap.addLocalVariableMapping(args.get(i).name().toString(), args.get(i).type().toString(), line, lastX10Line, file, false);
		            	// include "this" for non-static methods		            	
		            	if (!def.flags().isStatic() && ((ProcedureDecl)parent).reachable())
		            		lineNumberMap.addLocalVariableMapping("this", Emitter.mangled_non_method_name(context.currentClass().toString()), line, lastX10Line, file, true);
		            }
		        }
		    }
		}
	}

	/**
	 * Only for the backend -- use with care!
	 * FIXME: HACK!!! HACK!!! HACK!!!
	 */
	public void setContext(Context c) {
		context = c;
	}

    private static void maybeCopyTo (String file, String src_path_, String dest_path_) {
		File src_path = new File(src_path_);
    	File dest_path = new File(dest_path_);
    	// don't copy if the two dirs are the same...
    	if (src_path.equals(dest_path)) return;
    	if (!dest_path.exists()) dest_path.mkdir();
    	assert src_path.isDirectory() : src_path_+" is not a directory";
    	assert dest_path.isDirectory() : dest_path_+" is not a directory";
    	try {
    		dest_path.mkdirs();
			FileInputStream src = new FileInputStream(new File(src_path_+file));
	    	FileOutputStream dest = new FileOutputStream(new File(dest_path_+file));
	    	int b;
	    	while ((b = src.read()) != -1) {
	    		dest.write(b);
	    	}
    	} catch (IOException e) {
        	System.err.println("While copying "+file + " from "+src_path_+" to "+dest_path_);
    		System.err.println(e);
    	}
    }

	/* (non-Javadoc)
	 * @see polyglot.visit.Translator#translateSource(polyglot.ast.SourceFile)
	 */
	protected boolean translateSource(SourceFile sfn) {

		int outputWidth = job.compiler().outputWidth();
		Collection<String> outputFiles = job.compiler().outputFiles();

		try {

			String pkg = null;
			if (sfn.package_() != null) {
				Package p = sfn.package_().package_().get();
				pkg = p.fullName().toString();
			}

			X10CPPContext_c c = (X10CPPContext_c) context;
			X10CPPCompilerOptions opts = (X10CPPCompilerOptions) job.extensionInfo().getOptions();
	        X10TypeSystem_c xts = (X10TypeSystem_c) typeSystem();

			if (x10.Configuration.DEBUG)
				c.addData(FILE_TO_LINE_NUMBER_MAP, new HashMap<String, LineNumberMap>());

			// Use the class name to derive a default output file name.
			for (TopLevelDecl decl : sfn.decls()) {
				if (!(decl instanceof X10ClassDecl))
					continue;
				X10ClassDecl cd = (X10ClassDecl) decl;
				// Skip output of all files for a native rep class.
		        X10Ext ext = (X10Ext) cd.ext();
		        try {
		            String path = new File(cd.position().file()).getParent();
		            if (path==null) path = ""; else path = path + '/';
		            String out_path = opts.output_directory.toString();
		            if (out_path==null) out_path = ""; else out_path = out_path + '/';
		            String pkg_ = packagePath(pkg);
		            List<X10ClassType> as = ext.annotationMatching((Type) xts.systemResolver().find(QName.make("x10.compiler.NativeCPPInclude")));
		            for (Type at : as) {
		                ASTQuery.assertNumberOfInitializers(at, 1);
		                String include = getStringPropertyInit(at, 0);
		                outputFiles.add(pkg_+include);
		                maybeCopyTo(include, path, out_path+pkg_);
		            }
		            as = ext.annotationMatching((Type) xts.systemResolver().find(QName.make("x10.compiler.NativeCPPOutputFile")));
		            for (Type at : as) {
		                ASTQuery.assertNumberOfInitializers(at, 1);
		                String file = getStringPropertyInit(at, 0);
		                outputFiles.add(pkg_+file);
		                maybeCopyTo(file, path, out_path+pkg_);
		            }
		            as = ext.annotationMatching((Type) xts.systemResolver().find(QName.make("x10.compiler.NativeCPPCompilationUnit")));
		            for (Type at : as) {
		                ASTQuery.assertNumberOfInitializers(at, 1);
		                String compilation_unit = getStringPropertyInit(at, 0);
		                outputFiles.add(pkg_+compilation_unit);
		                opts.compilationUnits().add(pkg_+compilation_unit);
		                maybeCopyTo(compilation_unit, path, out_path+pkg_);
		            }
		        } catch (SemanticException e) {
		            assert false : e;
		        }

	        	if (getCppRep((X10ClassDef)cd.classDef()) != null) {
					continue;
				}
				String className = cd.classDef().name().toString();
				WriterStreams wstreams = new WriterStreams(className, pkg, job, tf);
				StreamWrapper sw = new StreamWrapper(wstreams, outputWidth);
				// [DC] TODO: This hack is to ensure the .inc is always generated.
				sw.getNewStream(StreamWrapper.Closures, true);
				// [IP] FIXME: This hack is to ensure the .cc is always generated.
				sw.getNewStream(StreamWrapper.CC, true);
                // [DC] TODO: This hack is to ensure the .h is always generated.
                sw.getNewStream(StreamWrapper.Header, true);

				String closures = wstreams.getStreamName(StreamWrapper.Closures);
				String cc = wstreams.getStreamName(StreamWrapper.CC);
				String header = wstreams.getStreamName(StreamWrapper.Header);

				outputFiles.add(closures);
				outputFiles.add(cc);
				outputFiles.add(header);
				opts.compilationUnits().add(cc);
				
				if (x10.Configuration.DEBUG) {
					HashMap<String, LineNumberMap> fileToLineNumberMap =
					    c.<HashMap<String, LineNumberMap>>getData(FILE_TO_LINE_NUMBER_MAP);
					fileToLineNumberMap.put(closures, new LineNumberMap());
					fileToLineNumberMap.put(cc, new LineNumberMap());
					fileToLineNumberMap.put(header, new LineNumberMap());
				}
				
				translateTopLevelDecl(sw, sfn, decl);
				
				if (x10.Configuration.DEBUG) {
					HashMap<String, LineNumberMap> fileToLineNumberMap =
					    c.<HashMap<String, LineNumberMap>>getData(FILE_TO_LINE_NUMBER_MAP);
//					sw.pushCurrentStream(sw.getNewStream(StreamWrapper.Closures, false));
//					printLineNumberMap(sw, pkg, className, StreamWrapper.Closures, fileToLineNumberMap);
//					sw.popCurrentStream();
//					sw.pushCurrentStream(sw.getNewStream(StreamWrapper.CC, false));
//					printLineNumberMap(sw, pkg, className, StreamWrapper.CC, fileToLineNumberMap);
//					sw.popCurrentStream();
//					sw.pushCurrentStream(sw.getNewStream(StreamWrapper.CC, false));
//					printLineNumberMap(sw, pkg, className, StreamWrapper.Header, fileToLineNumberMap);
//					sw.popCurrentStream();
					sw.pushCurrentStream(sw.getNewStream(StreamWrapper.CC, false));
					printLineNumberMapForCPPDebugger(sw, fileToLineNumberMap);
					sw.popCurrentStream();
				}
				
				wstreams.commitStreams();
			}

			return true;
		}
		catch (IOException e) {
			job.compiler().errorQueue().enqueue(ErrorInfo.IO_ERROR,
					"I/O error while translating: " + e.getMessage());
			return false;
		}
	}

	private void printLineNumberMapForCPPDebugger(StreamWrapper sw, HashMap<String, LineNumberMap> fileToLineNumberMap) {
	    final LineNumberMap map = fileToLineNumberMap.get(sw.getStreamName(StreamWrapper.CC));
	    sw.currentStream().registerCommitListener(new ClassifiedStream.CommitListener() {
	        public void run(ClassifiedStream s) {
//	            if (map.isEmpty())
//	                return;
	            s.forceNewline();
	            LineNumberMap.exportForCPPDebugger(s, map);
	        }
	    });
	}

	private void printLineNumberMap(StreamWrapper sw, String pkg, String className, final String ext, HashMap<String, LineNumberMap> fileToLineNumberMap) {
		String fName = sw.getStreamName(ext);
		final LineNumberMap map = fileToLineNumberMap.get(fName);
		final String lnmName = Emitter.translate_mangled_FQN(pkg, "_")+"_"+Emitter.mangled_non_method_name(className);
		sw.currentStream().registerCommitListener(new ClassifiedStream.CommitListener() {
		    public void run(ClassifiedStream s) {
		        if (map.isEmpty())
		            return;
		        s.forceNewline();
//		        sw.write("struct LNMAP_"+lnmName+"_"+ext+" { static const char* map; };");
//		        sw.newline();
//		        sw.write("const char* LNMAP_"+lnmName+"_"+ext+"::map = \"");
		        s.write("extern \"C\" { const char* LNMAP_"+lnmName+"_"+ext+" = \"");
		        s.write(StringUtil.escape(map.exportMap()));
//		        String v = map.exportMap();
//		        LineNumberMap m = LineNumberMap.importMap(v);
		        s.write("\"; }");
		        s.newline();
		    }
		});
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


	public static final String postcompile = "postcompile";

	public static final String MAIN_STUB_NAME = "xxx_main_xxx";

	/**
	 * The post-compiler option has the following structure:
	 * "[pre-command with options (usually g++)] [(#|%) [post-options (usually extra files)] [(#|%) [library options]]]".
	 * Using '%' instead of '#' to delimit a section will cause the default values in that section to be omitted.
	 */
	public static boolean postCompile(X10CPPCompilerOptions options, Compiler compiler, ErrorQueue eq) {
		if (eq.hasErrors())
			return false;

		if (options.post_compiler != null && !options.output_stdout) {
			// use set to avoid duplicates
			Set<String> compilationUnits = new HashSet<String>(options.compilationUnits());

			try {
			    final File file = outputFile(options, null, MAIN_STUB_NAME, "cc");
			    ExtensionInfo ext = compiler.sourceExtension();
			    SimpleCodeWriter sw = new SimpleCodeWriter(ext.targetFactory().outputWriter(file),
			            compiler.outputWidth());
			    List<MethodDef> mainMethods = new ArrayList<MethodDef>();
			    for (Job job : ext.scheduler().commandLineJobs()) {
			        mainMethods.addAll(getMainMethods(job));
			    }
			    if (mainMethods.size() < 1) {
			        // If there are no main() methods in the command-line jobs, try other files
			        for (Job job : ext.scheduler().jobs()) {
			            mainMethods.addAll(getMainMethods(job));
			        }
			    }
			    if (mainMethods.size() < 1) {
			        eq.enqueue(ErrorInfo.SEMANTIC_ERROR, "No main method found");
			        return false;
			    } else if (mainMethods.size() > 1) {
			        eq.enqueue(ErrorInfo.SEMANTIC_ERROR,
			                "Multiple main() methods found, please specify MAIN_CLASS:"+listMethods(mainMethods));
			        return false;
			    }
			    assert (mainMethods.size() == 1);
			    X10ClassType container = (X10ClassType) Types.get(mainMethods.get(0).container());
			    MessagePassingCodeGenerator.processMain(container, sw);
			    sw.flush();
			    sw.close();
			    compilationUnits.add(file.getName());
			}
			catch (IOException e) {
			    eq.enqueue(ErrorInfo.IO_ERROR, "I/O error while translating: " + e.getMessage());
			    return false;
			}

			CXXCommandBuilder ccb = CXXCommandBuilder.getCXXCommandBuilder(options, eq);
			String[] cxxCmd = ccb.buildCXXCommandLine(compilationUnits);

			if (!doPostCompile(options, eq, compilationUnits, cxxCmd)) return false;

			// FIXME: [IP] HACK: Prevent the java post-compiler from running
			options.post_compiler = null;
		}
		return true;
	}

	private static List<MethodDef> getMainMethods(Job job) {
	    X10CPPJobExt jobext = (X10CPPJobExt) job.ext();
	    if (Configuration.MAIN_CLASS != null) {
	        QName mainClass = QName.make(Configuration.MAIN_CLASS);
	        try {
	            ClassType mct = (ClassType) job.extensionInfo().typeSystem().forName(mainClass);
	            QName pkgName = mct.package_() == null ? null : mct.package_().fullName();
	            mainClass = QName.make(pkgName, StaticNestedClassRemover.mangleName(mct.def()));
	        } catch (SemanticException e) { }
	        for (MethodDef md : jobext.mainMethods()) {
	            QName containerName = ((X10ClassType) Types.get(md.container())).fullName();
	            if (containerName.equals(mainClass)) {
	                return Collections.singletonList(md);
	            }
	        }
	        return Collections.<MethodDef>emptyList();
	    } else {
	        return jobext.mainMethods();
	    }
	}

	private static String listMethods(List<MethodDef> mainMethods) {
	    StringBuilder sb = new StringBuilder();
	    for (MethodDef md : mainMethods) {
            sb.append("\n\t").append(md.toString());
        }
	    return sb.toString();
	}

    public static boolean doPostCompile(Options options, ErrorQueue eq, Collection<String> outputFiles, String[] cxxCmd) {
    	return doPostCompile(options, eq, outputFiles, cxxCmd, false);
    }
    public static boolean doPostCompile(Options options, ErrorQueue eq, Collection<String> outputFiles, String[] cxxCmd, boolean noError) {
        if (Report.should_report(postcompile, 1)) {
        	StringBuffer cmdStr = new StringBuffer();
        	for (int i = 0; i < cxxCmd.length; i++)
        		cmdStr.append(cxxCmd[i]+" ");
        	Report.report(1, "Executing post-compiler " + cmdStr);
        }

        try {
            Runtime runtime = Runtime.getRuntime();
        	Process proc = runtime.exec(cxxCmd, null, options.output_directory);

        	InputStreamReader err = new InputStreamReader(proc.getErrorStream());

        	String output = null;
        	try {
        		char[] c = new char[72];
        		int len;
        		StringBuffer sb = new StringBuffer();
        		while((len = err.read(c)) > 0) {
        			sb.append(String.valueOf(c, 0, len));
        		}

        		if (sb.length() != 0) {
        			output = sb.toString();
        		}
        	}
        	finally {
        		err.close();
        	}

        	proc.waitFor();

        	if (!options.keep_output_files) {
        		String[] rmCmd = new String[1+outputFiles.size()];
        		rmCmd[0] = "rm";
        		Iterator<String> iter = outputFiles.iterator();
        		for (int i = 1; iter.hasNext(); i++)
        			rmCmd[i] = iter.next();
        		runtime.exec(rmCmd);
        	}

        	if (output != null)
        		eq.enqueue((proc.exitValue() > 0 && !noError) ? ErrorInfo.POST_COMPILER_ERROR : ErrorInfo.WARNING, output);
        	if (proc.exitValue() > 0) {
        		eq.enqueue(noError?ErrorInfo.WARNING:ErrorInfo.POST_COMPILER_ERROR,
        				"Non-zero return code: " + proc.exitValue());
        		return false;
        	}
        }
        catch(Exception e) {
        	eq.enqueue(noError?ErrorInfo.WARNING:ErrorInfo.POST_COMPILER_ERROR, e.getMessage() != null ? e.getMessage() : e.toString());
        	return false;
        }
        return true;
    }

	private boolean translateSourceCollection(SourceCollection sc) {
		boolean okay = true;

		for (SourceFile sfn : sc.sources()) {
			okay &= translateSource(sfn);
		}

		if (true)
			throw new InternalCompilerError("Don't yet know how to translate source collections");
		return okay;
	}
}
