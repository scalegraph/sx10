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

package x10c;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Set;

import polyglot.frontend.ExtensionInfo;
import polyglot.main.UsageError;
import polyglot.main.Main.TerminationException;
import x10cpp.postcompiler.PrecompiledLibrary;

public class X10CCompilerOptions extends x10.X10CompilerOptions {

    public String main_source = null;

    public X10CCompilerOptions(ExtensionInfo extension) {
        super(extension);
    }

    public static String findJavaCommand(String name) {
        // Note: following code does not work on Windows since executable has .exe extension
        
        // First try: $JAVA_HOME/../bin/${name}
        // This should work with JDK 1.2 and 1.3
        //
        // If not found, try: $JAVA_HOME/bin/${name}
        // This should work for JDK 1.1.
        //
        // If neither found, assume ${name} is in the path.
        String java_home = System.getProperty("java.home");
        String command = java_home + File.separator + ".." + File.separator + "bin" + File.separator + name;
        if (! new File(command).exists()) {
            command = java_home + File.separator + "bin" + File.separator + name;
            if (! new File(command).exists()) {
                command = name;
            }
        }
        return command;
    }
    
    private static File createTempDir(String prefix, String suffix) throws IOException {
        File tempdir = File.createTempFile(prefix, suffix);
        // TODO following two statements should be done atomically
        tempdir.delete();
        tempdir.mkdir();
        return tempdir;
    }

    @Override
    public void setDefaultValues() {
        super.setDefaultValues();

        // default value of output_directory will be set in the last of parseCommandLine
        output_directory = null;

        String x10_dist = System.getProperty("x10.dist");
        String libdir = x10_dist + File.separator + "lib";        
        String stdlibdir = x10_dist + File.separator + "stdlib";
        String x10_jar = "x10.jar"; // FIXME: is this overridable?
        String math_jar = System.getProperty("x10c.math.jar", "commons-math3-3.0.jar");
        default_output_classpath = stdlibdir + File.separator + x10_jar + File.pathSeparator +
            libdir + File.separator + math_jar;
        output_classpath = default_output_classpath;

        // change post_compiler from "javac" to "java -jar ${x10.dist}/lib/ecj.jar"
        String ecj_jar = System.getProperty("x10c.ecj.jar", "ecj.jar");
        post_compiler = findJavaCommand("java") + " -jar \"" + libdir + File.separator + ecj_jar + "\" -1.5 -nowarn";
    }

    @Override
    public String constructFullClasspath() {
        StringBuilder sb = new StringBuilder(super.constructFullClasspath());
        for (PrecompiledLibrary pcl : x10libs) {
            sb.append(File.pathSeparator);
            sb.append(pcl.absolutePathToRoot + File.separator + pcl.sourceJar);
        }
        return sb.toString();
    }

    @Override
    public String constructPostCompilerClasspath() {
        StringBuilder sb = new StringBuilder(super.constructPostCompilerClasspath());
        for (PrecompiledLibrary pcl : x10libs) {
            sb.append(File.pathSeparator);
            sb.append(pcl.absolutePathToRoot + File.separator + pcl.sourceJar);
        }
        return sb.toString();
    }

    @Override
    protected int parseCommand(String[] args, int index, Set<String> source) throws UsageError, TerminationException {
        // create Main-Class attribute from main (= first) source name if MAIN_CLASS is not specified
        if (!args[index].startsWith("-")) {
            if (main_source == null) {
                main_source = args[index]; 
            }
        }
        
        return super.parseCommand(args, index, source);
    }

    @Override
    public void parseCommandLine(String[] args, Set<String> source) throws UsageError {
        super.parseCommandLine(args, source);

        // XTENLANG-2126
        if (!keep_output_files) { // -nooutput was specified
            // ignore -d output_directory if specified and
            // set a new temporary directory to output_directory.
            // after post-compile, the output_directory will be removed.
            try {
                String prefix = "x10c-" + System.getProperty("user.name") + ".";
                String suffix = "";
                output_directory = createTempDir(prefix, suffix);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if (output_directory == null) { // -d output_directory was not specified
            if (executable_path != null) {
                // set a new temporary directory to output_directory for creating a jar file
                try {
                    String prefix = "x10c-" + System.getProperty("user.name") + ".";
                    String suffix = "";
                    output_directory = createTempDir(prefix, suffix);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                String current_dir = System.getProperty("user.dir");
                output_directory = new File(current_dir);
            }
        }
    }
    
    @Override
    public void usage(PrintStream out) {
        super.usage(out);
        usageForFlag(out, "-c", "compile only to .java");
        usageForFlag(out, "-post <compiler>", 
                          "use <compiler> to generate .class files from generated .java");
    }

}
