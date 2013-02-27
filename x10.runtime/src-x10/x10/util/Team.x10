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

package x10.util;

import x10.compiler.Native;
import x10.compiler.NativeRep;
import x10.compiler.StackAllocate;

import x10.lang.Zero;

import x10.util.concurrent.AtomicInteger;

/** Interface to low level collective operations.  A team is a collection of
 * activities that work together by simultaneously doing 'collective
 * operations', expressed as calls to methods in the Team struct.  Each member
 * of the team identifies itself using the 'role' integer, which is a value
 * from 0 to team.size() - 1.  Each member can only live at a particular place,
 * which is indicated by the user when the Team is created.
 */
public struct Team {

    private static struct DoubleIdx(value:Double, idx:Int) {}

    /** A team that has one member at each place.
     */
    public static WORLD = Team(0, new Array[Place](PlaceGroup.WORLD.numPlaces(), (i:Int)=>PlaceGroup.WORLD(i)));

    /** The underlying representation of a team's identity.
     */
    private id: Int;
    private places: Array[Place](1);

    /** Returns the id of the team.
     */
    public def id() = id;

    /** Returns the places of the team.
     */
    public def places() = places;

    /** Returns the PlaceGroup of the places of the team.
     */
    public def placeGroup() : PlaceGroup = {
        val ps = new Array[Place](places);
        return new OrderedPlaceGroup(ps.sequence());
    }

    /** Returns the place corresponding to the given role.
     * @param role Our role in this team
     */
    public def getPlace(role:Int) : Place = places(role);

    /** Returns the role corresponding to the given place.
     * @param place Place in this team
     */
    public def getRole(place:Place) : Int = {
    	var role:Int = -1;
    	for ([p] in places) {
    		if (places(p) == place)
    			role = p;
    	}
    	return role;
    }
    
    private def this (id:Int, places:Array[Place](1)) { this.id = id; this.places = places; }

    /** Create a team by defining the place where each member lives.  This would usually be called before creating an async for each member of the team.
     * @param places The place of each member
     */
    public def this (places:Array[Place]) {
        this(places.raw(), places.size);
    }

    private def this (places:IndexedMemoryChunk[Place], count:Int) {
        val result = IndexedMemoryChunk.allocateUninitialized[Int](1);
        finish nativeMake(places, count, result);
        this.id = result(0);
        this.places =new Array[Place](places);
    }

    private static def nativeMake (places:IndexedMemoryChunk[Place], count:Int, result:IndexedMemoryChunk[Int]) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeMake(places, count, result);")
    	@Native("c++", "x10rt_team_new(count, (x10rt_place*)places->raw(), x10aux::coll_handler2, x10aux::coll_enter2(result->raw()));") {}
    }

    /** Returns the number of elements in the team.
     */
    public def size () : Int = nativeSize(id);

    private static def nativeSize (id:Int) : Int {
        @Native("java", "return x10.x10rt.TeamSupport.nativeSize(id);")
        @Native("c++", "return (x10_int)x10rt_team_sz(id);") { return -1; }
    }

    /** Blocks until all team members have reached the barrier.
     * @param role Our role in this collective operation
     */
    public def barrier (role:Int) : void {
        finish nativeBarrier(id, role);
    }

    private static def nativeBarrier (id:int, role:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeBarrier(id, role);")
        @Native("c++", "x10rt_barrier(id, role, x10aux::coll_handler, x10aux::coll_enter());") {}
    }
    
    /** Blocks until all members have received their part of root's array.
     * Each member receives a contiguous and distinct portion of the src array.
     * src should be structured so that the portions are sorted in ascending
     * order, e.g., the first member gets the portion at offset src_off of sbuf, and the
     * last member gets the last portion.
     *
     * @param role Our role in the team
     *
     * @param root The member who is supplying the data
     *
     * @param src The data that will be sent (will only be used by the root
     * member)
     *
     * @param src_off The offset into src at which to start reading
     *
     * @param dst The rail into which the data will be received for this member
     *
     * @param dst_off The offset into dst at which to start writing
     *
     * @param count The number of elements being transferred
     */
    public def scatter[T] (role:Int, root:Int, src:Array[T], src_off:Int, dst:Array[T], dst_off:Int, count:Int) : void {
        finish nativeScatter(id, role, root, src.raw(), src_off, dst.raw(), dst_off, count);
    }

    private static def nativeScatter[T] (id:Int, role:Int, root:Int, src:IndexedMemoryChunk[T], src_off:Int, dst:IndexedMemoryChunk[T], dst_off:Int, count:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeScatter(id, role, root, src, src_off, dst, dst_off, count);")
        @Native("c++", "x10rt_scatter(id, role, root, &src->raw()[src_off], &dst->raw()[dst_off], sizeof(TPMGL(T)), count, x10aux::coll_handler, x10aux::coll_enter());") {}
    }
    
    private static def dummyChunk[T]() { return IndexedMemoryChunk.allocateUninitialized[T](0); }; 

    private static def getRawOrDummyChunk[T](arr: Array[T]) {
        if (arr == null)
            return dummyChunk[T]();
        else
            return arr.raw();
    };
    
    /** Scatters the given array, called by the root.  Blocks until all members have received their part of root's array.
     * Each member receives a contiguous and distinct portion of the src array.
     * src should be structured so that the portions are sorted in ascending
     * order, e.g., the first member gets the portion at offset src_off of sbuf, and the
     * last member gets the last portion.  Note that the size of src is equal to n * count,
     * where n is the number of members of the team.
     *
     * @see #scatter
     * @see #scatterRecv
     *
     * @param role Our role in the team
     *
     * @param root The member who is supplying the data
     *
     * @param src The data that will be sent 
     *
     * @param count The number of elements being transferred
     *
     * @return received array
     *
     */
    public def scatter[T] (role:Int, root:Int, src:Array[T], count:Int) {
        val dst_raw = IndexedMemoryChunk.allocateUninitialized[T](count);
        finish nativeScatter(id, role, root, getRawOrDummyChunk(src), 0, dst_raw, 0, count);
        return new Array[T](dst_raw);
    }

    /**
     * @deprecated use {@link #scatter(Int, Int, Array[T], Int)} instead
     */
    public def scatterSend[T] (role:Int, root:Int, src:Array[T], count:Int) {
    	val dst_raw = IndexedMemoryChunk.allocateUninitialized[T](count);
        finish nativeScatter(id, role, root, src.raw(), 0, dst_raw, 0, count);
        return new Array[T](dst_raw);
    }

    /**
     * @deprecated use {@link #scatter(Int, Int, Array[T], Int)} instead
     */
    public def scatterRecv[T] (role:Int, root:Int, count:Int) {
    	val dst_raw = IndexedMemoryChunk.allocateUninitialized[T](count);
        finish nativeScatter(id, role, root, dummyChunk[T](), 0, dst_raw, 0, count);
        return new Array[T](dst_raw);
    }


    /** Almost same as scatter except for permitting messages to have different sizes.
     *
     * @see #scatter

     * @param role Our role in the team
     *
     * @param root The member who is supplying the data
     *
     * @param src The data that will be sent (will only be used by the root
     * member)
     *
     * @param src_offs The offsets into src at which to start reading
     *
     * @param src_counts The numbers of elements being sent
     * 
     * @param dst The rail into which the data will be received for this member
     *
     * @param dst_off The offset into dst at which to start writing
     *
     * @param dst_count The numbers of elements being received
     */
    public def scatterv[T] (role:Int, root:Int, src:Array[T], src_offs:Array[Int], src_counts:Array[Int], dst:Array[T], dst_off:Int, dst_count:Int) : void {
        finish nativeScatterv(id, role, root, src.raw(), src_offs.raw(), src_counts.raw(), dst.raw(), dst_off, dst_count);
    }

    private static def nativeScatterv[T] (id:Int, role:Int, root:Int, src:IndexedMemoryChunk[T], src_offs:IndexedMemoryChunk[Int], src_counts:IndexedMemoryChunk[Int], dst:IndexedMemoryChunk[T], dst_off:Int, dst_count:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeScatterV(id, role, root, src, src_offs, src_counts, dst, dst_off, dst_count);")
        @Native("c++", "x10rt_scatterv(id, role, root, src->raw(), src_offs->raw(), src_counts->raw(), &dst->raw()[dst_off], dst_count, sizeof(TPMGL(T)), x10aux::coll_handler, x10aux::coll_enter());") {}
    }
    
    /** Almost same as scatter except for permitting messages to have different sizes.
     * The received array is structured so that the portions are sorted in ascending
     * order, e.g., the first member gets the portion at the head of sbuf, and the
     * last member gets the last portion.
     *
     * @see #scatterv
     * @see #scattervRecv
     *
     * @param role Our role in the team
     *
     * @param root The member who is supplying the data
     *
     * @param src The data that will be sent 
     *
     * @param src_offs The offsets into src at which to start reading
     *
     * @param src_counts The numbers of elements being sent
     *
     * @param dst_count The numbers of elements being received
     *
     * @return received array
     */
    public def scatterv[T] (role:Int, root:Int, src:Array[T], src_counts:Array[Int], src_offs:Array[Int], dst_count:Int) {
        assert(role != root || src_counts.size == size());
        assert(role != root || src_offs.size == size());
        val dst_raw : IndexedMemoryChunk[T] = IndexedMemoryChunk.allocateUninitialized[T](dst_count);
        finish nativeScatterv(id, role, root, getRawOrDummyChunk(src), getRawOrDummyChunk(src_offs), getRawOrDummyChunk(src_counts), dst_raw, 0, dst_count);
        return new Array[T](dst_raw);
    }

    /**
     * @deprecated use {@link #scatterv(Int, Int, Array[T], Array[Int], Array[Int], Int)} instead
     */
    public def scattervSend[T] (role:Int, root:Int, src:Array[T], src_counts:Array[Int], src_offs:Array[Int], dst_count:Int) {
    	assert(src_counts.size == size());
    	assert(src_offs.size == size());
    	val dst_raw : IndexedMemoryChunk[T] = IndexedMemoryChunk.allocateUninitialized[T](dst_count);
    	finish nativeScatterv(id, role, root, src.raw(), src_offs.raw(), src_counts.raw(), dst_raw, 0, dst_count);
    	return new Array[T](dst_raw);
    }

    /**
     * @deprecated use {@link #scatterv(Int, Int, Array[T], Array[Int], Array[Int], Int)} instead
     */
    public def scattervRecv[T] (role:Int, root:Int, dst_count:Int) {
    	val dst_raw = IndexedMemoryChunk.allocateUninitialized[T](dst_count);
    	finish nativeScatterv(id, role, root, dummyChunk[T](), dummyChunk[Int](), dummyChunk[Int](), dst_raw, 0, dst_count);
    	return new Array[T](dst_raw);
    }
    
    /** Blocks until the root have received each part of all member's array.
     * Each member sends a contiguous and distinct portion of the src array.
     * dst will be structured so that the portions are sorted in ascending
     * order, e.g., the first member gets the portion at offset src_off of sbuf, and the
     * last member gets the last portion.
     *
     * @param role Our role in the team
     *
     * @param root The member who is receiving the data
     *
     * @param src The data that will be sent 
     *
     * @param src_off The offset into src at which to start reading
     *
     * @param dst The rail into which the data will be received (will only be used by the root
     * member)
     *
     * @param dst_off The offset into dst at which to start writing (will only be used by the root
     * member)
     *
     * @param count The number of elements being transferred
     */
    public def gather[T] (role:Int, root:Int, src:Array[T], src_off:Int, dst:Array[T], dst_off:Int, count:Int) : void {
        finish nativeGather(id, role, root, src.raw(), src_off, dst.raw(), dst_off, count);
    }

    private static def nativeGather[T] (id:Int, role:Int, root:Int, src:IndexedMemoryChunk[T], src_off:Int, dst:IndexedMemoryChunk[T], dst_off:Int, count:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeGather(id, role, root, src, src_off, dst, dst_off, count);")
        @Native("c++", "x10rt_gather(id, role, root, &src->raw()[src_off], &dst->raw()[dst_off], sizeof(TPMGL(T)), count, x10aux::coll_handler, x10aux::coll_enter());") {}
    }

    /** Gathers the given array.
     * Blocks until the root have received each part of all member's array.
     * Each member sends a contiguous and distinct portion of the src array.
     * dst will be structured so that the portions are sorted in ascending
     * order, e.g., the first member gets the portion at offset src_off of sbuf, and the
     * last member gets the last portion.
     *
     * @see #gather
     * @see #gatherRecv
     *
     * @param role Our role in the team
     *
     * @param root The member who is receiving the data
     *
     * @param src The data that will be sent 
     *
     * @param count The number of elements being transferred
     *
     * @return received array
     */
    public def gather[T] (role:Int, root:Int, src:Array[T], count:Int) {
    	val dst_raw = role == root ? IndexedMemoryChunk.allocateUninitialized[T](count) : dummyChunk[T]();
        finish nativeGather(id, role, root, src.raw(), 0, dst_raw, 0, count);
        return new Array[T](dst_raw);
    }

    /**
     * @deprecated use {@link #gather(Int, Int, Array[T], Int)} instead
     */
    public def gatherSend[T] (role:Int, root:Int, src:Array[T], count:Int) : void {
        finish nativeGather(id, role, root, src.raw(), 0, dummyChunk[T](), 0, count);
    }

    /**
     * @deprecated use {@link #gather(Int, Int, Array[T], Int)} instead
     */
    public def gatherRecv[T] (role:Int, root:Int, src:Array[T], count:Int) {
    	val dst_raw = IndexedMemoryChunk.allocateUninitialized[T](count);
        finish nativeGather(id, role, root, src.raw(), 0, dst_raw, 0, count);
        return new Array[T](dst_raw);
    }

    /** Almost same as gather except that each member sends one data.
     *
     * @see #gatherSend
     * @see #gatherRecv1
     *
     * @param role Our role in the team
     *
     * @param root The member who is receiving the data
     *
     * @param src The data that will be sent 
     *
     * @param count The number of elements being transferred
     */
    public def gather1[T] (role:Int, root:Int, src:T) {T haszero} : Array[T](1) {
    	val src_raw = IndexedMemoryChunk.allocateUninitialized[T](1);
    	src_raw(0) = src;
    	val dst_raw = role == root ? IndexedMemoryChunk.allocateUninitialized[T](size()) : dummyChunk[T]();
        finish nativeGather(id, role, root, src_raw, 0, dst_raw, 0, 1);
        return (role == root ? new Array[T](dst_raw) : null) as Array[T](1) ;
    }

    /**
     * @deprecated use {@link #gather1(Int, Int, T)} instead
     */
    public def gatherSend1[T] (role:Int, root:Int, src:T) : void {
    	val src_raw = IndexedMemoryChunk.allocateUninitialized[T](1);
    	src_raw(0) = src;
        finish nativeGather(id, role, root, src_raw, 0, dummyChunk[T](), 0, 1);
    }

    /**
     * @deprecated use {@link #gather1(Int, Int, T)} instead
     */
    public def gatherRecv1[T] (role:Int, root:Int, src:T) {
    	val src_raw = IndexedMemoryChunk.allocateUninitialized[T](1);
    	src_raw(0) = src;
    	val dst_raw = IndexedMemoryChunk.allocateUninitialized[T](size());
        finish nativeGather(id, role, root, src_raw, 0, dst_raw, 0, 1);
        return new Array[T](dst_raw);
    }

    /** Almost same as gather except for permitting messages to have different sizes.
     *
     * @param role Our role in the team
     *
     * @param root The member who is receiving the data
     *
     * @param src The data that will be sent 
     *
     * @param src_off The offset into src at which to start reading
     *
     * @param src_count The numbers of elements being sent
     * 
     * @param dst The rail into which the data will be received (will only be used by the root
     * member)
     *
     * @param dst_offs The offsets into dst at which to start writing (will only be used by the root
     * member)
     *
     * @param dst_counts The numbers of elements being transferred
     */
    public def gatherv[T] (role:Int, root:Int, src:Array[T], src_off:Int, src_count:Int, dst:Array[T], dst_offs:Array[Int], dst_counts:Array[Int]) : void {
        finish nativeGatherv(id, role, root, src.raw(), src_off, src_count, dst.raw(), dst_offs.raw(), dst_counts.raw());
    }

    private static def nativeGatherv[T] (id:Int, role:Int, root:Int, src:IndexedMemoryChunk[T], src_off:Int, src_count:Int, dst:IndexedMemoryChunk[T], dst_offs:IndexedMemoryChunk[Int], dst_counts:IndexedMemoryChunk[Int]) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeGatherV(id, role, root, src, src_off, src_count, dst, dst_offs, dst_counts);")
        @Native("c++", "x10rt_gatherv(id, role, root, &src->raw()[src_off], src_count, dst->raw(), dst_offs->raw(), dst_counts->raw(), sizeof(TPMGL(T)), x10aux::coll_handler, x10aux::coll_enter());") {}
    }

    public def gatherv[T] (role:Int, root:Int, src:Array[T], dst_offs:Array[Int], dst_counts:Array[Int] ) {
    	val dst_raw = role == root ? IndexedMemoryChunk.allocateUninitialized[T](dst_counts.reduce((x:Int, y:Int)=>x+y, 0)) : dummyChunk[T]();
    	finish nativeGatherv(id, role, root, src.raw(), 0, src.size, dst_raw, getRawOrDummyChunk(dst_offs), getRawOrDummyChunk(dst_counts));
        return role == root ? new Array[T](dst_raw) : null;
    }

    /**
     * @deprecated use {@link #gatherv(Int, Int, Array[T], Array[Int], Array[Int])} instead
     */
    public def gathervSend[T] (role:Int, root:Int, src:Array[T]) : void {
    	finish nativeGatherv(id, role, root, src.raw(), 0, src.size, dummyChunk[T](), dummyChunk[Int](), dummyChunk[Int]());
    }
    
    /**
     * @deprecated use {@link #gatherv(Int, Int, Array[T], Array[Int], Array[Int])} instead
     */
    public def gathervRecv[T] (role:Int, root:Int, src:Array[T], dst_offs:Array[Int], dst_counts:Array[Int] ) {
    	val dst_raw = IndexedMemoryChunk.allocateUninitialized[T](dst_counts.reduce((x:Int, y:Int)=>x+y, 0));
    	finish nativeGatherv(id, role, root, src.raw(), 0, src.size, dst_raw, dst_offs.raw(), dst_counts.raw());
        return new Array[T](dst_raw);
    }

    private static def countsToOffs (counts:Array[Int](1)) {
    	val acc = counts.scan((x:Int, y:Int)=> x+y, 0);
    	return new Array[Int](counts.size, (i:Int)=>(i==0) ? 0 : acc(i-1));
    }
    
    public def gatherv[T] (role:Int, root:Int, src:Array[T], dst_counts:Array[Int](1) ) {
        if (role == root) {
            val dst_offs = countsToOffs(dst_counts);
            return gatherv[T](role, root, src, dst_offs, dst_counts);
        } else {
            return gatherv[T](role, root, src, null, null);
        }
    }

    /**
     * @deprecated use {@link #gatherv(Int, Int, Array[T], Array[Int], Array[Int])} instead
     */
    public def gathervRecv[T] (role:Int, root:Int, src:Array[T], dst_counts:Array[Int](1) ) {
    	val dst_offs = countsToOffs(dst_counts);
        return gathervRecv[T](role, root, src, dst_offs, dst_counts);
    }

    /** Blocks until all members have received root's array.
     *
     * @param role Our role in the team
     *
     * @param root The member who is supplying the data
     *
     * @param src The data that will be sent (will only be used by the root member)
     *
     * @param src_off The offset into src at which to start reading
     *
     * @param dst The rail into which the data will be received for this member
     *
     * @param dst_off The offset into dst at which to start writing
     *
     * @param count The number of elements being transferred
     */
    public def bcast[T] (role:Int, root:Int, src:Array[T], src_off:Int, dst:Array[T], dst_off:Int, count:Int) : void {
        finish nativeBcast(id, role, root, src.raw(), src_off, dst.raw(), dst_off, count);
    }

    private static def nativeBcast[T] (id:Int, role:Int, root:Int, src:IndexedMemoryChunk[T], src_off:Int, dst:IndexedMemoryChunk[T], dst_off:Int, count:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeBcast(id, role, root, src, src_off, dst, dst_off, count);")
        @Native("c++", "x10rt_bcast(id, role, root, &src->raw()[src_off], &dst->raw()[dst_off], sizeof(TPMGL(T)), count, x10aux::coll_handler, x10aux::coll_enter());") {}
    }
    
    public def bcast1[T] (role:Int, root:Int, src:T) : T {
    	val src_raw = IndexedMemoryChunk.allocateUninitialized[T](1);
    	src_raw(0) = src;
    	val dst_raw = IndexedMemoryChunk.allocateUninitialized[T](1);
        finish nativeBcast(id, role, root, src_raw, 0, dst_raw, 0, 1);
        return dst_raw(0);
    }

    /**
     * @deprecated use {@link #bcast1(Int, Int, T)} instead
     */
    public def bcastSend1[T] (role:Int, root:Int, src:T) : T {
    	val src_raw = IndexedMemoryChunk.allocateUninitialized[T](1);
    	src_raw(0) = src;
    	val dst_raw = IndexedMemoryChunk.allocateUninitialized[T](1);
        finish nativeBcast(id, role, root, src_raw, 0, dst_raw, 0, 1);
        return dst_raw(0);
    }

    /**
     * @deprecated use {@link #bcast1(Int, Int, T)} instead
     */
    public def bcastRecv1[T] (role:Int, root:Int) : T {
    	val dst_raw = IndexedMemoryChunk.allocateUninitialized[T](1);
        finish nativeBcast(id, role, root, dummyChunk[T](), 0, dst_raw, 0, 1);
        return dst_raw(0);
    }

    public def bcast[T] (role:Int, root:Int, src:Array[T], count:Int) {
    	val dst_raw = IndexedMemoryChunk.allocateUninitialized[T](count);
        finish nativeBcast(id, role, root, getRawOrDummyChunk(src), 0, dst_raw, 0, count);
        return new Array[T](dst_raw);
    }

    /**
     * @deprecated use {@link #bcast(Int, Int, Array[T], Int)} instead
     */
    public def bcastSend[T] (role:Int, root:Int, src:Array[T], count:Int) {
    	val dst_raw = IndexedMemoryChunk.allocateUninitialized[T](count);
        finish nativeBcast(id, role, root, src.raw(), 0, dst_raw, 0, count);
        return new Array[T](dst_raw);
    }

    /**
     * @deprecated use {@link #bcast(Int, Int, Array[T], Int)} instead
     */
    public def bcastRecv[T] (role:Int, root:Int, count:Int) {
    	val dst_raw = IndexedMemoryChunk.allocateUninitialized[T](count);
        finish nativeBcast(id, role, root, dummyChunk[T](), 0, dst_raw, 0, count);
        return new Array[T](dst_raw);
    }

    public def allgather1[T] (role:Int, src:T) {
    	val src_raw = IndexedMemoryChunk.allocateUninitialized[T](1);
    	src_raw(0) = src;
    	val dst_raw = IndexedMemoryChunk.allocateUninitialized[T](size());
        finish nativeAllgather(id, role, src_raw, 0, dst_raw, 0, 1);
        return new Array[T](dst_raw);
    }

    public def allgather[T] (role:Int, src:Array[T]) {
    	val dst_raw = IndexedMemoryChunk.allocateUninitialized[T](src.size * size());
        finish nativeAllgather(id, role, src.raw(), 0, dst_raw, 0, src.size);
        return new Array[T](dst_raw);
    }

    public def allgather[T] (role:Int, src:Array[T], src_off:Int, dst:Array[T], dst_off:Int, count:Int) : void {
        finish nativeAllgather(id, role, src.raw(), src_off, dst.raw(), dst_off, count);
    }

    private static def nativeAllgather[T](id:Int, role:Int, src:IndexedMemoryChunk[T], src_off:Int, dst:IndexedMemoryChunk[T], dst_off:Int, count:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeAllGather(id, role, src, src_off, dst, dst_off, count);")
        @Native("c++", "x10rt_allgather(id, role, &src->raw()[src_off], &dst->raw()[dst_off], sizeof(TPMGL(T)), count, x10aux::coll_handler, x10aux::coll_enter());") {}
    }
    
    public def allgatherv[T] (role:Int, src:Array[T], dst_offs:Array[Int], dst_counts:Array[Int]) {
    	val dst_raw = IndexedMemoryChunk.allocateUninitialized[T](dst_counts.reduce((x:Int, y:Int)=>x+y, 0));
        finish nativeAllgatherv(id, role, src.raw(), 0, src.size, dst_raw, dst_offs.raw(), dst_counts.raw());
        return new Array[T](dst_raw);
    }

    public def allgatherv[T] (role:Int, root:Int, src:Array[T], src_off:Int, src_count:Int, dst:Array[T], dst_offs:Array[Int], dst_counts:Array[Int]) : void {
        finish nativeAllgatherv(id, role, src.raw(), src_off, src_count, dst.raw(), dst_offs.raw(), dst_counts.raw());
    }

    private static def nativeAllgatherv[T] (id:Int, role:Int, src:IndexedMemoryChunk[T], src_off:Int, src_count:Int, dst:IndexedMemoryChunk[T], dst_offs:IndexedMemoryChunk[Int], dst_counts:IndexedMemoryChunk[Int]) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeAllGatherV(id, role, src, src_off, src_count, dst, dst_offs, dst_counts);")
        @Native("c++", "x10rt_allgatherv(id, role, &src->raw()[src_off], src_count, dst->raw(), dst_offs->raw(), dst_counts->raw(), sizeof(TPMGL(T)), x10aux::coll_handler, x10aux::coll_enter());") {}
    }

    /** Blocks until all members have received their part of each other member's array.
     * Each member receives a contiguous and distinct portion of the src array.
     * src should be structured so that the portions are sorted in ascending
     * order, e.g., the first member gets the portion at offset src_off of sbuf, and the
     * last member gets the last portion.
     *
     * @param role Our role in the team
     *
     * @param src The data that will be sent (will only be used by the root
     * member)
     *
     * @param src_off The offset into src at which to start reading
     *
     * @param dst The rail into which the data will be received for this member
     *
     * @param dst_off The offset into dst at which to start writing
     *
     * @param count The number of elements being transferred
     */
    public def alltoall[T] (role:Int, src:Array[T], src_off:Int, dst:Array[T], dst_off:Int, count:Int) : void {
        finish nativeAlltoall(id, role, src.raw(), src_off, dst.raw(), dst_off, count);
    }

    private static def nativeAlltoall[T](id:Int, role:Int, src:IndexedMemoryChunk[T], src_off:Int, dst:IndexedMemoryChunk[T], dst_off:Int, count:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeAllToAll(id, role, src, src_off, dst, dst_off, count);")
        @Native("c++", "x10rt_alltoall(id, role, &src->raw()[src_off], &dst->raw()[dst_off], sizeof(TPMGL(T)), count, x10aux::coll_handler, x10aux::coll_enter());") {}
    }

    public def alltoall[T] (role:Int, src:Array[T]) {
    	assert(src.size % size() == 0);
    	val dst_raw = IndexedMemoryChunk.allocateUninitialized[T](src.size);
        finish nativeAlltoall(id, role, src.raw(), 0, dst_raw, 0, src.size / size());
        return new Array[T](dst_raw);
    }
    
    public def alltoallv[T] (role:Int, root:Int, src:Array[T], src_offs:Array[Int], src_counts:Array[Int], dst:Array[T], dst_offs:Array[Int], dst_counts:Array[Int]) : void {
        finish nativeAlltoallv(id, role, src.raw(), src_offs.raw(), src_counts.raw(), dst.raw(), dst_offs.raw(), dst_counts.raw());
    }

    private static def nativeAlltoallv[T] (id:Int, role:Int, src:IndexedMemoryChunk[T], src_offs:IndexedMemoryChunk[Int], src_counts:IndexedMemoryChunk[Int], dst:IndexedMemoryChunk[T], dst_offs:IndexedMemoryChunk[Int], dst_counts:IndexedMemoryChunk[Int]) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeAllToAllV(id, role, src, src_offs, src_counts, dst, dst_offs, dst_counts);")
        @Native("c++", "x10rt_alltoallv(id, role, src->raw(), src_offs->raw(), src_counts->raw(), dst->raw(), dst_offs->raw(), dst_counts->raw(), sizeof(TPMGL(T)), x10aux::coll_handler, x10aux::coll_enter());") {}
    }

    public def alltoallv[T] (role:Int, src:Array[T], src_offs:Array[Int], src_counts:Array[Int], dst_offs:Array[Int], dst_counts:Array[Int]) {
    	assert(src_counts.size == size());
    	assert(src_offs.size == size());
    	assert(dst_counts.size == size());
    	assert(dst_offs.size == size());
    	assert(size() > 0);
    	val dst_raw = IndexedMemoryChunk.allocateUninitialized[T](dst_counts.reduce((x:Int, y:Int)=>x+y, 0));
        finish nativeAlltoallv(id, role, src.raw(), src_offs.raw(), src_counts.raw(), dst_raw, dst_offs.raw(), dst_counts.raw());
        return new Array[T](dst_raw);
    }

    /** Indicates the operation to perform when reducing. */
    public static val ADD = 0;
    /** Indicates the operation to perform when reducing. */
    public static val MUL = 1;
    /** Indicates the operation to perform when reducing. */
    public static val AND = 3;
    /** Indicates the operation to perform when reducing. */
    public static val OR  = 4;
    /** Indicates the operation to perform when reducing. */
    public static val XOR = 5;
    /** Indicates the operation to perform when reducing. */
    public static val MAX = 6;
    /** Indicates the operation to perform when reducing. */
    public static val MIN = 7;

    /* using overloading is the correct thing to do here since the set of supported
     * types are finite, however the java backend will not be able to distinguish
     * these methods' prototypes so we use the unsafe generic approach for now.
     */

    /** Blocks until all members have received the computed result.  Note that not all values of T are valid.
     * The dst array is populated for all members with the result of the operation applied pointwise to all given src arrays.
     *
     * @param role Our role in the team
     *
     * @param src The data that will be sent (will only be used by the root
     * member)
     *
     * @param src_off The offset into src at which to start reading
     *
     * @param dst The rail into which the data will be received for this member
     *
     * @param dst_off The offset into dst at which to start writing
     *
     * @param count The number of elements being transferred
     *
     * @param op The operation to perform
     */
    public def allreduce[T] (role:Int, src:Array[T], src_off:Int, dst:Array[T], dst_off:Int, count:Int, op:Int) : void {
        finish nativeAllreduce(id, role, src.raw(), src_off, dst.raw(), dst_off, count, op);
    }

    private static def nativeAllreduce[T](id:Int, role:Int, src:IndexedMemoryChunk[T], src_off:Int, dst:IndexedMemoryChunk[T], dst_off:Int, count:Int, op:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeAllReduce(id, role, src, src_off, dst, dst_off, count, op);")
    	@Native("c++", "x10rt_allreduce(id, role, &src->raw()[src_off], &dst->raw()[dst_off], (x10rt_red_op_type)op, x10rt_get_red_type<TPMGL(T)>(), count, x10aux::coll_handler, x10aux::coll_enter());") {}
    }

    /** Performs a reduction on a single value, returning the result */
    public def allreduce (role:Int, src:Byte, op:Int) = genericAllreduce(role, src, op);
    /** Performs a reduction on a single value, returning the result */
    public def allreduce (role:Int, src:UByte, op:Int) = genericAllreduce(role, src, op);
    /** Performs a reduction on a single value, returning the result */
    public def allreduce (role:Int, src:Short, op:Int) = genericAllreduce(role, src, op);
    /** Performs a reduction on a single value, returning the result */
    public def allreduce (role:Int, src:UShort, op:Int) = genericAllreduce(role, src, op);
    /** Performs a reduction on a single value, returning the result */
    public def allreduce (role:Int, src:UInt, op:Int) = genericAllreduce(role, src, op);
    /** Performs a reduction on a single value, returning the result */
    public def allreduce (role:Int, src:Int, op:Int) = genericAllreduce(role, src, op);
    /** Performs a reduction on a single value, returning the result */
    public def allreduce (role:Int, src:Long, op:Int) = genericAllreduce(role, src, op);
    /** Performs a reduction on a single value, returning the result */
    public def allreduce (role:Int, src:ULong, op:Int) = genericAllreduce(role, src, op);
    /** Performs a reduction on a single value, returning the result */
    public def allreduce (role:Int, src:Float, op:Int) = genericAllreduce(role, src, op);
    /** Performs a reduction on a single value, returning the result */
    public def allreduce (role:Int, src:Double, op:Int) = genericAllreduce(role, src, op);

    private def genericAllreduce[T] (role:Int, src:T, op:Int) : T {
        val chk = IndexedMemoryChunk.allocateUninitialized[T](1);
        val dst = IndexedMemoryChunk.allocateUninitialized[T](1);
        chk(0) = src;
        finish nativeAllreduce[T](id, role, chk, dst, op);
        return dst(0);
    }

    private static def nativeAllreduce[T](id:Int, role:Int, src:IndexedMemoryChunk[T], dst:IndexedMemoryChunk[T], op:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeAllReduce(id, role, src, 0, dst, 0, 1, op);")
        @Native("c++", "x10rt_allreduce(id, role, src->raw(), dst->raw(), (x10rt_red_op_type)op, x10rt_get_red_type<TPMGL(T)>(), 1, x10aux::coll_handler, x10aux::coll_enter());") {}
    }

    /** Blocks until all members have received the computed result.  Note that not all values of T are valid.
     * The dst array is ignored when role is other than root.
     *
     * @param role Our role in the team
     *
     * @param root The member who is supplied the data
     *
     * @param src The data that will be sent (will only be used by the root
     * member)
     *
     * @param src_off The offset into src at which to start reading
     *
     * @param dst The rail into which the data will be received for this member
     *
     * @param dst_off The offset into dst at which to start writing
     *
     * @param count The number of elements being transferred
     *
     * @param op The operation to perform
     */
    public def reduce[T] (role:Int, root:Int, src:Array[T], src_off:Int, dst:Array[T], dst_off:Int, count:Int, op:Int) : void {
        finish nativeReduce(id, role, root, src.raw(), src_off, dst.raw(), dst_off, count, op);
    }

    private static def nativeReduce[T](id:Int, role:Int, root:Int, src:IndexedMemoryChunk[T], src_off:Int, dst:IndexedMemoryChunk[T], dst_off:Int, count:Int, op:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeReduce(id, role, root, src, src_off, dst, dst_off, count, op);")
        @Native("c++", "x10rt_reduce(id, role, root, &src->raw()[src_off], &dst->raw()[dst_off], (x10rt_red_op_type)op, x10rt_get_red_type<TPMGL(T)>(), count, x10aux::coll_handler, x10aux::coll_enter());") {}
    }

    /** Performs a reduction on a single value, returning the result @deprecated */
    public def reduceSend (role:Int, root:Int, src:Byte, op:Int) { genericReduceSend(role, root, src, op); }
    /** Performs a reduction on a single value, returning the result @deprecated */
    public def reduceSend (role:Int, root:Int, src:UByte, op:Int) { genericReduceSend(role, root, src, op); }
    /** Performs a reduction on a single value, returning the result @deprecated */
    public def reduceSend (role:Int, root:Int, src:Short, op:Int) { genericReduceSend(role, root, src, op); }
    /** Performs a reduction on a single value, returning the result @deprecated */
    public def reduceSend (role:Int, root:Int, src:UShort, op:Int) { genericReduceSend(role, root, src, op); }
    /** Performs a reduction on a single value, returning the result @deprecated */
    public def reduceSend (role:Int, root:Int, src:UInt, op:Int) { genericReduceSend(role, root, src, op); }
    /** Performs a reduction on a single value, returning the result @deprecated */
    public def reduceSend (role:Int, root:Int, src:Int, op:Int) { genericReduceSend(role, root, src, op); }
    /** Performs a reduction on a single value, returning the result @deprecated */
    public def reduceSend (role:Int, root:Int, src:Long, op:Int) { genericReduceSend(role, root, src, op); }
    /** Performs a reduction on a single value, returning the result @deprecated */
    public def reduceSend (role:Int, root:Int, src:ULong, op:Int) { genericReduceSend(role, root, src, op); }
    /** Performs a reduction on a single value, returning the result @deprecated */
    public def reduceSend (role:Int, root:Int, src:Float, op:Int) { genericReduceSend(role, root, src, op); }
    /** Performs a reduction on a single value, returning the result @deprecated */
    public def reduceSend (role:Int, root:Int, src:Double, op:Int) { genericReduceSend(role, root, src, op); }

    private def genericReduceSend[T] (role:Int, root:Int, src:T, op:Int) : void {
        val chk = IndexedMemoryChunk.allocateUninitialized[T](1);
        val dst = dummyChunk[T]();
        chk(0) = src;
        finish nativeReduce[T](id, role, root, chk, dst, op);
    }

    /** Performs a reduction on a single value, returning the result @deprecated */
    public def reduceRecv (role:Int, root:Int, src:Byte, op:Int) = genericReduceRecv(role, root, src, op);
    /** Performs a reduction on a single value, returning the result @deprecated */
    public def reduceRecv (role:Int, root:Int, src:UByte, op:Int) = genericReduceRecv(role, root, src, op);
    /** Performs a reduction on a single value, returning the result @deprecated */
    public def reduceRecv (role:Int, root:Int, src:Short, op:Int) = genericReduceRecv(role, root, src, op);
    /** Performs a reduction on a single value, returning the result @deprecated */
    public def reduceRecv (role:Int, root:Int, src:UShort, op:Int) = genericReduceRecv(role, root, src, op);
    /** Performs a reduction on a single value, returning the result @deprecated */
    public def reduceRecv (role:Int, root:Int, src:UInt, op:Int) = genericReduceRecv(role, root, src, op);
    /** Performs a reduction on a single value, returning the result @deprecated */
    public def reduceRecv (role:Int, root:Int, src:Int, op:Int) = genericReduceRecv(role, root, src, op);
    /** Performs a reduction on a single value, returning the result @deprecated */
    public def reduceRecv (role:Int, root:Int, src:Long, op:Int) = genericReduceRecv(role, root, src, op);
    /** Performs a reduction on a single value, returning the result @deprecated */
    public def reduceRecv (role:Int, root:Int, src:ULong, op:Int) = genericReduceRecv(role, root, src, op);
    /** Performs a reduction on a single value, returning the result @deprecated */
    public def reduceRecv (role:Int, root:Int, src:Float, op:Int) = genericReduceRecv(role, root, src, op);
    /** Performs a reduction on a single value, returning the result @deprecated */
    public def reduceRecv (role:Int, root:Int, src:Double, op:Int) = genericReduceRecv(role, root, src, op);

    private def genericReduceRecv[T] (role:Int, root:Int, src:T, op:Int) : T {
        val chk = IndexedMemoryChunk.allocateUninitialized[T](1);
        val dst = IndexedMemoryChunk.allocateUninitialized[T](1);
        chk(0) = src;
        finish nativeReduce[T](id, role, root, chk, dst, op);
        return dst(0);
    }

    /** Performs a reduction on a single value, returning the result  */
    public def reduce (role:Int, root:Int, src:Byte, op:Int) = genericReduce[Byte](role, root, src, op);
    /** Performs a reduction on a single value, returning the result  */
    public def reduce (role:Int, root:Int, src:UByte, op:Int) = genericReduce[UByte](role, root, src, op);
    /** Performs a reduction on a single value, returning the result  */
    public def reduce (role:Int, root:Int, src:Short, op:Int) = genericReduce[Short](role, root, src, op);
    /** Performs a reduction on a single value, returning the result  */
    public def reduce (role:Int, root:Int, src:UShort, op:Int) = genericReduce[UShort](role, root, src, op);
    /** Performs a reduction on a single value, returning the result  */
    public def reduce (role:Int, root:Int, src:UInt, op:Int) = genericReduce[UInt](role, root, src, op);
    /** Performs a reduction on a single value, returning the result  */
    public def reduce (role:Int, root:Int, src:Int, op:Int) = genericReduce[Int](role, root, src, op);
    /** Performs a reduction on a single value, returning the result  */
    public def reduce (role:Int, root:Int, src:Long, op:Int) = genericReduce[Long](role, root, src, op);
    /** Performs a reduction on a single value, returning the result  */
    public def reduce (role:Int, root:Int, src:ULong, op:Int) = genericReduce[ULong](role, root, src, op);
    /** Performs a reduction on a single value, returning the result  */
    public def reduce (role:Int, root:Int, src:Float, op:Int) = genericReduce[Float](role, root, src, op);
    /** Performs a reduction on a single value, returning the result  */
    public def reduce (role:Int, root:Int, src:Double, op:Int) = genericReduce[Double](role, root, src, op);

    private def genericReduce[T] (role:Int, root:Int, src:T, op:Int) {T haszero} : T {
        val chk = IndexedMemoryChunk.allocateUninitialized[T](1);
        val dst = role == root ? IndexedMemoryChunk.allocateUninitialized[T](1) : dummyChunk[T]();
        chk(0) = src;
        finish nativeReduce[T](id, role, root, chk, dst, op);
        return role == root ? dst(0) : Zero.get[T]();
    }

    private static def nativeReduce[T](id:Int, role:Int, root:Int, src:IndexedMemoryChunk[T], dst:IndexedMemoryChunk[T], op:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeReduce(id, role, root, src, 0, dst, 0, 1, op);")
        @Native("c++", "x10rt_reduce(id, role, root, src->raw(), dst->raw(), (x10rt_red_op_type)op, x10rt_get_red_type<TPMGL(T)>(), 1, x10aux::coll_handler, x10aux::coll_enter());") {}
    }

    /** Returns the index of the biggest double value across the team */
    public def indexOfMax (role:Int, v:Double, idx:Int) : Int {
        val src = IndexedMemoryChunk.allocateUninitialized[DoubleIdx](1);
        val dst = IndexedMemoryChunk.allocateUninitialized[DoubleIdx](1);
        src(0) = DoubleIdx(v, idx);
        finish nativeIndexOfMax(id, role, src, dst);
        return dst(0).idx;
    }

    private static def nativeIndexOfMax(id:Int, role:Int, src:IndexedMemoryChunk[DoubleIdx], dst:IndexedMemoryChunk[DoubleIdx]) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeIndexOfMax(id, role, src, dst);")
        @Native("c++", "x10rt_allreduce(id, role, src->raw(), dst->raw(), X10RT_RED_OP_MAX, X10RT_RED_TYPE_DBL_S32, 1, x10aux::coll_handler, x10aux::coll_enter());") {}
    }

    /** Returns the index of the smallest double value across the team */
    public def indexOfMin (role:Int, v:Double, idx:Int) : Int {
        val src = IndexedMemoryChunk.allocateUninitialized[DoubleIdx](1);
        val dst = IndexedMemoryChunk.allocateUninitialized[DoubleIdx](1);
        src(0) = DoubleIdx(v, idx);
        finish nativeIndexOfMin(id, role, src, dst);
        return dst(0).idx;
    }

    private static def nativeIndexOfMin(id:Int, role:Int, src:IndexedMemoryChunk[DoubleIdx], dst:IndexedMemoryChunk[DoubleIdx]) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeIndexOfMin(id, role, src, dst);")
        @Native("c++", "x10rt_allreduce(id, role, src->raw(), dst->raw(), X10RT_RED_OP_MIN, X10RT_RED_TYPE_DBL_S32, 1, x10aux::coll_handler, x10aux::coll_enter());") {}
    }

    /** Create new teams by subdividing an existing team.  This is called by each member
     * of an existing team, indicating which of the new teams it will be a member of, and its role
     * within that team.  The old team is still available after this call.  All the members
     * of the old team must collectively assign themselves to new teams such that there is exactly 1
     * member of the original team for each role of each new team.  It is undefined behaviour if two
     * members of the original team decide to play the same role in one of the new teams, or if one of
     * the roles of a new team is left unfilled.
     *
     * @param role The caller's role within the old team
     *
     * @param color The new team, must be a number between 0 and the number of new teams - 1
     *
     * @param new_role The caller's role within the new team
     */
    public def split (role:Int, color:Int, new_role:Int) : Team {
        val result = IndexedMemoryChunk.allocateUninitialized[Int](1);
        finish nativeSplit(id, role, color, new_role, result);
        val new_id = result(0);
        val place:Int = here.id;
        val new_size = nativeSize (new_id);
        
    	val src_raw = IndexedMemoryChunk.allocateUninitialized[Int](1);
    	src_raw(0) = place;
    	val dst_raw = IndexedMemoryChunk.allocateUninitialized[Int](new_size);
        finish nativeAllgather(new_id, new_role, src_raw, 0, dst_raw, 0, 1);
        
        return Team(new_id, new Array[Place](dst_raw.length(), (i:Int)=>new Place(dst_raw(i))));
    }

    private static def nativeSplit(id:Int, role:Int, color:Int, new_role:Int, result:IndexedMemoryChunk[Int]) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeSplit(id, role, color, new_role, result);")
        @Native("c++", "x10rt_team_split(id, role, color, new_role, x10aux::coll_handler2, x10aux::coll_enter2(result->raw()));") {}
    }

    /** Destroy a team that is no-longer needed.  Called simultaneously by each member of
     * the team.  There should be no operations on the team after this.
     *
     * @param role Our role in this team
     */
    public def del (role:Int) : void {
        if (this == WORLD) throw new IllegalArgumentException("Cannot delete Team.WORLD");
        finish nativeDel(id, role);
    }

    private static def nativeDel(id:Int, role:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeDel(id, role);")
        @Native("c++", "x10rt_team_del(id, role, x10aux::coll_handler, x10aux::coll_enter());") {}
    }

    public def toString() = "Team(" + this.id + "," + this.places +  ")";
    public def equals(that:Team) = that.id==this.id;
    public def equals(that:Any) = that instanceof Team && (that as Team).id==this.id;
    public def hashCode()=id;


    private def flatten[T] (src:Array[Array[T](1)](1)) : Pair[Array[T](1), Pair[Array[Int](1), Array[Int](1)]] {
        val sizes:Array[Int](1) = src.map((x:Array[T])=>x.size as Int);
        val size = sizes.reduce((x:Int, y:Int)=>x+y, 0);
        val acc:Array[Int](1) = sizes.scan((x:Int, y:Int)=> x+y, 0);
        val offs:Array[Int](1) = new Array[Int](acc.size, (i:Int)=>(i==0) ? 0 : acc(i-1));
        val find_arr = (i:Int) => {
            assert(i < size);
            val ind = ArrayUtils.binarySearch(acc, i );
            if (ind >= 0) {
                var max_ind:Int = ind;
                while (max_ind < acc.size - 1 && acc(max_ind) == acc(max_ind + 1)) ++max_ind;
                assert(max_ind + 1< acc.size);
                return max_ind + 1;
            }
            else return -(ind +1);
        };
        val flatten_src:Array[T](1) = new Array[T](size, (i:Int)=> 
            src(find_arr(i))(i - offs(find_arr(i)))
        );
        return Pair[Array[T](1), Pair[Array[Int](1), Array[Int](1)]](flatten_src, Pair[Array[Int](1), Array[Int](1)](offs, sizes));
    }


    public def scatter[T] (role:Int, root:Int, src:Array[T]) {
        val team_size = size();
        assert(role != root || src.size % team_size == 0);
        val count = bcastSend1(role, root, src.size / team_size);
        return scatterSend(role, root, src, count);
    }

    /**
     * @deprecated use {@link #scatter(Int, Int, Array[T])} instead
     */
    public def scatterSendAuto[T] (role:Int, root:Int, src:Array[T]) {
        val team_size = size();
        assert(src.size % team_size == 0);
        val count = bcastSend1(role, root, src.size / team_size);
        return scatterSend(role, root, src, count);
    }

    /**
     * @deprecated use {@link #scatter(Int, Int, Array[T])} instead
     */
    public def scatterRecvAuto[T] (role:Int, root:Int) {
        val count = bcastRecv1[Int](role, root);
        return scatterRecv[T](role, root, count);
    }

    public def scatterv[T] (role:Int, root:Int, src:Array[T], src_counts:Array[Int], src_offs:Array[Int]) {
        val team_size = size();
        assert(role != root || src_counts.size == team_size);
        assert(role != root || src_offs.size == team_size);
        val dst_count = scatter(role, root, src_counts, 1)(0);
        return scatterv(role, root, src, src_counts, src_offs, dst_count);
    }

    /**
     * @deprecated use {@link #scatter(Int, Int, Array[T], Array[Int], Array[Int])} instead
     */
    public def scattervSendAuto[T] (role:Int, root:Int, src:Array[T], src_counts:Array[Int], src_offs:Array[Int]) {
        val team_size = size();
        assert(role != root || src_counts.size == team_size);
        assert(role != root || src_offs.size == team_size);
        val dst_count = scatterSend(role, root, src_counts, 1)(0);
        return scattervSend(role, root, src, src_counts, src_offs, dst_count);
    }

    public def scatterv[T] (role:Int, root:Int, src:Array[T], src_counts:Array[Int](1)) {
        val src_offs = role == root ? countsToOffs(src_counts) : null as Array[Int](1);
        return scatterv[T](role, root, src, src_counts, src_offs);
    }

    /**
     * @deprecated use {@link #scatter(Int, Int, Array[T], Array[Int])} instead
     */
    public def scattervSendAuto[T] (role:Int, root:Int, src:Array[T], src_counts:Array[Int](1)) {
        val src_offs = countsToOffs(src_counts);
        return scattervSendAuto[T](role, root, src, src_counts, src_offs);
    }

    public def scatterv[T] (role:Int, root:Int, src:Array[Array[T](1)](1)) {
        if (role == root) {
            val flatten_src_tuple = flatten(src);
            val flatten_src = flatten_src_tuple.first;
            val src_offs = flatten_src_tuple.second.first;
            val src_sizes = flatten_src_tuple.second.second;
            return scatterv[T](role, root, flatten_src, src_sizes, src_offs);
        } else {
            return scatterv[T](role, root, null, null, null);
        }
    }

    /**
     * @deprecated use {@link #scatter(Int, Int, Array[Array[T]])} instead
     */
    public def scattervSendAuto[T] (role:Int, root:Int, src:Array[Array[T](1)](1)) {
        val flatten_src_tuple = flatten(src);
        val flatten_src = flatten_src_tuple.first;
        val src_offs = flatten_src_tuple.second.first;
        val src_sizes = flatten_src_tuple.second.second;
        return scattervSendAuto[T](role, root, flatten_src, src_sizes, src_offs);
    }

    /**
     * @deprecated use scatterv instead
     */
    public def scattervRecvAuto[T] (role:Int, root:Int) {
        val dst_count = scatterRecv[Int](role, root, 1)(0);
        val result = scattervRecv[T](role, root, dst_count);
        return result;
    }

    public def gatherv[T] (role:Int, root:Int, src:Array[T](1)) {
        val src_size = role == root ? src.size : Zero.get[Int]();
        val dst_counts = gather1[Int](role, root, src_size);
        return gatherv[T](role, root, src, dst_counts);
    }

    /**
     * @deprecated use {@link #gatherv(Int, Int, Array[Array[T]])} instead
     */
    public def gathervSendAuto[T] (role:Int, root:Int, src:Array[T]) : void {
        gatherSend1(role, root, src.size);
        gathervSend(role, root, src);
    }

    /**
     * @deprecated use {@link #gatherv(Int, Int, Array[Array[T]])} instead
     */
    public def gathervRecvAuto[T] (role:Int, root:Int, src:Array[T]) {
        val dst_counts = gatherRecv1[Int](role, root, src.size);
        return gathervRecv[T](role, root, src, dst_counts);
    }


    public def bcast[T] (role:Int, root:Int, src:Array[T]) {
        val src_size = role == root ? src.size : Zero.get[Int]();
        val count = bcast1(role, root, src_size);
        return bcast(role, root, src, count);
    }

    /**
     * @deprecated use {@link #bcast(Int, Int, Array[T])} instead
     */
    public def bcastSendAuto[T] (role:Int, root:Int, src:Array[T]) {
        val count = bcastSend1(role, root, src.size);
        return bcastSend(role, root, src, count);
    }

    /**
     * @deprecated use {@link #bcast(Int, Int, Array[T])} instead
     */
    public def bcastRecvAuto[T] (role:Int, root:Int) {
        val count = bcastRecv1[Int](role, root);
        return bcastRecv[T](role, root, count);
    }


    public def allgathervAuto[T] (role:Int, src:Array[T]) {
        val dst_counts = allgather1(role, src.size as Int);
        val dst_offs = countsToOffs(dst_counts);

        return allgatherv[T](role, src, dst_offs, dst_counts);
    }


    public def alltoallvAutoWithBreakdown[T] (role:Int, src:Array[T], src_offs:Array[Int], src_counts:Array[Int]) : Pair[Array[T](1),Array[Int](1)] {
        val dst_counts = alltoall(role, src_counts);
        val dst_offs = countsToOffs(dst_counts);
        val dst = alltoallv[T](role, src, src_offs, src_counts, dst_offs, dst_counts);
        return Pair[Array[T](1),Array[Int](1)](dst, dst_counts);
    }

    public def alltoallvAutoWithBreakdown[T] (role:Int, src:Array[Array[T](1)](1)) : Pair[Array[T](1),Array[Int](1)] {
        val flatten_src_tuple = flatten(src);
        val flatten_src = flatten_src_tuple.first;
        val src_offs = flatten_src_tuple.second.first;
        val src_sizes = flatten_src_tuple.second.second;
        return alltoallvAutoWithBreakdown(role, flatten_src, src_offs, src_sizes);
    }

    public def alltoallvAuto[T] (role:Int, src:Array[T], src_offs:Array[Int], src_counts:Array[Int]) {
        val dst_counts = alltoall(role, src_counts);
        val dst_offs = countsToOffs(dst_counts);
        val dst = alltoallv[T](role, src, src_offs, src_counts, dst_offs, dst_counts);
        return dst;
    }

    public def alltoallvAuto[T] (role:Int, src:Array[T], src_counts:Array[Int](1)) {
        val src_offs = countsToOffs(src_counts);
        return alltoallvAuto[T](role, src, src_offs, src_counts);
    }

    public def alltoallvAuto[T] (role:Int, src:Array[Array[T](1)](1)) {
        val flatten_src_tuple = flatten(src);
        val flatten_src = flatten_src_tuple.first;
        val src_offs = flatten_src_tuple.second.first;
        val src_sizes = flatten_src_tuple.second.second;
        return alltoallvAuto(role, flatten_src, src_offs, src_sizes);
    }

}

// vim: shiftwidth=4:tabstop=4:expandtab
