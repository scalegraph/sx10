/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2011.
 */

package x10.matrix.comm;

import x10.io.Console;
import x10.util.Timer;

import x10.compiler.Ifdef;
import x10.compiler.Ifndef;
import x10.compiler.Uninitialized;

import x10.matrix.Debug;

import x10.matrix.Matrix;
import x10.matrix.DenseMatrix;
import x10.matrix.comm.mpi.WrapMPI;
import x10.matrix.sparse.CompressArray;
import x10.matrix.sparse.SparseCSC;


/**
 * Ring cast, similar to broadcast, sends data to selected places, while using place-to-place
 * communication without optimization on its broadcast route.
 * It starts by sending data from here to the first place in the list  
 * Upon receiving data, the first place forwards the data to the second place, 
 * and so on until the end of list.
 * This operation is mainly used by SUMMA algorithm,
 * when broadcasting data among places in the same partition row (or column).
 *
 * <p> Two data types are supported, double-precision array and compressed
 * data array (CompressArray).
 * 
 * <p> Two implementations are available. One uses MPI routines, and the other
 * is based on X10 remote array copy.
 * To enable MPI communication, add "-define MPI_COMMU -cxx-prearg -DMPI_COMMU"
 * in x10c++ build command, when you include commu package in your application source
 * code, or link to the proper GML library (native_mpi version).
 * 
 * <p>For more information on how to build different backends and runtime, 
 * run command "make help" at the root directory of GML library.
 */
public class DistArrayRcast extends DistArrayRemoteCopy { 

	//===========================
	// Constructor
	//===========================
	public def this() {
		super();
	}
	//==================================================
	// RingCast: receive form previous one and send to one next in a ring 
	//==================================================

	/**
	 * Broadcast the whole distributed array at here to all places one by one 
	 *
	 * @param dmlist 		distributed storage for data arrays 
	 */
	public static def bcast(dmlist:DistDataArray) : void{
		val sz = dmlist(here.id()).size;
		rcast(dmlist, sz);
	}


	//--------------------------------------------
	/**
	 * Broadcast double-precision data array from here to all places.
	 *
	 * @param dmlist 		distributed storage for data arrays
	 * @param datCnt 		number of data elements in array to broadcast 
	 */
	public static def rcast(dmlist:DistDataArray, datCnt:Int) : void{
		val pcnt = dmlist.dist.region.size();
		val plist:Array[Int](1) = new Array[Int](pcnt, (i:Int)=>(i));
		rcast(dmlist, datCnt, plist);
	}


	/**
	 * Send double-precision data array from here to the specified places.
	 * 
	 * @param dmlist 		distributed storage for data arrays
	 * @param datCnt 		number of data to broadcast from here.
	 * @param plist  		the list of places in the ring cast
	 */
	public static def rcast(
			dmlist:DistDataArray, 
			datCnt:Int, 
			plist:Array[Int](1)) : void {
		
		@Ifdef("MPI_COMMU") {
			mpiRcast(dmlist, datCnt, plist);
		}

		@Ifndef("MPI_COMMU") {
			//Debug.flushln("start bcast to "+numPlaces);
			x10Rcast(dmlist, datCnt, plist);
		}
	}
	//----------------------------------------------------------------
	
	/**
	 * Send data from here to places in the list using
	 * MPI send/recv routines.
	 * 
	 * @param  dmlist 		distributed storage for data arrays
	 * @param  datCnt 		count of data elements to be sent
	 * @param  plist 		the list of place ids. Root must be in the list
	 */
	protected static def mpiRcast(
			dmlist:DistDataArray, 
			datCnt:Int, 
			plist:Array[Int](1)):void {
		
	@Ifdef("MPI_COMMU") {
		
		// Check place list 
		if (plist.size <= 0) return ;

		val root   = here.id();                     //Implicitly copied to all places
		finish {
	
			for (var p:Int=0; p < plist.size; p++) {
				val nxtpid = (p==plist.size-1)?root:plist(p+1); //Implicitly carry to next place
				val prepid = (p==0)?root:plist(p-1);            //Implicitly carry to next place
				val curpid = plist(p);

				at (dmlist.dist(curpid)) async {
					//Need: dmlist, root, nxtpid, prepid, colOff, datasz
					val mypid  = here.id();
					val matbuf = dmlist(mypid);

					if (mypid != root) {
						val dtag = prepid * 5000 + mypid;
						WrapMPI.world.recv(matbuf, 0, datCnt, prepid, dtag);
					}
					if (nxtpid != root) {
						val dtag = mypid * 5000 + nxtpid;
						WrapMPI.world.send(matbuf, 0, datCnt, nxtpid, dtag);
					}
				}
			}
		}
	}
	}	
	
	//======================================================================
	// Sparse matrix
	//======================================================================
	/**
	 * Broadcast data in compress array to all places.
	 *
	 * @param smlist 		dist compress array 
	 * @param catCnt 		number of nonzero data elements to broadcast 
	 */
	public static def rcast(smlist:DistCompArray, datCnt:Int) : void{
		val pcnt = smlist.dist.region.size();
		val plist:Array[Int](1) = new Array[Int](pcnt, (i:Int)=>(i));
		rcast(smlist, datCnt, plist);
	}

	/**
	 * Broadcast the whole compress array at here to all places 
	 * 
	 * @param smlist 		distributed storage for copies of compress array in all places
	 */
	public static def rcast(smlist:DistCompArray): void {
		rcast(smlist, smlist(here.id()).count());
	}

	/**
	 * Send compress array from here to a list of places.
	 *
	 * @param smlist 		distributed storage for copies of compress array
	 * @param datCnt 		number of nonzero data elements to broadcast
	 */
	public static def rcast(
			smlist:DistCompArray, 
			datCnt:Int, 
			plist:Array[Int](1)) : void {
		
		@Ifdef("MPI_COMMU") {
			mpiRcast(smlist, datCnt, plist);
		}

		@Ifndef("MPI_COMMU") {
			//Debug.flushln("start bcast to "+numPlaces);
			x10Rcast(smlist, datCnt, plist);
		}
	}

	/**
	 * Send compress array from here to a list of places by using
	 * X10 remote array copy.
	 *
	 * @param  smlist 		distributed storage for copies of compress array 
	 * @param  datCnt 		counts of nonzero data elements to send
	 * @param  plist 		the list of place IDs
	 */
	protected static def mpiRcast(
			smlist:DistCompArray, 
			datCnt:Int, 
			plist:Array[Int](1)):void {
		
		@Ifdef("MPI_COMMU") {
			// Check place list 
			if (plist.size <= 0) return;

			val root   = here.id();                     //Implicitly copied to all places
			val srcbuf = smlist(root);

			//Not matrix, no need to initialize
			//srcspa.initRemoteCopyAtSource(colOff, colCnt);
			finish {
	
				for (var p:Int=0; p < plist.size; p++) {
					val nxtpid = (p==plist.size-1)?root:plist(p+1); //Implicitly carry to next place
					val prepid = (p==0)?root:plist(p-1);            //Implicitly carry to next place
					//val curpid = p;
					val curpid = plist(p);

					async at (smlist.dist(curpid)) {
						//Need: dmlist, root, nxtpid, prepid, colOff, colCnt, datasz
						val mypid  = here.id();
						val matbuf = smlist(mypid);
					
						if (mypid != root) {
							val dtag = prepid * 10000 + mypid;

							//++++++++++++++++++++++++++++++++++++++++++++
							// If there is not enough place in destination
							// execution will exit
							//+++++++++++++++++++++++++++++++++++++++++++++
							WrapMPI.world.recv(matbuf.index, 0, datCnt, prepid, dtag);
							WrapMPI.world.recv(matbuf.value, 0, datCnt, prepid, dtag);
						}
						if (nxtpid != root) {
							val dtag = mypid * 10000 + nxtpid;
							WrapMPI.world.send(matbuf.index, 0, datCnt, nxtpid, dtag);
							WrapMPI.world.send(matbuf.value, 0, datCnt, nxtpid, dtag);
						}
						//Not matrix, no need to finalize
						//if (mypid != root)
						//	matspa.finalizeRemoteCopyAtDest();
					}
				}
			}
			//srcspa.finalizeRemoteCopyAtSource();
		}
	}

	/**
	 * Send the double-precision data array from here to a list of places by using
	 * x10 remote array copy.
	 *
	 * @param  dmlist 		distributed storage for copies of array 
	 * @param  datCnt 		counts of data to send
	 * @param  plist 		the list of place IDs.
	 */
	protected static def x10Rcast(
			dmlist:DistDataArray, 
			datCnt:Int, 
			plist:Array[Int](1)):void {
		
		//Check place list 
		if (plist.size == 0 || datCnt<=0) return;
		val root   = here.id();
		val srcden = dmlist(root);	

		val rmtbuf = new RemoteArray[Double](srcden as Array[Double]{self!=null});
		val nplist = new Array[Int](plist.size-1, (i:Int)=>plist(i+1));

		val nxtpid = plist(0);
		at (dmlist.dist(nxtpid)) {
			//Implicit capture: rmtbuf, dmlist, datasz, nplist, root
			copyToHere(rmtbuf, dmlist, datCnt, nplist, root);
		}
	}

	/**
	 *
	 */
	private static def copyToHere(
			srcbuf:RemoteArray[Double],
			dmlist:DistDataArray,
			datCnt:Int,
			plist:Array[Int](1),
			root:Int): void {
		
		val mypid  = here.id();
		val rcvden = dmlist(mypid);

		//Copy data from source place
		if (mypid != root) {
			//Debug.flushln("Copy data to here at Place "+mypid);
			finish Array.asyncCopy[Double](srcbuf, 0, rcvden, 0, datCnt);
		}
		//rcvden.print("Matrix data at "+mypid+" plist:"+plist.toString());
		
		//Goto next place in the list
		if (plist.size >= 1) {
			val nxtpid = plist(0); // Get next place id in the list
			val rmtbuf = new RemoteArray[Double](rcvden as Array[Double]{self!=null});
			val nplist = new Array[Int](plist.size-1, (i:Int)=>plist(i+1));
			at (dmlist.dist(nxtpid)) {
				//Need: rmtbuf, dmlist, colOff, offset, datasz, nplist, root
				copyToHere(rmtbuf, dmlist, datCnt, nplist, root);
			}
		}
	}


	/**
	 * Sending the data from here to a list places by using x10 remote copy
	 *
	 * @param  smlist 		distributed storage for copies of data array
	 * @param  datCnt 		counts of data elements to send
	 * @param  plist  		the list of place IDs.
	 */
	protected static def x10Rcast(
			smlist:DistCompArray, 
			datCnt:Int, 
			plist:Array[Int](1)):void {
		
		//Check place list 
		if (plist.size == 0 || datCnt<=0) return;
		val root   = here.id();
		val srcspa = smlist(root);	

		val rmtidx = new RemoteArray[Int   ](srcspa.index as Array[Int]{self!=null});
		val rmtval = new RemoteArray[Double](srcspa.value as Array[Double]{self!=null});
		val nplist = new Array[Int](plist.size-1, (i:Int)=>plist(i+1));

		val nxtpid = plist(0);
		
		//srcspa.initRemoteCopyAtSource(colOff, colCnt);
		at (smlist.dist(nxtpid)) {
			//Need: rmtidx, rmtval, dmlist, colOff, offset, cnlCnt, datasz, nplist, root
			copyToHere(rmtidx, rmtval, smlist, datCnt, nplist, root);
		}
		//srcspa.finalizeRemoteCopyAtSource();
	}

	/**
	 *
	 */
	private static def copyToHere(
			rmtIndex:RemoteArray[Int], 
			rmtValue:RemoteArray[Double],
			smlist:DistCompArray,
			datCnt:Int,
			plist:Array[Int](1),
			root:Int): void {
		
		val mypid  = here.id();
		val rcvspa = smlist(mypid);

		//Copy data from source place
		if (mypid != root) {
			//++++++++++++++++++++++++++++++++++++++++++++
			//If receive side does not have enough space, program will crush
			//+++++++++++++++++++++++++++++++++++++++++++++
			//rcvspa.initRemoteCopyAtDest(colOff, colCnt, datasz);
			finish Array.asyncCopy[Int   ](rmtIndex, 0, rcvspa.index, 0, datCnt);
			finish Array.asyncCopy[Double](rmtValue, 0, rcvspa.value, 0, datCnt);
		}

		//Goto next place in the list
		if (plist.size >= 1) {
			val nxtpid = plist(0); // Get next place id in the list
			val rmtidx = new RemoteArray[Int   ](rcvspa.index as Array[Int]{self!=null});
			val rmtval = new RemoteArray[Double](rcvspa.value as Array[Double]{self!=null});
			val nplist = new Array[Int](plist.size-1, (i:Int)=>plist(i+1));
			at (smlist.dist(nxtpid)) {
				//Need: rmtidx, rmtval, dmlist, colOff, offset, datasz, nplist, root
				copyToHere(rmtidx, rmtval, smlist, datCnt, nplist, root);
			}
		}
		//Not matrix, no need to finalize
		//if (mypid != root)
		//	rcvspa.finalizeRemoteCopyAtDest();
	}

}