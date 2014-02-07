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

package polyglot.types.reflect;

import java.util.*;
import java.io.*;

/**
 * Exceptions describes the types of exceptions that a method may throw.
 * The Exceptions attribute stores a list of indices into the constant
 * pool of the types of exceptions thrown by the method.
 *
 * @see polyglot.types.reflect Method
 *
 * @author Nate Nystrom
 *         (<a href="mailto:nystrom@cs.purdue.edu">nystrom@cs.purdue.edu</a>)
 */
public class Exceptions extends Attribute {
  private int[] exceptions;
  private ClassFile clazz;

  /**
   * Constructor for create an <code>Exceptions</code> from scratch.
   *
   * @param nameIndex
   *        The index of the UTF8 string "Exceptions" in the class's
   *        constant pool
   * @param exceptions
   *        A non-<code>null</code> array of indices into the constant
   *        pool for the types of the exceptions
   */
  public Exceptions(ClassFile clazz, int nameIndex, int[] exceptions) {
    super(nameIndex, (2 * exceptions.length) + 2);
    this.clazz = clazz;
    this.exceptions = exceptions;
  }

  /**
   * Constructor.  Create an Exceptions attribute from a data stream.
   *
   * @param in
   *        The data stream of the class file.
   * @param nameIndex
   *        The index into the constant pool of the name of the attribute.
   * @param length
   *        The length of the attribute, excluding the header.
   * @exception IOException
   *        If an error occurs while reading.
   */
  public Exceptions(ClassFile clazz, DataInputStream in,
		    int nameIndex, int length) throws IOException
  {
    super(nameIndex, length);

    this.clazz = clazz;

    int count = in.readUnsignedShort();

    exceptions = new int[count];

    for (int i = 0; i < count; i++) {
      exceptions[i] = in.readUnsignedShort();
    }
  }
  public ClassFile getClazz() {
      return clazz;
  }
  public int[] getThrowTypes() {
      return exceptions;
  }
}
