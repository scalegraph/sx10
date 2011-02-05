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

package x10.types;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import polyglot.frontend.ClassPathResourceLoader;
import polyglot.frontend.Compiler;
import polyglot.frontend.CyclicDependencyException;
import polyglot.frontend.ExtensionInfo;
import polyglot.frontend.FileSource;
import polyglot.frontend.Goal;
import polyglot.frontend.Job;
import polyglot.frontend.Resource;
import polyglot.frontend.Scheduler;
import polyglot.main.Reporter;
import polyglot.types.Named;
import polyglot.types.NoClassException;
import polyglot.types.QName;
import polyglot.types.SemanticException;
import polyglot.types.TopLevelResolver;
import polyglot.types.TypeSystem;
import polyglot.util.CollectionUtil;
import x10.util.CollectionFactory;
import polyglot.util.InternalCompilerError;

/**
 * Loads class information from class files, or serialized class infomation from
 * within class files. It does not load from source files.
 */
public class X10SourceClassResolver implements TopLevelResolver {
    protected TypeSystem ts;
    protected Reporter reporter;
    protected String classpath;
    protected Set<QName> nocache;

    protected final static Collection<String> report_topics = CollectionUtil.list(Reporter.types, Reporter.resolver, Reporter.loader);

    protected Compiler compiler;
    protected ExtensionInfo ext;
    protected boolean compileCommandLineOnly;
    protected boolean ignoreModTimes;

    /**
     * Create a loaded class resolver.
     * 
     * @param compiler
     *            The compiler.
     * @param ext
     *            The extension to load sources for.
     * @param classpath
     *            The class path.
     * @param compileCommandLineOnly
     *            TODO
     * @param ignoreModTimes
     *            TODO
     */
    public X10SourceClassResolver(Compiler compiler, ExtensionInfo ext, String classpath, boolean compileCommandLineOnly, boolean ignoreModTimes) {

        this.ts = (TypeSystem) ext.typeSystem();
        this.reporter = ext.getOptions().reporter;
        this.classpath = classpath;
        this.nocache = CollectionFactory.newHashSet();

        this.compiler = compiler;
        this.ext = ext;
        this.compileCommandLineOnly = compileCommandLineOnly;
        this.ignoreModTimes = ignoreModTimes;
    }

    ClassPathResourceLoader loader;
    
    /**
     * Load a class file for class <code>name</code>.
     */
    protected Resource loadFile(QName name) {
        if (nocache.contains(name)) {
            return null;
        }
        
        if (loader == null)
            loader = new ClassPathResourceLoader(classpath);

        try {
            String fileName = name.toString().replace('.', '/');
            fileName += ".class";
            Resource clazz = loader.loadResource(fileName);

            if (clazz == null) {
                if (reporter.should_report(report_topics, 4)) {
                    reporter.report(4, "Class " + name + " not found in classpath " + loader.classpath());
                }
            }
            else {
                if (reporter.should_report(report_topics, 4)) {
                    reporter.report(4, "Class " + name + " found in classpath " + loader.classpath());
                }
                return clazz;
            }
        }
        catch (ClassFormatError e) {
            if (reporter.should_report(report_topics, 4))
                reporter.report(4, "Class " + name + " format error");
        }

        nocache.add(name);

        return null;
    }
    
    public Named getEncodedType(Resource clazz, QName name) throws SemanticException {
        return null;
    }

    /**
     * Manifest support.
     * @param name
     * @return whether the given class should be considered part of the final executable
     */
    protected boolean isOutput(QName name) {
        String fname = name.toString().replace('.', '/')+".x10"; // FIXME: hard-codes the source extension.
        return !((x10.ExtensionInfo) ext).manifestContains(fname);
    }

    /**
     * Manifest support.
     * @param name
     * @return whether the given class should be compiled
     */
    private boolean shouldCompile(QName name) {
        return !compileCommandLineOnly && isOutput(name);
    }

    public Named find(QName name) throws SemanticException {
        TypeSystem ts = (TypeSystem) this.ts;

        if (name.equals(QName.make("void")))
            return (Named) ts.Void();

        if (reporter.should_report(report_topics, 3))
            reporter.report(3, "SourceCR.find(" + name + ")");

        Resource clazz = null;
        FileSource source = null;

        // First try the class file.
        clazz = loadFile(name);
        
        // Now, try to find the source file.
        source = ext.sourceLoader().classSource(name);

        // Check if a job for the source already exists.
        if (source != null && ext.scheduler().sourceHasJob(source)) {
            // the source has already been compiled; what are we doing here?
            return getTypeFromSource(source, name, shouldCompile(name));
        }
        
        if (source == null) {
            throw new NoClassException(name.toString());
        }

        // Check if the .class file exists; if so we don't need to compile the source completely.
        // We decide which to use based on modification times.
        if (clazz != null) {
            long classModTime = clazz.file().lastModified();
            long sourceModTime = source.lastModified().getTime();

            if (!ignoreModTimes && classModTime < sourceModTime) {
                if (reporter.should_report(report_topics, 3))
                    reporter.report(3, "Source file version is newer than compiled for " + name + ".");
                clazz = null;
            } else {
                handleUpToDateTarget(name, clazz);
            }
        }

        Named result = null;
        
        if (clazz != null) {
            if (reporter.should_report(report_topics, 4))
                reporter.report(4, "Using encoded class type for " + name);
            
            try {
                result = getEncodedType(clazz, name);
            }
            catch (SemanticException e) {
                if (reporter.should_report(report_topics, 4))
                    reporter.report(4, "Could not load encoded class " + name);
                clazz = null;
            }
        }
        
        if (result == null && source != null) {
            if (reporter.should_report(report_topics, 4))
                reporter.report(4, "Using source file for " + name);
            result = getTypeFromSource(source, name, shouldCompile(name) && clazz == null);
        }

        // Verify that the type we loaded has the right name. This prevents,
        // for example, requesting a type through its mangled (class file) name.
        if (result != null) {
            return result;
        }

        throw new NoClassException(name.toString());
    }
    
    protected void handleUpToDateTarget(QName name, Resource file) {
    }

    public boolean packageExists(QName name) {
        if (ext.sourceLoader().packageExists(name)) {
            return true;
        }
        return false;
    }
    
    protected Named getTypeFromSource(FileSource source, QName name, boolean compile) throws SemanticException {
        Scheduler scheduler = ext.scheduler();
        Job job = scheduler.loadSource(source, compile);
        
        if (reporter.should_report("sourceloader", 3))
            new Exception("loaded " + source).printStackTrace();
        
        if (job != null) {
            Named n = ts.systemResolver().check(name);
        
            if (n != null) {
                return n;
            }
        
            Goal g = scheduler.PreTypeCheck(job);
        
            if (!scheduler.reached(g)) {
                try {
                    scheduler.attempt(g);
                }
                catch (CyclicDependencyException e) {
                    throw new InternalCompilerError("Could not initialize symbol table for " + source + "; cyclic dependency found.", e);
                }
            }
        
            n = ts.systemResolver().check(name);
        
            if (n != null) {
                return n;
            }
        }
        
        // The source has already been compiled, but the type was not created
        // there.
        throw new NoClassException("Could not find \"" + name + "\" in " + source + ".");
    }
}
