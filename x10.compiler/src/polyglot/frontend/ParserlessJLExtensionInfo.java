/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.frontend;

import java.io.Reader;

import polyglot.ast.NodeFactory;
import polyglot.ast.NodeFactory_c;
import polyglot.main.Version;
import polyglot.types.LoadedClassResolver;
import polyglot.types.MemberClassResolver;
import polyglot.types.SemanticException;
import polyglot.types.SourceClassResolver;
import polyglot.types.TopLevelResolver;
import polyglot.types.TypeSystem;
import polyglot.types.TypeSystem_c;
import polyglot.util.ErrorQueue;
import polyglot.util.InternalCompilerError;

/** This class implements most of the <code>ExtensionInfo</code> for the Java language.
 * It does not include a parser, however.  EPL-licensed extensions should extend this class
 * rather than JLExtensionInfo since they should not use the CUP-based grammar.
 * @author nystrom
 *
 */
public abstract class ParserlessJLExtensionInfo extends AbstractExtensionInfo {

    protected void initTypeSystem() {
        try {
            LoadedClassResolver lr;
            lr = new SourceClassResolver(compiler, this, getOptions().constructFullClasspath(),
                                         compiler.loader(), true,
                                         getOptions().compile_command_line_only,
                                         getOptions().ignore_mod_times);

            TopLevelResolver r = lr;

            // Resolver to handle lookups of member classes.
            if (TypeSystem.SERIALIZE_MEMBERS_WITH_CONTAINER) {
                MemberClassResolver mcr = new MemberClassResolver(ts, lr, true);
                r = mcr;
            }

            ts.initialize(r, this);
        }
        catch (SemanticException e) {
            throw new InternalCompilerError(
                "Unable to initialize type system: " + e.getMessage(), e);
        }
    }
    
    protected polyglot.frontend.Scheduler createScheduler() {
        return new JLScheduler(this);
    }

    public String defaultFileExtension() {
        return "jl";
    }

    public String compilerName() {
        return "jlc";
    }

    public Version version() {
        return new JLVersion();
    }

    /** Create the type system for this extension. */
    abstract protected TypeSystem createTypeSystem();

    /** Create the node factory for this extension. */
    abstract protected NodeFactory createNodeFactory();

    public JobExt jobExt() {
      return null;
    }

    /**
     * Return a parser for <code>source</code> using the given
     * <code>reader</code>.
     */
    public abstract Parser parser(Reader reader, FileSource source, ErrorQueue eq);
    
    static { Topics t = new Topics(); }
}
