/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2014.
 */

package x10c.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import x10.visit.X10PrettyPrinterVisitor;

/**
 * Generator of x10/core/fun/{Fun,VoidFun}_0_n.java
 * Run this command in x10.runtime/src-java
 * @author mtake
 */
public class MakeFun {
	private static final String copyright = "/*\n"
			+ " *  This file is part of the X10 project (http://x10-lang.org).\n"
			+ " *\n"
			+ " *  This file is licensed to You under the Eclipse Public License (EPL);\n"
			+ " *  You may not use this file except in compliance with the License.\n"
			+ " *  You may obtain a copy of the License at\n"
			+ " *      http://www.opensource.org/licenses/eclipse-1.0.php\n"
			+ " *\n"
			+ " *  (C) Copyright IBM Corporation 2006-2014.\n"
			+ " */\n";

	private static final String dont_modify = "/* This file is automatically generated. To change, modify MakeFun.java and regenerate. */\n";

	private static final String fun_imports = "package " + X10PrettyPrinterVisitor.X10_FUN_PACKAGE + ";\n\n"
			+ "import x10.rtt.FunType;\n"
			+ "import x10.rtt.RuntimeType;\n"
			+ "import x10.rtt.Type;\n";

	private static final String voidfun_imports = "package " + X10PrettyPrinterVisitor.X10_FUN_PACKAGE + ";\n\n"
			+ "import x10.rtt.RuntimeType;\n"
			+ "import x10.rtt.Type;\n"
			+ "import x10.rtt.VoidFunType;\n";

	// Functions that take less than MIN_METHOD_PARAMS are already in a repository
	private static final int MIN_METHOD_PARAMS = 10;
	private static final int MAX_METHOD_PARAMS = 127;

	public static void main(String[] args) throws FileNotFoundException {
	    int numGenerated = 0;
		int min = MIN_METHOD_PARAMS;
		int max = MAX_METHOD_PARAMS;
		if (args.length > 0) {
			int temp = Integer.parseInt(args[0]);
			if (temp < 1) {
				temp = 1;	// zero params need special treatment
			}
			min = max = temp;
			if (args.length > 1) {
				max = Integer.parseInt(args[1]);
			}
		}

		File dir = new File(X10PrettyPrinterVisitor.X10_FUN_PACKAGE.replace('.', '/'));
		dir.mkdirs();
		
		for (int n = min; n <= max; ++n) {
			
			/*
			 * Making Fun_0_n.java
			 */
			String fun_name = X10PrettyPrinterVisitor.X10_FUN_CLASS_NAME_PREFIX + "_0_" + n;
			File fun_f = new File(dir, fun_name + ".java");
			if (!fun_f.exists()) {
			    PrintStream fun_ps = new PrintStream(new FileOutputStream(fun_f));

			    fun_ps.println(copyright);
			    fun_ps.println(dont_modify);
			    fun_ps.println(fun_imports);

			    // public interface Fun_0_2<T1,T2,U> extends Fun {
			    fun_ps.print("public interface " + fun_name + "<");
			    for (int i = 1; i <= n; ++i) {
			        fun_ps.print("T" + i + ",");
			    }
			    fun_ps.println("U> extends " + X10PrettyPrinterVisitor.X10_FUN_CLASS_NAME_PREFIX + " {");

			    //     U $apply(T1 o1, Type t1, T2 o2, Type t2);
			    fun_ps.print("    U $apply(");
			    for (int i = 1; i <= n; ++i) {
			        fun_ps.print("T" + i + " o" + i + ", Type t" + i);
			        if (i < n)
			            fun_ps.print(", ");
			    }
			    fun_ps.println(");");

			    fun_ps.println();

			    //     public static final RuntimeType<Fun_0_2<?,?,?>> $RTT = FunType.<Fun_0_2<?,?,?>> make(
			    String fun_unknowntypes = "<";
			    for (int i = 1; i <= n + 1; ++i) {
			        fun_unknowntypes += "?";
			        if (i < n + 1)
			            fun_unknowntypes += ",";
			    }
			    fun_unknowntypes += ">";
			    fun_ps.print("    public static final RuntimeType<" + fun_name + fun_unknowntypes);
			    fun_ps.println("> " + X10PrettyPrinterVisitor.RTT_NAME + " = " + X10PrettyPrinterVisitor.X10_FUN_CLASS_NAME_PREFIX + "Type.<" + fun_name + fun_unknowntypes + "> make(");

			    //         Fun_0_2.class,
			    fun_ps.println("        " + fun_name + ".class,");

			    // 3
			    fun_ps.println("        " + (n + 1));

			    //     );
			    // }
			    fun_ps.println("    );");
			    fun_ps.println("}");

			    fun_ps.close();
			    numGenerated++;
			}
			

			/*
			 * Making VoidFun_0_n.java
			 */
			String voidfun_name = X10PrettyPrinterVisitor.X10_VOIDFUN_CLASS_NAME_PREFIX + "_0_" + n;
			File voidfun_f = new File(dir, voidfun_name + ".java");
			if (!voidfun_f.exists()) {
			    PrintStream voidfun_ps = new PrintStream(new FileOutputStream(voidfun_f));

			    voidfun_ps.println(copyright);
			    voidfun_ps.println(dont_modify);
			    voidfun_ps.println(voidfun_imports);

			    // public interface VoidFun_0_2<T1,T2> extends VoidFun {
			    voidfun_ps.print("public interface " + voidfun_name + "<");
			    for (int i = 1; i <= n; ++i) {
			        voidfun_ps.print("T" + i);
			        if (i < n)
			            voidfun_ps.print(",");
			    }
			    voidfun_ps.println("> extends " + X10PrettyPrinterVisitor.X10_VOIDFUN_CLASS_NAME_PREFIX + " {");

			    //     Object $apply(T1 o1, Type t1, T2 o2, Type t2);
			    voidfun_ps.print("    Object $apply(");
			    for (int i = 1; i <= n; ++i) {
			        voidfun_ps.print("T" + i + " o" + i + ", Type t" + i);
			        if (i < n)
			            voidfun_ps.print(", ");
			    }
			    voidfun_ps.println(");");

			    voidfun_ps.println();

			    //     public static final RuntimeType<VoidFun_0_2<?,?>> $RTT = VoidFunType.<VoidFun_0_2<?,?>> make(
			    String voidfun_unknowntypes = "<";
			    for (int i = 1; i <= n; ++i) {
			        voidfun_unknowntypes += "?";
			        if (i < n)
			            voidfun_unknowntypes += ",";
			    }
			    voidfun_unknowntypes += ">";
			    voidfun_ps.print("    public static final RuntimeType<" + voidfun_name + voidfun_unknowntypes);
			    voidfun_ps.println("> " + X10PrettyPrinterVisitor.RTT_NAME + " = " + X10PrettyPrinterVisitor.X10_VOIDFUN_CLASS_NAME_PREFIX + "Type.<" + voidfun_name + voidfun_unknowntypes + "> make(");

			    //         VoidFun_0_2.class,
			    voidfun_ps.println("        " + voidfun_name + ".class,");

			    // 2
			    voidfun_ps.println("        " + n);

			    //     );
			    // }
			    voidfun_ps.println("    );");
			    voidfun_ps.println("}");

			    voidfun_ps.close();
			    numGenerated++;
			}
			
		}
		if (numGenerated > 0) {
		    System.out.println("Generated "+numGenerated+" files.");
		}
	}

}
