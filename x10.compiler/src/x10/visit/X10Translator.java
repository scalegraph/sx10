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

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Iterator;

import polyglot.ast.Block;
import polyglot.ast.ClassDecl;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.FieldDecl;
import polyglot.ast.MethodDecl;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.SourceFile;
import polyglot.ast.Stmt;
import polyglot.ast.TopLevelDecl;
import polyglot.frontend.Compiler;
import polyglot.frontend.Job;
import polyglot.frontend.TargetFactory;
import polyglot.main.Reporter;
import polyglot.types.Package;
import polyglot.types.QName;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.ErrorInfo;
import polyglot.util.ErrorQueue;
import polyglot.util.QuotedStringTokenizer;
import polyglot.visit.Translator;
import x10.X10CompilerOptions;
import x10.util.FileUtils;
import x10c.X10CCompilerOptions;

public class X10Translator extends Translator {
	
    private boolean inInnerClass;

    public X10Translator(Job job, TypeSystem ts, NodeFactory nf, TargetFactory tf) {
           super(job, ts, nf, tf);
           inInnerClass = false;
    }
    
    private static String escapePath(String path) {
    	StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.length(); ++i) {
            char c = path.charAt(i);
            if (c == '\\') {
//                sb.append(c).append(c);
                sb.append('/');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
    @Override
    public void print(Node parent, Node n, CodeWriter w) {
    	assert n != null;
    	int line = n.position().line();
    	String file = n.position().file();
        if (line > 0 &&
                ((n instanceof Stmt && !(n instanceof Block)) ||
                 (n instanceof FieldDecl) ||
                 (n instanceof MethodDecl) ||
                 (n instanceof ConstructorDecl) ||
                 (n instanceof ClassDecl)))
        {
//            w.write("\n//#line " + line + "\n");
            w.write("\n//#line " + line + " \"" + escapePath(file) + "\"\n");
        }

        super.print(parent, n, w);
    }


	public boolean inInnerClass() {
		return inInnerClass;
	}

	public X10Translator inInnerClass(boolean inInnerClass) {
		if (inInnerClass == this.inInnerClass) return this;
		X10Translator tr = (X10Translator) shallowCopy();
		tr.inInnerClass = inInnerClass;
		return tr;
	}
	
	/** Override to not open a new file for each declaration. */
	@Override
	protected boolean translateSource(SourceFile sfn) {
	    TypeSystem ts = typeSystem();
	    NodeFactory nf = nodeFactory();
	    TargetFactory tf = this.tf;
	    int outputWidth = job.compiler().outputWidth();
	    CodeWriter w= null;

	    try {
	        File of;

	        QName pkg = null;

	        if (sfn.package_() != null) {
	            Package p = sfn.package_().package_().get();
	            pkg = p.fullName();
	        }

	        // Use the source name to derive a default output file name.
	        of = tf.outputFile(pkg, sfn.source());

	        String opfPath = of.getPath();
	        if (!opfPath.endsWith("$")) job.compiler().addOutputFile(sfn, of.getPath());
	        w = tf.outputCodeWriter(of, outputWidth);

	        writeHeader(sfn, w);

	        for (Iterator<TopLevelDecl> i = sfn.decls().iterator(); i.hasNext(); ) {
	            TopLevelDecl decl = i.next();

	            translateTopLevelDecl(w, sfn, decl);

	            if (i.hasNext()) {
	                w.newline(0);
	            }
	        }

	        w.flush();

            X10CompilerOptions options = (X10CompilerOptions) ts.extensionInfo().getOptions();
            if (options.post_compiler != null && !options.output_stdout && options.executable_path != null) {
                // copy *.x10 to output_directory in order to add them in a jar file
                File sourceFile = null; 
                File targetFile = null;
                try {
                    String sourceFilepath = sfn.source().toString();
                    sourceFile = new File(sourceFilepath);
                    if (sourceFile.isFile()) {
                        String targetDirpath = options.output_directory.getAbsolutePath();
                        if (pkg != null) {
                            targetDirpath += File.separator + pkg.toString().replace('.', File.separatorChar);
                        }
                        File targetDir = new File(targetDirpath);
//                        targetDir.mkdirs();
                        targetFile = new File(targetDir, sfn.source().name());
                        FileUtils.copyFile(sourceFile, targetFile);
                    }
                } finally {
                    if (sourceFile != null && targetFile != null) { 
                        targetFile.setLastModified(sourceFile.lastModified());
                    }
                }
            }

	        return true;
	    }
	    catch (IOException e) {
	        job.compiler().errorQueue().enqueue(ErrorInfo.IO_ERROR,
	                "I/O error while translating: " + e.getMessage());
	        return false;
	    } finally {
	        if (w != null) {
	            try {
	                w.close();
	            } catch (IOException e) {
	                job.compiler().errorQueue().enqueue(ErrorInfo.IO_ERROR,
	                        "I/O error while closing output file: " + e.getMessage());
	            }
	        }
	    }
	}

    public static final String postcompile = "postcompile";

    public static boolean postCompile(X10CompilerOptions options, Compiler compiler, ErrorQueue eq) {
        if (eq.hasErrors())
            return false;

        if (options.post_compiler != null && !options.output_stdout) {
            Runtime runtime = Runtime.getRuntime();
            java.util.ArrayList<String> javacCmd = new java.util.ArrayList<String>();
            String[] strarray = new String[0];
            QuotedStringTokenizer st = new QuotedStringTokenizer(options.post_compiler, '?');
            while (st.hasMoreTokens()) {
            	javacCmd.add(st.nextToken());
            }
            
            javacCmd.add("-classpath");
            javacCmd.add(options.constructPostCompilerClasspath());
            
            javacCmd.add("-encoding");
            javacCmd.add("utf-8");
            
//            javacCmd.add("-warn:+boxing");	// only for ecj

            for (Collection<String> files : compiler.outputFiles().values()) {
                javacCmd.addAll(files);
            }

            Reporter reporter = options.reporter;
            if (reporter.should_report(postcompile, 1)) {
            	StringBuilder cmdStr = new StringBuilder();                
                for (int i = 0; i < javacCmd.size(); i++)
                    cmdStr.append(javacCmd.get(i)+" ");
                reporter.report(1, "Executing post-compiler " + cmdStr);
            }

            try {
                Process proc = runtime.exec(javacCmd.toArray(strarray));

                InputStreamReader err = new InputStreamReader(proc.getErrorStream());

                try {
                    char[] c = new char[72];
                    int len;
                    StringBuilder sb = new StringBuilder();
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
                	java.util.ArrayList<String> rmCmd = new java.util.ArrayList<String>();
                	rmCmd.add("rm");
                	for (Collection<String> files : compiler.outputFiles().values()) {
                	    rmCmd.addAll(files);
                	}
                    runtime.exec(rmCmd.toArray(strarray));
                }

                if (proc.exitValue() > 0) {
                    eq.enqueue(ErrorInfo.POST_COMPILER_ERROR,
                            "Non-zero return code: " + proc.exitValue());
                    return false;
                }

                if (options.executable_path != null) {  // -o executable_path
                    // create jar file
                    
                    java.util.ArrayList<String> jarCmdList = new java.util.ArrayList<String>();
                    jarCmdList.add(X10CCompilerOptions.findJavaCommand("jar"));
                    
                    // create Main-Class attribute from main (= first) source name if MAIN_CLASS is not specified
                    String main_class = options.x10_config.MAIN_CLASS;
                    // Fix for XTENLANG-2410
                    // Guessing main_class from the first .x10 file is incorrect, because
                    // 1) main_source may not be relative path from currect directory
                    // 2) the first .x10 file may not have $Main class.
                    /*
                    if (main_class == null) {
                        String main_source = ((X10CCompilerOptions) options).main_source;
                        if (main_source != null) {
                            main_class = main_source.substring(0, main_source.length() - ".x10".length());
                        }
                    }
                    */
                    
                    // create manifest file
                    File manifest = File.createTempFile("x10c.manifest.", null);
                    manifest.deleteOnExit();    // TODO delete explicitly
                    java.io.PrintWriter out = new java.io.PrintWriter(new java.io.FileWriter(manifest));
                    if (main_class != null) {
                        // add Main-Class attribute for executable jar
                        out.println("Main-Class: " + main_class + "$" + X10PrettyPrinterVisitor.MAIN_CLASS);
                        // TODO Cannot add x10.jar in Class-Path attribute because it will be loaded by system class loader and static initialization will fail
                        //out.println("Class-Path: x10.jar commons-math-2.2.jar");
                    }
                    out.println("Created-By: " + compiler.sourceExtension().compilerName() + " version " + compiler.sourceExtension().version());
                    out.close();

                    // create directory for jar file
                    File jarFile = new File(options.executable_path);
                    File directoryHoldingJarFile = jarFile.getParentFile();
                    if (directoryHoldingJarFile != null) {
                    	directoryHoldingJarFile.mkdirs();
                    }
                    
                    // execute "jar cmf ${manifest_file} ${executable_path} -C ${output_directory} ."
                    jarCmdList.add("cmf");
                    jarCmdList.add(manifest.getAbsolutePath());
                    jarCmdList.add(options.executable_path);
                    jarCmdList.add("-C");
                    jarCmdList.add(options.output_directory.getAbsolutePath()); // -d output_directory
                    jarCmdList.add(".");
                    
                    String[] jarCmd = jarCmdList.toArray(strarray);
                    Process jarProc = runtime.exec(jarCmd);
                    InputStreamReader jarErr = new InputStreamReader(jarProc.getErrorStream());
                    try {
                        char[] c = new char[72];
                        int len;
                        StringBuilder sb = new StringBuilder();
                        while ((len = jarErr.read(c)) > 0) {
                            sb.append(String.valueOf(c, 0, len));
                        }
                        if (sb.length() != 0) {
                            eq.enqueue(ErrorInfo.POST_COMPILER_ERROR, sb.toString());
                        }
                    }
                    finally {
                        jarErr.close();
                    }
                    jarProc.waitFor();

                    if (jarProc.exitValue() > 0) {
                        eq.enqueue(ErrorInfo.POST_COMPILER_ERROR, "Non-zero return code: " + jarProc.exitValue());
                        return false;
                    }
                }
            }
            catch(Exception e) {
                eq.enqueue(ErrorInfo.POST_COMPILER_ERROR, e.getMessage());
                return false;
            }
        }
        return true;
    }
}
