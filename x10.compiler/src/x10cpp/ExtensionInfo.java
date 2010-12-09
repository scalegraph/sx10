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
import polyglot.main.Report;
import polyglot.types.MemberClassResolver;
import polyglot.types.SemanticException;
import polyglot.types.TopLevelResolver;
import polyglot.types.TypeSystem;
import polyglot.util.ErrorQueue;
import polyglot.visit.ContextVisitor;
import polyglot.visit.PostCompiled;
import polyglot.util.InternalCompilerError;
import x10.Configuration;
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
        // Inline from superclass, replacing SourceClassResolver
        try {
            if (Configuration.MANIFEST == null) {
                String[] MANIFEST_LOCATIONS = CXXCommandBuilder.MANIFEST_LOCATIONS;
                for (int i = 0; i < MANIFEST_LOCATIONS.length; i++) {
                    File x10lang_m = new File(MANIFEST_LOCATIONS[i]+"/"+CXXCommandBuilder.MANIFEST);
                    if (!x10lang_m.exists())
                        continue;
                    Configuration.MANIFEST = x10lang_m.getPath();
                }
            }
            // FIXME: [IP] HACK
            if (Report.should_report("manifest", 1))
                Report.report(1, "Manifest is "+Configuration.MANIFEST);
            if (Configuration.MANIFEST != null) {
                try {
                    FileReader fr = new FileReader(Configuration.MANIFEST);
                    BufferedReader br = new BufferedReader(fr);
                    String file = "";
                    while ((file = br.readLine()) != null)
                        if (file.endsWith(".x10") || file.endsWith(".jar")) // FIXME: hard-codes the source extension.
                            manifest.add(file);
                } catch (IOException e) { }
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
		    if (x10.Configuration.EXPERIMENTAL) {
		        FinallyEliminator(job).addPrereq(Lowerer(job));
		        for (Goal g: Optimizer.goals(this, job)) {
		            FinallyEliminator(job).addPrereq(g);
		        }
		        StaticNestedClassRemover(job).addPrereq(FinallyEliminator(job));
		    } else {
		        StaticNestedClassRemover(job).addPrereq(Lowerer(job));
		        for (Goal g: Optimizer.goals(this, job)) {
		            StaticNestedClassRemover(job).addPrereq(g);
		        }
		    }
		    return goals;
		}
	}

	// TODO: [IP] Override targetFactory() (rather, add createTargetFactory to polyglot)

	protected Options createOptions() {
		return new X10CPPCompilerOptions(this);
	}
}
// vim:tabstop=4:shiftwidth=4:expandtab
