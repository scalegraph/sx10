/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2013.
 */

package x10.runtime.impl.java;

import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class X10SimpleFormatter extends SimpleFormatter {
    
    @Override
    public synchronized String format(LogRecord record) {
        String message = super.format(record);
        x10.core.Thread thread = x10.core.Thread.currentThread();
        long placeId = thread.home().id;
        int workerId = ((x10.lang.Runtime.Worker) thread).workerId;
        long timestamp = java.lang.System.nanoTime() / 1000000L;
        message = String.format("[P%d,W%d,T%d] %s", placeId, workerId, timestamp, message);
        return message;
    }
}
