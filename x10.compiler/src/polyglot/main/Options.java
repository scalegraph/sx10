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

package polyglot.main;
import java.io.File;
import java.io.PrintStream;
import java.util.*;

import polyglot.frontend.ExtensionInfo;
import polyglot.util.CollectionUtil;
import x10.util.CollectionFactory;

/** 
 * This object encapsulates various polyglot options. 
 */
public class Options {
    /**
     * Back pointer to the extension that owns this options
     */
    protected ExtensionInfo extension = null;
    
    /*
     * Fields for storing values for options.
     */
    public int error_count = 100;
    public List<File> source_path; // List<File>
    public File output_directory;
    public String default_classpath;
    public String default_output_classpath;
    public String classpath;
    public String output_classpath;
    public String bootclasspath = null;
    public boolean assertions = false;
    
    public boolean compile_command_line_only = false;

    public String[] source_ext = null; // e.g., java, jl, pj
    public String output_ext = "java"; // java, by default
    public boolean output_stdout = false; // whether to output to stdout
    public String post_compiler;
      // compiler to run on java output file
  
    public int output_width = 80;
    public boolean fully_qualified_names = false;
  
    /** Inject type information in serialized form into output file? */
    public boolean serialize_type_info = true;
  
    /** Dump the AST after the following passes? */
    public Set<String> dump_ast = CollectionFactory.newHashSet();
  
    /** -define */
    public Set<String> macros = CollectionFactory.newHashSet();

    /** Pretty-print the AST after the following passes? */
    public Set<String> print_ast = CollectionFactory.newHashSet();
 
    /** Disable the following passes? */
    public Set<String> disable_passes = CollectionFactory.newHashSet();
  
    /** keep output files */
    public boolean keep_output_files = true;
    
    /** Generate position information for compiler-generated code. */
    public boolean precise_compiler_generated_positions = false;
    
    /** Use SimpleCodeWriter instead of OptimalCodeWriter */
    public boolean use_simple_code_writer = false;
    
    public Reporter reporter = new Reporter();
    
    /**
     * Constructor
     */
    public Options(ExtensionInfo extension) {
        this.extension = extension;
        setDefaultValues();
    }
    
    /**
     * Set default values for options
     */
    public void setDefaultValues() {
        String default_bootpath = System.getProperty("sun.boot.class.path");
        if (default_bootpath == null) {
          default_bootpath = System.getProperty("java.home") +
                     File.separator + "jre" +
                     File.separator + "lib" +
                     File.separator + "rt.jar";
        }
    
        default_classpath = System.getProperty("java.class.path") +
                            File.pathSeparator + default_bootpath;
        classpath = default_classpath;        

        default_output_classpath = System.getProperty("java.class.path");
        output_classpath = default_output_classpath;

        String java_home = System.getProperty("java.home");
        String current_dir = System.getProperty("user.dir");
    
        source_path = new LinkedList<File>();
        source_path.add(new File(current_dir));
    
        output_directory = new File(current_dir);
    
        // First try: $JAVA_HOME/../bin/javac
        // This should work with JDK 1.2 and 1.3
        //
        // If not found, try: $JAVA_HOME/bin/javac
        // This should work for JDK 1.1.
        //
        // If neither found, assume "javac" is in the path.
        //
        post_compiler = java_home + File.separator + ".." + File.separator +
                            "bin" + File.separator + "javac";
    
        if (! new File(post_compiler).exists()) {
          post_compiler = java_home + File.separator +
                              "bin" + File.separator + "javac";
    
          if (! new File(post_compiler).exists()) {
            post_compiler = "javac";
          }
        }
    }
    
    /**
     * Parse the command line
     * 
     * @throws UsageError if the usage is incorrect.
     */
    public void parseCommandLine(String args[], Set<String> source) throws UsageError {
        if(args.length < 1) {
            throw new UsageError("No command line arguments given");
        }
    
        for(int i = 0; i < args.length; ) {
            try {
                int ni = parseCommand(args, i, source);                
                if (ni == i) {
                    throw new UsageError("illegal option -- " + args[i]);
                }
                
                i = ni;

            }
            catch (ArrayIndexOutOfBoundsException e) {
                throw new UsageError("missing argument");
            }
        }
                    
        if (source.size() < 1) {
          throw new UsageError("must specify at least one source file");
        }
    }
    
    /**
     * Parse a command
     * @return the next index to process. i.e., if calling this method
     *         processes two commands, then the return value should be index+2
     */
    protected int parseCommand(String args[], int index, Set<String> source) 
            throws UsageError, Main.TerminationException {
        int i = index;
        if (args[i].equals("-h") || 
            args[i].equals("-help") || 
            args[i].equals("--help")) {
            throw new UsageError("", 0);
        }
        else if (args[i].equals("-version")) {
            StringBuffer sb = new StringBuffer();
            if (extension != null) {
                sb.append(extension.compilerName() + 
                          " version " + extension.version());
            }
            throw new Main.TerminationException(sb.toString(), 0);
        }
        else if (args[i].equals("-d"))
        {
            i++;
            output_directory = new File(args[i]);
            i++;
        }
        else if (args[i].equals("-classpath") ||
                 args[i].equals("-cp")) {
            i++;
            classpath = args[i] + File.pathSeparator + default_classpath;
            output_classpath = args[i] + File.pathSeparator + default_output_classpath;
            i++;
        }
        else if (args[i].equals("-bootclasspath")) {
            i++;
            bootclasspath = args[i];
            i++;
        }
        else if (args[i].equals("-sourcepath"))
        {
            i++;
            StringTokenizer st = new StringTokenizer(args[i], File.pathSeparator);
            while(st.hasMoreTokens())
            {
                File f = new File(st.nextToken());
                if (!source_path.contains(f))
                    source_path.add(f);
            }
            i++;
        }
        else if (args[i].equals("-commandlineonly")) 
        {
            i++;
            compile_command_line_only = true;
        }
        else if (args[i].equals("-preferclassfiles")) 
        {
            i++;
            ignore_mod_times = true;
        }
        else if (args[i].equals("-assert")) 
        {
            i++;
            assertions = true;
        }
        else if (args[i].equals("-fqcn")) 
        {
            i++;
            fully_qualified_names = true;
        }
        else if (args[i].equals("-c"))
        {
            post_compiler = null;
            i++;
        }
        else if (args[i].equals("-errors"))
        {
            i++;
            try {
                error_count = Integer.parseInt(args[i]);
                } catch (NumberFormatException e) {}
                i++;
        }
        else if (args[i].equals("-w"))
        {
            i++;
            try {
                output_width = Integer.parseInt(args[i]);
                } catch (NumberFormatException e) {}
                i++;
        }
        else if (args[i].equals("-post"))
        {
            i++;
            post_compiler = args[i];
            i++;
        }
        else if (args[i].equals("-stdout")) 
        {
            i++;
            output_stdout = true;
        }
        else if (args[i].equals("-sx")) 
        {
            i++;
            if (source_ext == null) {
                source_ext = new String[] { args[i] };
            }
            else {
                String[] s = new String[source_ext.length+1];
                System.arraycopy(source_ext, 0, s, 0, source_ext.length);
                s[s.length-1] = args[i];
                source_ext = s;
            }
            i++;
        }
        else if (args[i].equals("-ox"))
        {
            i++;
            output_ext = args[i];
            i++;
        }
        else if (args[i].equals("-noserial"))
        {
            i++;
            serialize_type_info = false;
        }
        else if (args[i].equals("-dump"))
        {
            i++;
            String pass_name = args[i];
            dump_ast.add(pass_name);
            i++;
        }
        else if (args[i].equals("-print"))
        {
            i++;
            String pass_name = args[i];
            print_ast.add(pass_name);
            i++;
        }
        else if (args[i].equals("-disable"))
        {
            i++;
            String pass_name = args[i];
            disable_passes.add(pass_name);
            i++;
        }
        else if (args[i].equals("-nooutput"))
        {
            i++;
            keep_output_files = false;
            output_width = 1000; // we do not keep the output files, so
                                 // set the output_width to a large number
                                 // to reduce the time spent pretty-printing 
        }
        else if (args[i].equals("-v") || args[i].equals("-verbose"))
        {
            i++;
            reporter.addTopic(Reporter.verbose, 1);
        }
        else if (args[i].equals("-report")) {
            i++;
            String report_option = args[i];
            StringTokenizer st = new StringTokenizer(args[i], "=");
            String topic = ""; int level = 0;
            if (st.hasMoreTokens()) topic = st.nextToken();
            if (st.hasMoreTokens()) {
                try {
                    level = Integer.parseInt(st.nextToken());
                } 
                catch (NumberFormatException e) {}
            }
            reporter.addTopic(topic, level);
            i++;
        }        
        else if (args[i].equals("-debugpositions")) {
            precise_compiler_generated_positions = true;
            i++;
        }
        else if (args[i].equals("-simpleoutput")) {
        	use_simple_code_writer = true;
        	i++;
        }
        else if (args[i].equals("-define"))
        {
            i++;
            String macro = args[i];
            macros.add(macro);
            i++;
        }
        else if (!args[i].startsWith("-")) {
            source.add(args[i]);
            File f = new File(args[i]).getParentFile();
            if (f != null && !source_path.contains(f))
                source_path.add(f);
            i++;
        }
        
        return i;
    }
    
    /**
     * Print usage information
     */
    public void usage(PrintStream out) {
        out.println("usage: " + extension.compilerName() + " [options] " +
                           "<source-file>." + extension.fileExtensions()[0] + " ...");
        out.println("where [options] includes:");
        usageForFlag(out, "@<file>", "read options from <file>");
        usageForFlag(out, "-d <directory>", "output directory");
        usageForFlag(out, "-assert", "recognize the assert keyword");
        usageForFlag(out, "-sourcepath <path>", "source path");
        usageForFlag(out, "-bootclasspath <path>", 
                          "path for bootstrap class files");
        usageForFlag(out, "-ext <extension>", "use language extension");
        usageForFlag(out, "-extclass <ext-class>", "use language extension");
        usageForFlag(out, "-commandlineonly", "only compile files named on the command-line (may also require -c)");
        usageForFlag(out, "-preferclassfiles", "prefer class files to source files even if the source is newer");
        usageForFlag(out, "-fqcn", "use fully-qualified class names");
        usageForFlag(out, "-sx <ext>", "set source extension");
        usageForFlag(out, "-ox <ext>", "set output extension");
        usageForFlag(out, "-errors <num>", "set the maximum number of errors");
        usageForFlag(out, "-w <num>", 
                          "set the maximum column width of the output files");
        usageForFlag(out, "-dump <pass>|dumpall", "dump the ast after pass <pass>");
        usageForFlag(out, "-print <pass>|printall",
	                      "pretty-print the ast after pass <pass>");
        usageForFlag(out, "-disable <pass>", "disable pass <pass>");
//        usageForFlag(out, "-scramble [seed]", "scramble the ast (for testing)");
        usageForFlag(out, "-noserial", "disable class serialization");
        usageForFlag(out, "-nooutput", "delete output files after compilation");
        usageForFlag(out, "-debugpositions", "generate position information for compiler-generated code");
        usageForFlag(out, "-simpleoutput", "use SimpleCodeWriter");
        usageForFlag(out, "-define <macro>", "define <macro>");
        usageForFlag(out, "-v -verbose", "print verbose debugging information");
        usageForFlag(out, "-report <topic>=<level>", 
                          "print verbose debugging information about " +
                          "topic at specified verbosity");

        StringBuffer allowedTopics = new StringBuffer("Allowed topics: ");
        for (Iterator<String> iter = reporter.topics.iterator(); iter.hasNext(); ) {
            allowedTopics.append(iter.next());
            if (iter.hasNext()) {
                allowedTopics.append(", ");
            }
        }
        usageSubsection(out, allowedTopics.toString());
        
        usageForFlag(out, "-version", "print version info");
        usageForFlag(out, "-h", "print this message");
    }
    
    /**
     * The maximum width of a line when printing usage information. Used
     * by <code>usageForFlag</code> and <code>usageSubsection</code>.
     */
    protected int USAGE_SCREEN_WIDTH = 76;
    /**
     * The number of spaces from the left that the descriptions for flags will
     * be displayed. Used
     * by <code>usageForFlag</code>.
     */
    protected int USAGE_FLAG_WIDTH = 27;
    /**
     * The number of spaces to indent a subsection of usage information.
     * Used by <code>usageSubsection</code>.
     */
    protected int USAGE_SUBSECTION_INDENT = 8;

    /** Ignore source and class file modification times when compiling; always prefer the class file. */
    public boolean ignore_mod_times;

    /**
     * Output a flag and a description of its usage in a nice format. This 
     * makes it easier for extensions to output their usage in a consistent
     * format.
     * 
     * @param out output PrintStream
     * @param flag 
     * @param description description of the flag.
     */
    protected void usageForFlag(PrintStream out, String flag, String description) {        
        out.print("  ");
        out.print(flag);
        // cur is where the cursor is on the screen.
        int cur = flag.length() + 2;
        
        // print space to get up to indentation level
        if (cur < USAGE_FLAG_WIDTH) {
            printSpaces(out, USAGE_FLAG_WIDTH - cur);
        }
        else {
            // the flag is long. Get a new line before printing the
            // description.
            out.println();
            printSpaces(out, USAGE_FLAG_WIDTH);
        }
        cur = USAGE_FLAG_WIDTH;
        
        // break up the description.
        StringTokenizer st = new StringTokenizer(description);
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            if (cur + s.length() > USAGE_SCREEN_WIDTH) {
                out.println();
                printSpaces(out, USAGE_FLAG_WIDTH);
                cur = USAGE_FLAG_WIDTH;
            }
            out.print(s);
            cur += s.length();
            if (st.hasMoreTokens()) {
                if (cur + 1 > USAGE_SCREEN_WIDTH) {
                    out.println();
                    printSpaces(out, USAGE_FLAG_WIDTH);
                    cur = USAGE_FLAG_WIDTH;
                }
                else {
                    out.print(" ");
                    cur++;
                }
            }
        }
        out.println();
    }
    
    /**
     * Output a section of text for usage information. This text will be
     * displayed indented a certain amount from the left, controlled by
     * the field <code>USAGE_SUBSECTION_INDENT</code>
     * 
     * @param out the output PrintStream
     * @param text the text to output.
     */
    protected void usageSubsection(PrintStream out, String text) {        
        // print space to get up to indentation level
        printSpaces(out, USAGE_SUBSECTION_INDENT);

        // cur is where the cursor is on the screen.
        int cur = USAGE_SUBSECTION_INDENT;
        
        // break up the description.
        StringTokenizer st = new StringTokenizer(text);
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            if (cur + s.length() > USAGE_SCREEN_WIDTH) {
                out.println();
                printSpaces(out, USAGE_SUBSECTION_INDENT);
                cur = USAGE_SUBSECTION_INDENT;
            }
            out.print(s);
            cur += s.length();
            if (st.hasMoreTokens()) {
                if (cur + 1 > USAGE_SCREEN_WIDTH) {
                    out.println();
                    printSpaces(out, USAGE_SUBSECTION_INDENT);
                    cur = USAGE_SUBSECTION_INDENT;
                }
                else {
                    out.print(' ');
                    cur++;
                }
            }
        }
        out.println();
    }
    
    /**
     * Utility method to print a number of spaces to a PrintStream.
     * @param out output PrintStream
     * @param n number of spaces to print.
     */
    protected static void printSpaces(PrintStream out, int n) {
        while (n-- > 0) {
            out.print(' ');
        }
    } 

  public String constructFullClasspath() {
      StringBuffer fullcp = new StringBuffer();
      if (bootclasspath != null) {
	  fullcp.append(bootclasspath);
	  fullcp.append(File.pathSeparator);
      }
      fullcp.append(classpath);
      return fullcp.toString();
  }

  public String constructPostCompilerClasspath() {
      return output_directory + File.pathSeparator
              + "." + File.pathSeparator
              + output_classpath;
  }
  
  /**
   * Returns a string representation for the compiler options, usable from the command line.
   */
  public String toString(){
	  String result = "";
	  if (classpath != null && !classpath.equals("")){
		  result += "-classpath " + classpath;
	  }
	  if (output_directory != null){
		  result += " -d " + output_directory.getAbsolutePath();
	  }
	  if (assertions) {
		  result += " -assert";
	  }
	  if (source_path != null && !source_path.isEmpty()){
		  result += " -sourcepath ";
		  for (int i = 0; i < source_path.size(); i++){
			  result += ((i > 0)? ":" : "") + source_path.get(i).getAbsolutePath();
		  }
	  }
	  if (bootclasspath != null && !bootclasspath.equals("")) {
		  result += " -bootclasspath " + bootclasspath;
	  }
	  if (compile_command_line_only){
		  result += " -commandlineonly";
	  }
	  if (fully_qualified_names){
		  result += " -fqcn";
	  }
	  if (source_ext != null){
		  for (int i = 0; i < source_ext.length; i++){
			  result += " -sx "+ source_ext[i];
		  }
	  }
	  if (output_ext != null && !output_ext.equals("")){
		  result += " -ox " + output_ext;
	  }
	  result += " -errors " + error_count;
	  result += " -w " + output_width;
	  for (String s: dump_ast){
		  result += " -dump " + s;
	  }
	  for (String s: print_ast){
		  result += " -print " + s;
	  }
	  for (String s: disable_passes){
		  result += " -disable " + s;
	  }
	  if (!serialize_type_info){
		  result += " -noserial";
	  }
	  if (!keep_output_files){
		  result += " -nooutput";
	  }
	  if (post_compiler != null && !post_compiler.equals("")){
		  result += " -post " + post_compiler;
	  }
	  if (precise_compiler_generated_positions){
		  result += " -debugpositions";
	  }
	  if (use_simple_code_writer){
		  result += " -simpleoutput";
	  }
      for (String macro: macros){
          result += " -define " + macro;
      }
	  return result;
  }
}
