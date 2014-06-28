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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import com.hazelcast.core.IMap;

import x10.lang.GlobalRail;
import x10.x10rt.SocketTransport.RETURNCODE;

public class X10RT {
    enum State { UNINITIALIZED, INITIALIZED, RUNNING, TEARING_DOWN, TORN_DOWN };
    
    // environment variables we check here
	public static final String X10_JOIN_EXISTING = "X10_JOIN_EXISTING"; // used to join an existing set of places
	public static final String X10RT_IMPL = "X10RT_IMPL"; // disabled, javasockets, or x10rt_* (e.g. x10rt_sockets )
	public static final String X10RT_DATASTORE = "X10RT_DATASTORE"; // only hazelcast is valid currently

    static State state = State.UNINITIALIZED;
    static int hereId;
    static x10.lang.Place here = null;
    static boolean forceSinglePlace = false;
    public static SocketTransport javaSockets = null;
    
    public static boolean X10_EXITING_NORMALLY = false;
    static final boolean REPORT_UNCAUGHT_USER_EXCEPTIONS = true;
    public static final boolean VERBOSE = false;
    static HazelcastDatastore hazelcastDatastore = null;
    
    /**
     * Initialize the X10RT runtime.  This method, or the standard init() method below 
     * must be called before any other methods on this class or on any other X10RT 
     * related class can be successfully invoked.
     */
    public static synchronized String init_library(final x10.runtime.impl.java.Runtime mainClass) {
    	if (state != State.UNINITIALIZED && 
    			state != State.TORN_DOWN) return null; // already initialized

        // load libraries
        String property = System.getProperty("x10.LOAD");
        if (null != property) {
            String[] libs = property.split(":");
            for (int i = libs.length - 1; i >= 0; i--)
                System.loadLibrary(libs[i]);
        }

        String libName = System.getProperty(X10RT_IMPL, "sockets");
        if (libName.equals("disabled"))
            forceSinglePlace = true;
        else if (libName.equalsIgnoreCase("JavaSockets")) {
      	  	X10RT.javaSockets = new SocketTransport();
      	    state = State.INITIALIZED;
      	  	return X10RT.javaSockets.getLocalConnectionInfo();
        }
        else {
            libName = "x10rt_" + libName;
            try {
                System.loadLibrary(libName);
            } catch (UnsatisfiedLinkError e) {
                System.err.println("Unable to load "+libName+". Forcing single place execution");
                forceSinglePlace = true;
            }
        }
        
/*    	x10.lang.Runtime.get$staticMonitor();
        x10.lang.Runtime.get$STRICT_FINISH();
        x10.lang.Runtime.get$NTHREADS();
        x10.lang.Runtime.get$MAX_THREADS();
        x10.lang.Runtime.get$STATIC_THREADS();
        x10.lang.Runtime.get$WARN_ON_THREAD_CREATION();
        x10.lang.Runtime.get$BUSY_WAITING();
*/

        state = State.INITIALIZED;
        if (forceSinglePlace) {
        	hereId = 0;
         	x10.runtime.impl.java.Runtime.MAX_PLACES = 1;
            state = State.RUNNING;
        	return null;
        }
        else
        	return x10rt_preinit();
    }
    
    /*
     * This method is the second phase of the init_library() call above.  Init_library only initializes
     * internal variables, minus what is needed for communication with other places.
     * 
     * myPlace is which place this runtime is in the whole computation.
     * connectionInfo is an array the size of nplaces, and contains the connection string for each
     * remote place.  The connection string for myPlace may be null.
     * 
     * This method returns true if the runtime was successfully initialized.
     * If false is returned, the caller should call this method again until true is returned.
     */
    public static synchronized boolean connect_library(int myPlace, String[] connectionInfo) {
    	if (state != State.INITIALIZED) return true; // already initialized

        X10RT.hereId = myPlace;
        
    	int errcode;
    	if (X10RT.javaSockets != null)
    		errcode = X10RT.javaSockets.establishLinks(myPlace, connectionInfo);
    	else {
    		errcode = x10rt_init(myPlace, connectionInfo);
    		TeamSupport.initialize();
    	}
        if (errcode != 0) {
            System.err.println("Failed to initialize X10RT. errorcode = "+errcode);
            try { x10rt_finalize();
            } catch (java.lang.UnsatisfiedLinkError e){}
            return false;
        }

        if (connectionInfo == null)
        	x10.runtime.impl.java.Runtime.MAX_PLACES = 1;
        else
        	x10.runtime.impl.java.Runtime.MAX_PLACES = connectionInfo.length;

        state = State.RUNNING;
        initDataStore();

        return true;
    }

    
    /*
     * This method returns true if the runtime was successfully initialized.
     * If false is returned, the caller should call this method again until true is returned.
     */
    public static synchronized boolean init() {
      if (state != State.UNINITIALIZED) return true; // already initialized

      String libName = System.getProperty("X10RT_IMPL", "sockets");
      if (libName.equals("disabled")) {
          forceSinglePlace = true;
      } 
      else if (libName.equalsIgnoreCase("JavaSockets")) {
    	  int ret;
    	  X10RT.javaSockets = new SocketTransport();
    	  // check if we are joining an existing computation
  		  String join = System.getProperty(X10_JOIN_EXISTING);
  		  if (join != null)
  			  ret = X10RT.javaSockets.establishLinks(join);
  		  else
  			  ret = X10RT.javaSockets.establishLinks();
  		  
    	  if (ret != RETURNCODE.X10RT_ERR_OK.ordinal()) {
    		  forceSinglePlace = true;
    		  System.err.println("Unable to establish links!  errorcode: "+ret+". Forcing single place execution");
    	  }
    	  else {
    		  hereId = X10RT.javaSockets.x10rt_here();
    		  x10.runtime.impl.java.Runtime.MAX_PLACES = X10RT.javaSockets.x10rt_nplaces();
    	  }
      }
      else {
          libName = "x10rt_" + libName;
          try {
              System.loadLibrary(libName);
              int err = x10rt_init(0, null);
              if (err != 0) {
//                  System.err.println("Failed to initialize X10RT.");
                  x10rt_finalize();
                  return false;
              }

              TeamSupport.initialize();

              hereId = x10rt_here();
              x10.runtime.impl.java.Runtime.MAX_PLACES = x10rt_nplaces();
          } catch (UnsatisfiedLinkError e) {
              System.err.println("Unable to load "+libName+". Forcing single place execution");
              forceSinglePlace = true;
          }
      }

      if (forceSinglePlace) {
          hereId = 0;
          x10.runtime.impl.java.Runtime.MAX_PLACES = 1;
      }
      else {
          // Add a shutdown hook to automatically teardown X10RT as part of JVM teardown
          Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
              public void run() {
                  synchronized(X10RT.class) {
                      state = State.TEARING_DOWN;
                      if (X10_EXITING_NORMALLY) {
                          if (VERBOSE) System.err.println("Normal exit; x10rt_finalize called");
                          if (javaSockets != null)
                        	  javaSockets.shutdown();
                          else
                        	  x10rt_finalize();
                          if (VERBOSE) System.err.println("Normal exit; x10rt_finalize returned");
                      } else {
                          if (VERBOSE) System.err.println("Abnormal exit; skipping call to x10rt_finalize");
                      }
                      state = State.TORN_DOWN;
                      if (hazelcastDatastore != null)
                    	  hazelcastDatastore.shutdown();
                      System.err.flush();
                      System.out.flush();
                  }
              }}));
      }
      
      state = State.RUNNING;
      initDataStore();

      return true;
    }

    /**
     * This is a non-blocking call.
     * Checks network for incoming messages and returns.
     */
    public static int probe() {
        assert isBooted();
        if (javaSockets != null)
        	return javaSockets.x10rt_probe();
        else if (!forceSinglePlace)
        	return x10rt_probe();
        else
        	return 0;
    }

    /**
     * This is a blocking call.
     * Blocking probe will not block if there is any live activity
     * in the network or GPU.  Incoming network data, outging network
     * data, or outstanding asynchronous kernels/DMA's in the GPU
     * will cause blockingProbe() to act as probe.  If none of these 
     * are true, then blockingProbe() will block on the network.
     */
    public static int blockingProbe() {
        assert isBooted();
        if (javaSockets != null)
        	return javaSockets.x10rt_blocking_probe();
        else if (!forceSinglePlace)
        	return x10rt_blocking_probe();
        else 
        	return 0;
    }
    
    /**
     * Unblock a thread stuck in blockingProbe(), or, if none are currently blocked, 
     * prevent the next call to blockingProbe() from blocking.
     * Safe to call at any time, or to call multiple times in a row.
     */
    public static void unblockProbe() {
        assert isBooted();
        if (javaSockets != null)
        	javaSockets.wakeup();
        else if (!forceSinglePlace)
        	x10rt_unblock_probe();
    }

    /**
     * Return the numeric id of the current Place.
     * @return the numeric id of the current Place.
     */
    public static int hereId() {
      assert isBooted();
      return hereId;
    }

    /**
     * Return the current Place.
     * @return the current Place.
     */
    public static x10.lang.Place here() {
      assert isBooted();
      if (null == here) {
          here = new x10.lang.Place(hereId);
      }
      return here;
    }

    /**
     * Return the number of places in the computation.
     * @return the number of places in the computation.
     */
    public static int numPlaces() {
      assert isBooted();
      if (javaSockets != null) 
    	  return javaSockets.x10rt_nplaces();
      else if (!forceSinglePlace) 
    	  return x10rt_nplaces();
      else
    	  return 1;
    }

    /**
     * Return the number of dead places.
     * @return the number of dead places.
     */
    public static int numDead() {
    	assert isBooted();
    	if (javaSockets != null) 
    		return javaSockets.numDead();
    	else if (!forceSinglePlace) 
    		return x10rt_ndead();
    	else
    		return 0;
    }

    /**
     * Returns true if the place is dead.
     * @return true if the place is dead.
     */
    public static boolean isPlaceDead(int place) {
    	assert isBooted();
    	if (javaSockets != null) 
    		return javaSockets.isPlaceDead(place);
    	else if (!forceSinglePlace) 
    		return x10rt_is_place_dead(place);
    	else
    		return false;
    }

    public static int collectiveSupport() {
        assert isBooted();
        if (forceSinglePlace || javaSockets != null)
        	return 0;
        else
        	return x10rt_coll_support();
      }
    
    // returns false if blocking_probe is just a call to probe and unblock_probe is a no-op 
    // returns true if blocking_probe actually blocks
    public static boolean blockingProbeSupport() {
        assert isBooted();
        if (forceSinglePlace)
        	return false;
        else if (javaSockets != null)
        	return true;
        else
        	return x10rt_blocking_probe_support();
      }

    public static boolean isBooted() {
      return state.compareTo(State.RUNNING) >= 0;
    }

    /**
     * To be called once XRX is ready to process incoming asyncs.
     */
    public static void registration_complete() {
        if (!forceSinglePlace && javaSockets == null)
        	x10rt_registration_complete();
    }
    
    public static void registerHandlers() {
    	if (!forceSinglePlace && javaSockets == null)
    		x10.x10rt.MessageHandlers.registerHandlers();
    }
    
    // library-mode alternative to the shutdown hook in init()
    public static synchronized int disconnect() {
    	state = State.TEARING_DOWN;
    	int ret = 0;
    	if (javaSockets != null)
    		ret = javaSockets.shutdown();
    	else
    		ret = x10rt_finalize();
    	
    	if (hazelcastDatastore != null)
    		hazelcastDatastore.shutdown();
    	state = State.TORN_DOWN;
    	return ret;
    }
    
    // Retrieve a resilient data store from the underlying network transport
    // See details of the implementation here: http://hazelcast.org/docs/latest/javadoc/com/hazelcast/core/IMap.html
    @SuppressWarnings("rawtypes")
	public static IMap getResilientMap(String name) {
    	if (hazelcastDatastore != null)
    		return hazelcastDatastore.getResilientMap(name);
    	else
    		return null;
    }
    
    // this form of initDataStore is called as a part of normal startup.
    private static void initDataStore() {
        // initialize hazelcast if X10RT_HAZELCAST has been set to true, and this is place 0
    	// we only start at 0 because the other places need to join an existing hazelcast cluster, 
    	// and the cluster is seeded via at least one other hazelcast instance.  place 0 doesn't join
    	// an existing cluster - it is the start of one.
    	
    	if (hereId == 0 && "Hazelcast".equalsIgnoreCase(System.getProperty(X10RT_DATASTORE, "none"))) {
    		if (X10RT.javaSockets == null) {
    			System.err.println("Error: you specified X10RT_DATASTORE=Hazelcast, but are not using JavaSockets, which is required.  Hazelcast is disabled.");
    			return;
    		}
    		// initialize a new hazelcast cluster
        	hazelcastDatastore = new HazelcastDatastore(null);
        	
        	// go to all other places, and tell them to connect to my newly created hazelcast cluster (of one, so far)
			try {
				byte[] message = hazelcastDatastore.getConnectionInfo().getBytes(SocketTransport.UTF8);
	      	   	for (int i=1; i<numPlaces(); i++) {
	          	   	ByteBuffer[] connectionBytes = new ByteBuffer[]{ByteBuffer.wrap(message)};
	      	   		javaSockets.sendMessage(SocketTransport.MSGTYPE.CONNECT_DATASTORE, i, 0, connectionBytes);
	      	   	}

			} catch (UnsupportedEncodingException e) {
				// this won't happen, because UTF8 is a required encoding
				e.printStackTrace();
				assert(false);
			}
			
			// wait until the number of expected containers have joined us
			while (hazelcastDatastore.getContainerCount() < numPlaces() ) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// nothing to do - just go back and check again
				}
			}
			// hazelcast is up and running in all places.  Return, and allow the user program to begin
    	}
    }
    
    // this form of initDataStore is used to load the data store on demand after normal startup
    // it takes one argument which is used to describe where this datastore should connect to
    static void initDataStore(String connectTo) {
    	//System.out.println("Connecting to hazelcast at "+connectTo);
    	hazelcastDatastore = new HazelcastDatastore(connectTo);
    }
    
    /*
     * Support for remote operations
     */
    public static void remoteAdd(GlobalRail target, long idx, long val) {
        throw new UnsupportedOperationException("remoteAdd not implemented for Managed X10");
    }
    public static void remoteAdd__1$u(GlobalRail target, long idx, long val) {
        throw new UnsupportedOperationException("remoteAdd not implemented for Managed X10");
    }

    public static void remoteAnd(GlobalRail target, long idx, long val) {
        throw new UnsupportedOperationException("remoteAnd not implemented for Managed X10");
    }
    public static void remoteAnd__1$u(GlobalRail target, long idx, long val) {
        throw new UnsupportedOperationException("remoteAnd not implemented for Managed X10");
    }

    public static void remoteOr(GlobalRail target, long idx, long val) {
        throw new UnsupportedOperationException("remoteOr not implemented for Managed X10");
    }
    public static void remoteOr__1$u(GlobalRail target, long idx, long val) {
        throw new UnsupportedOperationException("remoteOr not implemented for Managed X10");
    }

    public static void remoteXor(GlobalRail target, long idx, long val) {
        throw new UnsupportedOperationException("remoteXor not implemented for Managed X10");
    }
    public static void remoteXor__1$u(GlobalRail target, long idx, long val) {
        throw new UnsupportedOperationException("remoteXor not implemented for Managed X10");
    }

    /*
     * Native methods exported from x10rt_front.h that are 
     * related to initialization and finalization of the X10RT library.
     * See X10RT API at x10-lang.org for semantics. 
     */
    private static native String x10rt_preinit();
    
    private static native int x10rt_init(int numArgs, String[] args);
    
    private static native int x10rt_finalize();

    private static native int x10rt_registration_complete();

    /*
     * Native method exported from x10rt_front.h that are related to Places
     */
    private static native int x10rt_nplaces();
        
    private static native int x10rt_ndead();
    
    private static native boolean x10rt_is_place_dead(int place);
    
    private static native int x10rt_here();
    
    private static native int x10rt_coll_support();
    
    /*
     * Subset of x10rt_front.h API related to messages that actually needs
     * to be exposed at the Java level (as opposed to being used
     * in the native code backing the native methods of MessageHandlers.
     */
    private static native int x10rt_probe();
    
    private static native boolean x10rt_blocking_probe_support();
    
    private static native int x10rt_blocking_probe();
    
    private static native int x10rt_unblock_probe();
}
