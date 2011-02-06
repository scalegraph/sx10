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

package x10cpp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;

import polyglot.ast.NodeFactory;
import polyglot.frontend.AllBarrierGoal;
import polyglot.frontend.BarrierGoal;
import polyglot.frontend.Compiler;
import polyglot.frontend.Globals;
import polyglot.frontend.Goal;
import polyglot.frontend.Job;
import polyglot.frontend.JobExt;
import polyglot.frontend.OutputGoal;
import polyglot.frontend.Scheduler;
import polyglot.frontend.VisitorGoal;
import polyglot.main.Options;
import polyglot.types.MemberClassResolver;
import polyglot.types.SemanticException;
import polyglot.types.TopLevelResolver;
import polyglot.types.TypeSystem;
import polyglot.util.ErrorQueue;
import polyglot.visit.ContextVisitor;
import polyglot.visit.PostCompiled;
import polyglot.util.InternalCompilerError;
import x10.Configuration;
import x10.X10CompilerOptions;
import x10.ExtensionInfo.X10Scheduler.ValidatingVisitorGoal;
import x10.ast.X10NodeFactory_c;
import x10.optimizations.Optimizer;
import x10.visit.CheckNativeAnnotationsVisitor;
import x10.visit.NativeClassVisitor;
import x10.visit.StaticNestedClassRemover;
import x10.visit.X10InnerClassRemover;
import x10cpp.ast.X10CPPDelFactory_c;
import x10cpp.ast.X10CPPExtFactory_c;
import x10cpp.postcompiler.CXXCommandBuilder;
import x10cpp.postcompiler.PrecompiledLibrary;
import x10cpp.types.X10CPPSourceClassResolver;
import x10cpp.types.X10CPPTypeSystem_c;
import x10cpp.visit.X10CPPTranslator;


/**
 * Extension information for x10 extension.
 * @author vj -- Adapted from the Polyglot2 ExtensionsInfo for X10 1.5
 */
public class ExtensionInfo extends x10.ExtensionInfo {


	public String compilerName() {
		return "x10c++";
	}

	public polyglot.main.Version version() {
		return new Version();
	}

	protected NodeFactory createNodeFactory() {
		return new X10NodeFactory_c(this, new X10CPPExtFactory_c(), new X10CPPDelFactory_c()) { };
	}

	protected TypeSystem createTypeSystem() {
		return new X10CPPTypeSystem_c();
	}

	@Override
    protected void initTypeSystem() {
        X10CPPCompilerOptions opts = (X10CPPCompilerOptions) getOptions();
        // Inline from superclass, replacing SourceClassResolver
        try {
            for (PrecompiledLibrary pco:opts.x10libs) {
                manifest.add(pco.sourceJar);
                manifest.addAll(pco.sourceFiles);
             }

            TopLevelResolver r =
                new X10CPPSourceClassResolver(compiler, this, getOptions().constructFullClasspath(),
                                              getOptions().compile_command_line_only,
                                              getOptions().ignore_mod_times);


            // Resolver to handle lookups of member classes.
            if (true || TypeSystem.SERIALIZE_MEMBERS_WITH_CONTAINER) {
                MemberClassResolver mcr = new MemberClassResolver(ts, r, true);
                r = mcr;
            }

            ts.initialize(r, this);
        }
        catch (SemanticException e) {
            throw new InternalCompilerError(
                "Unable to initialize type system: " + e.getMessage(), e);
        }
    }

    @Override
    public JobExt jobExt() {
        return new X10CPPJobExt();
    }

    // =================================
	// X10-specific goals and scheduling
	// =================================
	protected Scheduler createScheduler() {
		return new X10CPPScheduler(this);
	}

	public static class X10CPPScheduler extends x10.ExtensionInfo.X10Scheduler {
		protected X10CPPScheduler(ExtensionInfo extInfo) {
			super(extInfo);
		}

		@Override
		public ExtensionInfo extensionInfo() {
		    return (ExtensionInfo) this.extInfo;
		}

		@Override
		public String nativeAnnotationLanguage() { return "c++"; }

		@Override
		public Goal CodeGenerated(Job job) {
			TypeSystem ts = extInfo.typeSystem();
			NodeFactory nf = extInfo.nodeFactory();
			return new ValidatingOutputGoal(job, new X10CPPTranslator(job, ts, nf, extInfo.targetFactory())).intern(this);
		}
		@Override
		protected Goal PostCompiled() {
		    return new PostCompiled(extInfo) {
                private static final long serialVersionUID = 1834245937046911633L;

                protected boolean invokePostCompiler(Options options, Compiler compiler, ErrorQueue eq) {
		            if (System.getProperty("x10.postcompile", "TRUE").equals("FALSE"))
		                return true;
		            return X10CPPTranslator.postCompile((X10CPPCompilerOptions)options, compiler, eq);
		        }
		    }.intern(this);
		}
		@Override
		protected Goal codegenPrereq(Job job) {
		    return StaticNestedClassRemover(job);
		}
		@Override
		public List<Goal> goals(Job job) {
		    List<Goal> goals = super.goals(job);
		    FinallyEliminator(job).addPrereq(Lowerer(job));
		    for (Goal g: Optimizer.goals(this, job)) {
		        FinallyEliminator(job).addPrereq(g);
		    }
		    StaticNestedClassRemover(job).addPrereq(FinallyEliminator(job));
		    return goals;
		}
	}

	// TODO: [IP] Override targetFactory() (rather, add createTargetFactory to polyglot)

	protected X10CPPCompilerOptions createOptions() {
		return new X10CPPCompilerOptions(this);
	}

	public X10CPPCompilerOptions getOptions() {
	    return (X10CPPCompilerOptions) super.getOptions();
	}
}
// vim:tabstop=4:shiftwidth=4:expandtab
