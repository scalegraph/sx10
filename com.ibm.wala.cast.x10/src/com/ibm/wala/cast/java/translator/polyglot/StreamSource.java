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
 * Created on Oct 6, 2005
 */
package com.ibm.wala.cast.java.translator.polyglot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import polyglot.frontend.FileSource;
import polyglot.frontend.Resource;

/**
 * A Polyglot Source whose input comes from an InputStream.<br>
 * Currently extends FileSource since that's all that the Polyglot Compiler class
 * will accept.
 * @author rfuhrer
 */
public class StreamSource extends FileSource {
  public StreamSource(final InputStream s, final String fullPath) throws IOException {
    super(new Resource() {
      public File file() {
        return new File(fullPath);
      }

      public InputStream getInputStream() throws IOException {
        return s;
      }

      public String name() {
        int idx= fullPath.lastIndexOf(File.separatorChar);
        return (idx > 0) ? fullPath.substring(idx+1) : fullPath;
      }
      @Override
      public String toString() {
        return fullPath;
      }
    }, true);
  }
}
