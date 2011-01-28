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

package x10cpp.postcompiler;

import java.util.ArrayList;

import polyglot.main.Options;
import polyglot.util.ErrorQueue;

public class Linux_CXXCommandBuilder extends CXXCommandBuilder {
    protected static final boolean USE_BFD = System.getenv("USE_BFD")!=null;

    Linux_CXXCommandBuilder(Options options, PostCompileProperties x10rt, ErrorQueue eq) {
        super(options, x10rt, eq);
    }

    protected void addPreArgs(ArrayList<String> cxxCmd) {
        super.addPreArgs(cxxCmd);
        if (!usingXLC()) {
            cxxCmd.add("-pthread");
            if (getPlatform().endsWith("_x86")) {
                cxxCmd.add("-msse2");
                cxxCmd.add("-mfpmath=sse");
            }
        }
    }

    protected void addPostArgs(ArrayList<String> cxxCmd) {
        super.addPostArgs(cxxCmd);

        for (PrecompiledLibrary pcl:options.x10libs) {
            cxxCmd.add("-Wl,--rpath");
            cxxCmd.add("-Wl,"+pcl.absolutePathToRoot+"/lib");
        }
        
        // x10rt
        cxxCmd.add("-Wl,--rpath");
        cxxCmd.add("-Wl,"+options.distPath()+"/lib");

        cxxCmd.add("-Wl,-export-dynamic");
        cxxCmd.add("-lrt");
        if (USE_BFD) {
            cxxCmd.add("-lbfd");
            cxxCmd.add("-liberty");
        }
    }
}
