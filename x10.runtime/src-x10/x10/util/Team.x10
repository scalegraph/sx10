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

import x10.compiler.Pragma;
import x10.compiler.Inline;
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
public class Team {

    private static struct DoubleIdx(value:Double, idx:Int) {}

    private static isDebug = System.getenv().containsKey("X10_TEAM_DEBUG");
	public static @Inline def debugln(pkg:String, str: String) {
        if (isDebug) {
		//@Ifdef("DEBUGPRINT")
			Console.OUT.println("" + Timer.milliTime() + ":Place " + here.id + ":Worker " + Runtime.workerId() + ":" + pkg + ": " + str);
			Console.OUT.flush();
		}
	}

    /** A team that has one member at each place.
     */
    public static WORLD = new Team(0, new Array[Place](PlaceGroup.WORLD.numPlaces(), (i:Int)=>PlaceGroup.WORLD(i)));

    /** The underlying representation of a team's identity.
     */
    private id: Int;
    transient private var members: Array[Place](1);
    transient private var roleHere: Array[Int](1);

    private static def nativeMembers(id:Int, result:IndexedMemoryChunk[Int]) : void {
        //@Native("java", "x10.x10rt.TeamSupport.nativeSplit(id, role, color, new_role, result);")
        @Native("c++", "x10rt_team_members(id, (x10rt_place*)result->raw());") {}
    }

    private static def role(places:Array[Place](1), place:Place) {
        val role = new ArrayBuilder[Int]();
        for ([p] in places) {
            if (places(p) == place)
                role.add(p);
        }
        return role.result();    
    }
    
    private def setupMembers() {
    	val numMembers = size();
    	val membersimc = IndexedMemoryChunk.allocateUninitialized[Int](numMembers);
    	nativeMembers(id, membersimc);
    	members = new Array[Place](numMembers,  (i :Int) => Place(membersimc(i)));
    	roleHere = role(members, here);
    }

    /** Returns the id of the team.
     */
    public def id() = id;

    /** Returns the places of the team.
     */
    public def places() {
    	if(members == null) setupMembers();
    	return members;
    }

    /** Returns the role of here
     */
    public def role() : Array[Int](1) {
    	if(members == null) setupMembers();
    	return roleHere;
    }

    /** Returns the PlaceGroup of the places of the team.
     */
    public def placeGroup() : PlaceGroup = {
        return new OrderedPlaceGroup(places());
    }

    /** Returns the place corresponding to the given role.
     * @param role Our role in this team
     */
    public def place(role:Int) : Place = places()(role);

    /** Returns the role corresponding to the given place.
     * @param place Place in this team
     */
    public def role(place:Place) : Array[Int](1) = {
        return role(places(), place);
    }
    
    private def this (id:Int, places:Array[Place](1)) {
        val pg = new OrderedPlaceGroup(places);
        this.id = id;
        members = places;
        roleHere = role(members, here);
    }

    /** Create a team by defining the place where each member lives.  This would usually be called before creating an async for each member of the team.
     * @param places The place of each member
     */
    public def this (places :Array[Place](1)) {
       val result = IndexedMemoryChunk.allocateUninitialized[Int](1);
       finish nativeMake(places.raw(), places.size, result);
       id = result(0);
    	members = new Array[Place](places);
    	roleHere = role(members, here);
    }

    private static def nativeMake (places:IndexedMemoryChunk[Place], count:Int, result:IndexedMemoryChunk[Int]) : void {
        Runtime.increaseParallelism();
        @Native("java", "x10.x10rt.TeamSupport.nativeMake(places, count, result);")
    	@Native("c++", "x10rt_team_new(count, (x10rt_place*)places->raw(), x10aux::coll_handler2, x10aux::coll_enter2(result->raw()));") {}
        Runtime.decreaseParallelism(1);
    }

    /** Returns the number of elements in the team.
     */
    public def size () : Int = nativeSize(id);

    private static def nativeSize (id:Int) : Int {
        @Native("java", "return x10.x10rt.TeamSupport.nativeSize(id);")
        @Native("c++", "return (x10_int)x10rt_team_sz(id);") { return -1; }
    }

    public def needToSerialize[T] () : Boolean = nativeNeedToSerialize[T]();

    private static def nativeNeedToSerialize[T] () : Boolean {
        @Native("c++", "return x10aux::getRTT<TPMGL(T) >()->containsPtrs;") { return false; }
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
        scatter(id, role, root, getRawOrDummyChunk(src), src_off, dst.raw(), dst_off, count);
    }

    public def scatter[T] (id:Int, role:Int, root:Int, src:IndexedMemoryChunk[T], src_off:Int, dst:IndexedMemoryChunk[T], dst_off:Int, count:Int) : void {
        if (needToSerialize[T]()) {
            if (role == root) {
                val places = size();
                val src_offs = new Array[Int](places, (i :Int) => i * count);
                val src_counts = new Array[Int](places, count);
                val ser_offs = new Array[Int](places);
                val ser_counts = new Array[Int](places);
                val ser_src = ParallelSerialization.serialize(src, src_offs.raw(), src_counts.raw(), ser_offs.raw(), ser_counts.raw());
                val deser_counts = scatter[Int](role, root, ser_counts, 1);
                val deser_dst = new Array[Byte](deser_counts(0));
                finish nativeScatterv(id, role, root, ser_src, ser_offs.raw(), ser_counts.raw(), deser_dst.raw(), 0, deser_counts(0));
                ParallelSerialization.deserialize(dst, dst_off, count, deser_dst.raw(), 0, deser_counts(0));
            }
            else {
                val deser_counts = scatter[Int](role, root, null, 1);
                val deser_dst = new Array[Byte](deser_counts(0));
                finish nativeScatterv(id, role, root, dummyChunk[Byte](), dummyChunk[Int](), dummyChunk[Int](), deser_dst.raw(), 0, deser_counts(0));
                ParallelSerialization.deserialize(dst, dst_off, count, deser_dst.raw(), 0, deser_counts(0));
            }
        }
        else {
            finish nativeScatter(id, role, root, src, src_off, dst, dst_off, count);
        }
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
    	assert (role != root || src != null);
        val dst_raw = IndexedMemoryChunk.allocateUninitialized[T](count);
        scatter(id, role, root, getRawOrDummyChunk(src), 0, dst_raw, 0, count);
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
        scatterv(id, role, root, getRawOrDummyChunk(src), getRawOrDummyChunk(src_offs), getRawOrDummyChunk(src_counts), getRawOrDummyChunk(dst), dst_off, dst_count);
    }

    public def scatterv[T] (id:Int, role:Int, root:Int, src:IndexedMemoryChunk[T], src_offs:IndexedMemoryChunk[Int], src_counts:IndexedMemoryChunk[Int], dst:IndexedMemoryChunk[T], dst_off:Int, dst_count:Int) : void {
        if (needToSerialize[T]()) {
            if (role == root) {
                val places = size();
                val ser_offs = new Array[Int](places);
                val ser_counts = new Array[Int](places);
                val ser_src = ParallelSerialization.serialize(src, src_offs, src_counts, ser_offs.raw(), ser_counts.raw());
                val deser_counts = scatter[Int](role, root, ser_counts, 1);
                val deser_dst = new Array[Byte](deser_counts(0));
                finish nativeScatterv(id, role, root, ser_src, ser_offs.raw(), ser_counts.raw(), deser_dst.raw(), 0, deser_counts(0));
                ParallelSerialization.deserialize(dst, dst_off, dst_count, deser_dst.raw(), 0, deser_counts(0));
            }
            else {
                val deser_counts = scatter[Int](role, root, null, 1);
                val deser_dst = new Array[Byte](deser_counts(0));
                finish nativeScatterv(id, role, root, dummyChunk[Byte](), dummyChunk[Int](), dummyChunk[Int](), deser_dst.raw(), 0, deser_counts(0));
                ParallelSerialization.deserialize(dst, dst_off, dst_count, deser_dst.raw(), 0, deser_counts(0));
            }
        }
        else {
            finish nativeScatterv(id, role, root, src, src_offs, src_counts, dst, dst_off, dst_count);
        }
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
        val dst = new Array[T](IndexedMemoryChunk.allocateUninitialized[T](dst_count));
        scatterv(role, root, src, src_offs, src_counts, dst, 0, dst_count);
        return dst;
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
        gather(id, role, root, src.raw(), src_off, getRawOrDummyChunk(dst), dst_off, count);
    }

    public def gather[T] (id:Int, role:Int, root:Int, src:IndexedMemoryChunk[T], src_off:Int, dst:IndexedMemoryChunk[T], dst_off:Int, count:Int) : void {
        if (needToSerialize[T]()) {
            if (role == root) {
                val places = size();
                val ser_src = ParallelSerialization.serialize(src, src_off, count);
                val ser_count = ser_src.length();
                val deser_counts = gather1[Int](role, root, ser_count);
                val deser_offs = new Array[Int](places+1);
                deser_offs(0) = 0;
                for (i in 0..(places-1)) deser_offs(i+1) = deser_counts(i) + deser_offs(i);
                val deser_dst = new Array[Byte](deser_offs(places));
                finish nativeGatherv(id, role, root, ser_src, 0, ser_count, deser_dst.raw(), deser_offs.raw(), deser_counts.raw());
                val dst_counts = new Array[Int](places, count);
                val dst_offs = new Array[Int](places, (i :Int) => i * count);
                ParallelSerialization.deserialize(dst, dst_offs.raw(), dst_counts.raw(), deser_dst.raw(), deser_offs.raw(), deser_counts.raw());
            }
            else {
                val ser_src = ParallelSerialization.serialize(src, src_off, count);
                val ser_count = ser_src.length();
                val deser_counts = gather1[Int](role, root, ser_count);
                finish nativeGatherv(id, role, root, ser_src, 0, ser_count, dummyChunk[Byte](), dummyChunk[Int](), dummyChunk[Int]());
            }
        }
        else {
            finish nativeGather(id, role, root, src, src_off, dst, dst_off, count);
        }
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
        val dst = (role == root) ? new Array[T](IndexedMemoryChunk.allocateUninitialized[T](count * size())) : null;
        gather(role, root, src, 0, dst, 0, count);
        return dst;
    }

    /** Almost same as gather except that each member sends one data.
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
        val dst : Array[T](1) = new Array[T](IndexedMemoryChunk.allocateUninitialized[T](role == root ? size() : 0)) ;
        gather(role, root, new Array[T](src_raw), 0, dst, 0, 1);
        return dst;
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
        gatherv(id, role, root, getRawOrDummyChunk(src), src_off, src_count, getRawOrDummyChunk(dst), getRawOrDummyChunk(dst_offs), getRawOrDummyChunk(dst_counts));
    }

    public def gatherv[T] (id:Int, role:Int, root:Int, src:IndexedMemoryChunk[T], src_off:Int, src_count:Int, dst:IndexedMemoryChunk[T], dst_offs:IndexedMemoryChunk[Int], dst_counts:IndexedMemoryChunk[Int]) : void {
        if (needToSerialize[T]()) {
            if (role == root) {
                val places = size();
                val ser_src = ParallelSerialization.serialize(src, src_off, src_count);
                val ser_count = ser_src.length();
                val deser_counts = gather1[Int](role, root, ser_count);
                val deser_offs = new Array[Int](places+1);
                deser_offs(0) = 0;
                for (i in 0..(places-1)) deser_offs(i+1) = deser_counts(i) + deser_offs(i);
                val deser_dst = new Array[Byte](deser_offs(places));
                finish nativeGatherv(id, role, root, ser_src, 0, ser_count, deser_dst.raw(), deser_offs.raw(), deser_counts.raw());
                ParallelSerialization.deserialize(dst, dst_offs, dst_counts, deser_dst.raw(), deser_offs.raw(), deser_counts.raw());
            }
            else {
                val ser_src = ParallelSerialization.serialize(src, src_off, src_count);
                val ser_count = ser_src.length();
                val deser_counts = gather1[Int](role, root, ser_count);
                finish nativeGatherv(id, role, root, ser_src, 0, ser_count, dummyChunk[Byte](), dummyChunk[Int](), dummyChunk[Int]());
            }
        }
        else {
            finish nativeGatherv(id, role, root, src, src_off, src_count, dst, dst_offs, dst_counts);
        }
    }

    private static def nativeGatherv[T] (id:Int, role:Int, root:Int, src:IndexedMemoryChunk[T], src_off:Int, src_count:Int, dst:IndexedMemoryChunk[T], dst_offs:IndexedMemoryChunk[Int], dst_counts:IndexedMemoryChunk[Int]) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeGatherV(id, role, root, src, src_off, src_count, dst, dst_offs, dst_counts);")
        @Native("c++", "x10rt_gatherv(id, role, root, &src->raw()[src_off], src_count, dst->raw(), dst_offs->raw(), dst_counts->raw(), sizeof(TPMGL(T)), x10aux::coll_handler, x10aux::coll_enter());") {}
    }

    public def gatherv[T] (role:Int, root:Int, src:Array[T], dst_offs:Array[Int], dst_counts:Array[Int] ) {
        val dst = (role == root) ? new Array[T](IndexedMemoryChunk.allocateUninitialized[T](dst_counts.reduce((x:Int, y:Int)=>x+y, 0))) : null;
        gatherv(role, root, src, 0, src.size, dst, dst_offs, dst_counts);
        return dst;
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
        bcast(id, role, root, getRawOrDummyChunk(src), src_off, dst.raw(), dst_off, count);
    }

    public def bcast[T] (id:Int, role:Int, root:Int, src:IndexedMemoryChunk[T], src_off:Int, dst:IndexedMemoryChunk[T], dst_off:Int, count:Int) : void {
        if (needToSerialize[T]()) {
            if (role == root) {
                val places = size();
                val ser_src = ParallelSerialization.serialize(src, src_off, count);
                val ser_count = ser_src.length();
                val deser_count = bcast1[Int](role, root, ser_count);
                val deser_dst = new Array[Byte](deser_count);
                finish nativeBcast(id, role, root, ser_src, 0, deser_dst.raw(), 0, deser_count);
                ParallelSerialization.deserialize(dst, dst_off, count, deser_dst.raw(), 0, deser_count);
            }
            else {
                val deser_count = bcast1[Int](role, root, 0);
                val deser_dst = new Array[Byte](deser_count);
                finish nativeBcast(id, role, root, dummyChunk[Byte](), 0, deser_dst.raw(), 0, deser_count);
                ParallelSerialization.deserialize(dst, dst_off, count, deser_dst.raw(), 0, deser_count);
            }

        }
        else {
            finish nativeBcast(id, role, root, src, src_off, dst, dst_off, count);
        }
    }

    private static def nativeBcast[T] (id:Int, role:Int, root:Int, src:IndexedMemoryChunk[T], src_off:Int, dst:IndexedMemoryChunk[T], dst_off:Int, count:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeBcast(id, role, root, src, src_off, dst, dst_off, count);")
        @Native("c++", "x10rt_bcast(id, role, root, &src->raw()[src_off], &dst->raw()[dst_off], sizeof(TPMGL(T)), count, x10aux::coll_handler, x10aux::coll_enter());") {}
    }
    
    public def bcast1[T] (role:Int, root:Int, src:T) : T {
    	val src_raw = IndexedMemoryChunk.allocateUninitialized[T](1);
    	src_raw(0) = src;
    	val dst_raw = IndexedMemoryChunk.allocateUninitialized[T](1);
        bcast(id, role, root, src_raw, 0, dst_raw, 0, 1);
        return dst_raw(0);
    }

    public def bcast[T] (role:Int, root:Int, src:Array[T], count:Int) {
    	val dst_raw = IndexedMemoryChunk.allocateUninitialized[T](count);
        bcast(id, role, root, getRawOrDummyChunk(src), 0, dst_raw, 0, count);
        return new Array[T](dst_raw);
    }

    public def allgather1[T] (role:Int, src:T) {
        val src_raw = IndexedMemoryChunk.allocateUninitialized[T](1);
        src_raw(0) = src;
        val dst = new Array[T](IndexedMemoryChunk.allocateUninitialized[T](size()));
        allgather(role, new Array[T](src_raw), 0, dst, 0, 1);
        return dst;
    }

    public def allgather[T] (role:Int, src:Array[T]) {
        val dst = new Array[T](IndexedMemoryChunk.allocateUninitialized[T](src.size * size()));
        allgather(role, src, 0, dst, 0, src.size);
        return dst;
    }

    public def allgather[T] (role:Int, src:Array[T], src_off:Int, dst:Array[T], dst_off:Int, count:Int) : void {
        allgather(id, role, src.raw(), src_off, dst.raw(), dst_off, count);
    }

    public def allgather[T](id:Int, role:Int, src:IndexedMemoryChunk[T], src_off:Int, dst:IndexedMemoryChunk[T], dst_off:Int, count:Int) : void {
        if (needToSerialize[T]()) {
            val places = size();
            val ser_src = ParallelSerialization.serialize(src, src_off, count);
            val ser_count = ser_src.length();
            val deser_counts = allgather1[Int](role, ser_count);
            val deser_offs = new Array[Int](places + 1);
            deser_offs(0) = 0;
            for (i in 0..(places-1)) deser_offs(i+1) = deser_counts(i) + deser_offs(i);
            val deser_dst = new Array[Byte](deser_offs(places));
            finish nativeAllgatherv(id, role, ser_src, 0, ser_count, deser_dst.raw(), deser_offs.raw(), deser_counts.raw());
            val dst_offs = new Array[Int](places, (i :Int) => i * count);
            val dst_counts = new Array[Int](places, count);
            ParallelSerialization.deserialize(dst, dst_offs.raw(), dst_counts.raw(), deser_dst.raw(), deser_offs.raw(), deser_counts.raw());
        }
        else {
            finish nativeAllgather(id, role, src, src_off, dst, dst_off, count);
        }
    }

    private static def nativeAllgather[T](id:Int, role:Int, src:IndexedMemoryChunk[T], src_off:Int, dst:IndexedMemoryChunk[T], dst_off:Int, count:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeAllGather(id, role, src, src_off, dst, dst_off, count);")
        @Native("c++", "x10rt_allgather(id, role, &src->raw()[src_off], &dst->raw()[dst_off], sizeof(TPMGL(T)), count, x10aux::coll_handler, x10aux::coll_enter());") {}
    }
    
    public def allgatherv[T] (role:Int, src:Array[T], dst_offs:Array[Int], dst_counts:Array[Int]) {
        val dst = new Array[T](IndexedMemoryChunk.allocateUninitialized[T](dst_counts.reduce((x:Int, y:Int)=>x+y, 0)));
        allgatherv(role, src, 0, src.size, dst, dst_offs, dst_counts);
        return dst;
    }

    public def allgatherv[T] (role:Int, src:Array[T], src_off:Int, src_count:Int, dst:Array[T], dst_offs:Array[Int], dst_counts:Array[Int]) : void {
        allgatherv(id, role, src.raw(), src_off, src_count, dst.raw(), dst_offs.raw(), dst_counts.raw());
    }

    public def allgatherv[T] (id:Int, role:Int, src:IndexedMemoryChunk[T], src_off:Int, src_count:Int, dst:IndexedMemoryChunk[T], dst_offs:IndexedMemoryChunk[Int], dst_counts:IndexedMemoryChunk[Int]) : void {
        if (needToSerialize[T]()) {
            val places = size();
            val ser_src = ParallelSerialization.serialize(src, src_off, src_count);
            val ser_count = ser_src.length();
            val deser_counts = allgather1[Int](role, ser_count);
            val deser_offs = new Array[Int](places + 1);
            deser_offs(0) = 0;
            for (i in 0..(places-1)) deser_offs(i+1) = deser_counts(i) + deser_offs(i);
            val deser_dst = new Array[Byte](deser_offs(places));
            finish nativeAllgatherv(id, role, ser_src, 0, ser_count, deser_dst.raw(), deser_offs.raw(), deser_counts.raw());
            ParallelSerialization.deserialize(dst, dst_offs, dst_counts, deser_dst.raw(), deser_offs.raw(), deser_counts.raw());
        }
        else {
            finish nativeAllgatherv(id, role, src, src_off, src_count, dst, dst_offs, dst_counts);
        }
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
        alltoall(id, role, src.raw(), src_off, dst.raw(), dst_off, count);
    }

    public def alltoall[T](id:Int, role:Int, src:IndexedMemoryChunk[T], src_off:Int, dst:IndexedMemoryChunk[T], dst_off:Int, count:Int) : void {
        if (needToSerialize[T]()) {
            val places = size();
            val src_counts = new Array[Int](places, count);
            val src_offs = new Array[Int](places, (i :Int) => i * count);
            val ser_offs = new Array[Int](places);
            val ser_counts = new Array[Int](places);
            val ser_src = ParallelSerialization.serialize(src, src_offs.raw(), src_counts.raw(), ser_offs.raw(), ser_counts.raw());
            val deser_counts = new Array[Int](places);
            finish nativeAlltoall(id, role, ser_counts.raw(), 0, deser_counts.raw(), 0, 1);
            val deser_offs = new Array[Int](places + 1);
            deser_offs(0) = 0;
            for (i in 0..(places-1)) deser_offs(i+1) = deser_counts(i) + deser_offs(i);
            val deser_dst = new Array[Byte](deser_offs(places));
            finish nativeAlltoallv(id, role, ser_src, ser_offs.raw(), ser_counts.raw(), deser_dst.raw(), deser_offs.raw(), deser_counts.raw());
            val dst_counts = new Array[Int](places, count);
            val dst_offs = new Array[Int](places + 1);
            dst_offs(0) = 0;
            for (i in 0..(places-1)) dst_offs(i+1) = dst_counts(i) + dst_offs(i);
            ParallelSerialization.deserialize(dst, dst_offs.raw(), dst_counts.raw(), deser_dst.raw(), deser_offs.raw(), deser_counts.raw());
        }
        else {
            finish nativeAlltoall(id, role, src, src_off, dst, dst_off, count);
        }
    }

    private static def nativeAlltoall[T](id:Int, role:Int, src:IndexedMemoryChunk[T], src_off:Int, dst:IndexedMemoryChunk[T], dst_off:Int, count:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeAllToAll(id, role, src, src_off, dst, dst_off, count);")
        @Native("c++", "x10rt_alltoall(id, role, &src->raw()[src_off], &dst->raw()[dst_off], sizeof(TPMGL(T)), count, x10aux::coll_handler, x10aux::coll_enter());") {}
    }

    public def alltoall[T] (role:Int, src:Array[T]) {
        assert(src != null);
    	assert(src.size % size() == 0);
    	val dst_raw = IndexedMemoryChunk.allocateUninitialized[T](src.size);
        alltoall(id, role, src.raw(), 0, dst_raw, 0, src.size / size());
        return new Array[T](dst_raw);
    }
    
    public def alltoallv[T] (role:Int, src:Array[T], src_offs:Array[Int], src_counts:Array[Int], dst:Array[T], dst_offs:Array[Int], dst_counts:Array[Int]) : void {
        alltoallv(id, role, src.raw(), src_offs.raw(), src_counts.raw(), dst.raw(), dst_offs.raw(), dst_counts.raw());
    }

    public def alltoallv[T] (id:Int, role:Int, src:IndexedMemoryChunk[T], src_offs:IndexedMemoryChunk[Int], src_counts:IndexedMemoryChunk[Int], dst:IndexedMemoryChunk[T], dst_offs:IndexedMemoryChunk[Int], dst_counts:IndexedMemoryChunk[Int]) : void {
        if (needToSerialize[T]()) {
            val places = size();
            val ser_offs = new Array[Int](places);
            val ser_counts = new Array[Int](places);
            val ser_src = ParallelSerialization.serialize(src, src_offs, src_counts, ser_offs.raw(), ser_counts.raw());
            val deser_counts = new Array[Int](places);
            finish nativeAlltoall(id, role, ser_counts.raw(), 0, deser_counts.raw(), 0, 1);
            val deser_offs = new Array[Int](places + 1);
            deser_offs(0) = 0;
            for (i in 0..(places-1)) deser_offs(i+1) = deser_counts(i) + deser_offs(i);
            val deser_dst = new Array[Byte](deser_offs(places));
            finish nativeAlltoallv(id, role, ser_src, ser_offs.raw(), ser_counts.raw(), deser_dst.raw(), deser_offs.raw(), deser_counts.raw());
            ParallelSerialization.deserialize(dst, dst_offs, dst_counts, deser_dst.raw(), deser_offs.raw(), deser_counts.raw());
        }
        else {
            finish nativeAlltoallv(id, role, src, src_offs, src_counts, dst, dst_offs, dst_counts);
        }
    }

    private static def nativeAlltoallv[T] (id:Int, role:Int, src:IndexedMemoryChunk[T], src_offs:IndexedMemoryChunk[Int], src_counts:IndexedMemoryChunk[Int], dst:IndexedMemoryChunk[T], dst_offs:IndexedMemoryChunk[Int], dst_counts:IndexedMemoryChunk[Int]) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeAllToAllV(id, role, src, src_offs, src_counts, dst, dst_offs, dst_counts);")
        @Native("c++", "x10rt_alltoallv(id, role, src->raw(), src_offs->raw(), src_counts->raw(), dst->raw(), dst_offs->raw(), dst_counts->raw(), sizeof(TPMGL(T)), x10aux::coll_handler, x10aux::coll_enter());") {}
    }

    public def alltoallv[T] (role:Int, src:Array[T], src_offs:Array[Int], src_counts:Array[Int], dst_offs:Array[Int], dst_counts:Array[Int]) {
        assert(src != null);
        assert(src_counts.size == size());
        assert(src_offs.size == size());
        assert(dst_counts.size == size());
        assert(dst_offs.size == size());
        assert(size() > 0);
        val dst = new Array[T](IndexedMemoryChunk.allocateUninitialized[T](dst_counts.reduce((x:Int, y:Int)=>x+y, 0)));
        alltoallv(role, src, src_offs, src_counts, dst, dst_offs, dst_counts);
        return dst;
    }

    /** Indicates the operation to perform when reducing. */
    public static val ADD  = 0;
    /** Indicates the operation to perform when reducing. */
    public static val MUL  = 1;
    /** Indicates the operation to perform when reducing. */
    public static val AND  = 3;
    /** Indicates the operation to perform when reducing. */
    public static val OR   = 4;
    /** Indicates the operation to perform when reducing. */
    public static val XOR  = 5;
    /** Indicates the operation to perform when reducing. */
    public static val BAND = 6;
    /** Indicates the operation to perform when reducing. */
    public static val BOR  = 7;
    /** Indicates the operation to perform when reducing. */
    public static val BXOR = 8;
    /** Indicates the operation to perform when reducing. */
    public static val MAX  = 9;
    /** Indicates the operation to perform when reducing. */
    public static val MIN  = 10;

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
    	if(role == root) assert(dst != null);
        finish nativeReduce(id, role, root, src.raw(), src_off, getRawOrDummyChunk(dst), dst_off, count, op);
    }

    private static def nativeReduce[T](id:Int, role:Int, root:Int, src:IndexedMemoryChunk[T], src_off:Int, dst:IndexedMemoryChunk[T], dst_off:Int, count:Int, op:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeReduce(id, role, root, src, src_off, dst, dst_off, count, op);")
        @Native("c++", "x10rt_reduce(id, role, root, &src->raw()[src_off], &dst->raw()[dst_off], (x10rt_red_op_type)op, x10rt_get_red_type<TPMGL(T)>(), count, x10aux::coll_handler, x10aux::coll_enter());") {}
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
        val new_size = nativeSize (new_id);
    	 val dst_raw = IndexedMemoryChunk.allocateUninitialized[Int](new_size);
        nativeMembers(new_id, dst_raw);
        
        return new Team(new_id, new Array[Place](new_size, (i :Int) => Place(dst_raw(i))));
    }

    private static def nativeSplit(id:Int, role:Int, color:Int, new_role:Int, result:IndexedMemoryChunk[Int]) : void {
        Runtime.increaseParallelism();
        @Native("java", "x10.x10rt.TeamSupport.nativeSplit(id, role, color, new_role, result);")
        @Native("c++", "x10rt_team_split(id, role, color, new_role, x10aux::coll_handler2, x10aux::coll_enter2(result->raw()));") {}
        Runtime.decreaseParallelism(1);
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

    public def toString() = "Team(" + this.id + "," + this.places() +  ")";
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
        assert(role != root || src != null);
        assert(role != root || src.size % team_size == 0);
        val src_size = role == root ? src.size : Zero.get[Int]();
        val count = bcast1(role, root, src_size / team_size);
        debugln("scatter", "count: " + count);
        return scatter(role, root, src, count);
    }

    public def scatterv[T] (role:Int, root:Int, src:Array[T], src_counts:Array[Int], src_offs:Array[Int]) {
        assert(role != root || src_counts != null);
        assert(role != root || src_offs != null);
        val team_size = size();
        assert(role != root || src_counts.size == team_size);
        assert(role != root || src_offs.size == team_size);
        val dst_count = scatter(role, root, src_counts, 1)(0);
        debugln("scatterv", "dst_count: " + dst_count);
        return scatterv(role, root, src, src_counts, src_offs, dst_count);
    }

    public def scatterv[T] (role:Int, root:Int, src:Array[T], src_counts:Array[Int]) {
        assert(role != root || src_counts != null);
        val src_offs : Array[Int] = role == root ? countsToOffs(src_counts as Array[Int](1)) : null;
        debugln("scatterv", "src_offs: " +  src_offs);
        return scatterv[T](role, root, src, src_counts, src_offs);
    }

    public def scatterv[T] (role:Int, root:Int, src:Array[Array[T](1)](1)) {
        if (role == root) {
            assert(src != null);
            val flatten_src_tuple = flatten(src);
            val flatten_src = flatten_src_tuple.first;
            val src_offs = flatten_src_tuple.second.first;
            val src_sizes = flatten_src_tuple.second.second;
            debugln("scatterv", "flatten_src_tuple: " + flatten_src_tuple);
            return scatterv[T](role, root, flatten_src, src_sizes, src_offs);
        } else {
            debugln("scatterv", "non root");
            return scatterv[T](role, root, null, null, null);
        }
    }

    public def gatherv[T] (role:Int, root:Int, src:Array[T](1)) {
        assert(src != null);
        val src_size = (role == root) ? src.size : 0;
        val dst_counts = gather1[Int](role, root, src_size);
        debugln("gatherv", "dst_counts: " + dst_counts);
        return gatherv[T](role, root, src, dst_counts);
    }

    public def bcast[T] (role:Int, root:Int, src:Array[T]) {
        assert(role != root || src != null);
        val src_size = (role == root) ? src.size : 0;
        val count = bcast1(role, root, src_size);
        debugln("bcast", "count: " + count);
        bcast(role, root, src, count);
    }

    public def allgatherv[T] (role:Int, src:Array[T]) {
        assert(src != null);
        val dst_counts = allgather1(role, src.size as Int);
        val dst_offs = countsToOffs(dst_counts);
        debugln("allgatherv", "dst_counts: " + dst_counts);
        debugln("allgatherv", "dst_offs: " + dst_offs);

        return allgatherv[T](role, src, dst_offs, dst_counts);
    }

    public def alltoallvWithBreakdown[T] (role:Int, src:Array[T], src_offs:Array[Int], src_counts:Array[Int]) : Pair[Array[T](1),Array[Int](1)] {
        assert(src != null);
        assert(src_offs != null);
        assert(src_counts != null);
        val dst_counts = alltoall(role, src_counts);
        val dst_offs = countsToOffs(dst_counts);
        val dst = alltoallv[T](role, src, src_offs, src_counts, dst_offs, dst_counts);
        debugln("alltoallvWithBreakdown", "dst_counts: " + dst_counts);
        debugln("alltoallvWithBreakdown", "dst_offs: " + dst_offs);
        debugln("alltoallvWithBreakdown", "dst: " + dst);
        return Pair[Array[T](1),Array[Int](1)](dst, dst_counts);
    }

    public def alltoallvWithBreakdown[T] (role:Int, src:Array[Array[T](1)](1)) : Pair[Array[T](1),Array[Int](1)] {
        assert(src != null);
        val flatten_src_tuple = flatten(src);
        val flatten_src = flatten_src_tuple.first;
        val src_offs = flatten_src_tuple.second.first;
        val src_sizes = flatten_src_tuple.second.second;
        debugln("alltoallvWithBreakdown", "src_counts: " + src_sizes);
        debugln("alltoallvWithBreakdown", "src_offs: " + src_offs);
        debugln("alltoallvWithBreakdown", "flatten_src: " + flatten_src);
        return alltoallvWithBreakdown(role, flatten_src, src_offs, src_sizes);
    }

    public def alltoallv[T] (role:Int, src:Array[T], src_offs:Array[Int], src_counts:Array[Int]) {
        assert(src != null);
        assert(src_offs != null);
        assert(src_counts != null);
        val dst_counts = alltoall(role, src_counts);
        val dst_offs = countsToOffs(dst_counts);
        val dst = alltoallv[T](role, src, src_offs, src_counts, dst_offs, dst_counts);
        debugln("alltoallv", "dst_counts: " + dst_counts);
        debugln("alltoallv", "dst_offs: " + dst_offs);
        debugln("alltoallv", "dst: " + dst);
        return dst;
    }

    public def alltoallv[T] (role:Int, src:Array[T], src_counts:Array[Int](1)) {
        assert(src != null);
        assert(src_counts != null);
        val src_offs = countsToOffs(src_counts);
        debugln("alltoallv", "src_offs: " + src_offs);
        return alltoallv[T](role, src, src_offs, src_counts);
    }

    public def alltoallv[T] (role:Int, src:Array[Array[T](1)](1)) {
        assert(src != null);
        val flatten_src_tuple = flatten(src);
        val flatten_src = flatten_src_tuple.first;
        val src_offs = flatten_src_tuple.second.first;
        val src_sizes = flatten_src_tuple.second.second;
        debugln("alltoallv", "src_counts: " + src_sizes);
        debugln("alltoallv", "src_offs: " + src_offs);
        debugln("alltoallv", "flatten_src: " + flatten_src);
        return alltoallv(role, flatten_src, src_offs, src_sizes);
    }

    private static val OPT_REMOTE_OP = 0;
    private static val OPT_COLLECTIVES = 1;
    private static val OPT_COLLECTIVES_APPEND = 2;

    private static def nativeSupports (opt:Int) : Int {
        @Native("java", "return x10.x10rt.TeamSupport.nativeSize(opt);")
        @Native("c++", "return (x10_int)x10rt_supports(static_cast<x10rt_opt>(opt));") { return -1; }
    }
}

// vim: shiftwidth=4:tabstop=4:expandtab
