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

package x10.core;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;

import x10.rtt.RuntimeType;

public class ThrowableUtilities {
    
    static public x10.core.Throwable getCorrespondingX10Exception(java.lang.RuntimeException e) {
        String newExcName = "x10.lang.RuntimeException";
        if (e instanceof java.lang.ArithmeticException) {
            newExcName = "x10.lang.ArithmeticException";
        } else if (e instanceof java.lang.ArrayIndexOutOfBoundsException) {
            newExcName = "x10.lang.ArrayIndexOutOfBoundsException";
        } else if (e instanceof java.lang.ClassCastException) {
            newExcName = "x10.lang.ClassCastException";
        } else if (e instanceof java.lang.NumberFormatException) {
            newExcName = "x10.lang.NumberFormatException";
        } else if (e instanceof java.lang.IllegalArgumentException) {
            newExcName = "x10.lang.IllegalArgumentException";
        } else if (e instanceof java.util.NoSuchElementException) {
            newExcName = "x10.util.NoSuchElementException";
        } else if (e instanceof java.lang.NullPointerException) {
            newExcName = "x10.lang.NullPointerException";
        } else if (e instanceof java.lang.UnsupportedOperationException) {
            newExcName = "x10.lang.UnsupportedOperationException";
        } else {
            // no corresponding x10 exceptions defined
        }

        try {
            x10.core.Throwable t = Class.forName(newExcName).asSubclass(x10.core.Throwable.class).getConstructor(new Class[] { String.class }).newInstance(new Object[] { e.getMessage() });
            t.setStackTrace(e.getStackTrace());
            return t;
        } catch (java.lang.ClassNotFoundException e1) {
        } catch (java.lang.InstantiationException e2) {
        } catch (java.lang.IllegalAccessException e3) {
        } catch (java.lang.NoSuchMethodException e4) {
        } catch (java.lang.reflect.InvocationTargetException e5) {
        }
        throw new java.lang.Error();
    }

    static public x10.core.Throwable getCorrespondingX10Exception(java.lang.Exception e) {
        String newExcName = "x10.lang.Exception";
        if (e instanceof java.io.FileNotFoundException) {
            newExcName = "x10.io.FileNotFoundException";
        } else if (e instanceof java.io.EOFException) {
            newExcName = "x10.io.EOFException";
        } else if (e instanceof java.io.IOException) {
            newExcName = "x10.io.IOException";
        } else if (e instanceof java.lang.InterruptedException) {
            newExcName = "x10.lang.InterruptedException";
        } else {
            // no corresponding x10 exceptions defined
        }

        try {
            x10.core.Throwable t = Class.forName(newExcName).asSubclass(x10.core.Throwable.class).getConstructor(new Class[] { String.class }).newInstance(new Object[] { e.getMessage() });
            t.setStackTrace(e.getStackTrace());
            return t;
        } catch (java.lang.ClassNotFoundException e1) {
        } catch (java.lang.InstantiationException e2) {
        } catch (java.lang.IllegalAccessException e3) {
        } catch (java.lang.NoSuchMethodException e4) {
        } catch (java.lang.reflect.InvocationTargetException e5) {
        }
        throw new java.lang.Error();
    }

    static public x10.core.Throwable getCorrespondingX10Error(java.lang.Error e) {
        String newExcName = "x10.lang.Error";
        if (e instanceof java.lang.OutOfMemoryError) {
            newExcName = "x10.lang.OutOfMemoryError";
        } else if (e instanceof java.lang.AssertionError) {
            newExcName = "x10.lang.AssertionError";
        } else {
            // no corresponding x10 errors defined
        }

        try {
            x10.core.Throwable t = Class.forName(newExcName).asSubclass(x10.core.Throwable.class).getConstructor(new Class[] { String.class }).newInstance(new Object[] { e.getMessage() });
            t.setStackTrace(e.getStackTrace());
            return t;
        } catch (java.lang.ClassNotFoundException e1) {
        } catch (java.lang.InstantiationException e2) {
        } catch (java.lang.IllegalAccessException e3) {
        } catch (java.lang.NoSuchMethodException e4) {
        } catch (java.lang.reflect.InvocationTargetException e5) {
        }
        throw new java.lang.Error();
    }

    public static Rail<String> getStackTrace(Throwable t) {
        StackTraceElement[] elements = t.getStackTrace();
        String str[] = new String[elements.length];
        for (int i=0 ; i<elements.length ; ++i) {
            str[i] = elements[i].toString();
        }
        return new Rail<String>(new RuntimeType<String>(String.class),str.length,(Object)str);
    }
    
    public static void printStackTrace(Throwable t, Object/*x10.io.Printer*/ p) {
        Class<?> x10_io_Printer = null;
        try {
            x10_io_Printer = Class.forName("x10.io.Printer");
        } catch (Exception e) {
        }
        Method printStackTrace = null;
        if (x10_io_Printer != null) {
            try {
                printStackTrace = t.getClass().getMethod("printStackTrace", x10_io_Printer);
            } catch (Exception e) {
            }
        }
        if (printStackTrace != null) {
            try {
                printStackTrace.invoke(t, p);
            } catch (Exception e) {
            }
        } else {
            try {
                Method getNativeOutputStream = x10_io_Printer.getMethod("getNativeOutputStream");
                PrintWriter printWriter = new java.io.PrintWriter((OutputStream) getNativeOutputStream.invoke(p));
                t.printStackTrace(printWriter);
            } catch (Exception e) {
            }
        }
    }
    
}
