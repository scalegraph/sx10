/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2015.
 */

package apgas.impl;

import java.util.List;

/**
 * The {@link Launcher} interface.
 */
public interface Launcher {
  /**
   * Launches n processes with the given command line.
   *
   * @param n
   *          number of processes to launch
   * @param command
   *          command line
   * @param verbose
   *          dumps the executed commands to stderr
   * @throws Exception
   *           if launching fails
   */
  void launch(int n, List<String> command, boolean verbose) throws Exception;

  /**
   * Shuts down the {@link Launcher} instance.
   */
  void shutdown();
}
