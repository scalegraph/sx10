/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 * This file was originally derived from the Polyglot extensible compiler framework.
 *
 *  (C) Copyright 2000-2007 Polyglot project group, Cornell University
 *  (C) Copyright IBM Corporation 2007-2014.
 */

package polyglot.frontend;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import polyglot.ast.NodeFactory;
import polyglot.ast.PackageNode;
import polyglot.ast.SourceFile;
import polyglot.types.QName;
import polyglot.types.TypeSystem;
import polyglot.types.reflect.ClassFileLoader;
import polyglot.util.CodeWriter;
import polyglot.util.ErrorInfo;
import polyglot.util.ErrorLimitError;
import polyglot.util.ErrorQueue;
import polyglot.util.InternalCompilerError;
import polyglot.util.OptimalCodeWriter;
import polyglot.util.SimpleCodeWriter;
import polyglot.util.StdErrorQueue;
import x10.optimizations.inlining.DeclStore;
import x10.optimizations.inlining.Inliner;
import x10.util.CollectionFactory;

/**
 * This is the main entry point for the compiler. It contains a work list that
 * contains entries for all classes that must be compiled (or otherwise worked
 * on).
 */
public class Compiler
{
    
    /** The extension info */
    private ExtensionInfo extensionInfo;

    /** A list of all extension infos active in this compiler. */
    private List<ExtensionInfo> allExtensions;

    /** The error queue handles outputting error messages. */
    private ErrorQueue eq;

    /** AST information retained for use by the Inliner.
     *
     *  This needs to be preserved until all Job's have been inlined.
     *  It may be blown away after the code-gen barrier.
     *
     */
    private DeclStore inlinerData;
    
    public DeclStore getInlinerData(Job job, TypeSystem ts, NodeFactory nf) {
        if (null == inlinerData) 
            inlinerData = new DeclStore(ts, nf);
        inlinerData.startJob(job);
        return inlinerData;
    }

    /**
     * Do this only after all inlining is over (i.e. after the code-gen barrier)!
     */
    public void purgeInlinerData() {
        inlinerData = null;
    }

    /** FIXME: TEMPRORARY Inliner hack: Errors in speculative compilation for inlining should not be fatal
     * @depricated DO NOT USE
     * TODO remove this, if inlining results in an error, compilation should fail!
     */
    public ErrorQueue swapErrorQueue (ErrorQueue newEq) {
        ErrorQueue oldEq = eq;
        eq = newEq;
        return oldEq;
    }
    /**
     * Class file loader.  There should be only one of these so we can cache
     * across type systems.
     */
    private ClassFileLoader loader;

    /**
     * The output files generated by the compiler.  This is used to to call the
     * post-compiler (e.g., javac).
     */
    private Map<String,Collection<String>> outputFiles = CollectionFactory.newHashMap();

    /**
     * Initialize the compiler.
     *
     * @param extensionInfo the <code>ExtensionInfo</code> this compiler is for.
     */
    public Compiler(ExtensionInfo extensionInfo) {    
        this(extensionInfo, new StdErrorQueue(System.err, 
                                              extensionInfo.getOptions().error_count,
                                              extensionInfo.compilerName()));
    }
        
    /**
     * Initialize the compiler.
     *
     * @param extensionInfo the <code>ExtensionInfo</code> this compiler is for.
     */
    public Compiler(ExtensionInfo extensionInfo, ErrorQueue eq) {
        this.extensionInfo = extensionInfo;
        this.eq = eq;
        this.allExtensions = new ArrayList<ExtensionInfo>(2);
        
        loader = new ClassFileLoader(extensionInfo);

        // This must be done last.
        extensionInfo.initCompiler(this);
    }

    /*** Compiler statistics gatherer. */
    public Stats stats;

    /** Return a mapping from input files to all output files produced from that input file 
     * */
    public Map<String, Collection<String>> outputFiles() {
        return outputFiles;
    }

    public Collection<String> flatOutputFiles() {
        Set<String> ans = CollectionFactory.newHashSet();
        for (Collection<String> files : outputFiles.values()) {
            ans.addAll(files);
        }
        
        return ans;
    }
    
    /**
     * Add an output file
     * @param source the source file
     * @param output the output file
     */
    public void addOutputFile(String source, String output) {
        Collection<String> outputs = outputFiles.get(source);
        if (outputs == null) {
            outputs = CollectionFactory.newHashSet();
            outputFiles.put(source, outputs);
        }
        outputs.add(output);
    }
    
    /**
     * Add an output file
     * @param source the source
     * @param output the output file
     */
    public void addOutputFile(SourceFile source, String output) {
        PackageNode pkg = source.package_();
        String key = pkg == null ? "" : pkg.package_().get().fullName().toString() + ".";
        key += source.source().name().substring(0, source.source().name().lastIndexOf(".x10"));
        key = key.replace('.', File.separatorChar);
        addOutputFile(key, output);
    }
    
    /**
     * Add an output file
     * @param gname the qualified name of the source entity
     * @param output the output file
     */
    public void addOutputFile(QName qname, String output) {
        String key = qname.toString().replace('.', File.separatorChar);
        addOutputFile(key, output);
    }
    
    /**
     * Compile all the files listed in the set of strings <code>source</code>.
     * Return true on success. The method <code>outputFiles</code> can be
     * used to obtain the output of the compilation.  This is the main entry
     * point for the compiler, called from main().
     */
    public boolean compileFiles(Collection<String> filenames) {
        List<Source> sources = new ArrayList<Source>(filenames.size());

        // Construct a list of sources from the list of file names.
        try {
            try {
                SourceLoader source_loader = sourceExtension().sourceLoader();

                for (String sourceName : filenames) {
                    // mark this source as being explicitly specified
                    // by the user.
                    FileSource source = source_loader.fileSource(sourceName, true);

                    sources.add(source);
                }
            }
            catch (FileNotFoundException e) {
                eq.enqueue(ErrorInfo.IO_ERROR,
                    "Cannot find source file \"" + e.getMessage() + "\".");
                eq.flush();
                return false;
	    }
	    catch (IOException e) {
		eq.enqueue(ErrorInfo.IO_ERROR, e.getMessage());
                eq.flush();
                return false;
	    }
	    catch (InternalCompilerError e) {
                // Report it like other errors, but rethrow to get the stack
                // trace.
		try {
                    eq.enqueue(ErrorInfo.INTERNAL_ERROR, e.message(),
                               e.position());
		}
		catch (ErrorLimitError e2) {
		}

		eq.flush();
		throw e;
	    }
	    catch (RuntimeException e) {
		// Flush the error queue, then rethrow to get the stack trace.
		eq.flush();
		throw e;
	    }
        }
	catch (ErrorLimitError e) {
            eq.flush();
            return false;
	}

        return compile(sources);
    }

    /**
     * Compile all the files listed in the set of Sources <code>source</code>.
     * Return true on success. The method <code>outputFiles</code> can be
     * used to obtain the output of the compilation.  This is the main entry
     * point for the compiler, called from main().
     */
    public boolean compile(Collection<Source> sources) {
	boolean okay = false;
    
	try {
	    try {
                Scheduler scheduler = sourceExtension().scheduler();

                // clearing state
                final x10.ExtensionInfo x10ext = (x10.ExtensionInfo) extensionInfo;
                x10ext.warningSet().clear(); // again, to clear caching of warnings (to prevent duplicates)
                x10ext.errorSet().clear();
                scheduler.clearAll(sources); // to clear the fail flag of the scheduler

                List<Job> jobs = new ArrayList<Job>();

                // Create a job for each source file.
                for (Source source : sources) {
                    if (scheduler.sourceHasJob(source)) continue; // in case we invoke compile again and we already compiled this source
                    // Add a new SourceJob for the given source. If a Job for the source
                    // already exists, then we will be given the existing job.
                    Job job = scheduler.addJob(source);
                    jobs.add(job);
                }

                scheduler.setCommandLineJobs(jobs);

                for (Job job : jobs) {
                    scheduler.addDependenciesForJob(job, true);
                }

                // Compile the files to completion.
                okay = scheduler.runToCompletion();
	    }
	    catch (InternalCompilerError e) {
		// Report it like other errors, but rethrow to get the stack trace.
		try {
		    eq.enqueue(ErrorInfo.INTERNAL_ERROR, e.message() != null ? e.message() : "InternalCompilerError", e.position());
		}
		catch (ErrorLimitError e2) {
		}
		eq.flush();
		throw e;
	    }
	    catch (StackOverflowError e) {
		// Flush the error queue, then rethrow to get the stack trace.
		eq.flush();
		throw e;
	    }
	    catch (RuntimeException e) {
		// Flush the error queue, then rethrow to get the stack trace.
		eq.flush();
		throw e;
	    }
	}
	catch (ErrorLimitError e) {
	}

	eq.flush();

	return okay;
    }

    /** Get the compiler's class file loader. */
    public ClassFileLoader loader() {
        return this.loader;
    }

    /** Should fully qualified class names be used in the output? */
    public boolean useFullyQualifiedNames() {
        return extensionInfo.getOptions().fully_qualified_names;
    }

    /** Return a list of all languages extensions active in the compiler. */
    public void addExtension(ExtensionInfo ext) {
        allExtensions.add(ext);
    }

    /** Return a list of all languages extensions active in the compiler. */
    public List<ExtensionInfo> allExtensions() {
        return allExtensions;
    }

    /** Get information about the language extension being compiled. */
    public ExtensionInfo sourceExtension() {
	return extensionInfo;
    }

    /** Maximum number of characters on each line of output */
    public int outputWidth() {
        return extensionInfo.getOptions().output_width;
    }

    /** Should class info be serialized into the output? */
    public boolean serializeClassInfo() {
	return extensionInfo.getOptions().serialize_type_info;
    }

    /** Get the compiler's error queue. */
    public ErrorQueue errorQueue() {
	return eq;
    }

    static {
      // FIXME: if we get an io error (due to too many files open, for example)
      // it will throw an exception. but, we won't be able to do anything with
      // it since the exception handlers will want to load
      // polyglot.util.CodeWriter and polyglot.util.ErrorInfo to print and
      // enqueue the error; but the classes must be in memory since the io
      // can't open any files; thus, we force the classloader to load the class
      // file.
      try {
	ClassLoader loader = Compiler.class.getClassLoader();
	// loader.loadClass("polyglot.util.CodeWriter");
	// loader.loadClass("polyglot.util.ErrorInfo");
	loader.loadClass("polyglot.util.StdErrorQueue");
      }
      catch (ClassNotFoundException e) {
	throw new InternalCompilerError(e.getMessage());
      }
    }
    
    public static CodeWriter createCodeWriter(OutputStream w) {
        return createCodeWriter(w, Globals.Options().output_width);
    }
    public static CodeWriter createCodeWriter(OutputStream w, int width) {
        if (Globals.Options().use_simple_code_writer)
            return new SimpleCodeWriter(w, width);
        else
	    return new OptimalCodeWriter(w, width);
    }
    public static CodeWriter createCodeWriter(Writer w) {
        return createCodeWriter(w, Globals.Options().output_width);
    }
    public static CodeWriter createCodeWriter(Writer w, int width) {
        if (Globals.Options().use_simple_code_writer)
            return new SimpleCodeWriter(w, width);
        else
            return new OptimalCodeWriter(w, width);
    }
}
