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
import x10.util.Pair;

import x10.compiler.Ifdef;
import x10.compiler.Ifndef;
import x10.compiler.Uninitialized;

import x10.matrix.sparse.CompressArray;
import x10.matrix.Debug;





/**
 * This class supports inter-place communication for data arrays which are defined
 * in PlaceLocalHandle. 
 * 
 * <p>To enable MPI communication, add "-define MPI_COMMU -cxx-prearg -DMPI_COMMU"
 * in x10c++ build command, when you include commu package in your application source
 * code, or link to the proper GML library (native_mpi version).
 * 
 * <p>For more information on how to build different backends and runtime, 
 * run command "make help" at the root directory of GML library.
 */
public class ArrayRemoteCopy {

	private static val baseTagCopyTo:Int=100000;
	private static val baseTagCopyFrom:Int=200000;
	private static val baseTagCopyIdxTo:Int=300000;
	private static val baseTagCopyIdxFrom:Int=400000;
	private static val baseTagCopyValTo:Int=500000;
	private static val baseTagCopyValFrom:Int=600000;
	private static val baseTagCopyToPLH:Int=700000;
	private static val baseTagCopyFromPLH:Int=800000;
	
	//public static val uniqdist:Dist(1) = Dist.makeUnique();
			
	// Constructor
	public def this() {

	}

	//=============================================================
	// PlaceLocalHandle data access
	//=============================================================
	//=============================================================	
	/**
	 * Copy multiple columns from source array data at here to the specified remote 
	 * place in DistArray of dense matrix.
	 * 
	 * @param src      -- the source array
	 * @param srcOff   -- starting column in in source matrix
	 * @param dstplh   -- target in PlaceLocalHandle 
	 * @param dstpid   -- the place id of target in DistArray.
	 * @param dstOff   -- starting column offset in target matrix at destination place
	 * @param dataCnt  -- count of columns to be copied from source dense matrix
	 */
	public static def copy(
			src:Array[Double](1), srcOff:Int, 
			dstplh:DataArrayPLH, dstpid:Int, dstOff:Int, 
			dataCnt:Int) :void  {
		
		if (here.id() == dstpid) {
			Array.copy(src, srcOff, dstplh(), dstOff, dataCnt);
			return;
		}
		Debug.assure(srcOff+dataCnt <= src.size,
				"At source place, illegal data offset:"+srcOff+
				                                       " or data count:"+dataCnt);
		
		@Ifdef("MPI_COMMU") {
			{
				mpiCopy(src, srcOff, dstplh, dstpid, dstOff, dataCnt);
			}
		}
		@Ifndef("MPI_COMMU") {
			{
				x10Copy(src, srcOff, dstplh, dstpid, dstOff, dataCnt);
			}
		}
	}
		
	//--------------------------------
	// CopyTo PlaceLocalHandle access
	//--------------------------------
	protected static def mpiCopy(
			src:Array[Double](1), srcOff:Int, 
			dstplh:DataArrayPLH, dstpid:Int, dstOff:Int, 
			dataCnt:Int) :void  {
		
	@Ifdef("MPI_COMMU") {
		val srcpid = here.id();
		finish {
			val tag = baseTagCopyToPLH+(srcpid+1)*(dstpid+1)+srcpid;
			async {
				WrapMPI.world.send(src, srcOff, dataCnt, dstpid, tag);
			}		
			async {
				at (new Place(dstpid)) {
					val rcvbuf = dstplh();
					WrapMPI.world.recv(rcvbuf, dstOff, dataCnt, srcpid, tag);
				}
			}
		}	
	}
	}
	
	//------------------------------------------
	protected static def x10Copy(
			src:Array[Double](1), srcOff:Int, 
			dstplh:DataArrayPLH, dstpid:Int, dstOff:Int, 
			dataCnt:Int) :void  {

		val srcpid = here.id();
		val dstbuf = at (new Place(dstpid)) 
			new RemoteArray[Double](dstplh() as Array[Double](1){self!=null});
		finish {
			Array.asyncCopy[Double](src, srcOff, dstbuf, dstOff, dataCnt);
		}
	}
	
	//TODO: Check this code. Introduced to support a method in DistArrayScatter.
	// unsure of how to get an Array[Double] at remote place from a 
	// DistDataArray.
	protected static def x10Copy(
			src:Array[Double](1), srcOff:Int, 
			dst:DistDataArray, dstpid:Int, dstOff:Int, 
			dataCnt:Int) :void  {

		val srcpid = here.id();
		val dstbuf = at (new Place(dstpid)) 
		new RemoteArray[Double](dst.getLocalPortion()(0) as Array[Double]{self!=null});
		finish {
			Array.asyncCopy[Double](src, srcOff, dstbuf, dstOff, dataCnt);
		}
	}

	//--------------------------------
	// CopyFrom PlaceLocalHandle access
	//--------------------------------	
	
	/**
	 * Copy data array from in the specified place at 
	 * here to target
	 * 
	 * @param srcplh   -- source data array in PlaceLocalHandle
	 * @param srcpid    -- source array's place id.
	 * @param srcOff    -- starting offset in source matrix
	 * @param dst       -- destination vector array
	 * @param dstOff    -- starting offset in receiving array at here
	 * @param dataCnt   -- count of data to copy
	 */
	public static def copy(
			srcplh:DataArrayPLH, srcpid:Int, srcOff:Int,
			dst:Array[Double](1), dstOff:Int, 
			dataCnt:Int): void {
		
		if (here.id() == srcpid) {
			val src = srcplh();
			Array.copy(src, srcOff, dst, dstOff, dataCnt);
			return;
		}
		Debug.assure(dstOff+dataCnt <= dst.size, "Receiving array overflow");
		
		@Ifdef("MPI_COMMU") {
			{
				mpiCopy(srcplh, srcpid, srcOff, dst, dstOff, dataCnt);
			}
		}

		@Ifndef("MPI_COMMU") {
			{
				x10Copy(srcplh, srcpid, srcOff, dst, dstOff, dataCnt);
			}
		}
	}	
	
	protected static def mpiCopy(
			srcplh:DataArrayPLH, srcpid:Int, srcOff:Int,
			dst:Array[Double](1), dstOff:Int, 
			dataCnt:Int): void {
		
	@Ifdef("MPI_COMMU") {
		val dstpid = here.id();
		finish {
			val tag = baseTagCopyFromPLH+(srcpid+1)*(dstpid+1)+srcpid;
			async {
				at (new Place(srcpid)) {
					val src = srcplh();
					WrapMPI.world.send(src, srcOff, dataCnt, dstpid, tag);
				}
			}
			async {
				WrapMPI.world.recv(dst, dstOff, dataCnt, srcpid, tag);
			}
		}
	}
	}	

	protected static def x10Copy(
			srcplh:DataArrayPLH, srcpid:Int, srcOff:Int,
			dst:Array[Double](1), dstOff:Int, 
			dataCnt:Int): void {
		
		val dstpid = here.id();
		val srcbuf = at (new Place(srcpid))
			new RemoteArray[Double](srcplh() as Array[Double](1){self!=null});
		finish {
			Array.asyncCopy[Double](srcbuf, srcOff, dst, dstOff, dataCnt);
		}
	}
	
	//=============================================================
	// PlaceLocalHandle compress data access
	//=============================================================		
	/**
	 * Copy compress array data from here to the specified remote 
	 * place in DistArray.
	 * 
	 * @param src    -- source of compress array at here
	 * @param srcOff -- offset in source 
	 * @param dstplh -- target compress array in DistArray
	 * @param dstpid -- place id of target in DistArray.
	 * @param dstOff -- column offset in target 
	 * @param dataCnt -- count of data to copy from source 
	 */
	public static def copy(
			src:CompressArray, srcOff:Int,
			dstplh:CompArrayPLH, dstpid:Int, dstOff:Int, 
			dataCnt:Int): void {

		if (here.id() == dstpid) {
			val dst = dstplh();
			CompressArray.copy(src, srcOff, dst, dstOff, dataCnt);
			return;
		}

		Debug.assure(srcOff+dataCnt <= src.storageSize(), "Sending side storage overlfow");
		
		@Ifdef("MPI_COMMU") {
			{
				mpiCopy(src, srcOff, dstplh, dstpid, dstOff, dataCnt);
			}
		}
		@Ifndef("MPI_COMMU") {
			{
				x10Copy(src, srcOff, dstplh, dstpid, dstOff, dataCnt);
			}
		}
	}


	/**
	 * Based on mpi send/recv, copy data from here to remote place
	 */
	protected static def mpiCopy(
			src:CompressArray, srcOff:Int,
			dstplh:CompArrayPLH, dstpid:Int, dstOff:Int, 
			dataCnt:Int): void {
		
	@Ifdef("MPI_COMMU") {
		val srcpid = here.id();        
		finish {
			async {
				val tag    = baseTagCopyIdxTo + dstpid;
				WrapMPI.world.send(src.index, srcOff, dataCnt, dstpid, tag);
				WrapMPI.world.send(src.value, srcOff, dataCnt, dstpid, tag+100001);
			}

			at (new Place(dstpid)) async {
				// Need: dstlist, srcpid, dstOff, dataCnt;
				val dst = dstplh();
				val tag = srcpid * baseTagCopyIdxTo + here.id();
				Debug.assure(dstOff+dataCnt<=dst.storageSize(), "Receiving side arrays overflow");
				WrapMPI.world.recv(dst.index, dstOff, dataCnt, srcpid, tag);
				WrapMPI.world.recv(dst.value, dstOff, dataCnt, srcpid, tag+100001);
			}
		}
	}
	}

	//Sparse matrix remote copy To
	protected static def x10Copy(
			src:CompressArray, srcOff:Int,
			dstplh:CompArrayPLH, dstpid:Int, dstOff:Int, 
			dataCnt:Int): void {

		val idxbuf = src.index as Array[Int]{self!=null};
		val valbuf = src.value as Array[Double]{self!=null};
		val rmtidx = new RemoteArray[Int](idxbuf);
		val rmtval = new RemoteArray[Double](valbuf);

		at (new Place(dstpid)) {
			//Implicit copy:dstlist, dataCnt, rmtidx, rmtval, srcOff dstOff
			val dst = dstplh();
			Debug.assure(dstOff+dataCnt<=dst.storageSize(), "Receiving side arrays overflow");
			finish Array.asyncCopy[Int   ](rmtidx, srcOff, dst.index, dstOff, dataCnt);
			finish Array.asyncCopy[Double](rmtval, srcOff, dst.value, dstOff, dataCnt);
		}
	}

	//--------------------------------------------------
	// Remote compress array copy From
	//--------------------------------------------------
	/**
	 * Copy data of compress array from remote place to here
	 * 
	 * @param dstplh  -- source compress array in DistArray
	 * @param srcpid  -- source place id.
	 * @param srcOff  -- offset in source
	 * @param dstspa  -- destination place id
	 * @param dstOff  -- offset in target
	 * @param dataCnt  -- data count to be copied in source matrix
	 */
	public static def copy(
			srcplh:CompArrayPLH, srcpid:Int, srcOff:Int,
			dst:CompressArray, dstOff:Int, 
			dataCnt:Int): void {
		
		if (here.id() == srcpid) {
			CompressArray.copy(srcplh(), srcOff, dst, dstOff, dataCnt);
			return;
		}

		@Ifdef("MPI_COMMU") {
			{
				mpiCopy(srcplh, srcpid, srcOff, dst, dstOff, dataCnt);
			}
		}

		@Ifndef("MPI_COMMU") {
			{
				x10Copy(srcplh, srcpid, srcOff, dst, dstOff, dataCnt);
			}
		}
	}

	//Remote sparse matrix copy from
	protected static def mpiCopy(
			srcplh:CompArrayPLH, srcpid:Int, srcOff:Int,
			dst:CompressArray, dstOff:Int, 
			dataCnt:Int): void {
		
	@Ifdef("MPI_COMMU") {

		val dstbid = here.id();
		finish {
			at (new Place(srcpid)) async {
				//Need: dstpid, srclist, srcOff, dataCnt
				val src = srcplh();
				val tag    = here.id() * baseTagCopyIdxFrom + dstbid;
				WrapMPI.world.send(src.index, srcOff, dataCnt, dstbid, tag);
				WrapMPI.world.send(src.value, srcOff, dataCnt, dstbid, tag+1000);
			}
			
			async {
				val tag = srcpid * baseTagCopyIdxFrom + dstbid;

				WrapMPI.world.recv(dst.index, dstOff, dataCnt, srcpid, tag);
				WrapMPI.world.recv(dst.value, dstOff, dataCnt, srcpid, tag+1000);
			}
		}
	}
	}
	
	//Sparse matrix remote copyt from
	protected static def x10Copy(
			srcplh:CompArrayPLH, srcpid:Int, srcOff:Int,
			dst:CompressArray, dstOff:Int, 
			dataCnt:Int): void {

		val rmt:RemotePair = at (new Place(srcpid)) { 
			//Need: srclist
			val src = srcplh();
			val idxbuf = src.index as Array[Int]{self!=null};
			val valbuf = src.value as Array[Double]{self!=null};
			val rmtidx = new RemoteArray[Int](idxbuf);
			val rmtval = new RemoteArray[Double](valbuf);
			RemotePair(rmtidx, rmtval)
		};

		finish Array.asyncCopy[Int   ](rmt.first,  srcOff, dst.index, dstOff, dataCnt);
		finish Array.asyncCopy[Double](rmt.second, dstOff, dst.value, dstOff, dataCnt);
	}
	
}