package x10cpp.postcompiler;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

import polyglot.util.DiffWriter;
import polyglot.util.ErrorQueue;
import polyglot.util.QuotedStringTokenizer;
import x10.X10CompilerOptions;

public class CXXMakeBuilder extends CXXCommandBuilder {
    DiffWriter dw;
    CXXCommandBuilder ccb;

    public CXXMakeBuilder(X10CompilerOptions options, PostCompileProperties x10rt_props, SharedLibProperties shared_lib_props, ErrorQueue eq) throws FileNotFoundException, IOException
    {
        dw = new DiffWriter(new File(options.output_directory, "Makefile"));
        ccb = getCXXCommandBuilder(options, x10rt_props, shared_lib_props, eq);
        setData(options, x10rt_props, shared_lib_props, eq);
    }

    public void println(String str) throws IOException
    {
        dw.println(str);
    }

    public void println(String preToken, Collection<String> tokenArray) throws IOException
    {
        StringBuilder str = new StringBuilder();
        str.append(preToken);
        for (String token : tokenArray) {
            str.append(token+" ");
        }
        dw.println(str.toString());
    }

    /** Construct the C++ compilation command */
    public final String[] buildCXXMakefile(Collection<String> outputFiles) throws IOException
    {
        String post_compiler = options.post_compiler;
        if (post_compiler.contains("javac")) {
            post_compiler = defaultPostCompiler();
        }

        QuotedStringTokenizer st = new QuotedStringTokenizer(post_compiler);
        int pc_size = st.countTokens();
        ArrayList<String> firstTokens = new ArrayList<String>();
        String token = "";
        for (int i = 0; i < pc_size; i++) {
            token = st.nextToken();
            // A # as the first token signifies that the default postcompiler for this platform be used
            if (i==0 && token.equals("#")) {
            	firstTokens.add(defaultPostCompiler());
            	continue;
            }

        	// consume all tokens up until the next # (or %) whereupon we will insert (or not)
        	// default CXXFLAGS parameters and generated compilation units
            if (token.equals("#") || token.equals("%")) {
                break;
            }
            firstTokens.add(token);
        }

        ArrayList<String> preArgs = new ArrayList<String>();
        boolean skipArgs = token.equals("%");
        if (!skipArgs) {
            ccb.addPreArgs(preArgs);
            if (options.buildX10Lib != null && sharedLibProps.staticLib) {
                preArgs.add("-c");
            } else {
                addExecutablePath(preArgs);
            }
        }

        ArrayList<String> srcFiles = new ArrayList<String>();
        for (String file : outputFiles) {
            file = file.replace(File.separatorChar,'/');
            if (file.endsWith(".cu")) continue;
            srcFiles.add(file);
        }

        ArrayList<String> secondTokens = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            token = st.nextToken();
            // The second '#' delimits the libraries that have to go at the end
            if (token.equals("#") || token.equals("%")) {
                break;
            }
            secondTokens.add(token);
        }

        ArrayList<String> postArgs = new ArrayList<String>();
        boolean skipLibs = token.equals("%");
        if (!skipLibs) {
            ccb.addPostArgs(postArgs);
        }

        ArrayList<String> thirdTokens = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            thirdTokens.add(st.nextToken());
        }


        println("SRC=", srcFiles);
        println("OBJS=$(patsubst %.cc,%.o,$(SRC))");
        println("DEPENDS=$(patsubst %.cc,%.d,$(SRC))");
        println("FIRST=", firstTokens);
        println("PREARGS=", preArgs);
        println("SECOND=", secondTokens);
        println("POSTARGS=", postArgs);
        println("THIRD=", thirdTokens);

        println("all: $(OBJS)");
        println("\t$(FIRST) $(PREARGS) $(OBJS) $(SECOND) $(POSTARGS) $(THIRD)");
        println(".cc.o:");
        println("\t$(FIRST) $(PREARGS) -MMD -MF $(patsubst %.cc,%.d,$<) -c $< -o $@ $(SECOND) $(POSTARGS) $(THIRD)");
        println("-include $(DEPENDS)");

        dw.flush();
        dw.close();

        ArrayList<String> cxxCmd = new ArrayList<String>();
        cxxCmd.add("make");
        cxxCmd.add("all");
        cxxCmd.addAll(options.makeOptions);
        return cxxCmd.toArray(new String[cxxCmd.size()]);
    }

}
