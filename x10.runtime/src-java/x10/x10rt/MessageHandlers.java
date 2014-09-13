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
package x10.x10rt;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import x10.core.fun.VoidFun_0_0;
import x10.lang.FinishState;
import x10.lang.Place;
import x10.runtime.impl.java.Runtime;
import x10.serialization.X10JavaDeserializer;

/**
 * A class to contain the Java portion of message send/receive pairs.
 */
public class MessageHandlers {
    
    // values set in native method registerHandlers()
    private static int closureMessageID;
    private static int simpleAsyncMessageID;
		
    /**
     * Register the native methods that will invoke runClosureAtReceive
     * and runSimpleAsyncAtReceive as message handlers with the x10rt layer.
     * The message ids obtained from this registration will be stored
     * in the static fields closureMessageId and simpleAsyncMessageId.
     */
    public static synchronized native void registerHandlers();
    
    /**
     * Send an active message.
     */
    private static native void sendMessage(int place, int msg_id, int arraylen, byte[] rawBytes);

	/*
	 * This send/receive pair is used to serialize a ()=>void closure to
	 * a remote place, which will deserialize the closure object and calls apply on it.
	 * 
	 * One important use of this message pair is the non-optimized implementation of
	 * x10.lang.Runtime.runClosureAt and x10.lang.Runtime.runClosureCopyAt. 
	 */
	
    public static void runClosureAtSend(int place, byte[] rawBytes) {
        sendMessage(place, closureMessageID, rawBytes.length, rawBytes);
    }
        
    // Invoked from native code at receiving place
    // This function gets called by the x10rt callback that is registered to handle
    // the receipt of general closures.
    private static void runClosureAtReceive(byte[] args) {
    	try{
    		if (X10RT.VERBOSE) System.out.println("runClosureAtReceive is called");
    		java.io.ByteArrayInputStream byteStream = new java.io.ByteArrayInputStream(args);
    		if (X10RT.VERBOSE) System.out.println("runClosureAtReceive: ByteArrayInputStream");

    		long start = Runtime.PROF_SER ? System.nanoTime() : 0;
    		DataInputStream objStream = new DataInputStream(byteStream);
    		if (X10RT.VERBOSE) System.out.println("runClosureAtReceive: ObjectInputStream");
    		X10JavaDeserializer deserializer = new X10JavaDeserializer(objStream);
    		if (x10.runtime.impl.java.Runtime.TRACE_SER_DETAIL) {
    			System.out.println("Starting deserialization ");
    		}
    		VoidFun_0_0 actObj = (VoidFun_0_0) deserializer.readObject();
    		if (Runtime.PROF_SER) {
    			long stop = System.nanoTime();
    			long duration = stop-start;
    			if (duration >= Runtime.PROF_SER_FILTER) {
    				System.out.println("Deserialization took "+(((double)duration)/1e6)+" ms.");
    			}
    		}
    		if (x10.runtime.impl.java.Runtime.TRACE_SER_DETAIL) {
    			System.out.println("Ending deserialization ");
    		}

    		if (X10RT.VERBOSE) System.out.println("runClosureAtReceive: after cast and deserialization");
    		actObj.$apply();
    		if (X10RT.VERBOSE) System.out.println("runClosureAtReceive: after apply");
    		objStream.close();
    		if (X10RT.VERBOSE) System.out.println("runClosureAtReceive is done !");
    	} catch(Exception ex){
    		System.out.println("runClosureAtReceive error !!!");
    		ex.printStackTrace();
    	}
    }

    /*
     * This send/receive pair is used to serialize a simple async
     * (finish state + async body closure) to
     * a remote place, which deserializes the two objects and invokes the
     * async body. 
     * 
     * This is the "normal" case of used to implement a typical X10-level async.
     */
    
    public static void runSimpleAsyncAtSend(int place, byte[] rawBytes) { 
        sendMessage(place, simpleAsyncMessageID, rawBytes.length, rawBytes);
    }
    
    /**
     * Receive a simple async
     */
    private static void runSimpleAsyncAtReceive(byte[] args) {
    	try{
    		if (X10RT.VERBOSE) System.out.println("runSimpleAsyncAtReceive is called");
    		ByteArrayInputStream byteStream = new ByteArrayInputStream(args);
    		if (X10RT.VERBOSE) System.out.println("runSimpleAsyncAtReceive: ByteArrayInputStream");
    		VoidFun_0_0 actObj;

    		long start = Runtime.PROF_SER ? System.nanoTime() : 0;
    		DataInputStream objStream = new DataInputStream(byteStream);
    		if (X10RT.VERBOSE) System.out.println("runSimpleAsyncAtReceive: ObjectInputStream");
    		X10JavaDeserializer deserializer = new X10JavaDeserializer(objStream);
    		if (x10.runtime.impl.java.Runtime.TRACE_SER_DETAIL) {
    			System.out.println("Starting deserialization ");
    		}
    		FinishState finishState = (FinishState) deserializer.readObject();
            Place src = (Place) deserializer.readObject();
            
            try {
                actObj = (VoidFun_0_0) deserializer.readObject();
                if (Runtime.PROF_SER) {
                    long stop = System.nanoTime();
                    long duration = stop-start;
                    if (duration >= Runtime.PROF_SER_FILTER) {
                        System.out.println("Deserialization took "+(((double)duration)/1e6)+" ms.");
                    }
                }
                if (x10.runtime.impl.java.Runtime.TRACE_SER_DETAIL) {
                    System.out.println("Ending deserialization ");
                }
            } catch (Throwable e) {
                if (X10RT.VERBOSE) System.out.println("runSimpleAsyncAtReceive: handling exception during deserialization");
                finishState.notifyActivityCreation$O(src);
                finishState.pushException(new x10.io.SerializationException(e));
                finishState.notifyActivityTermination();
                if (X10RT.VERBOSE) System.out.println("runSimpleAsyncAtReceive: exception pushed; bookkeeping complete");
                return;
            }
    		
    		if (X10RT.VERBOSE) System.out.println("runSimpleAsyncAtReceive: after cast and deserialization");
    		x10.lang.Runtime.execute(actObj, src, finishState);
    		if (X10RT.VERBOSE) System.out.println("runSimpleAsyncAtReceive: after apply");
    		objStream.close();
    		if (X10RT.VERBOSE) System.out.println("runSimpleAsyncAtReceive is done !");
    	} catch(Exception ex){
    		System.out.println("runSimpleAsyncAtReceive error !!!");
    		ex.printStackTrace();
    	}
    }
}
