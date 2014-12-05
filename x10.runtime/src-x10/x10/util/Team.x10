/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2013.
 */

package x10.util;

import x10.compiler.Pragma;
import x10.compiler.Inline;
import x10.compiler.Native;

import x10.compiler.NativeRep;
import x10.compiler.StackAllocate;
import x10.lang.Zero;

import x10.array.OrderedPlaceGroup;

import x10.util.concurrent.AtomicInteger;
import x10.util.concurrent.Lock;
import x10.util.Pair;
import x10.compiler.Uncounted;
import x10.compiler.Pragma;

/** Interface to low level collective operations, using the Rail API.  
 * A team is a collection of activities that work together by simultaneously 
 * doing 'collective operations', expressed as calls to methods in the Team struct.
 * All methods are blocking operations.

 * Each member of the team identifies itself using the 'role' integer, which is a value
 * from 0 to team.size() - 1.  Each member can only live at a particular place,
 * which is indicated by the user when the Team is created.

 */
public struct Team {
    private static struct DoubleIdx(value:Double, idx:Int) {}
    private static val DEBUG:Boolean = true;
    private static val DEBUGINTERNALS:Boolean = true;
    
    // on native X10, probe is faster, but sleep works too.
    // on Managed X10, probe sometimes deadlocks, so sleep is required
    // TODO: Figure out why probe doesn't work on Managed X10
    @Native("java", "false")
    @Native("c++", "false") // was true
    public static native def useProbeNotSleep():Boolean;

    private static isDebug = System.getenv().containsKey("X10_TEAM_DEBUG");
	public static @Inline def debugln(pkg:String, str: String) {
        if (isDebug) {
		//@Ifdef("DEBUGPRINT")
			Console.OUT.println("" + Timer.milliTime() + ":Place " + here.id + ":Worker " + Runtime.workerId() + ":" + pkg + ": " + str);
			Console.OUT.flush();
		}
	}

    /** Returns the places of the team.
     */
    public def places() {
    	val numMembers = size();
    	val membersimc = new Rail[Int](numMembers);
    	nativeMembers(id, membersimc);
    	return new Rail[Place](numMembers,  (i :Long) => Place(membersimc(i)));

    	////if(members == null) setupMembers();
        //val members = new RailBuilder[Place]();
        //for (p in Team.state(id).places) {
        //    members.add(p);
        //}
    	//return members.result();
    }

    /** Returns the role of here
     */
    public def role() : Rail[Int] {
    	return role(placeGroup(), here);

    //	//if(members == null) setupMembers();
    //	return role(Team.state(id).places, here);
    }
    public def role(place:Place) : Rail[Int] = {
    	return role(placeGroup(), place);

        //return role(Team.state(id).places, place);
    }
    
    private static def role(places:PlaceGroup, place:Place) {
        val role = new RailBuilder[Int]();
        //for ([p] in places) {
        var i : Int = 0n;
        for(p in places){
            //if (places(p) == place)
            if (p == place)
                //role.add(p as Int);
                role.add(i as Int);
            i++;
        }
        return role.result();    
    }

    /** Returns the PlaceGroup of the places of the team.
     */
    public def placeGroup() : PlaceGroup = {
        return new OrderedPlaceGroup(places());
        //return new SparsePlaceGroup(places());

//        //Console.OUT.println(""+id);
//        //Console.OUT.flush();
//        return Team.state(id).places;
//        //return Team.state(0).places;
    }

    /** Returns the place corresponding to the given role.
     * @param role Our role in this team
     */
    public def place(role:Int) : Place = places()(role);

    private static def nativeMembers(id:Int, result:Rail[Int]) : void {
        //@Native("java", "x10.x10rt.TeamSupport.nativeSplit(id, role, color, new_role, result);")
        @Native("c++", "x10rt_team_members(id, (x10rt_place*)result->raw);") {}
    }

    private def setupMembers() {
    }

//<<<<<<< HEAD
//    private static isDebug = System.getenv().containsKey("X10_TEAM_DEBUG");
//	public static @Inline def debugln(pkg:String, str: String) {
//        if (isDebug) {
//		//@Ifdef("DEBUGPRINT")
//			Console.OUT.println("" + Timer.milliTime() + ":Place " + here.id + ":Worker " + Runtime.workerId() + ":" + pkg + ": " + str);
//			Console.OUT.flush();
//		}
//	}
//
//    /** A team that has one member at each place.
//     */
//    public static WORLD = new Team(0, new Array[Place](PlaceGroup.WORLD.numPlaces(), (i:Int)=>PlaceGroup.WORLD(i)));
//
//    /** The underlying representation of a team's identity.
//     */
//    private id: Int;
//    transient private var members: Array[Place](1);
//    transient private var roleHere: Array[Int](1);
//
//    private static def nativeMembers(id:Int, result:IndexedMemoryChunk[Int]) : void {
//        //@Native("java", "x10.x10rt.TeamSupport.nativeSplit(id, role, color, new_role, result);")
//        @Native("c++", "x10rt_team_members(id, (x10rt_place*)result->raw());") {}
//    }
//
//    private static def role(places:Array[Place](1), place:Place) {
//        val role = new ArrayBuilder[Int]();
//        for ([p] in places) {
//            if (places(p) == place)
//                role.add(p);
//        }
//        return role.result();    
//    }
//    
//    private def setupMembers() {
//    	val numMembers = size();
//    	val membersimc = IndexedMemoryChunk.allocateUninitialized[Int](numMembers);
//    	nativeMembers(id, membersimc);
//    	members = new Array[Place](numMembers,  (i :Int) => Place(membersimc(i)));
//    	roleHere = role(members, here);
//    }
//
//    /** Returns the id of the team.
//     */
//    public def id() = id;
//
//    /** Returns the places of the team.
//     */
//    public def places() {
//    	if(members == null) setupMembers();
//    	return members;
//    }
//
//    /** Returns the role of here
//     */
//    public def role() : Array[Int](1) {
//    	if(members == null) setupMembers();
//    	return roleHere;
//    }
//
//    /** Returns the PlaceGroup of the places of the team.
//     */
//    public def placeGroup() : PlaceGroup = {
//        return new OrderedPlaceGroup(places());
//    }
//
//    /** Returns the place corresponding to the given role.
//     * @param role Our role in this team
//     */
//    public def place(role:Int) : Place = places()(role);
//
//    /** Returns the role corresponding to the given place.
//     * @param place Place in this team
//     */
//    public def role(place:Place) : Array[Int](1) = {
//        return role(places(), place);
//    }
//    
//    private def this (id:Int, places:Array[Place](1)) {
//        val pg = new OrderedPlaceGroup(places);
//        this.id = id;
//        members = places;
//        roleHere = role(members, here);
//    }
//
//    /** Create a team by defining the place where each member lives.  This would usually be called before creating an async for each member of the team.
//     * @param places The place of each member
//     */
//    public def this (places :Array[Place](1)) {
//       val result = IndexedMemoryChunk.allocateUninitialized[Int](1);
//       finish nativeMake(places.raw(), places.size, result);
//       id = result(0);
//    	members = new Array[Place](places);
//    	roleHere = role(members, here);
//    }
//
//    private static def nativeMake (places:IndexedMemoryChunk[Place], count:Int, result:IndexedMemoryChunk[Int]) : void {
//        Runtime.increaseParallelism();
//        @Native("java", "x10.x10rt.TeamSupport.nativeMake(places, count, result);")
//    	@Native("c++", "x10rt_team_new(count, (x10rt_place*)places->raw(), x10aux::coll_handler2, x10aux::coll_enter2(result->raw()));") {}
//        Runtime.decreaseParallelism(1);
//=======
    /** A team that has one member at each place. */
    public static val WORLD = Team(0n, PlaceGroup.WORLD, here.id());
    //public static val WORLD = Team(PlaceGroup.WORLD);
    
    // TODO: the role argument is not really needed, and can be buried in lower layers, 
    // but BG/P is difficult to modify so we need to track it for now
    private static val roles:GrowableRail[Int] = new GrowableRail[Int](); // only used with native collectives
    private static val state:GrowableRail[LocalTeamState] = new GrowableRail[LocalTeamState](); // only used with X10 emulated collectives

    private val collectiveSupportLevel:Int; // what level of collectives are supported
    // these values correspond to x10rt_types:x10rt_coll_support
    private static val X10RT_COLL_NOCOLLECTIVES:Int = 0n;
    private static val X10RT_COLL_BARRIERONLY:Int = 1n;
    private static val X10RT_COLL_ALLBLOCKINGCOLLECTIVES:Int = 2n;
    private static val X10RT_COLL_NONBLOCKINGBARRIER:Int = 3n;
    private static val X10RT_COLL_ALLNONBLOCKINGCOLLECTIVES:Int = 4n;
    
    private val id:Int; // team ID
    public def id() = id;
    
    // this constructor is intended to be called at all places of a split, at the same time.
    private def this (id:Int, places:PlaceGroup, role:Long) {
    	this.id = id;
        collectiveSupportLevel = nativeCollectiveSupport();
        if (DEBUG) Runtime.println(here + " reported native collective support level of " + collectiveSupportLevel);
        if (collectiveSupportLevel > X10RT_COLL_NOCOLLECTIVES) {
            if (Team.roles.capacity() <= id){ // TODO move this check into the GrowableRail.grow() method
                Team.roles.grow(id+1);
            }
            while (Team.roles.size() < id){
                Team.roles.add(-1n); // I am not a member of this team id.  Insert a dummy value.
            }
            Team.roles(id) = role as Int;
            if (DEBUG) Runtime.println(here + " created native team "+id);
    	}
        if (collectiveSupportLevel < X10RT_COLL_ALLNONBLOCKINGCOLLECTIVES) {
            if (DEBUG) Runtime.println(here + " creating our own team "+id);
            if (Team.state.capacity() <= id){ // TODO move this check into the GrowableRail.grow() method
                Team.state.grow(id+1);
            }
            while (Team.state.size() < id){
                Team.state.add(null); // I am not a member of this team id.  Insert a dummy value.
            }
            val teamState = new LocalTeamState(places, id, places.indexOf(here));
        if(id == 0n){
            Team.state(id) = teamState;
        }else{
            atomic { Team.state(id) = teamState; }
            teamState.init();
        }
            if (DEBUG) Runtime.println(here + " created our own team "+id);
    	}
    }

    /** Create a team by defining the place where each member lives.
     * Unlike most methods on Team, this is called by only ONE place, not all places
     * @param places The place of each member in the team
     */
    public def this (places:PlaceGroup) {
        if (DEBUG) Runtime.println(here + " creating new team ");
        collectiveSupportLevel = nativeCollectiveSupport();
        if (DEBUG) Runtime.println(here + " reported native collective support level of " + collectiveSupportLevel);
	    if (collectiveSupportLevel > X10RT_COLL_NOCOLLECTIVES) {
	        val result = new Rail[Int](1);
	        val count = places.size();
	        // CRITICAL!! placeRail is a Rail of Int because in x10rt "x10rt_place" is 32bits
	        val placeRail = new Rail[Int](count);
	        for (var i:Long=0; i<count; i++)
	            placeRail(i) = places(i).id() as Int;
	        finish nativeMake(placeRail, count as Int, result);
	        this.id = result(0);
	        
	        // team created - fill in the role at all places
	        val teamidcopy:Long = this.id as Long;
	        PlaceGroup.WORLD.broadcastFlat(()=>{
	            if (Team.roles.capacity() <= teamidcopy) // TODO move this check into the GrowableRail.grow() method
	                Team.roles.grow(teamidcopy+1);
	            while (Team.roles.size() < teamidcopy)
	                Team.roles.add(-1n); // I am not a member of this team id.  Insert a dummy value.
	       	    Team.roles(teamidcopy) = places.indexOf(here) as Int;
	        });
	    }
	    else
	    	this.id = Team.state.size() as Int; // id is determined by the number of pre-defined places
	    if (DEBUG) Runtime.println(here + " new team ID is "+this.id);
	    if (collectiveSupportLevel < X10RT_COLL_ALLNONBLOCKINGCOLLECTIVES) {
            val teamidcopy = this.id;
            PlaceGroup.WORLD.broadcastFlat(()=>{
                if (Team.state.capacity() <= teamidcopy)
                    Team.state.grow(teamidcopy+1);
                while (Team.state.size() < teamidcopy)
                    Team.state.add(null); // I am not a member of this team id.  Insert a dummy value.
                Team.state(teamidcopy) = new LocalTeamState(places, teamidcopy, places.indexOf(here));
                Team.state(teamidcopy).init();
            });
	    }
    }

    private static def nativeMake (places:Rail[Int], count:Int, result:Rail[Int]) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeMake(places, count, result);")
    	@Native("c++", "x10rt_team_new(count, (x10rt_place*)places->raw, ::x10aux::coll_handler2, ::x10aux::coll_enter2(result->raw));") {}
    }
    
    private static def nativeCollectiveSupport() : Int {
    	@Native("java", "return x10.x10rt.X10RT.collectiveSupport();")
    	@Native("c++", "return x10rt_coll_support();") { return -1n; }
//>>>>>>> mergeSvn
    }

    /** Returns the number of places in the team.
     */
    public def size () : Long {
    	if (collectiveSupportLevel >= X10RT_COLL_ALLNONBLOCKINGCOLLECTIVES)
    	    return nativeSize(id);
    	else
    	    return Team.state(id).places.size();
    }

    private static def nativeSize (id:Int) : Int {
        @Native("java", "return x10.x10rt.TeamSupport.nativeSize(id);")
        @Native("c++", "return (x10_int)x10rt_team_sz(id);") { return -1n; }
    }

    public def needToSerialize[T] () : Boolean = nativeNeedToSerialize[T]();

    private static def nativeNeedToSerialize[T] () : Boolean {
        @Native("c++", "return x10aux::getRTT<TPMGL(T) >()->containsPtrs;") { return false; }
    }

    /** Blocks until all team members have reached the barrier.
     */
    public def barrier () : void {
    	if (collectiveSupportLevel >= X10RT_COLL_NONBLOCKINGBARRIER) {
            if (DEBUG) Runtime.println(here + " entering native barrier on team "+id);
            finish nativeBarrier(id, (id==0n?here.id() as Int:Team.roles(id)));
    	}
    	else {
            if (DEBUG) Runtime.println(here + " entering Team.x10 barrier on team "+id);
    	    state(id).collective_impl[Int](LocalTeamState.COLL_BARRIER, 0, null, 0, null, 0, 0, 0n);
    	}
        if (DEBUG) Runtime.println(here + " leaving barrier of team "+id);
    }
    
    public def nativeBarrier () : void {
    	finish nativeBarrier(id, (id==0n?here.id() as Int:Team.roles(id)));
    }

    private static def nativeBarrier (id:Int, role:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeBarrier(id, role);")
        @Native("c++", "x10rt_barrier(id, role, ::x10aux::coll_handler, ::x10aux::coll_enter());") {}
    }
    
    /** Blocks until all members have received their part of root's array.
     * Each member receives a contiguous and distinct portion of the src array.
     * src should be structured so that the portions are sorted in ascending
     * order, e.g., the first member gets the portion at offset src_off of sbuf, and the
     * last member gets the last portion.
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
    public def scatter[T] (root:Int, src:Rail[T], src_off:Long, dst:Rail[T], dst_off:Long, count:Long) : void {
        if (collectiveSupportLevel == X10RT_COLL_ALLNONBLOCKINGCOLLECTIVES)
            finish nativeScatter(id, id==0n?here.id() as Int:Team.roles(id), root, src, src_off as Int, dst, dst_off as Int, count as Int);
        else if (collectiveSupportLevel == X10RT_COLL_ALLBLOCKINGCOLLECTIVES || collectiveSupportLevel == X10RT_COLL_NONBLOCKINGBARRIER) {
            barrier();
            finish nativeScatter(id, id==0n?here.id() as Int:Team.roles(id), root, src, src_off as Int, dst, dst_off as Int, count as Int);
        }
    	else
    	    state(id).collective_impl[T](LocalTeamState.COLL_SCATTER, root as Long, src, src_off, dst, dst_off, count, 0n);
    }

    private static def nativeScatter[T] (id:Int, role:Int, root:Int, src:Rail[T], src_off:Int, dst:Rail[T], dst_off:Int, count:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeScatter(id, role, root, src, src_off, dst, dst_off, count);")
        @Native("c++", "x10rt_scatter(id, role, root, &src->raw[src_off], &dst->raw[dst_off], sizeof(TPMGL(T)), count, ::x10aux::coll_handler, ::x10aux::coll_enter());") {}
    }
    
    //private static def dummyChunk[T]() { return IndexedMemoryChunk.allocateUninitialized[T](0); }; 

    //private static def getRawOrDummyChunk[T](arr: Array[T]) {
    //    if (arr == null)
    //        return dummyChunk[T]();
    //    else
    //        return arr.raw();
    //};
    //

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
    public def scatter[T] (role:Int, root:Int, src:Rail[T], count:Int){T haszero}{
    	assert (role != root || src != null);
        val dst_raw = new Rail[T](count);
        scatter(root, src, 0, dst_raw, 0, count as Long);
        return new Rail[T](dst_raw);
    }


//    /** Almost same as scatter except for permitting messages to have different sizes.
//     *
//     * @see #scatter
//
//     * @param role Our role in the team
//     *
//     * @param root The member who is supplying the data
//     *
//     * @param src The data that will be sent (will only be used by the root
//     * member)
//     *
//     * @param src_offs The offsets into src at which to start reading
//     *
//     * @param src_counts The numbers of elements being sent
//     * 
//     * @param dst The rail into which the data will be received for this member
//     *
//     * @param dst_off The offset into dst at which to start writing
//     *
//     * @param dst_count The numbers of elements being received
//     */
//    public def scatterv[T] (role:Int, root:Int, src:Array[T], src_offs:Array[Int], src_counts:Array[Int], dst:Array[T], dst_off:Int, dst_count:Int) : void {
//        scatterv(id, role, root, getRawOrDummyChunk(src), getRawOrDummyChunk(src_offs), getRawOrDummyChunk(src_counts), getRawOrDummyChunk(dst), dst_off, dst_count);
//    }
//
//    public def scatterv[T] (id:Int, role:Int, root:Int, src:IndexedMemoryChunk[T], src_offs:IndexedMemoryChunk[Int], src_counts:IndexedMemoryChunk[Int], dst:IndexedMemoryChunk[T], dst_off:Int, dst_count:Int) : void {
//        if (needToSerialize[T]()) {
//            if (role == root) {
//                val places = size();
//                val ser_offs = new Array[Int](places);
//                val ser_counts = new Array[Int](places);
//                val ser_src = ParallelSerialization.serialize(src, src_offs, src_counts, ser_offs.raw(), ser_counts.raw());
//                val deser_counts = scatter[Int](role, root, ser_counts, 1);
//                val deser_dst = new Array[Byte](deser_counts(0));
//                finish nativeScatterv(id, role, root, ser_src, ser_offs.raw(), ser_counts.raw(), deser_dst.raw(), 0, deser_counts(0));
//                ParallelSerialization.deserialize(dst, dst_off, dst_count, deser_dst.raw(), 0, deser_counts(0));
//            }
//            else {
//                val deser_counts = scatter[Int](role, root, null, 1);
//                val deser_dst = new Array[Byte](deser_counts(0));
//                finish nativeScatterv(id, role, root, dummyChunk[Byte](), dummyChunk[Int](), dummyChunk[Int](), deser_dst.raw(), 0, deser_counts(0));
//                ParallelSerialization.deserialize(dst, dst_off, dst_count, deser_dst.raw(), 0, deser_counts(0));
//            }
//        }
//        else {
//            finish nativeScatterv(id, role, root, src, src_offs, src_counts, dst, dst_off, dst_count);
//        }
//    }
//
//    private static def nativeScatterv[T] (id:Int, role:Int, root:Int, src:IndexedMemoryChunk[T], src_offs:IndexedMemoryChunk[Int], src_counts:IndexedMemoryChunk[Int], dst:IndexedMemoryChunk[T], dst_off:Int, dst_count:Int) : void {
//        @Native("java", "x10.x10rt.TeamSupport.nativeScatterV(id, role, root, src, src_offs, src_counts, dst, dst_off, dst_count);")
//        @Native("c++", "x10rt_scatterv(id, role, root, src->raw(), src_offs->raw(), src_counts->raw(), &dst->raw()[dst_off], dst_count, sizeof(TPMGL(T)), x10aux::coll_handler, x10aux::coll_enter());") {}
//    }
//    
//    /** Almost same as scatter except for permitting messages to have different sizes.
//     * The received array is structured so that the portions are sorted in ascending
//     * order, e.g., the first member gets the portion at the head of sbuf, and the
//     * last member gets the last portion.
//     *
//     * @see #scatterv
//     *
//     * @param role Our role in the team
//     *
//     * @param root The member who is supplying the data
//     *
//     * @param src The data that will be sent 
//     *
//     * @param src_offs The offsets into src at which to start reading
//     *
//     * @param src_counts The numbers of elements being sent
//     *
//     * @param dst_count The numbers of elements being received
//     *
//     * @return received array
//     */
//    public def scatterv[T] (role:Int, root:Int, src:Array[T], src_counts:Array[Int], src_offs:Array[Int], dst_count:Int) {
//        assert(role != root || src_counts.size == size());
//        assert(role != root || src_offs.size == size());
//        val dst = new Array[T](IndexedMemoryChunk.allocateUninitialized[T](dst_count));
//        scatterv(role, root, src, src_offs, src_counts, dst, 0, dst_count);
//        return dst;
//    }
//
//    /** Blocks until the root have received each part of all member's array.
//     * Each member sends a contiguous and distinct portion of the src array.
//     * dst will be structured so that the portions are sorted in ascending
//     * order, e.g., the first member gets the portion at offset src_off of sbuf, and the
//     * last member gets the last portion.
//     *
//     * @param role Our role in the team
//     *
//     * @param root The member who is receiving the data
//     *
//     * @param src The data that will be sent 
//     *
//     * @param src_off The offset into src at which to start reading
//     *
//     * @param dst The rail into which the data will be received (will only be used by the root
//     * member)
//     *
//     * @param dst_off The offset into dst at which to start writing (will only be used by the root
//     * member)
//     *
//     * @param count The number of elements being transferred
//     */
//    public def gather[T] (role:Int, root:Int, src:Array[T], src_off:Int, dst:Array[T], dst_off:Int, count:Int) : void {
//        gather(id, role, root, src.raw(), src_off, getRawOrDummyChunk(dst), dst_off, count);
//    }
//
//    public def gather[T] (id:Int, role:Int, root:Int, src:IndexedMemoryChunk[T], src_off:Int, dst:IndexedMemoryChunk[T], dst_off:Int, count:Int) : void {
//        if (needToSerialize[T]()) {
//            if (role == root) {
//                val places = size();
//                val ser_src = ParallelSerialization.serialize(src, src_off, count);
//                val ser_count = ser_src.length();
//                val deser_counts = gather1[Int](role, root, ser_count);
//                val deser_offs = new Array[Int](places+1);
//                deser_offs(0) = 0;
//                for (i in 0..(places-1)) deser_offs(i+1) = deser_counts(i) + deser_offs(i);
//                val deser_dst = new Array[Byte](deser_offs(places));
//                finish nativeGatherv(id, role, root, ser_src, 0, ser_count, deser_dst.raw(), deser_offs.raw(), deser_counts.raw());
//                val dst_counts = new Array[Int](places, count);
//                val dst_offs = new Array[Int](places, (i :Int) => i * count);
//                ParallelSerialization.deserialize(dst, dst_offs.raw(), dst_counts.raw(), deser_dst.raw(), deser_offs.raw(), deser_counts.raw());
//            }
//            else {
//                val ser_src = ParallelSerialization.serialize(src, src_off, count);
//                val ser_count = ser_src.length();
//                val deser_counts = gather1[Int](role, root, ser_count);
//                finish nativeGatherv(id, role, root, ser_src, 0, ser_count, dummyChunk[Byte](), dummyChunk[Int](), dummyChunk[Int]());
//            }
//        }
//        else {
//            finish nativeGather(id, role, root, src, src_off, dst, dst_off, count);
//        }
//    }
//
//    private static def nativeGather[T] (id:Int, role:Int, root:Int, src:IndexedMemoryChunk[T], src_off:Int, dst:IndexedMemoryChunk[T], dst_off:Int, count:Int) : void {
//        @Native("java", "x10.x10rt.TeamSupport.nativeGather(id, role, root, src, src_off, dst, dst_off, count);")
//        @Native("c++", "x10rt_gather(id, role, root, &src->raw()[src_off], &dst->raw()[dst_off], sizeof(TPMGL(T)), count, x10aux::coll_handler, x10aux::coll_enter());") {}
//    }
//
//    /** Gathers the given array.
//     * Blocks until the root have received each part of all member's array.
//     * Each member sends a contiguous and distinct portion of the src array.
//     * dst will be structured so that the portions are sorted in ascending
//     * order, e.g., the first member gets the portion at offset src_off of sbuf, and the
//     * last member gets the last portion.
//     *
//     * @see #gather
//     *
//     * @param role Our role in the team
//     *
//     * @param root The member who is receiving the data
//     *
//     * @param src The data that will be sent 
//     *
//     * @param count The number of elements being transferred
//     *
//     * @return received array
//     */
//    public def gather[T] (role:Int, root:Int, src:Array[T], count:Int) {
//        val dst = (role == root) ? new Array[T](IndexedMemoryChunk.allocateUninitialized[T](count * size())) : null;
//        gather(role, root, src, 0, dst, 0, count);
//        return dst;
//    }
//
//    /** Almost same as gather except that each member sends one data.
//     *
//     * @param role Our role in the team
//     *
//     * @param root The member who is receiving the data
//     *
//     * @param src The data that will be sent 
//     *
//     * @param count The number of elements being transferred
//     */
//    public def gather1[T] (role:Int, root:Int, src:T) {T haszero} : Array[T](1) {
//        val src_raw = IndexedMemoryChunk.allocateUninitialized[T](1);
//        src_raw(0) = src;
//        val dst : Array[T](1) = new Array[T](IndexedMemoryChunk.allocateUninitialized[T](role == root ? size() : 0)) ;
//        gather(role, root, new Array[T](src_raw), 0, dst, 0, 1);
//        return dst;
//    }
//
//    /** Almost same as gather except for permitting messages to have different sizes.
//     *
//     * @param role Our role in the team
//     *
//     * @param root The member who is receiving the data
//     *
//     * @param src The data that will be sent 
//     *
//     * @param src_off The offset into src at which to start reading
//     *
//     * @param src_count The numbers of elements being sent
//     * 
//     * @param dst The rail into which the data will be received (will only be used by the root
//     * member)
//     *
//     * @param dst_offs The offsets into dst at which to start writing (will only be used by the root
//     * member)
//     *
//     * @param dst_counts The numbers of elements being transferred
//     */
//
//    public def gatherv[T] (role:Int, root:Int, src:Array[T], src_off:Int, src_count:Int, dst:Array[T], dst_offs:Array[Int], dst_counts:Array[Int]) : void {
//        gatherv(id, role, root, getRawOrDummyChunk(src), src_off, src_count, getRawOrDummyChunk(dst), getRawOrDummyChunk(dst_offs), getRawOrDummyChunk(dst_counts));
//    }
//
//    public def gatherv[T] (id:Int, role:Int, root:Int, src:IndexedMemoryChunk[T], src_off:Int, src_count:Int, dst:IndexedMemoryChunk[T], dst_offs:IndexedMemoryChunk[Int], dst_counts:IndexedMemoryChunk[Int]) : void {
//        if (needToSerialize[T]()) {
//            if (role == root) {
//                val places = size();
//                val ser_src = ParallelSerialization.serialize(src, src_off, src_count);
//                val ser_count = ser_src.length();
//                val deser_counts = gather1[Int](role, root, ser_count);
//                val deser_offs = new Array[Int](places+1);
//                deser_offs(0) = 0;
//                for (i in 0..(places-1)) deser_offs(i+1) = deser_counts(i) + deser_offs(i);
//                val deser_dst = new Array[Byte](deser_offs(places));
//                finish nativeGatherv(id, role, root, ser_src, 0, ser_count, deser_dst.raw(), deser_offs.raw(), deser_counts.raw());
//                ParallelSerialization.deserialize(dst, dst_offs, dst_counts, deser_dst.raw(), deser_offs.raw(), deser_counts.raw());
//            }
//            else {
//                val ser_src = ParallelSerialization.serialize(src, src_off, src_count);
//                val ser_count = ser_src.length();
//                val deser_counts = gather1[Int](role, root, ser_count);
//                finish nativeGatherv(id, role, root, ser_src, 0, ser_count, dummyChunk[Byte](), dummyChunk[Int](), dummyChunk[Int]());
//            }
//        }
//        else {
//            finish nativeGatherv(id, role, root, src, src_off, src_count, dst, dst_offs, dst_counts);
//        }
//    }
//
//    private static def nativeGatherv[T] (id:Int, role:Int, root:Int, src:IndexedMemoryChunk[T], src_off:Int, src_count:Int, dst:IndexedMemoryChunk[T], dst_offs:IndexedMemoryChunk[Int], dst_counts:IndexedMemoryChunk[Int]) : void {
//        @Native("java", "x10.x10rt.TeamSupport.nativeGatherV(id, role, root, src, src_off, src_count, dst, dst_offs, dst_counts);")
//        @Native("c++", "x10rt_gatherv(id, role, root, &src->raw()[src_off], src_count, dst->raw(), dst_offs->raw(), dst_counts->raw(), sizeof(TPMGL(T)), x10aux::coll_handler, x10aux::coll_enter());") {}
//    }
//
//    public def gatherv[T] (role:Int, root:Int, src:Array[T], dst_offs:Array[Int], dst_counts:Array[Int] ) {
//        val dst = (role == root) ? new Array[T](IndexedMemoryChunk.allocateUninitialized[T](dst_counts.reduce((x:Int, y:Int)=>x+y, 0))) : null;
//        gatherv(role, root, src, 0, src.size, dst, dst_offs, dst_counts);
//        return dst;
//    }
//
//    private static def countsToOffs (counts:Array[Int](1)) {
//    	val acc = counts.scan((x:Int, y:Int)=> x+y, 0);
//    	return new Array[Int](counts.size, (i:Int)=>(i==0) ? 0 : acc(i-1));
//    }
//    
//    public def gatherv[T] (role:Int, root:Int, src:Array[T], dst_counts:Array[Int](1) ) {
//        if (role == root) {
//            val dst_offs = countsToOffs(dst_counts);
//            return gatherv[T](role, root, src, dst_offs, dst_counts);
//        } else {
//            return gatherv[T](role, root, src, null, null);
//        }
//    }

    /** Blocks until all members have received root's array.
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
//<<<<<<< HEAD
//    public def bcast[T] (role:Int, root:Int, src:Array[T], src_off:Int, dst:Array[T], dst_off:Int, count:Int) : void {
//        bcast(id, role, root, getRawOrDummyChunk(src), src_off, dst.raw(), dst_off, count);
//    }
//
//    public def bcast[T] (id:Int, role:Int, root:Int, src:IndexedMemoryChunk[T], src_off:Int, dst:IndexedMemoryChunk[T], dst_off:Int, count:Int) : void {
//        if (needToSerialize[T]()) {
//            if (role == root) {
//                val places = size();
//                val ser_src = ParallelSerialization.serialize(src, src_off, count);
//                val ser_count = ser_src.length();
//                val deser_count = bcast1[Int](role, root, ser_count);
//                val deser_dst = new Array[Byte](deser_count);
//                finish nativeBcast(id, role, root, ser_src, 0, deser_dst.raw(), 0, deser_count);
//                ParallelSerialization.deserialize(dst, dst_off, count, deser_dst.raw(), 0, deser_count);
//            }
//            else {
//                val deser_count = bcast1[Int](role, root, 0);
//                val deser_dst = new Array[Byte](deser_count);
//                finish nativeBcast(id, role, root, dummyChunk[Byte](), 0, deser_dst.raw(), 0, deser_count);
//                ParallelSerialization.deserialize(dst, dst_off, count, deser_dst.raw(), 0, deser_count);
//            }
//
//        }
//        else {
//            finish nativeBcast(id, role, root, src, src_off, dst, dst_off, count);
//        }
//=======
     public def bcast[T] (root:Int, src:Rail[T], src_off:Long, dst:Rail[T], dst_off:Long, count:Long) : void {
        if (collectiveSupportLevel == X10RT_COLL_ALLNONBLOCKINGCOLLECTIVES)
            finish nativeBcast(id, id==0n?here.id() as Int:Team.roles(id), root, src, src_off as Int, dst, dst_off as Int, count as Int);
        else if (collectiveSupportLevel == X10RT_COLL_ALLBLOCKINGCOLLECTIVES || collectiveSupportLevel == X10RT_COLL_NONBLOCKINGBARRIER) {
            barrier();
            finish nativeBcast(id, id==0n?here.id() as Int:Team.roles(id), root, src, src_off as Int, dst, dst_off as Int, count as Int);
        }
     	else
     	    state(id).collective_impl[T](LocalTeamState.COLL_BROADCAST, root as Long, src, src_off, dst, dst_off, count, 0n);
//>>>>>>> mergeSvn
    }

    private static def nativeBcast[T] (id:Int, role:Int, root:Int, src:Rail[T], src_off:Int, dst:Rail[T], dst_off:Int, count:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeBcast(id, role, root, src, src_off, dst, dst_off, count);")
        @Native("c++", "x10rt_bcast(id, role, root, &src->raw[src_off], &dst->raw[dst_off], sizeof(TPMGL(T)), count, ::x10aux::coll_handler, ::x10aux::coll_enter());") {}
    }
    
//    public def bcast1[T] (root:Int, src:T) : T {
//    	val src_raw = new Rail[T](1);
//    	src_raw(0) = src;
//    	val dst_raw = new Rail[T](1);
//        bcast(root, src_raw, 0, dst_raw, 0, 1);
//        return dst_raw(0);
//    }
//
//    public def bcast[T] (role:Int, root:Int, src:Rail[T], count:Int) {
//    	val dst_raw = new Rail[T](count);
//        bcast(root, src, 0, dst_raw, 0, count);
//        return new dst_raw;
//    }
//
//    public def allgather1[T] (role:Int, src:T) {
//        val src_raw = IndexedMemoryChunk.allocateUninitialized[T](1);
//        src_raw(0) = src;
//        val dst = new Array[T](IndexedMemoryChunk.allocateUninitialized[T](size()));
//        allgather(role, new Array[T](src_raw), 0, dst, 0, 1);
//        return dst;
//    }
//
//    public def allgather[T] (role:Int, src:Array[T]) {
//        val dst = new Array[T](IndexedMemoryChunk.allocateUninitialized[T](src.size * size()));
//        allgather(role, src, 0, dst, 0, src.size);
//        return dst;
//    }
//
//    public def allgather[T] (role:Int, src:Array[T], src_off:Int, dst:Array[T], dst_off:Int, count:Int) : void {
//        allgather(id, role, src.raw(), src_off, dst.raw(), dst_off, count);
//    }
//
//    public def allgather[T](id:Int, role:Int, src:IndexedMemoryChunk[T], src_off:Int, dst:IndexedMemoryChunk[T], dst_off:Int, count:Int) : void {
//        if (needToSerialize[T]()) {
//            val places = size();
//            val ser_src = ParallelSerialization.serialize(src, src_off, count);
//            val ser_count = ser_src.length();
//            val deser_counts = allgather1[Int](role, ser_count);
//            val deser_offs = new Array[Int](places + 1);
//            deser_offs(0) = 0;
//            for (i in 0..(places-1)) deser_offs(i+1) = deser_counts(i) + deser_offs(i);
//            val deser_dst = new Array[Byte](deser_offs(places));
//            finish nativeAllgatherv(id, role, ser_src, 0, ser_count, deser_dst.raw(), deser_offs.raw(), deser_counts.raw());
//            val dst_offs = new Array[Int](places, (i :Int) => i * count);
//            val dst_counts = new Array[Int](places, count);
//            ParallelSerialization.deserialize(dst, dst_offs.raw(), dst_counts.raw(), deser_dst.raw(), deser_offs.raw(), deser_counts.raw());
//        }
//        else {
//            finish nativeAllgather(id, role, src, src_off, dst, dst_off, count);
//        }
//    }
//
//    private static def nativeAllgather[T](id:Int, role:Int, src:IndexedMemoryChunk[T], src_off:Int, dst:IndexedMemoryChunk[T], dst_off:Int, count:Int) : void {
//        @Native("java", "x10.x10rt.TeamSupport.nativeAllGather(id, role, src, src_off, dst, dst_off, count);")
//        @Native("c++", "x10rt_allgather(id, role, &src->raw()[src_off], &dst->raw()[dst_off], sizeof(TPMGL(T)), count, x10aux::coll_handler, x10aux::coll_enter());") {}
//    }
//    
//    public def allgatherv[T] (role:Int, src:Array[T], dst_offs:Array[Int], dst_counts:Array[Int]) {
//        val dst = new Array[T](IndexedMemoryChunk.allocateUninitialized[T](dst_counts.reduce((x:Int, y:Int)=>x+y, 0)));
//        allgatherv(role, src, 0, src.size, dst, dst_offs, dst_counts);
//        return dst;
//    }
//
//    public def allgatherv[T] (role:Int, src:Array[T], src_off:Int, src_count:Int, dst:Array[T], dst_offs:Array[Int], dst_counts:Array[Int]) : void {
//        allgatherv(id, role, src.raw(), src_off, src_count, dst.raw(), dst_offs.raw(), dst_counts.raw());
//    }
//
//    public def allgatherv[T] (id:Int, role:Int, src:IndexedMemoryChunk[T], src_off:Int, src_count:Int, dst:IndexedMemoryChunk[T], dst_offs:IndexedMemoryChunk[Int], dst_counts:IndexedMemoryChunk[Int]) : void {
//        if (needToSerialize[T]()) {
//            val places = size();
//            val ser_src = ParallelSerialization.serialize(src, src_off, src_count);
//            val ser_count = ser_src.length();
//            val deser_counts = allgather1[Int](role, ser_count);
//            val deser_offs = new Array[Int](places + 1);
//            deser_offs(0) = 0;
//            for (i in 0..(places-1)) deser_offs(i+1) = deser_counts(i) + deser_offs(i);
//            val deser_dst = new Array[Byte](deser_offs(places));
//            finish nativeAllgatherv(id, role, ser_src, 0, ser_count, deser_dst.raw(), deser_offs.raw(), deser_counts.raw());
//            ParallelSerialization.deserialize(dst, dst_offs, dst_counts, deser_dst.raw(), deser_offs.raw(), deser_counts.raw());
//        }
//        else {
//            finish nativeAllgatherv(id, role, src, src_off, src_count, dst, dst_offs, dst_counts);
//        }
//    }
//
//    private static def nativeAllgatherv[T] (id:Int, role:Int, src:IndexedMemoryChunk[T], src_off:Int, src_count:Int, dst:IndexedMemoryChunk[T], dst_offs:IndexedMemoryChunk[Int], dst_counts:IndexedMemoryChunk[Int]) : void {
//        @Native("java", "x10.x10rt.TeamSupport.nativeAllGatherV(id, role, src, src_off, src_count, dst, dst_offs, dst_counts);")
//        @Native("c++", "x10rt_allgatherv(id, role, &src->raw()[src_off], src_count, dst->raw(), dst_offs->raw(), dst_counts->raw(), sizeof(TPMGL(T)), x10aux::coll_handler, x10aux::coll_enter());") {}
//    }

    /** Blocks until all members have received their part of each other member's array.
     * Each member receives a contiguous and distinct portion of the src array.
     * src should be structured so that the portions are sorted in ascending
     * order, e.g., the first member gets the portion at offset src_off of sbuf, and the
     * last member gets the last portion.
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
//<<<<<<< HEAD
//    public def alltoall[T] (role:Int, src:Array[T], src_off:Int, dst:Array[T], dst_off:Int, count:Int) : void {
//        alltoall(id, role, src.raw(), src_off, dst.raw(), dst_off, count);
//    }
//
//    public def alltoall[T](id:Int, role:Int, src:IndexedMemoryChunk[T], src_off:Int, dst:IndexedMemoryChunk[T], dst_off:Int, count:Int) : void {
//        if (needToSerialize[T]()) {
//            val places = size();
//            val src_counts = new Array[Int](places, count);
//            val src_offs = new Array[Int](places, (i :Int) => i * count);
//            val ser_offs = new Array[Int](places);
//            val ser_counts = new Array[Int](places);
//            val ser_src = ParallelSerialization.serialize(src, src_offs.raw(), src_counts.raw(), ser_offs.raw(), ser_counts.raw());
//            val deser_counts = new Array[Int](places);
//            finish nativeAlltoall(id, role, ser_counts.raw(), 0, deser_counts.raw(), 0, 1);
//            val deser_offs = new Array[Int](places + 1);
//            deser_offs(0) = 0;
//            for (i in 0..(places-1)) deser_offs(i+1) = deser_counts(i) + deser_offs(i);
//            val deser_dst = new Array[Byte](deser_offs(places));
//            finish nativeAlltoallv(id, role, ser_src, ser_offs.raw(), ser_counts.raw(), deser_dst.raw(), deser_offs.raw(), deser_counts.raw());
//            val dst_counts = new Array[Int](places, count);
//            val dst_offs = new Array[Int](places + 1);
//            dst_offs(0) = 0;
//            for (i in 0..(places-1)) dst_offs(i+1) = dst_counts(i) + dst_offs(i);
//            ParallelSerialization.deserialize(dst, dst_offs.raw(), dst_counts.raw(), deser_dst.raw(), deser_offs.raw(), deser_counts.raw());
//        }
//        else {
//            finish nativeAlltoall(id, role, src, src_off, dst, dst_off, count);
//        }
//=======
    public def alltoall[T] (src:Rail[T], src_off:Long, dst:Rail[T], dst_off:Long, count:Long) : void {
        if (collectiveSupportLevel == X10RT_COLL_ALLNONBLOCKINGCOLLECTIVES) {
            if (DEBUG) Runtime.println(here + " entering native alltoall of team "+id);
            finish nativeAlltoall(id, id==0n?here.id() as Int:Team.roles(id), src, src_off as Int, dst, dst_off as Int, count as Int);
        }
        else if (collectiveSupportLevel == X10RT_COLL_ALLBLOCKINGCOLLECTIVES || collectiveSupportLevel == X10RT_COLL_NONBLOCKINGBARRIER) {
            if (DEBUG) Runtime.println(here + " entering pre-alltoall barrier of team "+id);
       	    barrier();
       	    if (DEBUG) Runtime.println(here + " entering native alltoall of team "+id);
            finish nativeAlltoall(id, id==0n?here.id() as Int:Team.roles(id), src, src_off as Int, dst, dst_off as Int, count as Int);
        }
    	else {
            if (DEBUG) Runtime.println(here + " entering Team.x10 alltoall of team "+id);
    	    state(id).collective_impl[T](LocalTeamState.COLL_ALLTOALL, 0, src, src_off, dst, dst_off, count, 0n);
    	}
        if (DEBUG) Runtime.println(here + " leaving alltoall of team "+id);
//>>>>>>> mergeSvn
    }
    
    private static def nativeAlltoall[T](id:Int, role:Int, src:Rail[T], src_off:Int, dst:Rail[T], dst_off:Int, count:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeAllToAll(id, role, src, src_off, dst, dst_off, count);")
        @Native("c++", "x10rt_alltoall(id, role, &src->raw[src_off], &dst->raw[dst_off], sizeof(TPMGL(T)), count, ::x10aux::coll_handler, ::x10aux::coll_enter());") {}
    }

//    public def alltoall[T] (role:Int, src:Rail[T]){T haszero} {
//        assert(src != null);
//    	assert(src.size % size() == 0);
//    	val dst_raw = new Rail[T](src.size);//IndexedMemoryChunk.allocateUninitialized[T](src.size);
//        alltoall(id, role, src, 0, dst_raw, 0, src.size / size());
//        return new Rail[T](dst_raw);
//    }
//    
//    public def alltoallv[T] (role:Int, src:Array[T], src_offs:Array[Int], src_counts:Array[Int], dst:Array[T], dst_offs:Array[Int], dst_counts:Array[Int]) : void {
//        alltoallv(id, role, src.raw(), src_offs.raw(), src_counts.raw(), dst.raw(), dst_offs.raw(), dst_counts.raw());
//    }
//
//    public def alltoallv[T] (id:Int, role:Int, src:IndexedMemoryChunk[T], src_offs:IndexedMemoryChunk[Int], src_counts:IndexedMemoryChunk[Int], dst:IndexedMemoryChunk[T], dst_offs:IndexedMemoryChunk[Int], dst_counts:IndexedMemoryChunk[Int]) : void {
//        if (needToSerialize[T]()) {
//            val places = size();
//            val ser_offs = new Array[Int](places);
//            val ser_counts = new Array[Int](places);
//            val ser_src = ParallelSerialization.serialize(src, src_offs, src_counts, ser_offs.raw(), ser_counts.raw());
//            val deser_counts = new Array[Int](places);
//            finish nativeAlltoall(id, role, ser_counts.raw(), 0, deser_counts.raw(), 0, 1);
//            val deser_offs = new Array[Int](places + 1);
//            deser_offs(0) = 0;
//            for (i in 0..(places-1)) deser_offs(i+1) = deser_counts(i) + deser_offs(i);
//            val deser_dst = new Array[Byte](deser_offs(places));
//            finish nativeAlltoallv(id, role, ser_src, ser_offs.raw(), ser_counts.raw(), deser_dst.raw(), deser_offs.raw(), deser_counts.raw());
//            ParallelSerialization.deserialize(dst, dst_offs, dst_counts, deser_dst.raw(), deser_offs.raw(), deser_counts.raw());
//        }
//        else {
//            finish nativeAlltoallv(id, role, src, src_offs, src_counts, dst, dst_offs, dst_counts);
//        }
//    }
//
//    private static def nativeAlltoallv[T] (id:Int, role:Int, src:IndexedMemoryChunk[T], src_offs:IndexedMemoryChunk[Int], src_counts:IndexedMemoryChunk[Int], dst:IndexedMemoryChunk[T], dst_offs:IndexedMemoryChunk[Int], dst_counts:IndexedMemoryChunk[Int]) : void {
//        @Native("java", "x10.x10rt.TeamSupport.nativeAllToAllV(id, role, src, src_offs, src_counts, dst, dst_offs, dst_counts);")
//        @Native("c++", "x10rt_alltoallv(id, role, src->raw(), src_offs->raw(), src_counts->raw(), dst->raw(), dst_offs->raw(), dst_counts->raw(), sizeof(TPMGL(T)), x10aux::coll_handler, x10aux::coll_enter());") {}
//    }
//
//    public def alltoallv[T] (role:Int, src:Array[T], src_offs:Array[Int], src_counts:Array[Int], dst_offs:Array[Int], dst_counts:Array[Int]) {
//        assert(src != null);
//        assert(src_counts.size == size());
//        assert(src_offs.size == size());
//        assert(dst_counts.size == size());
//        assert(dst_offs.size == size());
//        assert(size() > 0);
//        val dst = new Array[T](IndexedMemoryChunk.allocateUninitialized[T](dst_counts.reduce((x:Int, y:Int)=>x+y, 0)));
//        alltoallv(role, src, src_offs, src_counts, dst, dst_offs, dst_counts);
//        return dst;
//    }

    /** Indicates the operation to perform when reducing. */
    public static val ADD = 0n;
    /** Indicates the operation to perform when reducing. */
    public static val MUL = 1n;
    /** Indicates the operation to perform when reducing. */
    public static val AND = 3n;
    /** Indicates the operation to perform when reducing. */
    public static val OR  = 4n;
    /** Indicates the operation to perform when reducing. */
    public static val XOR = 5n;
    /** Indicates the operation to perform when reducing. */
    public static val BAND = 6n;
    /** Indicates the operation to perform when reducing. */
    public static val BOR  = 7n;
    /** Indicates the operation to perform when reducing. */
    public static val BXOR = 8n;
    /** Indicates the operation to perform when reducing. */
    public static val MAX = 9n;//6n;
    /** Indicates the operation to perform when reducing. */
    public static val MIN = 10n;//7n;

    /* using overloading is the correct thing to do here since the set of supported
     * types are finite, however the java backend will not be able to distinguish
     * these methods' prototypes so we use the unsafe generic approach for now.
     */

    /** Blocks until all members have received the computed result.  Note that not all values of T are valid.
     *
     * @param root Which place will recieve the reduced value(s)
     * 
     * @param src The data that will be sent 
     *
     * @param src_off The offset into src at which to start reading
     *
     * @param dst The rail into which the data will be received for (will only be used by the root member)
     *
     * @param dst_off The offset into dst at which to start writing (will only be used by the root member)
     *
     * @param count The number of elements being transferred
     *
     * @param op The operation to perform
     */
    public def reduce[T] (root:Int, src:Rail[T], src_off:Long, dst:Rail[T], dst_off:Long, count:Long, op:Int) : void {
        if (collectiveSupportLevel == X10RT_COLL_ALLNONBLOCKINGCOLLECTIVES)
            finish nativeReduce(id, id==0n?here.id() as Int:Team.roles(id), root, src, src_off as Int, dst, dst_off as Int, count as Int, op);
        else if (collectiveSupportLevel == X10RT_COLL_ALLBLOCKINGCOLLECTIVES || collectiveSupportLevel == X10RT_COLL_NONBLOCKINGBARRIER) {
            barrier();
            finish nativeReduce(id, id==0n?here.id() as Int:Team.roles(id), root, src, src_off as Int, dst, dst_off as Int, count as Int, op);
        }
    	else
    	    state(id).collective_impl[T](LocalTeamState.COLL_REDUCE, root as Long, src, src_off, dst, dst_off, count, op);
    }
	
    private static def nativeReduce[T](id:Int, role:Int, root:Int, src:Rail[T], src_off:Int, dst:Rail[T], dst_off:Int, count:Int, op:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeReduce(id, role, root, src, src_off, dst, dst_off, count, op);")
    	@Native("c++", "x10rt_reduce(id, role, root, &src->raw[src_off], &dst->raw[dst_off], (x10rt_red_op_type)op, x10rt_get_red_type<TPMGL(T)>(), count, ::x10aux::coll_handler, ::x10aux::coll_enter());") {}
    }

    /** Performs a reduction on a single value, returning the result at the root */
    public def reduce (root:Int, src:Byte, op:Int) = genericReduce(root, src, op);
    /** Performs a reduction on a single value, returning the result at the root */
    public def reduce (root:Int, src:UByte, op:Int) = genericReduce(root, src, op);
    /** Performs a reduction on a single value, returning the result at the root */
    public def reduce (root:Int, src:Short, op:Int) = genericReduce(root, src, op);
    /** Performs a reduction on a single value, returning the result at the root */
    public def reduce (root:Int, src:UShort, op:Int) = genericReduce(root, src, op);
    /** Performs a reduction on a single value, returning the result at the root */
    public def reduce (root:Int, src:UInt, op:Int) = genericReduce(root, src, op);
    /** Performs a reduction on a single value, returning the result at the root */
    public def reduce (root:Int, src:Int, op:Int) = genericReduce(root, src, op);
    /** Performs a reduction on a single value, returning the result at the root */
    public def reduce (root:Int, src:Long, op:Int) = genericReduce(root, src, op);
    /** Performs a reduction on a single value, returning the result at the root */
    public def reduce (root:Int, src:ULong, op:Int) = genericReduce(root, src, op);
    /** Performs a reduction on a single value, returning the result at the root */
    public def reduce (root:Int, src:Float, op:Int) = genericReduce(root, src, op);
    /** Performs a reduction on a single value, returning the result at the root */
    public def reduce (root:Int, src:Double, op:Int) = genericReduce(root, src, op);

    private def genericReduce[T] (root:Int, src:T, op:Int) : T {
        val chk = new Rail[T](1, src);
        val dst = new Rail[T](1, src);
        if (collectiveSupportLevel == X10RT_COLL_ALLNONBLOCKINGCOLLECTIVES)
            finish nativeReduce[T](id, id==0n?here.id() as Int:Team.roles(id), root, chk, dst, op);
        else if (collectiveSupportLevel == X10RT_COLL_ALLBLOCKINGCOLLECTIVES || collectiveSupportLevel == X10RT_COLL_NONBLOCKINGBARRIER) {
            barrier();
            finish nativeReduce[T](id, id==0n?here.id() as Int:Team.roles(id), root, chk, dst, op);
        }
        else
        	state(id).collective_impl[T](LocalTeamState.COLL_REDUCE, root as Long, chk, 0, dst, 0, 1, op);
        return dst(0);
    }

    private static def nativeReduce[T](id:Int, role:Int, root:Int, src:Rail[T], dst:Rail[T], op:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeReduce(id, role, root, src, 0, dst, 0, 1, op);")
        @Native("c++", "x10rt_reduce(id, role, root, src->raw, dst->raw, (x10rt_red_op_type)op, x10rt_get_red_type<TPMGL(T)>(), 1, ::x10aux::coll_handler, ::x10aux::coll_enter());") {}
    }

    /** Blocks until all members have received the computed result.  Note that not all values of T are valid.
     * The dst array is populated for all members with the result of the operation applied pointwise to all given src arrays.
     *
     * @param src The data that will be sent to all members
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
    public def allreduce[T] (src:Rail[T], src_off:Long, dst:Rail[T], dst_off:Long, count:Long, op:Int) : void {
        if (collectiveSupportLevel == X10RT_COLL_ALLNONBLOCKINGCOLLECTIVES) {
            if (DEBUG) Runtime.println(here + " entering native allreduce on team "+id);
            finish nativeAllreduce(id, id==0n?here.id() as Int:Team.roles(id), src, src_off as Int, dst, dst_off as Int, count as Int, op);
        }
        else if (collectiveSupportLevel == X10RT_COLL_ALLBLOCKINGCOLLECTIVES || collectiveSupportLevel == X10RT_COLL_NONBLOCKINGBARRIER) {
            if (DEBUG) Runtime.println(here + " entering pre-allreduce barrier on team "+id);
            barrier();
            if (DEBUG) Runtime.println(here + " entering native allreduce on team "+id);
            finish nativeAllreduce(id, id==0n?here.id() as Int:Team.roles(id), src, src_off as Int, dst, dst_off as Int, count as Int, op);
        }
    	else {
            if (DEBUG) Runtime.println(here + " entering Team.x10 allreduce on team "+id);
    	    state(id).collective_impl[T](LocalTeamState.COLL_ALLREDUCE, 0, src, src_off, dst, dst_off, count, op);
    	}
        if (DEBUG) Runtime.println(here + " Finished allreduce on team "+id);
    }

    private static def nativeAllreduce[T](id:Int, role:Int, src:Rail[T], src_off:Int, dst:Rail[T], dst_off:Int, count:Int, op:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeAllReduce(id, role, src, src_off, dst, dst_off, count, op);")
    	@Native("c++", "x10rt_allreduce(id, role, &src->raw[src_off], &dst->raw[dst_off], (x10rt_red_op_type)op, x10rt_get_red_type<TPMGL(T)>(), count, ::x10aux::coll_handler, ::x10aux::coll_enter());") {}
    }

    /** Performs a reduction on a single value, returning the result */
    public def allreduce (src:Byte, op:Int) = genericAllreduce(src, op);
    /** Performs a reduction on a single value, returning the result */
    public def allreduce (src:UByte, op:Int) = genericAllreduce(src, op);
    /** Performs a reduction on a single value, returning the result */
    public def allreduce (src:Short, op:Int) = genericAllreduce(src, op);
    /** Performs a reduction on a single value, returning the result */
    public def allreduce (src:UShort, op:Int) = genericAllreduce(src, op);
    /** Performs a reduction on a single value, returning the result */
    public def allreduce (src:UInt, op:Int) = genericAllreduce(src, op);
    /** Performs a reduction on a single value, returning the result */
    public def allreduce (src:Int, op:Int) = genericAllreduce(src, op);
    /** Performs a reduction on a single value, returning the result */
    public def allreduce (src:Long, op:Int) = genericAllreduce(src, op);
    /** Performs a reduction on a single value, returning the result */
    public def allreduce (src:ULong, op:Int) = genericAllreduce(src, op);
    /** Performs a reduction on a single value, returning the result */
    public def allreduce (src:Float, op:Int) = genericAllreduce(src, op);
    /** Performs a reduction on a single value, returning the result */
    public def allreduce (src:Double, op:Int) = genericAllreduce(src, op);

    private def genericAllreduce[T] (src:T, op:Int) : T {
        val chk = new Rail[T](1, src);
        val dst = new Rail[T](1, src);
        if (collectiveSupportLevel == X10RT_COLL_ALLNONBLOCKINGCOLLECTIVES)
            finish nativeAllreduce[T](id, id==0n?here.id() as Int:Team.roles(id), chk, dst, op);
        else if (collectiveSupportLevel == X10RT_COLL_ALLBLOCKINGCOLLECTIVES || collectiveSupportLevel == X10RT_COLL_NONBLOCKINGBARRIER) {
            barrier();
            finish nativeAllreduce[T](id, id==0n?here.id() as Int:Team.roles(id), chk, dst, op);
        }
        else
            state(id).collective_impl[T](LocalTeamState.COLL_ALLREDUCE, 0, chk, 0, dst, 0, 1, op);
        return dst(0);
    }

    private static def nativeAllreduce[T](id:Int, role:Int, src:Rail[T], dst:Rail[T], op:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeAllReduce(id, role, src, 0, dst, 0, 1, op);")
        @Native("c++", "x10rt_allreduce(id, role, src->raw, dst->raw, (x10rt_red_op_type)op, x10rt_get_red_type<TPMGL(T)>(), 1, ::x10aux::coll_handler, ::x10aux::coll_enter());") {}
    }

    /** This operation blocks until all members have received the computed result.  
     * 
     * @param v The value which is compared across all team members
     * 
     * @param idx An integer which is paired with v
     * 
     * @return The value of idx, which was passed in along with the largest v, by the first place with that v
     */
    public def indexOfMax (v:Double, idx:Int) : Int {
        val src = new Rail[DoubleIdx](1, DoubleIdx(v, idx));
        val dst = new Rail[DoubleIdx](1, DoubleIdx(0.0, -1n));
        if (collectiveSupportLevel == X10RT_COLL_ALLNONBLOCKINGCOLLECTIVES)
            finish nativeIndexOfMax(id, id==0n?here.id() as Int:Team.roles(id), src, dst);
        else if (collectiveSupportLevel == X10RT_COLL_ALLBLOCKINGCOLLECTIVES || collectiveSupportLevel == X10RT_COLL_NONBLOCKINGBARRIER) {
            barrier();
            finish nativeIndexOfMax(id, id==0n?here.id() as Int:Team.roles(id), src, dst);
        }
        else
            state(id).collective_impl[DoubleIdx](LocalTeamState.COLL_INDEXOFMAX, 0, src, 0, dst, 0, 1, 0n);
        return dst(0).idx;
    }

    private static def nativeIndexOfMax(id:Int, role:Int, src:Rail[DoubleIdx], dst:Rail[DoubleIdx]) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeIndexOfMax(id, role, src, dst);")
        @Native("c++", "x10rt_allreduce(id, role, src->raw, dst->raw, X10RT_RED_OP_MAX, X10RT_RED_TYPE_DBL_S32, 1, ::x10aux::coll_handler, ::x10aux::coll_enter());") {}
    }

    /** This operation blocks until all members have received the computed result.  
     * 
     * @param v The value which is compared across all team members
     * 
     * @param idx An integer which is paired with v
     * 
     * @return The value of idx, which was passed in along with the smallest v, by the first place with that v
     */
    public def indexOfMin (v:Double, idx:Int) : Int {
        val src = new Rail[DoubleIdx](1, DoubleIdx(v, idx));
        val dst = new Rail[DoubleIdx](1, DoubleIdx(0.0, -1n));
        if (collectiveSupportLevel == X10RT_COLL_ALLNONBLOCKINGCOLLECTIVES)
            finish nativeIndexOfMin(id, id==0n?here.id() as Int:Team.roles(id), src, dst);
        else if (collectiveSupportLevel == X10RT_COLL_ALLBLOCKINGCOLLECTIVES || collectiveSupportLevel == X10RT_COLL_NONBLOCKINGBARRIER) {
            barrier();
            finish nativeIndexOfMin(id, id==0n?here.id() as Int:Team.roles(id), src, dst);
        }
        else
            state(id).collective_impl[DoubleIdx](LocalTeamState.COLL_INDEXOFMIN, 0, src, 0, dst, 0, 1, 0n);
        return dst(0).idx;
    }

    private static def nativeIndexOfMin(id:Int, role:Int, src:Rail[DoubleIdx], dst:Rail[DoubleIdx]) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeIndexOfMin(id, role, src, dst);")
        @Native("c++", "x10rt_allreduce(id, role, src->raw, dst->raw, X10RT_RED_OP_MIN, X10RT_RED_TYPE_DBL_S32, 1, ::x10aux::coll_handler, ::x10aux::coll_enter());") {}
    }

    /** Create new teams by subdividing an existing team.  This is called by each member
     * of an existing team, indicating which of the new teams it will be a member of, and its role
     * within that team.  The old team is still available after this call.  All the members
     * of the old team must collectively assign themselves to new teams such that there is exactly 1
     * member of the original team for each role of each new team.  It is undefined behaviour if two
     * members of the original team decide to play the same role in one of the new teams, or if one of
     * the roles of a new team is left unfilled.
     *
     * @param color The new team, must be a number between 0 and the number of new teams - 1
     *
     * @param new_role The caller's position within the new team
     */
    public def split (color:Int, new_role:Long) : Team {
        val result = new Rail[Int](1);
        if (collectiveSupportLevel == X10RT_COLL_ALLNONBLOCKINGCOLLECTIVES) {
        	if (DEBUG) Runtime.println(here + " calling native split on team "+id+" color="+color+" new_role="+new_role);        
            finish nativeSplit(id, id==0n?here.id() as Int:Team.roles(id), color, new_role as Int, result);
            if (DEBUG) Runtime.println(here + " finished native split on team "+id+" color="+color+" new_role="+new_role);
            return Team(result(0), null, new_role);
        }
        else {
            if (DEBUG) Runtime.println(here + " creating PlaceGroup for splitting team "+id+"(size="+this.size()+") color="+color+" new_role="+new_role);
            // all-to-all to distribute team and role information around        
            val myInfo:Rail[Int] = new Rail[Int](2);
            myInfo(0) = color;
            myInfo(1) = new_role as Int; // TODO: may need to preserve long someday
            val allInfo:Rail[Int] = new Rail[Int](this.size() * 2);
            alltoall(myInfo, 0, allInfo, 0, 2);
            
            // In case the underlying alltoall does not copy my info from src to dst
            myTeamPosition:Long = Team.state(this.id).places.indexOf(here.id()) * 2;
            allInfo(myTeamPosition) = color;
            allInfo(myTeamPosition+1) = new_role as Int;
            
            if (DEBUGINTERNALS) Runtime.println(here + " completed alltoall for splitting team "+id+" color="+color+" new_role="+new_role+" allInfo="+allInfo);
        	// use the above to figure out the members of *my* team
            // count the new team size
            var numPlacesInMyTeam:Int = 0n;
            for (var i:Long=0; i<allInfo.size; i+=2)
                if (allInfo(i) == color)
                	numPlacesInMyTeam++;

            if (DEBUGINTERNALS) Runtime.println(here + " my new team has "+numPlacesInMyTeam+" places");
            // create a new PlaceGroup with all members of my new team
            val newTeamPlaceRail:Rail[Place] = new Rail[Place](numPlacesInMyTeam);
            for (var i:Long=0; i<allInfo.size; i+=2) {
            	if (allInfo(i) == color) {
                    if (DEBUGINTERNALS) Runtime.println(here + " setting new team position "+allInfo(i+1)+" to place "+Team.state(this.id).places(i/2));
            	    newTeamPlaceRail(allInfo(i+1)) = Team.state(this.id).places(i/2);
            }   }
            newTeamPlaceGroup:SparsePlaceGroup = new SparsePlaceGroup(newTeamPlaceRail);
            if (DEBUGINTERNALS) Runtime.println(here + " Created PlaceGroup for splitting team "+id+" color="+color+" new_role="+new_role+": "+newTeamPlaceRail);
            // now that we have a PlaceGroup for the new team, create it
            if (collectiveSupportLevel == X10RT_COLL_ALLBLOCKINGCOLLECTIVES || collectiveSupportLevel == X10RT_COLL_NONBLOCKINGBARRIER) {
                if (DEBUGINTERNALS) Runtime.println(here + " calling pre-native split barrier on team "+id+" color="+color+" new_role="+new_role);
            	barrier();
                if (DEBUGINTERNALS) Runtime.println(here + " calling native split on team "+id+" color="+color+" new_role="+new_role);
            	finish nativeSplit(id, id==0n?here.id() as Int:Team.roles(id), color, new_role as Int, result);
                if (DEBUG) Runtime.println(here + " finished native split on team "+id+" color="+color+" new_role="+new_role);
            	return Team(result(0), newTeamPlaceGroup, new_role);
            }
            else {
                if (DEBUG) Runtime.println(here + " returning new split team "+id+" color="+color+" new_role="+new_role);
            	return Team((Team.state.size() as Int) + color, newTeamPlaceGroup, new_role);
            }
        }
    }

    private static def nativeSplit(id:Int, role:Int, color:Int, new_role:Int, result:Rail[Int]) : void {
        Runtime.increaseParallelism();
        @Native("java", "x10.x10rt.TeamSupport.nativeSplit(id, role, color, new_role, result);")
        @Native("c++", "x10rt_team_split(id, role, color, new_role, ::x10aux::coll_handler2, ::x10aux::coll_enter2(result->raw));") {}
        Runtime.decreaseParallelism(1n);
    }

    /** Destroy a team that is no-longer needed.  Called simultaneously by each member of
     * the team.  There should be no operations on the team after this.
     */
    public def delete () : void {
        if (this == WORLD) throw new IllegalArgumentException("Cannot delete Team.WORLD");
        if (collectiveSupportLevel == X10RT_COLL_ALLNONBLOCKINGCOLLECTIVES)
            finish nativeDel(id, id==0n?here.id() as Int:Team.roles(id));
        else if (collectiveSupportLevel == X10RT_COLL_ALLBLOCKINGCOLLECTIVES || collectiveSupportLevel == X10RT_COLL_NONBLOCKINGBARRIER) {
            barrier();
            finish nativeDel(id, id==0n?here.id() as Int:Team.roles(id));
        }
        // TODO - see if there is something useful to delete with the local team implementation
    }

    private static def nativeDel(id:Int, role:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeDel(id, role);")
        @Native("c++", "x10rt_team_del(id, role, ::x10aux::coll_handler, ::x10aux::coll_enter());") {}
    }

    public def toString() = "Team(" + this.id + "," + this.places() +  ")";
    public def equals(that:Team) = that.id==this.id;
    public def equals(that:Any) = that instanceof Team && (that as Team).id==this.id;
    public def hashCode()=id;
    
    
    /*
     * State information for X10 collective operations
     * All collectives are implemented as a tree operation, with all members of the team 
     * communicating with a "parent" member in a gather phase up to the root of the team,
     * followed by a scatter phase from the root to all members.  Data and reduction operations
     * may be carried along as a part of these communication phases, depending on the collective.
     * 
     * All operations are initiated by leaf nodes, which push data to their parent's buffers.  The parent
     * then initiates a push to its parent, and so on, up to the root.  At the root, 
     * the direction changes, and the root pushes data to children, who push it to their children, etc.
     * 
     * For performance reasons, this implementation DOES NOT perform error checking.  It does not verify
     * array indexes, that all places call the same collective at the same time, that root matches, etc.
     */
    private static class LocalTeamState(places:PlaceGroup, teamid:Int, myIndex:Long) {
        private static struct TreeStructure(parentIndex:Long, child1Index:Long, child2Index:Long, totalChildren:Long){}
        
        private static PHASE_READY:Int = 0n;   // normal state, nothing in progress
        private static PHASE_INIT:Int = 1n;    // collective active, preparing local structures to accept data
        private static PHASE_GATHER1:Int = 2n; // waiting for data+signal from first child
        private static PHASE_GATHER2:Int = 3n; // waiting for data+signal from second child
        private static PHASE_SCATTER:Int = 4n; // waiting for data+signal from parent
        private static PHASE_DONE:Int = 5n;    // done, but not yet ready for the next collective call
        private val phase:AtomicInteger = new AtomicInteger(PHASE_READY); // which of the above phases we're in
        private val dstLock:Lock = new Lock();

        private static COLL_BARRIER:Int = 0n; // no data moved
        private static COLL_BROADCAST:Int = 1n; // data out only, single value
        private static COLL_SCATTER:Int = 2n; // data out only, many values
        private static COLL_ALLTOALL:Int = 3n; // data in and out, many values
        private static COLL_REDUCE:Int = 4n; // data in only
        private static COLL_ALLREDUCE:Int = 5n; // data in and out
        private static COLL_INDEXOFMIN:Int = 6n; // data in and out
        private static COLL_INDEXOFMAX:Int = 7n; // data in and out

        // local data movement fields associated with the local arguments passed in collective_impl
        private var local_src:Any = null; // becomes type Rail[T]{self!=null}
        private var local_src_off:Long = 0;
        private var local_dst:Any = null; // becomes type Rail[T]{self!=null}
        private var local_dst_off:Long = 0;
//        private var local_temp_buff:Any = null; // Used to hold intermediate data moving up or down the tree structure, becomes type Rail[T]{self!=null}
        private var local_count:Long = 0;
        private var local_grandchildren:Long = 0; // total number of nodes in the tree structure below us

        private static def getCollName(collType:Int):String {
            switch (collType) {
                case COLL_BARRIER: return "Barrier";
                case COLL_BROADCAST: return "Broadcast";
                case COLL_SCATTER: return "Scatter";
                case COLL_ALLTOALL: return "AllToAll";
                case COLL_REDUCE: return "Reduce";
                case COLL_ALLREDUCE: return "AllReduce";
                case COLL_INDEXOFMIN: return "IndexOfMin";
                case COLL_INDEXOFMAX: return "IndexOfMax";
                default: return "Unknown";
            }
        }
        
        private static def lockDst(teamidcopy:Int, lock:Lock) {
        	if (!lock.tryLock()) {
        		if (useProbeNotSleep()) {
        			while (!lock.tryLock())
        				Runtime.probe();
        		}
        		else {
        			Runtime.increaseParallelism();
        			while (!lock.tryLock())
        				System.threadSleep(0);
        			Runtime.decreaseParallelism(1n);
        		}
        	}
        }
        
        // recursive method used to find our parent and child links in the tree.  This method assumes that root is not in the tree (or root is at position 0)
        private def getLinks(parent:Long, startIndex:Long, endIndex:Long):TreeStructure {
            if (DEBUGINTERNALS) Runtime.println(here+" getLinks called with myIndex="+myIndex+" parent="+parent+" startIndex="+startIndex+", endIndex="+endIndex);
            
            if (myIndex == startIndex) { // we're at our own position in the tree
                val children:Long = endIndex-startIndex; // overall gap of children
                return new TreeStructure(parent, (children<1)?-1:(startIndex+1), (children<2)?-1:(startIndex+1+((endIndex-startIndex)/2)), children);
            }
            else {
                if (myIndex > startIndex+((endIndex-startIndex)/2)) // go down the tree, following the right branch (second child)
                	return getLinks(startIndex, startIndex+1+((endIndex-startIndex)/2), endIndex);
                else // go down the left branch (first child)
                    return getLinks(startIndex, startIndex+1, startIndex+((endIndex-startIndex)/2));
            }
        }
	    
	    // This is an internal barrier which can be called at the end of team creation.  The regular
	    // barrier method assumes that the team is already in place.  This method adds some pre-checks
	    // to ensure that the state information for the entire team is in place before running the 
	    // regular barrier, which does not have these checks.
	    private def init() {
            if (DEBUGINTERNALS) Runtime.println(here + " creating team "+teamid);
	        val myLinks:TreeStructure = getLinks(-1, 0, places.numPlaces()-1);

	        if (DEBUGINTERNALS) { 
	        	Runtime.println(here+":team"+this.teamid+", root=0 has parent "+((myLinks.parentIndex==-1)?Place.INVALID_PLACE:places(myLinks.parentIndex)));
	        	Runtime.println(here+":team"+this.teamid+", root=0 has children "+((myLinks.child1Index==-1)?Place.INVALID_PLACE:places(myLinks.child1Index))+", "+((myLinks.child2Index==-1)?Place.INVALID_PLACE:places(myLinks.child2Index)));
	        }
	    	val teamidcopy = this.teamid; // needed to prevent serializing "this"
		    if (myLinks.parentIndex != -1) {
			    @Pragma(Pragma.FINISH_ASYNC) finish at (places(myLinks.parentIndex)) async {
			        when (Team.state.size() > teamidcopy) {}
			}   }
		    if (DEBUGINTERNALS) Runtime.println(here+":team"+this.teamid+", moving on to init barrier");
		    collective_impl[Int](COLL_BARRIER, 0, null, 0, null, 0, 0, 0n); // barrier
		    if (DEBUGINTERNALS) Runtime.println(here + " leaving init phase");
		}
	    
	    private def performReduction[T](src:Rail[T], src_off:Long, dst:Rail[T], dst_off:Long, count:Long, operation:Int) {
	        switch (operation) {
		        case ADD:
		        break;
		        case MUL:
		        break;
		        case AND:
		        break;
		        case OR:
		        break;
		        case XOR:
		        break;
		        case MAX:
		        break;
		        case MIN:
		        break;
		        default:
		        	Runtime.println("ERROR: Unknown reduction operation: "+operation);
	        }
	    }
	    
	    /*
	     * This method contains the implementation for all collectives.  Some arguments are only valid
	     * for specific collectives.
	     */
	    private def collective_impl[T](collType:Int, rootIndex:Long, src:Rail[T], src_off:Long, dst:Rail[T], dst_off:Long, count:Long, operation:Int):void {
	        if (DEBUGINTERNALS) Runtime.println(here+":team"+teamid+" entered "+getCollName(collType)+" (phase="+phase.get()+", root="+rootIndex);
	        val teamidcopy = this.teamid; // needed to prevent serializing "this" in at() statements
	        // block if some other collective is in progress.
	        if (!this.phase.compareAndSet(PHASE_READY, PHASE_INIT)) {
	            if (useProbeNotSleep()) {
	        	    while (!this.phase.compareAndSet(PHASE_READY, PHASE_INIT))
	                    Runtime.probe();
	            }
	            else {
	                Runtime.increaseParallelism();
	                while (!this.phase.compareAndSet(PHASE_READY, PHASE_INIT))
	            		System.threadSleep(0);
	            	Runtime.decreaseParallelism(1n);
	            }
	        }
	        
	        // figure out our links in the tree structure
	        val myLinks:TreeStructure;
	        //val rootIndex:Long = places.indexOf(root);
	        if (myIndex > rootIndex || rootIndex == 0)
	        	myLinks = getLinks(-1, rootIndex, places.numPlaces()-1);
	        else if (myIndex < rootIndex)
	            myLinks = getLinks(rootIndex, 0, rootIndex-1);
	        else // non-zero root
	            myLinks = new TreeStructure(-1, 0, ((places.numPlaces()-1)==rootIndex)?-1:(rootIndex+1), places.numPlaces()-1);

	        if (DEBUGINTERNALS) { 
	            Runtime.println(here+":team"+teamidcopy+", root="+rootIndex+" has parent "+((myLinks.parentIndex==-1)?Place.INVALID_PLACE:places(myLinks.parentIndex)));
	            Runtime.println(here+":team"+teamidcopy+", root="+rootIndex+" has children "+((myLinks.child1Index==-1)?Place.INVALID_PLACE:places(myLinks.child1Index))+", "+((myLinks.child2Index==-1)?Place.INVALID_PLACE:places(myLinks.child2Index)));
	        }
	        
	        // make my local data arrays visible to other places
	        local_src = src;
	        local_src_off = src_off;
	        local_dst = dst;
	        local_dst_off = dst_off;
	        local_count = count;
	        local_grandchildren = myLinks.totalChildren;
/*	        if (collType == COLL_SCATTER || collType == COLL_REDUCE || collType == COLL_ALLREDUCE) // big chunks of data move around the tree
	        	local_temp_buff = Unsafe.allocRailUninitialized[T](myLinks.dataToCarryForChildren);
	        else if (myLinks.child1Index != -1 && (collType == COLL_INDEXOFMIN || collType == COLL_INDEXOFMAX)) // pairs of values move around
	            local_temp_buff = Unsafe.allocRailUninitialized[T]((myLinks.child2Index==-1)?1:2);
*/
            // check for valid input.  TODO: remove for performance?
	        //if (dst == null && collType != COLL_BARRIER) Runtime.println("ERROR: dst is NULL!");
	        //if (src == null && collType != COLL_BARRIER) Runtime.println("ERROR: src is NULL!");
	        
	        // perform local reduction operations.  Result is stored in dst, which will be updated again later
	        if (collType == COLL_REDUCE || collType == COLL_ALLREDUCE)
    	        performReduction(src, src_off, dst, dst_off, count, operation);
	        else if (collType == COLL_INDEXOFMAX || collType == COLL_INDEXOFMIN)
	            dst(0) = src(0);
	        
	        // allow children to update our dst array
	        if (DEBUGINTERNALS) Runtime.println(here+" ready to accept data");
	        this.phase.compareAndSet(PHASE_INIT, PHASE_GATHER1);
	        
	        // Skip our state ahead if we have fewer than 2 children to wait for
	    	if (myLinks.child1Index == -1) // no children to wait for
	    		this.phase.compareAndSet(PHASE_GATHER1, PHASE_SCATTER);
	    	else if (myLinks.child2Index == -1) { // only one child, so skip a phase waiting for the second child.
	    		if (!this.phase.compareAndSet(PHASE_GATHER1, PHASE_GATHER2)) // the only child has not yet checked in
	    			this.phase.compareAndSet(PHASE_GATHER2, PHASE_SCATTER); // the only child has already checked in
	    	}
	    
	        // wait for phase updates from children
	        if (this.phase.get() != PHASE_SCATTER) {
	            if (DEBUGINTERNALS) Runtime.println(here+":team"+teamidcopy+" waiting for children");
	            if (useProbeNotSleep()) {
		            while (this.phase.get() != PHASE_SCATTER) 
		                Runtime.probe();
		        }
		        else {
		            Runtime.increaseParallelism();
                    while (this.phase.get() != PHASE_SCATTER)
	                    System.threadSleep(0);
                    Runtime.decreaseParallelism(1n);
		        }
	        }
	        if (DEBUGINTERNALS) Runtime.println(here+":team"+teamidcopy+" released by children");
	    
	        // all children have checked in.  Update our parent, and then wait for the parent to update us 
	    	if (myLinks.parentIndex == -1) { // this is the root
	    		// copy data locally from src to dst if needed
	    		if (collType == COLL_BROADCAST)
	    			Rail.copy(src, src_off, dst, dst_off, count);
	    		else if (collType == COLL_SCATTER)
			    	Rail.copy(src, src_off+(count*myIndex), dst, dst_off, count);
                else if (collType == COLL_ALLTOALL)
                    Rail.copy(src, src_off+(count*myIndex), dst, dst_off+(count*myIndex), count);
	    		this.phase.set(PHASE_DONE); // the root node has no parent, and can skip its own state ahead
	    	}
	    	else {
	    		// make sure parent is ready to recieve data
	            @Pragma(Pragma.FINISH_ASYNC) finish at (places(myLinks.parentIndex)) async { 
                	if (Team.state(teamidcopy).phase.get() < PHASE_GATHER1 || Team.state(teamidcopy).phase.get() > PHASE_SCATTER) {
                        if (useProbeNotSleep()) {
                            while(Team.state(teamidcopy).phase.get() < PHASE_GATHER1 || Team.state(teamidcopy).phase.get() >= PHASE_SCATTER)
                        	    Runtime.probe();
                        }
                        else {
                            Runtime.increaseParallelism();
                            while(Team.state(teamidcopy).phase.get() < PHASE_GATHER1 || Team.state(teamidcopy).phase.get() >= PHASE_SCATTER)
                    	        System.threadSleep(0);
                            Runtime.decreaseParallelism(1n);
                        }
                    }
	    		}
	            // move data from children to parent
   	            // Scatter and broadcast only move data from parent to children, so they have no code here
	            if (collType >= COLL_ALLTOALL) {
	                if (DEBUGINTERNALS) Runtime.println(here+" moving data to parent");
	                val notnulldst:Rail[T]{self!=null} = dst as Rail[T]{self!=null};
                    gr:GlobalRail[T] = new GlobalRail[T](notnulldst);
                    if (collType == COLL_ALLTOALL) {
                        val totalData:Long = count*(myLinks.totalChildren+1);
	                    @Pragma(Pragma.FINISH_ASYNC) finish at (places(myLinks.parentIndex)) async {
	                        // copy my data, plus all the data filled in by my children, to my parent
	                        Rail.asyncCopy(gr, dst_off, Team.state(teamidcopy).local_dst as Rail[T], Team.state(teamidcopy).local_dst_off, totalData);
	                    }
	                }
	                else if (collType == COLL_REDUCE || collType == COLL_ALLREDUCE) {
	                    //TODO
	                }
	                else if (collType == COLL_INDEXOFMAX) {
	                    val childVal:DoubleIdx = dst(0) as DoubleIdx;
	                    @Pragma(Pragma.FINISH_ASYNC) finish at (places(myLinks.parentIndex)) async {
	                        lockDst(teamidcopy, Team.state(teamidcopy).dstLock);
	                        val ldi:Rail[DoubleIdx] = (Team.state(teamidcopy).local_dst as Rail[DoubleIdx]);
	                        if (DEBUGINTERNALS) Runtime.println(here+" IndexOfMax: parent="+ldi(0).value+" child="+childVal.value);
	                        
	                        // TODO: If there is  more than one instance of the min/max value, this 
	                        // implementation will return the index associated with "one of" them, not necessarily
	                        // the first one.  Do we need to return the "first", as the API says, or is that not really necessary?
	                        if (childVal.value > ldi(0).value)
	                            ldi(0) = childVal;
	                        Team.state(teamidcopy).dstLock.unlock();
	                    }
	                }
	                else if (collType == COLL_INDEXOFMIN) {
	                    val childVal:DoubleIdx = dst(0) as DoubleIdx;
	                    @Pragma(Pragma.FINISH_ASYNC) finish at (places(myLinks.parentIndex)) async {
	                        lockDst(teamidcopy, Team.state(teamidcopy).dstLock);
	                        val ldi:Rail[DoubleIdx] = (Team.state(teamidcopy).local_dst as Rail[DoubleIdx]);
	                        if (childVal.value < ldi(0).value)
	                            ldi(0) = childVal;
	                        Team.state(teamidcopy).dstLock.unlock();
	                     }
	                }
	            }
	            if (DEBUGINTERNALS) Runtime.println(here+" updating the phase of the parent "+places(myLinks.parentIndex));

	            // increment the phase of the parent
	            @Pragma(Pragma.FINISH_ASYNC) finish at (places(myLinks.parentIndex)) async { 
	                if (DEBUGINTERNALS) Runtime.println(here+" in phase "+Team.state(teamidcopy).phase.get());
                	if (!Team.state(teamidcopy).phase.compareAndSet(PHASE_GATHER1, PHASE_GATHER2) && 
                            !Team.state(teamidcopy).phase.compareAndSet(PHASE_GATHER2, PHASE_SCATTER)) {
                        if (useProbeNotSleep()) {
                            while(!Team.state(teamidcopy).phase.compareAndSet(PHASE_GATHER1, PHASE_GATHER2) && 
                        	        !Team.state(teamidcopy).phase.compareAndSet(PHASE_GATHER2, PHASE_SCATTER))
                        	    Runtime.probe();
                        }
                        else {
                            Runtime.increaseParallelism();
                            while(!Team.state(teamidcopy).phase.compareAndSet(PHASE_GATHER1, PHASE_GATHER2) && 
                    	            !Team.state(teamidcopy).phase.compareAndSet(PHASE_GATHER2, PHASE_SCATTER))
                    	        System.threadSleep(0);
                            Runtime.decreaseParallelism(1n);
                        }
                    }
	                if (DEBUGINTERNALS) Runtime.println(here+" has been set to phase "+Team.state(teamidcopy).phase.get());
	    		}
	            
	            if (this.phase.get() != PHASE_DONE) { // wait for parent to set us free
	                if (DEBUGINTERNALS) Runtime.println(here+ " waiting for parent "+places(myLinks.parentIndex)+":team"+teamidcopy+" to release us from phase "+phase.get());
	                if (useProbeNotSleep()) {
    	                while (this.phase.get() != PHASE_DONE)
	                        Runtime.probe();
	                } 
	                else {
                        Runtime.increaseParallelism();
	                    while (this.phase.get() != PHASE_DONE)
	                        System.threadSleep(0);
	                    Runtime.decreaseParallelism(1n);  
	                }
	            }
			    if (DEBUGINTERNALS) Runtime.println(here+ " released by parent");
	    	}

	    	// move data from parent to children
	    	// reduce and barrier do not move data in this direction, so they are not included here
	    	if (myLinks.child1Index != -1 && collType != COLL_BARRIER && collType != COLL_REDUCE) {
	    		val notnulldst:Rail[T]{self!=null} = dst as Rail[T]{self!=null};
	            gr:GlobalRail[T] = new GlobalRail[T](notnulldst);

                if (collType == COLL_ALLTOALL) {
		            // only copy over the data that did not come from this child in the first place
		            @Pragma(Pragma.FINISH_SPMD) finish {
		                at (places(myLinks.child1Index)) async {
                            @Pragma(Pragma.FINISH_ASYNC) finish {
                                // position 0 up to the child id
                                Rail.asyncCopy(gr, dst_off, Team.state(teamidcopy).local_dst as Rail[T], Team.state(teamidcopy).local_dst_off, Team.state(teamidcopy).local_count*Team.state(teamidcopy).myIndex);
                                // position of last child range, to the end
                                Rail.asyncCopy(gr, dst_off+(Team.state(teamidcopy).local_count*(Team.state(teamidcopy).local_grandchildren+1)), Team.state(teamidcopy).local_dst as Rail[T], Team.state(teamidcopy).local_dst_off, Team.state(teamidcopy).local_count);
                            }
                        }
		                if (myLinks.child2Index != -1) {
		                    at (places(myLinks.child2Index)) async {
		                        @Pragma(Pragma.FINISH_ASYNC) finish {
		                        // position 0 up to the child id
		                        Rail.asyncCopy(gr, dst_off, Team.state(teamidcopy).local_dst as Rail[T], Team.state(teamidcopy).local_dst_off, Team.state(teamidcopy).local_count);
		                        // position of last child range, to the end
		                        Rail.asyncCopy(gr, dst_off, Team.state(teamidcopy).local_dst as Rail[T], Team.state(teamidcopy).local_dst_off, Team.state(teamidcopy).local_count);
		                        }
		                    }
		                }
		            }
		        }
		    	else if (collType == COLL_BROADCAST || collType == COLL_ALLREDUCE || 
		    		collType == COLL_INDEXOFMIN || collType == COLL_INDEXOFMAX) {
		    		// these all move a single value from root to all other team members
		            @Pragma(Pragma.FINISH_SPMD) finish {
		    			at (places(myLinks.child1Index)) async {
		                    if (DEBUGINTERNALS) Runtime.println(here+ " pulling data from "+gr+" into "+(Team.state(teamidcopy).local_dst as Rail[T]));
		                    @Pragma(Pragma.FINISH_ASYNC) finish {
		    					Rail.asyncCopy(gr, dst_off, Team.state(teamidcopy).local_dst as Rail[T], Team.state(teamidcopy).local_dst_off, Team.state(teamidcopy).local_count);
		                    }
		    			}
		    			if (myLinks.child2Index != -1) {
		    				at (places(myLinks.child2Index)) async {
		                        if (DEBUGINTERNALS) Runtime.println(here+ " pulling data from "+gr+" into "+(Team.state(teamidcopy).local_dst as Rail[T]));
		                        @Pragma(Pragma.FINISH_ASYNC) finish {
		    						Rail.asyncCopy(gr, dst_off, Team.state(teamidcopy).local_dst as Rail[T], Team.state(teamidcopy).local_dst_off, Team.state(teamidcopy).local_count);
		                        }
		    				}
		    			}
		    		}
		    	}
		    	else if (collType == COLL_SCATTER) {
		            // TODO: scatter is more difficult because we want places to carry data that is not intended for them, so we need temp buffers
	    		}
		    	if (DEBUGINTERNALS) Runtime.println(here+ " finished moving data to children");
	    	}

	    	// our parent has updated us - update any children, and leave the collective
	        if (myLinks.child1Index != -1) { // free the first child, if it exists
	            // NOTE: there is some trickery here, which allows the parent to continue past this section
	            //   before the children have been set free.  This is necessary when there is a blocking
	            //   call immediately after this collective completes (e.g. the barrier before a blocking 
	            //   collective in MPI-2), because otherwise the at may not return before the barrier
	            //   locks up the worker thread.
	            // TODO: convert to Runtime.runUncountedAsync(), or Runtime.x10rtSendAsync(), or other such simpler mechanism
	            val origPlace:Place = here;
	            @Pragma(Pragma.FINISH_HERE) finish {
	                at (places(myLinks.child1Index)) async {
	                    at (origPlace) async {}
	    		        if (!Team.state(teamidcopy).phase.compareAndSet(PHASE_SCATTER, PHASE_DONE))
	    				    Runtime.println("ERROR root setting the first child "+here+":team"+teamidcopy+" to PHASE_DONE");
	    			    else if (DEBUGINTERNALS) Runtime.println("set the first child "+here+":team"+teamidcopy+" to PHASE_DONE");
	                }
	    		}
	    		if (myLinks.child2Index != -1) {
	    		    @Pragma(Pragma.FINISH_HERE) finish {
	                    at (places(myLinks.child2Index)) async {
	                        at (origPlace) async {}
	    		            if (!Team.state(teamidcopy).phase.compareAndSet(PHASE_SCATTER, PHASE_DONE))
	    					    Runtime.println("ERROR root setting the second child "+here+":team"+teamidcopy+" to PHASE_DONE");
	    			        else if (DEBUGINTERNALS) Runtime.println("set the second child "+here+":team"+teamidcopy+" to PHASE_DONE");
	    			    }
	    		    }
	    	    }
	        }
/* alternative form of the above
            val free_child = ()=> @x10.compiler.RemoteInvocation("free_child") {
                if (!Team.state(teamidcopy).phase.compareAndSet(PHASE_SCATTER, PHASE_DONE))
       		        Runtime.println("ERROR setting child "+here+" to PHASE_DONE");
            };
	        if (myLinks.child1Index != -1) {
	            Runtime.x10rtSendMessage(places(myLinks.child1Index).id, free_child, null);
	    		if (myLinks.child2Index != -1)
	                Runtime.x10rtSendMessage(places(myLinks.child2Index).id, free_child, null);
	        }
	        Unsafe.dealloc(free_child);
*/	        
	        local_src = null;
	        local_dst = null;
//	        local_temp_buff = null;
	        this.phase.set(PHASE_READY);
	        // done!
	        if (DEBUGINTERNALS) Runtime.println(here+":team"+teamidcopy+" leaving "+getCollName(collType));
	    }
	}

    //private def flatten[T] (src:Array[Array[T](1)](1)) : Pair[Array[T](1), Pair[Array[Int](1), Array[Int](1)]] {
    //    val sizes:Array[Int](1) = src.map((x:Array[T])=>x.size as Int);
    //    val size = sizes.reduce((x:Int, y:Int)=>x+y, 0);
    //    val acc:Array[Int](1) = sizes.scan((x:Int, y:Int)=> x+y, 0);
    //    val offs:Array[Int](1) = new Array[Int](acc.size, (i:Int)=>(i==0) ? 0 : acc(i-1));
    //    val find_arr = (i:Int) => {
    //        assert(i < size);
    //        val ind = ArrayUtils.binarySearch(acc, i );
    //        if (ind >= 0) {
    //            var max_ind:Int = ind;
    //            while (max_ind < acc.size - 1 && acc(max_ind) == acc(max_ind + 1)) ++max_ind;
    //            assert(max_ind + 1< acc.size);
    //            return max_ind + 1;
    //        }
    //        else return -(ind +1);
    //    };
    //    val flatten_src:Array[T](1) = new Array[T](size, (i:Int)=> 
    //        src(find_arr(i))(i - offs(find_arr(i)))
    //    );
    //    return Pair[Array[T](1), Pair[Array[Int](1), Array[Int](1)]](flatten_src, Pair[Array[Int](1), Array[Int](1)](offs, sizes));
    //}


    //public def scatter[T] (role:Int, root:Int, src:Array[T]) {
    //    val team_size = size();
    //    assert(role != root || src != null);
    //    assert(role != root || src.size % team_size == 0);
    //    val src_size = role == root ? src.size : Zero.get[Int]();
    //    val count = bcast1(role, root, src_size / team_size);
    //    debugln("scatter", "count: " + count);
    //    return scatter(role, root, src, count);
    //}

    //public def scatterv[T] (role:Int, root:Int, src:Array[T], src_counts:Array[Int], src_offs:Array[Int]) {
    //    assert(role != root || src_counts != null);
    //    assert(role != root || src_offs != null);
    //    val team_size = size();
    //    assert(role != root || src_counts.size == team_size);
    //    assert(role != root || src_offs.size == team_size);
    //    val dst_count = scatter(role, root, src_counts, 1)(0);
    //    debugln("scatterv", "dst_count: " + dst_count);
    //    return scatterv(role, root, src, src_counts, src_offs, dst_count);
    //}

    //public def scatterv[T] (role:Int, root:Int, src:Array[T], src_counts:Array[Int]) {
    //    assert(role != root || src_counts != null);
    //    val src_offs : Array[Int] = role == root ? countsToOffs(src_counts as Array[Int](1)) : null;
    //    debugln("scatterv", "src_offs: " +  src_offs);
    //    return scatterv[T](role, root, src, src_counts, src_offs);
    //}

    //public def scatterv[T] (role:Int, root:Int, src:Array[Array[T](1)](1)) {
    //    if (role == root) {
    //        assert(src != null);
    //        val flatten_src_tuple = flatten(src);
    //        val flatten_src = flatten_src_tuple.first;
    //        val src_offs = flatten_src_tuple.second.first;
    //        val src_sizes = flatten_src_tuple.second.second;
    //        debugln("scatterv", "flatten_src_tuple: " + flatten_src_tuple);
    //        return scatterv[T](role, root, flatten_src, src_sizes, src_offs);
    //    } else {
    //        debugln("scatterv", "non root");
    //        return scatterv[T](role, root, null, null, null);
    //    }
    //}

    //public def gatherv[T] (role:Int, root:Int, src:Array[T](1)) {
    //    assert(src != null);
    //    val src_size = (role == root) ? src.size : 0;
    //    val dst_counts = gather1[Int](role, root, src_size);
    //    debugln("gatherv", "dst_counts: " + dst_counts);
    //    return gatherv[T](role, root, src, dst_counts);
    //}

    //public def bcast[T] (role:Int, root:Int, src:Array[T]) {
    //    assert(role != root || src != null);
    //    val src_size = (role == root) ? src.size : 0;
    //    val count = bcast1(role, root, src_size);
    //    debugln("bcast", "count: " + count);
    //    bcast(role, root, src, count);
    //}

    //public def allgatherv[T] (role:Int, src:Array[T]) {
    //    assert(src != null);
    //    val dst_counts = allgather1(role, src.size as Int);
    //    val dst_offs = countsToOffs(dst_counts);
    //    debugln("allgatherv", "dst_counts: " + dst_counts);
    //    debugln("allgatherv", "dst_offs: " + dst_offs);

    //    return allgatherv[T](role, src, dst_offs, dst_counts);
    //}

    //public def alltoallvWithBreakdown[T] (role:Int, src:Array[T], src_offs:Array[Int], src_counts:Array[Int]) : Pair[Array[T](1),Array[Int](1)] {
    //    assert(src != null);
    //    assert(src_offs != null);
    //    assert(src_counts != null);
    //    val dst_counts = alltoall(role, src_counts);
    //    val dst_offs = countsToOffs(dst_counts);
    //    val dst = alltoallv[T](role, src, src_offs, src_counts, dst_offs, dst_counts);
    //    debugln("alltoallvWithBreakdown", "dst_counts: " + dst_counts);
    //    debugln("alltoallvWithBreakdown", "dst_offs: " + dst_offs);
    //    debugln("alltoallvWithBreakdown", "dst: " + dst);
    //    return Pair[Array[T](1),Array[Int](1)](dst, dst_counts);
    //}

    //public def alltoallvWithBreakdown[T] (role:Int, src:Array[Array[T](1)](1)) : Pair[Array[T](1),Array[Int](1)] {
    //    assert(src != null);
    //    val flatten_src_tuple = flatten(src);
    //    val flatten_src = flatten_src_tuple.first;
    //    val src_offs = flatten_src_tuple.second.first;
    //    val src_sizes = flatten_src_tuple.second.second;
    //    debugln("alltoallvWithBreakdown", "src_counts: " + src_sizes);
    //    debugln("alltoallvWithBreakdown", "src_offs: " + src_offs);
    //    debugln("alltoallvWithBreakdown", "flatten_src: " + flatten_src);
    //    return alltoallvWithBreakdown(role, flatten_src, src_offs, src_sizes);
    //}

    //public def alltoallv[T] (role:Int, src:Array[T], src_offs:Array[Int], src_counts:Array[Int]) {
    //    assert(src != null);
    //    assert(src_offs != null);
    //    assert(src_counts != null);
    //    val dst_counts = alltoall(role, src_counts);
    //    val dst_offs = countsToOffs(dst_counts);
    //    val dst = alltoallv[T](role, src, src_offs, src_counts, dst_offs, dst_counts);
    //    debugln("alltoallv", "dst_counts: " + dst_counts);
    //    debugln("alltoallv", "dst_offs: " + dst_offs);
    //    debugln("alltoallv", "dst: " + dst);
    //    return dst;
    //}

    //public def alltoallv[T] (role:Int, src:Array[T], src_counts:Array[Int](1)) {
    //    assert(src != null);
    //    assert(src_counts != null);
    //    val src_offs = countsToOffs(src_counts);
    //    debugln("alltoallv", "src_offs: " + src_offs);
    //    return alltoallv[T](role, src, src_offs, src_counts);
    //}

    //public def alltoallv[T] (role:Int, src:Array[Array[T](1)](1)) {
    //    assert(src != null);
    //    val flatten_src_tuple = flatten(src);
    //    val flatten_src = flatten_src_tuple.first;
    //    val src_offs = flatten_src_tuple.second.first;
    //    val src_sizes = flatten_src_tuple.second.second;
    //    debugln("alltoallv", "src_counts: " + src_sizes);
    //    debugln("alltoallv", "src_offs: " + src_offs);
    //    debugln("alltoallv", "flatten_src: " + flatten_src);
    //    return alltoallv(role, flatten_src, src_offs, src_sizes);
    //}

    //private static val OPT_REMOTE_OP = 0;
    //private static val OPT_COLLECTIVES = 1;
    //private static val OPT_COLLECTIVES_APPEND = 2;

    //private static def nativeSupports (opt:Int) : Int {
    //    @Native("java", "return x10.x10rt.TeamSupport.nativeSize(opt);")
    //    @Native("c++", "return (x10_int)x10rt_supports(static_cast<x10rt_opt>(opt));") { return -1; }
    //}
}

// vim: shiftwidth=4:tabstop=4:expandtab
