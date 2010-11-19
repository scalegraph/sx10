/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
/*
 * Created on Oct 7, 2005
 */
package com.ibm.wala.cast.x10.loader;

import java.io.IOException;
import java.util.Set;

import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl;
import com.ibm.wala.cast.java.translator.SourceModuleTranslator;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;

public class PolyglotSourceLoaderImpl extends JavaSourceLoaderImpl {

  public PolyglotSourceLoaderImpl(ClassLoaderReference loaderRef, IClassLoader parent, SetOfClasses exclusions,
      IClassHierarchy cha) throws IOException {
    super(loaderRef, parent, exclusions, cha);
  }

  @Override
  protected SourceModuleTranslator getTranslator() {
    return new SourceModuleTranslator() {
      public void loadAllSources(Set s) {}
    };
  }
}
