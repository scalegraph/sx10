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

package x10;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import polyglot.ast.ClassMember;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.SourceFile;
import polyglot.ast.TopLevelDecl;
import polyglot.ast.TypeNode;
import polyglot.frontend.AllBarrierGoal;
import polyglot.frontend.BarrierGoal;
import polyglot.frontend.Compiler;
import polyglot.frontend.CyclicDependencyException;
import polyglot.frontend.FileResource;
import polyglot.frontend.FileSource;
import polyglot.frontend.ForgivingVisitorGoal;
import polyglot.frontend.Globals;
import polyglot.frontend.Goal;
import polyglot.frontend.JLScheduler;
import polyglot.frontend.Job;
import polyglot.frontend.JobExt;
import polyglot.frontend.OutputGoal;
import polyglot.frontend.Parser;
import polyglot.frontend.ParserGoal;
import polyglot.frontend.Scheduler;
import polyglot.frontend.Source;
import polyglot.frontend.SourceGoal;
import polyglot.frontend.SourceGoal_c;
import polyglot.frontend.TargetFactory;
import polyglot.frontend.VisitorGoal;
import polyglot.main.Options;
import polyglot.main.Report;
import polyglot.types.Flags;
import polyglot.types.MemberClassResolver;
import polyglot.types.QName;
import polyglot.types.SemanticException;
import polyglot.types.TopLevelResolver;
import polyglot.types.TypeSystem;
import polyglot.util.ErrorInfo;
import polyglot.util.ErrorQueue;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.ConformanceChecker;
import polyglot.visit.ConstructorCallChecker;
import polyglot.visit.ContextVisitor;
import polyglot.visit.ExceptionChecker;
import polyglot.visit.ExitChecker;
import polyglot.visit.FwdReferenceChecker;
import polyglot.visit.InitChecker;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PruningVisitor;
import polyglot.visit.ReachChecker;
import polyglot.visit.Translator;
import x10.ast.X10NodeFactory_c;
import x10.compiler.ws.WSCodeGenerator;
import x10.errors.Warnings;
import x10.extension.X10Ext;
//import x10.finish.table.CallTableKey;
//import x10.finish.table.CallTableVal;
import x10.optimizations.Optimizer;
import x10.parser.X10Lexer;
import x10.parser.X10SemanticRules;
import x10.plugin.CompilerPlugin;
import x10.plugin.LoadJobPlugins;
import x10.plugin.LoadPlugins;
import x10.plugin.RegisterPlugins;
import x10.types.X10SourceClassResolver;
import polyglot.types.TypeSystem;
import x10.types.X10TypeSystem_c;
import x10.visit.CheckNativeAnnotationsVisitor;
import x10.visit.Lowerer;
import x10.visit.Desugarer;
import x10.visit.ExpressionFlattener;
import x10.visit.FieldInitializerMover;
//import x10.visit.FinishAsyncVisitor;
import x10.visit.MainMethodFinder;
import x10.visit.NativeClassVisitor;
import x10.visit.RewriteAtomicMethodVisitor;
import x10.visit.StaticNestedClassRemover;
import x10.visit.X10Caster;
import x10.visit.X10ImplicitDeclarationExpander;
import x10.visit.X10InnerClassRemover;
import x10.visit.X10MLVerifier;
import x10.visit.X10Translator;
import x10.visit.X10TypeBuilder;
import x10.visit.X10TypeChecker;
import x10.visit.PositionInvariantChecker;
import x10.visit.InstanceInvariantChecker;
import x10.visit.CheckEscapingThis;

/**
 * Extension information for x10 extension.
 */
public class ExtensionInfo extends polyglot.frontend.ParserlessJLExtensionInfo {
    static final boolean DEBUG_ = false;
    static {
        // force Topics to load
        Topics t = new Topics();
    }
    
    protected Map<QName,CompilerPlugin> plugins;

	public static final String XML_FILE_EXTENSION = "x10ml";
	public static final String XML_FILE_DOT_EXTENSION = "." + XML_FILE_EXTENSION;

//	private static HashMap<CallTableKey, LinkedList<CallTableVal>> calltable = new HashMap<CallTableKey, LinkedList<CallTableVal>>();
    public static String clock = "clock";

    static {
        Report.topics.add(clock);
    }

    public polyglot.main.Version version() {
    	return new Version();
    }
    public String[] fileExtensions() {
    	return new String[] { "x10", XML_FILE_EXTENSION };
    }
//    public String defaultFileExtension() {
//        return "x10";
//    }

    public String compilerName() {
        return "x10c";
    }

    //
    // Replace the Flex/Cup parser with a JikesPG parser
    //
    //    public Parser parser(Reader reader, FileSource source, ErrorQueue eq) { 
    //        Lexer lexer = new Lexer_c(reader, source.name(), eq);
    //        Grm grm = new Grm(lexer, ts, nf, eq);
    //        return new CupParser(grm, source, eq);
    //    }
    //
    
    public Parser parser(Reader reader, FileSource source, ErrorQueue eq) {
    // ###
//        if (source.path().endsWith(XML_FILE_DOT_EXTENSION)) {
//        	return new DomParser(reader, (X10TypeSystem) ts, (X10NodeFactory) nf, source, eq);
//        }

    	try {
            //
            // X10Lexer may be invoked using one of two constructors.
            // One constructor takes as argument a string representing
            // a (fully-qualified) filename; the other constructor takes
            // as arguments a (file) Reader and a string representing the
            // name of the file in question. Invoking the first
            // constructor is more efficient because a buffered File is created
            // from that string and read with one (read) operation. However,
            // we depend on Polyglot to provide us with a fully qualified
            // name for the file. In Version 1.3.0, source.name() yielded a
            // fully-qualied name. In 1.3.2, source.path() yields a fully-
            // qualified name. If this assumption still holds then the 
            // first constructor will work.
            // The advantage of using the Reader constructor is that it
            // will always work, though not as efficiently.
            //
            // X10Lexer x10_lexer = new X10Lexer(reader, source.name());
            //
            // FIXME: HACK! Unwrap the escaping unicode reader, because LPG will do its own decoding
//          if (reader instanceof polyglot.lex.EscapedUnicodeReader)
//              reader = ((polyglot.lex.EscapedUnicodeReader)reader).getSource();
            X10Lexer x10_lexer =
                // Optimization: it's faster to read from a file
                source.resource().getClass() == FileResource.class ?
                                new X10Lexer(source.path()) :
                                new X10Lexer(reader, source.toString());
            X10SemanticRules x10_parser = new X10SemanticRules(x10_lexer.getILexStream(), ts, nf, source, eq); // Create the parser
            x10_lexer.lexer(x10_parser.getIPrsStream());
            return x10_parser; // Parse the token stream to produce an AST
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Could not parse " + source.path());
    }

    protected HashSet<String> manifest = new HashSet<String>();
    public boolean manifestContains(String path) {
        path = path.replace(File.separatorChar, '/');
        // FIXME: HACK! Try all prefixes
        int s = 0;
        while ((s = path.indexOf('/')) != -1) {
            if (manifest.contains(path))
                return true;
            path = path.substring(s+1);
        }
        if (manifest.contains(path))
            return true;
        return false;
    }

    static class WarningComparator implements Comparator<ErrorInfo> {
        public int compare(ErrorInfo a, ErrorInfo b) {
            Position pa = a.getPosition();
            Position pb = b.getPosition();
            if (pa == null && pb == null) {
                if (a.getMessage() == null && b.getMessage() == null)
                    return 0;
                else if (a.getMessage() != null && a.getMessage().equals(b.getMessage()))
                    return 0;
            }
            if (pa == null)
                return -1;
            if (pb == null)
                return 1;
            if (pa.line() < pb.line())
                return -1;
            if (pb.line() < pa.line())
                return 1;
            if (pa.column() < pb.column())
                return -1;
            if (pb.column() < pa.column())
                return 1;
            return 0;
        }
    }
    Set<ErrorInfo> warnings = new TreeSet<ErrorInfo>(new WarningComparator());

    public Set<ErrorInfo> warningSet() {
        return warnings;
    }

    static class ExceptionComparator implements Comparator<SemanticException> {
        public int compare(SemanticException a, SemanticException b) {
            int r = (a.getClass().toString().compareToIgnoreCase(b.getClass().toString()));
            if (r != 0)
                return r;
            Position pa = a.position();
            Position pb = b.position();
            if (pa == null && pb == null) {
                if (a.getMessage() == null && b.getMessage() == null)
                    return 0;
                else if (a.getMessage() != null && a.getMessage().equals(b.getMessage()))
                    return 0;
            }
            if (pa == null)
                return -1;
            if (pb == null)
                return 1;
            if (pa.line() < pb.line())
                return -1;
            if (pb.line() < pa.line())
                return 1;
            // If they have the same message and are on the same line, they're equal
            if (a.getMessage() == null && b.getMessage() == null)
                return 0;
            if (a.getMessage() == null)
                return -1;
            if (b.getMessage() == null)
                return 1;
            if (!a.getMessage().equals(b.getMessage()))
                return a.getMessage().compareToIgnoreCase(b.getMessage());
            if (a.getMessage().equals(b.getMessage()))
                return 0;
            if (pa.column() < pb.column())
                return -1;
            if (pb.column() < pa.column())
                return 1;
            return 0;
        }
    }
    Set<SemanticException> errors = new TreeSet<SemanticException>(new ExceptionComparator());

    public Set<SemanticException> errorSet() {
        return errors;
    }

    private int weakCallsCount = 0;
    public void incrWeakCallsCount() { 
        weakCallsCount++;
    }
    public int weakCallsCount() {
        return weakCallsCount;
    }

    protected void initTypeSystem() {
        try {
            TopLevelResolver r = new X10SourceClassResolver(compiler, this, getOptions().constructFullClasspath(),
                                                            getOptions().compile_command_line_only,
                                                            getOptions().ignore_mod_times);
            // FIXME: [IP] HACK
            if (Configuration.MANIFEST != null) {
                try {
                    FileReader fr = new FileReader(Configuration.MANIFEST);
                    BufferedReader br = new BufferedReader(fr);
                    String file = "";
                    while ((file = br.readLine()) != null)
                        if (file.endsWith(".x10") || file.endsWith(".jar")) // FIXME: hard-codes the source extension.
                            manifest.add(file);
                } catch (IOException e) { }
            } else {
                for (File f : getOptions().source_path) {
                    if (f.getName().endsWith("x10.jar")) {
                        try {
                            JarFile jf = new JarFile(f);
                            manifest.add("x10.jar");
                            Enumeration<JarEntry> entries = jf.entries();
                            while (entries.hasMoreElements()) {
                                JarEntry je = entries.nextElement();
                                if (je.getName().endsWith(".x10")) { // FIXME: hard-codes the source extension.
                                    manifest.add(je.getName());
                                }
                            }
                        } catch (IOException e) { }
                    }
                }
            }
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

    protected NodeFactory createNodeFactory() {
        return new X10NodeFactory_c(this);
    }

    protected TypeSystem createTypeSystem() {
        return new X10TypeSystem_c();
    }

    public void initCompiler(Compiler compiler) {
	super.initCompiler(compiler);
	//QueryEngine.init(this);
    }

    // =================================
    // X10-specific goals and scheduling
    // =================================
    protected Scheduler createScheduler() {
        return new X10Scheduler(this);
    }
    
    public static class X10Scheduler extends JLScheduler {
       public X10Scheduler(ExtensionInfo extInfo) {
		   super(extInfo);
	   }

       protected List<Goal> validateOnlyGoals(Job job) {
           List<Goal> goals = new ArrayList<Goal>();
           addValidateOnlyGoals(job, goals);
           return goals;
       }
       private void addValidateOnlyGoals(Job job, List<Goal> goals) {
           goals.add(Parsed(job));
           goals.add(ImportTableInitialized(job));
           goals.add(TypesInitialized(job));

           if (job.source() != null && job.source().path().endsWith(XML_FILE_DOT_EXTENSION)) {
               goals.add(X10MLTypeChecked(job));
           }
           
           // Do not include LoadPlugins in list.  It would cause prereqs to be added 
//           goals.add(LoadPlugins());
           goals.add(PropagateAnnotations(job));
           goals.add(LoadJobPlugins(job));
           goals.add(RegisterPlugins(job));
           
           goals.add(PreTypeCheck(job));
           goals.add(TypesInitializedForCommandLineBarrier());

           goals.add(TypeChecked(job));
           goals.add(ReassembleAST(job));
           
           goals.add(ConformanceChecked(job));

           // Data-flow analyses
           goals.add(ReachabilityChecked(job)); // This must be the first dataflow analysis (see DataFlow.reportCFG_Errors)
       //    goals.add(ExceptionsChecked(job));
           goals.add(ExitPathsChecked(job));
           goals.add(InitializationsChecked(job));
           goals.add(ConstructorCallsChecked(job));
           goals.add(ForwardReferencesChecked(job));
//           goals.add(CheckNativeAnnotations(job));
           goals.add(CheckEscapingThis(job));
           goals.add(CheckASTForErrors(job));
//           goals.add(TypeCheckBarrier());

           goals.add(End(job));
       }
       public List<Goal> goals(Job job) {
           List<Goal> goals = new ArrayList<Goal>();

           addValidateOnlyGoals(job, goals);
           Goal endGoal = goals.remove(goals.size()-1);

           if (!x10.Configuration.ONLY_TYPE_CHECKING) {

       
           if (x10.Configuration.WALA || x10.Configuration.FINISH_ASYNCS) {
               try{
                   ClassLoader cl = Thread.currentThread().getContextClassLoader();
                   Class<?> c = cl.loadClass("com.ibm.wala.cast.x10.translator.X102IRGoal");
                   Constructor<?> con = c.getConstructor(Job.class);
                   Method hasMain = c.getMethod("hasMain", String.class);
                   Goal ir = ((Goal) con.newInstance(job)).intern(this);
                   goals.add(ir);
                   Goal finder = MainMethodFinder(job, hasMain);
                   finder.addPrereq(TypeCheckBarrier());
                   ir.addPrereq(finder);
                   Goal barrier;
                   if(x10.Configuration.FINISH_ASYNCS){
                       Method buildCallTableMethod = c.getMethod("analyze");
                       barrier = IRBarrier(ir, buildCallTableMethod);
                   } else {
                       Method printCallGraph = c.getMethod("printCallGraph");
                       barrier = IRBarrier(ir, printCallGraph);
                   }
                   goals.add(barrier);
                   goals.add(FinishAsyncBarrier(barrier,job,this));
               } catch (Throwable e) {
                   System.err.println("WALA not found.");
                   e.printStackTrace();
               }
           }
           
           goals.add(Desugarer(job));
           
           goals.add(X10Casted(job));
           goals.add(MoveFieldInitializers(job));
           goals.add(X10Expanded(job));
           goals.add(X10RewriteAtomicMethods(job));
           
           
           
           goals.add(NativeClassVisitor(job));

           goals.add(Serialized(job));
           if (x10.Configuration.WORK_STEALING) {
               Goal wsCodeGenGoal = WSCodeGenerator(job);
               goals.add(wsCodeGenGoal);                   
               wsCodeGenGoal.addPrereq(WSCallGraphBarrier());
           }
           
           goals.add(Preoptimization(job));
           goals.addAll(Optimizer.goals(this, job));
           goals.add(Postoptimization(job));
           
           goals.add(Lowerer(job));
           goals.add(InnerClassRemover(job)); // TODO: move earlier
           goals.add(CodeGenerated(job));
           
           InnerClassRemover(job).addPrereq(Serialized(job));
           InnerClassRemover(job).addPrereq(TypeCheckBarrier());

           // the barrier will handle prereqs on its own
           Desugarer(job).addPrereq(TypeCheckBarrier());
           CodeGenerated(job).addPrereq(CodeGenBarrier());
           Lowerer(job).addPrereq(TypeCheckBarrier());
           CodeGenerated(job).addPrereq(Lowerer(job));
           List<Goal> optimizations = Optimizer.goals(this, job);
           for (Goal goal : optimizations) {
               goal.addPrereq(TypeCheckBarrier());
               CodeGenerated(job).addPrereq(goal);
           }

           }

           goals.add(endGoal);

           if (x10.Configuration.CHECK_INVARIANTS) {
               ArrayList<Goal> newGoals = new ArrayList<Goal>(goals.size()*2);
               boolean reachedTypeChecking = false;
               int ctr = 0;
               for (Goal g : goals) {
                   newGoals.add(g);
                   if (!reachedTypeChecking)
                       newGoals.add(new VisitorGoal("PositionInvariantChecker"+(ctr++), job, new PositionInvariantChecker(job, g.name())).intern(this));
                   if (g==TypeChecked(job)) {
                       newGoals.add(new VisitorGoal("InstanceInvariantChecker", job, new InstanceInvariantChecker(job)).intern(this));
                       reachedTypeChecking = true;
                   }
               }
               goals = newGoals;
           }

           return goals;
       }

       public Goal Preoptimization(Job job) { 
           return new SourceGoal_c("Preoptimization", job) {
               private static final long serialVersionUID = 1L;
               public boolean runTask() { return true; }
           }.intern(this);
       }

       public Goal Postoptimization(Job job) { 
           return new SourceGoal_c("Postoptimization", job) {
               private static final long serialVersionUID = 1L;
               public boolean runTask() { return true; }
           }.intern(this);
       }

       Goal PrintWeakCallsCount;
       @Override
       protected Goal EndAll() {
    	   if (PrintWeakCallsCount == null) {
    		   PrintWeakCallsCount = new PrintWeakCallsCount((ExtensionInfo) extInfo).intern(this);
    		   Goal postcompiled = PostCompiled();
    		   PrintWeakCallsCount.addPrereq(postcompiled);
    	   }
    	   return PrintWeakCallsCount;
       }

       static class PrintWeakCallsCount extends AllBarrierGoal {
           private static final long serialVersionUID = 6978580691962888126L;

           ExtensionInfo ext;
           public PrintWeakCallsCount(ExtensionInfo extInfo) {
               super("PrintWeakCallsCount",extInfo.scheduler());
               this.ext = extInfo;
           }
           public Goal prereqForJob(Job job) {
               if (scheduler.shouldCompile(job)) {
                   return scheduler.End(job);
               }
               else {
                   return new SourceGoal_c("WCCDummyEnd", job) {
                       private static final long serialVersionUID = 1L;
                       public boolean runTask() { return true; }
                   }.intern(scheduler);
               }
           }
           public boolean runTask() {
               Compiler compiler = ext.compiler();

               if ((! Configuration.VERBOSE_CALLS) && (! Configuration.STATIC_CALLS)) {
                   int count = ext.weakCallsCount();
                   if (count > 0) {
                       compiler.errorQueue().enqueue(ErrorInfo.WARNING, count + " dynamically checked calls or field accesses, run with -VERBOSE_CALLS for more details.");
                   }
               }
               return true;
           }
       }

       @SuppressWarnings("unchecked")
       public static <T> T invokeGeneric(Method method) {
           try {
               return (T) method.invoke(null);
           } catch (IllegalArgumentException e) {
           } catch (IllegalAccessException e) {
           } catch (InvocationTargetException e) {
           }
           return null;
       }
       public Goal IRBarrier(final Goal goal, final Method method) {
           return new AllBarrierGoal("IRBarrier", this) {
               private static final long serialVersionUID = -3692329571101709400L;
               @Override
               public Goal prereqForJob(Job job) {
                   if (!super.scheduler.commandLineJobs().contains(job) &&
                           ((ExtensionInfo) extInfo).manifestContains(job.source().path())) {
                       return null;
                   }
                   return goal;
               }
               public boolean runTask() {
                   if (Configuration.FINISH_ASYNCS) {
//                   calltable = X10Scheduler.<HashMap<CallTableKey, LinkedList<CallTableVal>>>invokeGeneric(method);
                   } else {
                       try {
                           method.invoke(null);
                       } catch (IllegalArgumentException e) {
                       } catch (IllegalAccessException e) {
                       } catch (InvocationTargetException e) {
                       }
                   }
                   return true;
               }
           }.intern(this);
       }

       public Goal TypeCheckBarrier() {
           String name = "TypeCheckBarrier";
    	   if (extInfo.getOptions().compile_command_line_only) {
               return new BarrierGoal(name, commandLineJobs()) {
                   private static final long serialVersionUID = -1495893515710977644L;
                   @Override
                   public Goal prereqForJob(Job job) {
                       return CheckASTForErrors(job);
                   }
               }.intern(this);
    	   }
    	   else {
               return new AllBarrierGoal(name, this) {
                   private static final long serialVersionUID = 6757229496093951388L;
                   @Override
                   public Goal prereqForJob(Job job) {
                       if (!super.scheduler.commandLineJobs().contains(job) &&
                               ((ExtensionInfo) extInfo).manifestContains(job.source().path()))
                       {
                           return null;
                       }
                       return CheckASTForErrors(job);
                   }
               }.intern(this);
    	    }
       }
     
       protected Goal codegenPrereq(Job job) {
           return InnerClassRemover(job);
       }
       
       public Goal CodeGenBarrier() {
           String name = "CodeGenBarrier";
           if (extInfo.getOptions().compile_command_line_only) {
               return new BarrierGoal(name, commandLineJobs()) {
                   private static final long serialVersionUID = 2258041064037983928L;
                   @Override
                   public Goal prereqForJob(Job job) {
                       return codegenPrereq(job);
                   }
               }.intern(this);
           }
           else {
               return new AllBarrierGoal(name, this) {
                   private static final long serialVersionUID = 4089824072381830523L;
                   @Override
                   public Goal prereqForJob(Job job) {
                       if (!super.scheduler.commandLineJobs().contains(job) &&
                               ((ExtensionInfo) extInfo).manifestContains(job.source().path()))
                       {
                           return null;
                       }
                       return codegenPrereq(job);
                   }
               }.intern(this);
           }
       }

       public Goal CheckASTForErrors(Job job) {
           return new SourceGoal_c("CheckASTForErrors", job) {
               private static final long serialVersionUID = 565345690079406384L;
               public boolean runTask() {
                   if (job.reportedErrors()) {
                       Node ast = job.ast();
                       job.ast(((X10Ext)ast.ext()).setSubtreeValid(false));
                   }
                   return true;
               }
           }.intern(this);
       }

       public Goal Serialized(Job job) {
           Compiler compiler = job.extensionInfo().compiler();
           TypeSystem ts = job.extensionInfo().typeSystem();
           NodeFactory nf = job.extensionInfo().nodeFactory();
           TargetFactory tf = job.extensionInfo().targetFactory();
           return new SourceGoal_c("Serialized", job) {
               private static final long serialVersionUID = 4813624372034550114L;
               public boolean runTask() {
                   return true;
               }
           }.intern(this);
       }


//       @Override
//       public Goal ImportTableInitialized(Job job) {
//	   TypeSystem ts = job.extensionInfo().typeSystem();
//	   NodeFactory nf = job.extensionInfo().nodeFactory();
//	   Goal g = new VisitorGoal("ImportTableInitialized", job, new X10InitImportsVisitor(job, ts, nf));
//	   Goal g2 = g.intern(this);
//	   if (g == g2) {
//	       g.addPrereq(TypesInitializedForCommandLine());
//	   }
//	   return g2;
//       }

       public Goal LoadPlugins() {
           return new LoadPlugins((ExtensionInfo) extInfo).intern(this);
       }

       public Goal LoadJobPlugins(Job job) {
           return new LoadJobPlugins(job).intern(this);
       }

       public Goal RegisterPlugins(Job job) {
           Goal g = new RegisterPlugins(job);
           Goal g2 = g.intern(this);
           if (g == g2) {
               g.addPrereq(LoadPlugins());
           }
           return g2;
       }
       
       public Goal PropagateAnnotations(Job job) {
           // ###
           return new VisitorGoal("PropagateAnnotations", job, new PruningVisitor()).intern(this);
       }

       @Override
       public Goal InitializationsChecked(Job job) {
	   TypeSystem ts = job.extensionInfo().typeSystem();
	   NodeFactory nf = job.extensionInfo().nodeFactory();
           return new ForgivingVisitorGoal("InitializationsChecked", job, new InitChecker(job, ts, nf)).intern(this);
       }

       public static class ValidatingOutputGoal extends OutputGoal {
           private static final long serialVersionUID = -7021152686342225925L;
           public ValidatingOutputGoal(Job job, Translator translator) {
               super(job, translator);
           }
           public boolean runTask() {
               Node ast = job().ast();
               if (ast != null && !((X10Ext)ast.ext()).subtreeValid()) {
                   return false;
               }
               return super.runTask();
           }
       }

       @Override
       public Goal CodeGenerated(Job job) {
    	   TypeSystem ts = extInfo.typeSystem();
    	   NodeFactory nf = extInfo.nodeFactory();
    	   Goal cg = new ValidatingOutputGoal(job, new X10Translator(job, ts, nf, extInfo.targetFactory()));
           Goal cg2 = cg.intern(this);
           return cg2;
       }

       static class X10ParserGoal extends ParserGoal {
           private static final long serialVersionUID = 6811976416160592748L;
           public X10ParserGoal(Compiler compiler, Job job) {
               super(compiler, job);
           }

           @Override
           protected SourceFile createDummyAST() {
               NodeFactory nf = job().extensionInfo().nodeFactory();
               String fName = job.source().name();
               Position pos = new Position("", job.source().path(), 1, 1);
               String name = fName.substring(fName.lastIndexOf(File.separatorChar)+1, fName.lastIndexOf('.'));
               TopLevelDecl decl = nf.ClassDecl(pos, nf.FlagsNode(pos, Flags.PUBLIC),
                       nf.Id(pos, name), null, Collections.<TypeNode>emptyList(),
                       nf.ClassBody(pos, Collections.<ClassMember>emptyList()));
               SourceFile ast = nf.SourceFile(pos, Collections.singletonList(decl)).source(job.source());
               return ast;
           }
       }

       @Override
       public Goal Parsed(Job job) {
           return new X10ParserGoal(extInfo.compiler(), job).intern(this);
       }

       @Override
       public Goal constructTypesInitialized(Job job) {
           TypeSystem ts = job.extensionInfo().typeSystem();
           NodeFactory nf = job.extensionInfo().nodeFactory();
           return new ForgivingVisitorGoal("TypesInitialized", job, new X10TypeBuilder(job, ts, nf)).intern(this);
       }

       public Goal X10MLTypeChecked(Job job) {
           TypeSystem ts = extInfo.typeSystem();
           NodeFactory nf = extInfo.nodeFactory();
           return new VisitorGoal("X10MLTypeChecked", job, new X10MLVerifier(job, ts, nf)).intern(this);
       }

       @Override
       public Goal TypeChecked(Job job) {
           TypeSystem ts = job.extensionInfo().typeSystem();
           NodeFactory nf = job.extensionInfo().nodeFactory();
           return new ForgivingVisitorGoal("TypeChecked", job, new X10TypeChecker(job, ts, nf, job.nodeMemo())).intern(this);
       }

       public Goal ConformanceChecked(Job job) {
           TypeSystem ts = job.extensionInfo().typeSystem();
           NodeFactory nf = job.extensionInfo().nodeFactory();
           return new ForgivingVisitorGoal("ConformanceChecked", job, new ConformanceChecker(job, ts, nf)).intern(this);
       }

       public Goal ReachabilityChecked(Job job) {
           TypeSystem ts = job.extensionInfo().typeSystem();
           NodeFactory nf = job.extensionInfo().nodeFactory();
           return new ForgivingVisitorGoal("ReachChecked", job, new ReachChecker(job, ts, nf)).intern(this);
       }

       public Goal ExceptionsChecked(Job job) {
           TypeSystem ts = job.extensionInfo().typeSystem();
           NodeFactory nf = job.extensionInfo().nodeFactory();
           return new ForgivingVisitorGoal("ExceptionsChecked", job, new ExceptionChecker(job, ts, nf)).intern(this);
       }

       public Goal ExitPathsChecked(Job job) {
           TypeSystem ts = job.extensionInfo().typeSystem();
           NodeFactory nf = job.extensionInfo().nodeFactory();
           return new ForgivingVisitorGoal("ExitChecked", job, new ExitChecker(job, ts, nf)).intern(this);
       }

       public Goal ConstructorCallsChecked(Job job) {
           TypeSystem ts = job.extensionInfo().typeSystem();
           NodeFactory nf = job.extensionInfo().nodeFactory();
           return new ForgivingVisitorGoal("ContructorCallsChecked", job, new ConstructorCallChecker(job, ts, nf)).intern(this);
       }

       public Goal ForwardReferencesChecked(Job job) {
           TypeSystem ts = job.extensionInfo().typeSystem();
           NodeFactory nf = job.extensionInfo().nodeFactory();
           return new ForgivingVisitorGoal("ForwardRefsChecked", job, new FwdReferenceChecker(job, ts, nf)).intern(this);
       }

       public Goal CheckEscapingThis(Job job) {
           return new VisitorGoal("CheckEscapingThis", job, new CheckEscapingThis.Main(job)).intern(this);
       }


       public String nativeAnnotationLanguage() { return "java"; }

       public Goal CheckNativeAnnotations(Job job) {
           TypeSystem ts = extInfo.typeSystem();
           NodeFactory nf = extInfo.nodeFactory();
           return new VisitorGoal("CheckNativeAnnotations", job, new CheckNativeAnnotationsVisitor(job, ts, nf, nativeAnnotationLanguage())).intern(this);
       }

       public static class ValidatingVisitorGoal extends VisitorGoal {
           private static final long serialVersionUID = 5119557747721488612L;
           public ValidatingVisitorGoal(String name, Job job, NodeVisitor v) {
               super(name, job, v);
           }
           public boolean runTask() {
               Node ast = job().ast();
               if (ast != null && !((X10Ext)ast.ext()).subtreeValid()) {
                   //Warnings.issue(job(), "Invalid Visitor Goal: " +this.name()+ ", visitor: " +this.visitor(), Position.COMPILER_GENERATED);
                   return true;
               }
               return super.runTask();
           }
       }

       public Goal X10Casted(Job job) {
           TypeSystem ts = extInfo.typeSystem();
           NodeFactory nf = extInfo.nodeFactory();
           return new ValidatingVisitorGoal("X10Casted", job, new X10Caster(job, ts, nf)).intern(this);
       }

       public Goal MoveFieldInitializers(Job job) {
           TypeSystem ts = extInfo.typeSystem();
           NodeFactory nf = extInfo.nodeFactory();
           return new ValidatingVisitorGoal("MoveFieldInitializers", job, new FieldInitializerMover(job, ts, nf)).intern(this);
       }
       
       public Goal X10RewriteAtomicMethods(Job job) {
           TypeSystem ts = extInfo.typeSystem();
           NodeFactory nf = extInfo.nodeFactory();
           return new ValidatingVisitorGoal("X10RewriteAtomicMethods", job, new RewriteAtomicMethodVisitor(job, ts, nf)).intern(this);
       }
       
       public Goal X10Expanded(Job job) {
           TypeSystem ts = extInfo.typeSystem();
           NodeFactory nf = extInfo.nodeFactory();
           return new ValidatingVisitorGoal("X10Expanded", job, new X10ImplicitDeclarationExpander(job, ts, nf)).intern(this);
       }
       
       public Goal NativeClassVisitor(Job job) {
           TypeSystem ts = extInfo.typeSystem();
           NodeFactory nf = extInfo.nodeFactory();
           return new ValidatingVisitorGoal("NativeClassVisitor", job, new NativeClassVisitor(job, ts, nf, nativeAnnotationLanguage())).intern(this);
       }
       
       public Goal MainMethodFinder(Job job, Method hasMain) {
           TypeSystem ts = extInfo.typeSystem();
           NodeFactory nf = extInfo.nodeFactory();
           return new ValidatingVisitorGoal("MainMethodFinder", job, new MainMethodFinder(job, ts, nf, hasMain)).intern(this);
       }
       
       public Goal WSCallGraphBarrier() {
           final TypeSystem ts  = extInfo.typeSystem();
           final NodeFactory nf = extInfo.nodeFactory();
           return new AllBarrierGoal("WSCallGraphBarrier", this) {
               @Override
               public Goal prereqForJob(Job job) {
                   return TypeChecked(job);
               }
               @Override
               public boolean runTask() {
                   WSCodeGenerator.buildCallGraph(ts, nf, nativeAnnotationLanguage());
                   return true;
               }
           }.intern(this);
       }

       public Goal WSCodeGenerator(Job job) {
           TypeSystem ts = extInfo.typeSystem();
           NodeFactory nf = extInfo.nodeFactory();
           return new ValidatingVisitorGoal("WSCodeGenerator", job, new WSCodeGenerator(job, ts, nf)).intern(this);
       }
       
       public Goal Desugarer(Job job) {
           TypeSystem ts = extInfo.typeSystem();
           NodeFactory nf = extInfo.nodeFactory();
           return new ValidatingVisitorGoal("Desugarer", job, new Desugarer(job, ts, nf)).intern(this);
       }
       
       public Goal Lowerer(Job job) {
           TypeSystem ts = extInfo.typeSystem();
           NodeFactory nf = extInfo.nodeFactory();
           return new ValidatingVisitorGoal("Lowerer", job, new Lowerer(job, ts, nf)).intern(this);
       }

       public Goal WSExpressionFlattener(Job job) {
           TypeSystem ts = extInfo.typeSystem();
           NodeFactory nf = extInfo.nodeFactory();
           VisitorGoal ef = new ValidatingVisitorGoal("WorkStealing ExpressionFlattener", job, new ExpressionFlattener(job, ts, nf));
           Goal ef2 = ef.intern(this);
           if (ef == ef2) {
               ef.addPrereq(Serialized(job));
           }
           return ef2;
       }
       
       
       
       public Goal InnerClassRemover(Job job) {
           TypeSystem ts = extInfo.typeSystem();
           NodeFactory nf = extInfo.nodeFactory();
           return new ValidatingVisitorGoal("InnerClassRemover", job, new X10InnerClassRemover(job, ts, nf)).intern(this);
       }
       public Goal StaticNestedClassRemover(Job job) {
           TypeSystem ts = extInfo.typeSystem();
           NodeFactory nf = extInfo.nodeFactory();
           return new ValidatingVisitorGoal("StaticNestedClassRemover", job, new StaticNestedClassRemover(job, ts, nf)).intern(this);
       }
       
       public Goal FinishAsyncBarrier(final Goal goal, final Job job, final Scheduler s) {
           return new AllBarrierGoal("FinishAsyncBarrier", this) {
               private static final long serialVersionUID = -4172220184246138069L;
               @Override
               public Goal prereqForJob(Job job) {
                   //TODO: probably need to also annotation code in "runtime"
                   if (!super.scheduler.commandLineJobs().contains(job) &&
                           ((ExtensionInfo) extInfo).manifestContains(job.source().path())) {
                       return null;
                   }
                   return goal;
               }
               public boolean runTask() {
                   try {
                       TypeSystem ts = extInfo.typeSystem();
                       NodeFactory nf = extInfo.nodeFactory();
//                       FinishAsyncVisitor favisitor = new FinishAsyncVisitor(job, ts, nf, nativeAnnotationLanguage(), calltable);
//                       Goal finish = new ValidatingVisitorGoal("FinishAsyncs", job, favisitor).intern(s);
//                       finish.runTask();
                   } catch (Throwable t) {}
                   return true;
               }
           }.intern(this);
       }
       
       public static class X10Job extends Job {
           public X10Job(polyglot.frontend.ExtensionInfo lang, JobExt ext, Source source, Node ast) {
               super(lang, ext, source, ast);
           }
           public int initialErrorCount() { return initialErrorCount; }
           public void initialErrorCount(int count) { initialErrorCount = count; }
       }
       
       @Override
       protected Job createSourceJob(Source source, Node ast) {
           return new X10Job(extInfo, extInfo.jobExt(), source, ast);
       }
       
       @Override
       protected boolean runPass(Goal goal) throws CyclicDependencyException {
           Job job = goal instanceof SourceGoal ? ((SourceGoal) goal).job() : null;
           int savedInitialErrorCount = -1;
           if (job != null)
               savedInitialErrorCount = ((X10Job) job).initialErrorCount();
           boolean result = super.runPass(goal);
           if (job != null)
               ((X10Job) job).initialErrorCount(savedInitialErrorCount);
           return result;
       }
    }
    
    protected Options createOptions() {
    	return new X10CompilerOptions(this);
    }
    
    public Map<QName,CompilerPlugin> plugins() {
    	if (plugins == null) {
    		return Collections.emptyMap();
    	}
    	return plugins;
    }
    
	public void addPlugin(QName pluginName, CompilerPlugin plugin) {
		if (plugins == null) {
			plugins = new HashMap<QName,CompilerPlugin>();
		}
		plugins.put(pluginName, plugin);
	}
	
	public CompilerPlugin getPlugin(QName pluginName) {
		if (plugins == null) {
			return null;
		}
		
		return plugins.get(pluginName);
	}
}
