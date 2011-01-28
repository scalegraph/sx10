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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import polyglot.frontend.ExtensionInfo;
import polyglot.main.Main;
import polyglot.main.UsageError;
import polyglot.util.InternalCompilerError;
import x10.config.ConfigurationError;
import x10.config.OptionError;
import x10cpp.postcompiler.PrecompiledLibrary;

public class X10CompilerOptions extends polyglot.main.Options {
    
    public String executable_path = null;
    public Configuration x10_config;
    
    /**
     * Absolute path to the X10 distribution
     */
    private String distPath;
    public void setDistPath(String dp) { distPath = dp; }
    public String distPath() { return distPath; }
    
    public final List<PrecompiledLibrary> x10libs = new ArrayList<PrecompiledLibrary>();
    
	public X10CompilerOptions(ExtensionInfo extension) {
		super(extension);
		serialize_type_info = false; // turn off type info serialization for X10
		assertions = true; // turn on assertion generation for X10
		x10_config = new Configuration();
	}

	@Override
	protected int parseCommand(String args[], int index, Set<String> source) 
		throws UsageError, Main.TerminationException
	{
		int i = super.parseCommand(args, index, source);
		if (i != index) return i;

		if (args[i].equals("-noassert")) {
			assertions = false;
			return ++i;
		}
		if (args[i].equals("-o")) {
		    ++i;
		    executable_path = args[i];
		    return ++i;
		}
		
		if (args[i].equals("-x10lib")) {
		    ++i;
		    String libFile = args[i];
	        try {
	            Properties properties = new Properties();
	            File f = new File(libFile);
	            properties.load(new FileInputStream(f));
	            PrecompiledLibrary libObj = new PrecompiledLibrary(f.getParentFile().getAbsolutePath(), properties);
	            x10libs.add(libObj);
	        } catch(IOException e) {
	            UsageError ue = new UsageError("Unable to load x10library file "+libFile+" "+ e.getMessage());
	            ue.initCause(e);
	            throw ue;
	        }
	        return ++i;
		}

		try {
			x10_config.parseArgument(args[index]);
			return ++index;
		}
		catch (OptionError e) { }
		catch (ConfigurationError e) { }
		return index;
	}

	public int checkCommand(String args[], int index, Set<String> source)
		throws UsageError, Main.TerminationException, OptionError, ConfigurationError
	{
		int i = super.parseCommand(args, index, source);
		if (i != index) return i;
		
		x10_config.parseArgument(args[index]);
		return ++index;
	}

	/**
	 * Print usage information
	 */
	@Override
	public void usage(PrintStream out) {
		super.usage(out);
		usageForFlag(out, "-noassert", "turn off assertion generation");
		usageForFlag(out, "-o <path>", "set generated executable path (for the post-compiler)");
		usageForFlag(out, "-x10lib <lib.properties>", "use the precompiled x10 library described by <lib.properties>");

		String[][] options = x10_config.options();
		for (int i = 0; i < options.length; i++) {
			String[] optinfo = options[i];
			String optflag = "-"+optinfo[0]+"="+optinfo[1];
			String optdesc = optinfo[2]+"(default = "+optinfo[3]+")";
			usageForFlag(out, optflag, optdesc);
		}
	}
}
