/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.frontend;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import polyglot.main.Report;
import polyglot.types.Name;
import polyglot.types.QName;
import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;
import polyglot.util.UnicodeWriter;

/** A <code>TargetFactory</code> is responsible for opening output files. */
public class TargetFactory
{
    protected File outputDirectory;
    protected String outputExtension;
    protected boolean outputStdout;

    public TargetFactory(File outDir, String outExt, boolean so) {
	outputDirectory = outDir;
	outputExtension = outExt;
	outputStdout = so;
    }

    /** Open a writer to the output file for the class in the given package. */
    public Writer outputWriter(QName packageName, Name className,
	    Source source) throws IOException 
    {
	return outputWriter(outputFile(packageName, className, source));
    }

    public CodeWriter outputCodeWriter(File f, int width) throws IOException {
    	Writer w = outputWriter(f);
        return Compiler.createCodeWriter(w, width);
    }

    /** Open a writer to the output file. */
    public Writer outputWriter(File outputFile) throws IOException {
	if (Report.should_report(Report.frontend, 2))
	    Report.report(2, "Opening " + outputFile + " for output.");

	if (outputStdout) {
	    return new UnicodeWriter(new PrintWriter(System.out));
	}

	if (! outputFile.getParentFile().exists()) {
	    File parent = outputFile.getParentFile();
	    parent.mkdirs(); // ignore return; new FileWriter will check
	}

	return new UnicodeWriter(new FileWriter(outputFile));
    }

    /** Return a file object for the output of the source file in the given package. */
    public File outputFile(QName packageName, Source source) {
	String name;
	name = new File(source.name()).getName();
	name = name.substring(0, name.lastIndexOf('.'));
	return outputFile(packageName, Name.make(name), source);
    }

    /** Return a file object for the output of the class in the given package. */
    public File outputFile(QName packageName, Name className, Source source)
    {
	if (outputDirectory == null) {
	      throw new InternalCompilerError("Output directory not set.");
	}

	String pkgString;
	
	if (packageName == null)
	    pkgString = "";
	else
	    pkgString = packageName.toString();

	File outputFile = new File(outputDirectory,
				   pkgString.replace('.', File.separatorChar)
				   + File.separatorChar
				   + className
				   + "." + outputExtension);

        if (source != null && outputFile.getPath().equals(source.path())) {
	    throw new InternalCompilerError("The output file is the same as the source file");
	}
	
	return outputFile;
    }
}
