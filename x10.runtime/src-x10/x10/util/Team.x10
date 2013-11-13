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

import x10.compiler.Native;
import x10.util.concurrent.AtomicInteger;
import x10.util.concurrent.Lock;
import x10.util.Pair;
import x10.compiler.Uncounted;
import x10.compiler.Pragma;

/** Interface to low level collective operations, using the Rail API.  
 * A team is a collection of activities that work together by simultaneously 
 * doing 'collective operations', expressed as calls to methods in the Team struct.
 * All methods are blocking operations.
 */
public struct Team {
    private static struct DoubleIdx(value:Double, idx:Int) {}
    private static val DEBUG:Boolean = false;
    private static val DEBUGINTERNALS:Boolean = false;

    /** A team that has one member at each place. */
    public static val WORLD = Team(0n, PlaceGroup.WORLD, here.id());
    
    // TODO: the role argument is not really needed, and can be buried in lower layers, 
    // but BG/P is difficult to modify so we need to track it for now
    private static val roles:GrowableRail[Int] = new GrowableRail[Int](); // only used with native collectives
    private static val state:GrowableRail[LocalTeamState] = new GrowableRail[LocalTeamState](); // only used with X10 emulated collectives

    private val collectiveSupportLevel:int; // what level of collectives are supported
    // these values correspond to x10rt_types:x10rt_coll_support
    private static val X10RT_COLL_NOCOLLECTIVES:int = 0n;
    private static val X10RT_COLL_BARRIERONLY:int = 1n;
    private static val X10RT_COLL_ALLBLOCKINGCOLLECTIVES:int = 2n;
    private static val X10RT_COLL_NONBLOCKINGBARRIER:int = 3n;
    private static val X10RT_COLL_ALLNONBLOCKINGCOLLECTIVES:int = 4n;
    
    private val id:Int; // team ID
    public def id() = id;
    
    // this constructor is intended to be called at all places of a split, at the same time.
    private def this (id:Int, places:PlaceGroup, role:Long) {
    	this.id = id;
        collectiveSupportLevel = nativeCollectiveSupport();
        if (DEBUG) Runtime.println(here + " reported native collective support level of " + collectiveSupportLevel);
        if (collectiveSupportLevel > X10RT_COLL_NOCOLLECTIVES) {
            if (Team.roles.capacity() <= id) // TODO move this check into the GrowableRail.grow() method
                Team.roles.grow(id+1);
            while (Team.roles.size() < id)
                Team.roles.add(-1n); // I am not a member of this team id.  Insert a dummy value.
            Team.roles(id) = role as Int;
            if (DEBUG) Runtime.println(here + " created native team "+id);
    	}
        if (collectiveSupportLevel < X10RT_COLL_ALLNONBLOCKINGCOLLECTIVES) {
            if (DEBUG) Runtime.println(here + " creating our own team "+id);
            if (Team.state.capacity() <= id) // TODO move this check into the GrowableRail.grow() method
                Team.state.grow(id+1);
            while (Team.state.size() < id)
                Team.state.add(null); // I am not a member of this team id.  Insert a dummy value.
            Team.state(id) = new LocalTeamState(places, id);
            Team.state(id).init();
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
	        for (var i:Long=0L; i<count; i++)
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
            atomic {
                val teamidcopy = this.id;
                PlaceGroup.WORLD.broadcastFlat(()=>{
                    if (Team.state.capacity() <= teamidcopy)
                        Team.state.grow(teamidcopy+1);
                    while (Team.state.size() < teamidcopy)
                        Team.state.add(null); // I am not a member of this team id.  Insert a dummy value.
                    Team.state(teamidcopy) = new LocalTeamState(places, teamidcopy);
                    Team.state(teamidcopy).init();
                });
	        }
	    }
    }

    private static def nativeMake (places:Rail[Int], count:Int, result:Rail[Int]) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeMake(places, count, result);")
    	@Native("c++", "x10rt_team_new(count, (x10rt_place*)places->raw, x10aux::coll_handler2, x10aux::coll_enter2(result->raw));") {}
    }
    
    private static def nativeCollectiveSupport() : Int {
    	@Native("java", "return x10.x10rt.X10RT.collectiveSupport();")
    	@Native("c++", "return x10rt_coll_support();") { return -1n; }
    }

    /** Returns the number of places in the team.
     */
    public def size () : Long {
    	if (collectiveSupportLevel > X10RT_COLL_NOCOLLECTIVES)
    	    return nativeSize(id);
    	else
    	    return Team.state(id).places.size();
    }

    private static def nativeSize (id:Int) : Int {
        @Native("java", "return x10.x10rt.TeamSupport.nativeSize(id);")
        @Native("c++", "return (x10_int)x10rt_team_sz(id);") { return -1n; }
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
    	    state(id).collective_impl[Int](LocalTeamState.COLL_BARRIER, Place.FIRST_PLACE, null, 0, null, 0, 0, 0n);
    	}
        if (DEBUG) Runtime.println(here + " leaving barrier of team "+id);
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
    public def scatter[T] (root:Place, src:Rail[T], src_off:Long, dst:Rail[T], dst_off:Long, count:Long) : void {
        if (collectiveSupportLevel == X10RT_COLL_ALLNONBLOCKINGCOLLECTIVES)
            finish nativeScatter(id, id==0n?here.id() as Int:Team.roles(id), root.id() as Int, src, src_off as Int, dst, dst_off as Int, count as Int);
        else if (collectiveSupportLevel == X10RT_COLL_ALLBLOCKINGCOLLECTIVES || collectiveSupportLevel == X10RT_COLL_NONBLOCKINGBARRIER) {
            barrier();
            finish nativeScatter(id, id==0n?here.id() as Int:Team.roles(id), root.id() as Int, src, src_off as Int, dst, dst_off as Int, count as Int);
        }
    	else
    	    state(id).collective_impl[T](LocalTeamState.COLL_SCATTER, root, src, src_off, dst, dst_off, count, 0n);
    }

    private static def nativeScatter[T] (id:Int, role:Int, root:Int, src:Rail[T], src_off:Int, dst:Rail[T], dst_off:Int, count:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeScatter(id, role, root, src, src_off, dst, dst_off, count);")
        @Native("c++", "x10rt_scatter(id, role, root, &src->raw[src_off], &dst->raw[dst_off], sizeof(TPMGL(T)), count, x10aux::coll_handler, x10aux::coll_enter());") {}
    }

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
     public def bcast[T] (root:Place, src:Rail[T], src_off:Long, dst:Rail[T], dst_off:Long, count:Long) : void {
        if (collectiveSupportLevel == X10RT_COLL_ALLNONBLOCKINGCOLLECTIVES)
            finish nativeBcast(id, id==0n?here.id() as Int:Team.roles(id), root.id() as Int, src, src_off as Int, dst, dst_off as Int, count as Int);
        else if (collectiveSupportLevel == X10RT_COLL_ALLBLOCKINGCOLLECTIVES || collectiveSupportLevel == X10RT_COLL_NONBLOCKINGBARRIER) {
            barrier();
            finish nativeBcast(id, id==0n?here.id() as Int:Team.roles(id), root.id() as Int, src, src_off as Int, dst, dst_off as Int, count as Int);
        }
     	else
     	    state(id).collective_impl[T](LocalTeamState.COLL_BROADCAST, root, src, src_off, dst, dst_off, count, 0n);
    }

    private static def nativeBcast[T] (id:Int, role:Int, root:Int, src:Rail[T], src_off:Int, dst:Rail[T], dst_off:Int, count:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeBcast(id, role, root, src, src_off, dst, dst_off, count);")
        @Native("c++", "x10rt_bcast(id, role, root, &src->raw[src_off], &dst->raw[dst_off], sizeof(TPMGL(T)), count, x10aux::coll_handler, x10aux::coll_enter());") {}
    }

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
    	    state(id).collective_impl[T](LocalTeamState.COLL_ALLTOALL, Place.FIRST_PLACE, src, src_off, dst, dst_off, count, 0n);
    	}
        if (DEBUG) Runtime.println(here + " leaving alltoall of team "+id);
    }
    
    private static def nativeAlltoall[T](id:Int, role:Int, src:Rail[T], src_off:Int, dst:Rail[T], dst_off:Int, count:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeAllToAll(id, role, src, src_off, dst, dst_off, count);")
        @Native("c++", "x10rt_alltoall(id, role, &src->raw[src_off], &dst->raw[dst_off], sizeof(TPMGL(T)), count, x10aux::coll_handler, x10aux::coll_enter());") {}
    }

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
    public static val MAX = 6n;
    /** Indicates the operation to perform when reducing. */
    public static val MIN = 7n;

    /* using overloading is the correct thing to do here since the set of supported
     * types are finite, however the java backend will not be able to distinguish
     * these methods' prototypes so we use the unsafe generic approach for now.
     */

    /** Blocks until all members have received the computed result.  Note that not all values of T are valid.
     * The dst array is populated for all members with the result of the operation applied pointwise to all given src arrays.
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
    public def reduce[T] (root:Place, src:Rail[T], src_off:Long, dst:Rail[T], dst_off:Long, count:Long, op:Int) : void {
        if (collectiveSupportLevel == X10RT_COLL_ALLNONBLOCKINGCOLLECTIVES)
            finish nativeReduce(id, id==0n?here.id() as Int:Team.roles(id), root.id() as Int, src, src_off as Int, dst, dst_off as Int, count as Int, op);
        else if (collectiveSupportLevel == X10RT_COLL_ALLBLOCKINGCOLLECTIVES || collectiveSupportLevel == X10RT_COLL_NONBLOCKINGBARRIER) {
            barrier();
            finish nativeReduce(id, id==0n?here.id() as Int:Team.roles(id), root.id() as Int, src, src_off as Int, dst, dst_off as Int, count as Int, op);
        }
    	else
    	    state(id).collective_impl[T](LocalTeamState.COLL_REDUCE, root, src, src_off, dst, dst_off, count, op);
    }
	
    private static def nativeReduce[T](id:Int, role:Int, root:Int, src:Rail[T], src_off:Int, dst:Rail[T], dst_off:Int, count:Int, op:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeReduce(id, role, root, src, src_off, dst, dst_off, count, op);")
    	@Native("c++", "x10rt_reduce(id, role, root, &src->raw[src_off], &dst->raw[dst_off], (x10rt_red_op_type)op, x10rt_get_red_type<TPMGL(T)>(), count, x10aux::coll_handler, x10aux::coll_enter());") {}
    }

    /** Performs a reduction on a single value, returning the result at the root */
    public def reduce (root:Place, src:Byte, op:Int) = genericReduce(root, src, op);
    /** Performs a reduction on a single value, returning the result at the root */
    public def reduce (root:Place, src:UByte, op:Int) = genericReduce(root, src, op);
    /** Performs a reduction on a single value, returning the result at the root */
    public def reduce (root:Place, src:Short, op:Int) = genericReduce(root, src, op);
    /** Performs a reduction on a single value, returning the result at the root */
    public def reduce (root:Place, src:UShort, op:Int) = genericReduce(root, src, op);
    /** Performs a reduction on a single value, returning the result at the root */
    public def reduce (root:Place, src:UInt, op:Int) = genericReduce(root, src, op);
    /** Performs a reduction on a single value, returning the result at the root */
    public def reduce (root:Place, src:Int, op:Int) = genericReduce(root, src, op);
    /** Performs a reduction on a single value, returning the result at the root */
    public def reduce (root:Place, src:Long, op:Int) = genericReduce(root, src, op);
    /** Performs a reduction on a single value, returning the result at the root */
    public def reduce (root:Place, src:ULong, op:Int) = genericReduce(root, src, op);
    /** Performs a reduction on a single value, returning the result at the root */
    public def reduce (root:Place, src:Float, op:Int) = genericReduce(root, src, op);
    /** Performs a reduction on a single value, returning the result at the root */
    public def reduce (root:Place, src:Double, op:Int) = genericReduce(root, src, op);

    private def genericReduce[T] (root:Place, src:T, op:Int) : T {
        val chk = new Rail[T](1, src);
        val dst = new Rail[T](1, src);
        if (collectiveSupportLevel == X10RT_COLL_ALLNONBLOCKINGCOLLECTIVES)
            finish nativeReduce[T](id, id==0n?here.id() as Int:Team.roles(id), root.id() as Int, chk, dst, op);
        else if (collectiveSupportLevel == X10RT_COLL_ALLBLOCKINGCOLLECTIVES || collectiveSupportLevel == X10RT_COLL_NONBLOCKINGBARRIER) {
            barrier();
            finish nativeReduce[T](id, id==0n?here.id() as Int:Team.roles(id), root.id() as Int, chk, dst, op);
        }
        else
        	state(id).collective_impl[T](LocalTeamState.COLL_REDUCE, root, chk, 0, dst, 0, 1, op);
        return dst(0);
    }

    private static def nativeReduce[T](id:Int, role:Int, root:Int, src:Rail[T], dst:Rail[T], op:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeReduce(id, role, root, src, 0, dst, 0, 1, op);")
        @Native("c++", "x10rt_reduce(id, role, root, src->raw, dst->raw, (x10rt_red_op_type)op, x10rt_get_red_type<TPMGL(T)>(), 1, x10aux::coll_handler, x10aux::coll_enter());") {}
    }

    /** Blocks until all members have received the computed result.  Note that not all values of T are valid.
     * The dst array is populated for all members with the result of the operation applied pointwise to all given src arrays.
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
    	    state(id).collective_impl[T](LocalTeamState.COLL_ALLREDUCE, Place.FIRST_PLACE, src, src_off, dst, dst_off, count, op);
    	}
        if (DEBUG) Runtime.println(here + " Finished allreduce on team "+id);
    }

    private static def nativeAllreduce[T](id:Int, role:Int, src:Rail[T], src_off:Int, dst:Rail[T], dst_off:Int, count:Int, op:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeAllReduce(id, role, src, src_off, dst, dst_off, count, op);")
    	@Native("c++", "x10rt_allreduce(id, role, &src->raw[src_off], &dst->raw[dst_off], (x10rt_red_op_type)op, x10rt_get_red_type<TPMGL(T)>(), count, x10aux::coll_handler, x10aux::coll_enter());") {}
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
            state(id).collective_impl[T](LocalTeamState.COLL_ALLREDUCE, Place.FIRST_PLACE, chk, 0, dst, 0, 1, op);
        return dst(0);
    }

    private static def nativeAllreduce[T](id:Int, role:Int, src:Rail[T], dst:Rail[T], op:Int) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeAllReduce(id, role, src, 0, dst, 0, 1, op);")
        @Native("c++", "x10rt_allreduce(id, role, src->raw, dst->raw, (x10rt_red_op_type)op, x10rt_get_red_type<TPMGL(T)>(), 1, x10aux::coll_handler, x10aux::coll_enter());") {}
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
            state(id).collective_impl[DoubleIdx](LocalTeamState.COLL_INDEXOFMAX, Place.FIRST_PLACE, src, 0, dst, 0, 1, 0n);
        return dst(0).idx;
    }

    private static def nativeIndexOfMax(id:Int, role:Int, src:Rail[DoubleIdx], dst:Rail[DoubleIdx]) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeIndexOfMax(id, role, src, dst);")
        @Native("c++", "x10rt_allreduce(id, role, src->raw, dst->raw, X10RT_RED_OP_MAX, X10RT_RED_TYPE_DBL_S32, 1, x10aux::coll_handler, x10aux::coll_enter());") {}
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
            state(id).collective_impl[DoubleIdx](LocalTeamState.COLL_INDEXOFMIN, Place.FIRST_PLACE, src, 0, dst, 0, 1, 0n);
        return dst(0).idx;
    }

    private static def nativeIndexOfMin(id:Int, role:Int, src:Rail[DoubleIdx], dst:Rail[DoubleIdx]) : void {
        @Native("java", "x10.x10rt.TeamSupport.nativeIndexOfMin(id, role, src, dst);")
        @Native("c++", "x10rt_allreduce(id, role, src->raw, dst->raw, X10RT_RED_OP_MIN, X10RT_RED_TYPE_DBL_S32, 1, x10aux::coll_handler, x10aux::coll_enter());") {}
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
            myTeamPosition:long = Team.state(this.id).places.indexOf(here.id()) * 2;
            allInfo(myTeamPosition) = color;
            allInfo(myTeamPosition+1) = new_role as Int;
            
            if (DEBUGINTERNALS) Runtime.println(here + " completed alltoall for splitting team "+id+" color="+color+" new_role="+new_role+" allInfo="+allInfo);
        	// use the above to figure out the members of *my* team
            // count the new team size
            var numPlacesInMyTeam:Int = 0n;
            for (var i:long=0; i<allInfo.size; i+=2)
                if (allInfo(i) == color)
                	numPlacesInMyTeam++;

            if (DEBUGINTERNALS) Runtime.println(here + " my new team has "+numPlacesInMyTeam+" places");
            // create a new PlaceGroup with all members of my new team
            val newTeamPlaceRail:Rail[Place] = new Rail[Place](numPlacesInMyTeam);
            for (var i:long=0; i<allInfo.size; i+=2) {
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
        @Native("java", "x10.x10rt.TeamSupport.nativeSplit(id, role, color, new_role, result);")
        @Native("c++", "x10rt_team_split(id, role, color, new_role, x10aux::coll_handler2, x10aux::coll_enter2(result->raw));") {}
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
        @Native("c++", "x10rt_team_del(id, role, x10aux::coll_handler, x10aux::coll_enter());") {}
    }

    public def toString() = "Team(" + this.id + ")";
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
    private static class LocalTeamState(places:PlaceGroup, teamid:Int) {	    
        private static PHASE_IDLE:Int = 0n;    // normal state, nothing in progress
        private static PHASE_GATHER1:Int = 1n; // waiting for signal/data from first child
        private static PHASE_GATHER2:Int = 2n; // waiting for signal/data from second child
        private static PHASE_SCATTER:Int = 3n; // waiting for signal/data from parent
        private var phase:AtomicInteger = new AtomicInteger(PHASE_IDLE); // which of the above phases we're in

        private static COLL_BARRIER:Int = 0n; // no data moved
        private static COLL_SCATTER:Int = 1n; // data out only
        private static COLL_BROADCAST:Int = 2n; // data out only
        private static COLL_REDUCE:Int = 3n; // data in only
        private static COLL_ALLTOALL:Int = 4n; // data in and out
        private static COLL_ALLREDUCE:Int = 5n; // data in and out
        private static COLL_INDEXOFMIN:Int = 6n; // data in and out
        private static COLL_INDEXOFMAX:Int = 7n; // data in and out

        // local data movement fields associated with the local arguments passed in collective_impl
        private var local_src:Any = null; // becomes type Rail[T]{self!=null}
        private var local_src_off:Long = 0;
        private var local_dst:Any = null; // becomes type Rail[T]{self!=null}
        private var local_dst_off:Long = 0;
        private var local_count:Long = 0;
        private var myPosition:Long = Place.INVALID_PLACE.id();

        private static def getCollName(collType:Int):String {
            switch (collType) {
                case COLL_BARRIER: return "Barrier";
                case COLL_SCATTER: return "Scatter";
                case COLL_BROADCAST: return "Broadcast";
                case COLL_REDUCE: return "Reduce";
                case COLL_ALLTOALL: return "AllToAll";
                case COLL_ALLREDUCE: return "AllReduce";
                case COLL_INDEXOFMIN: return "IndexOfMin";
                case COLL_INDEXOFMAX: return "IndexOfMax";
                default: return "Unknown";
            }
        }
	    
	    /* Utility methods to traverse binary tree structure.  The tree is not built using the place id's 
	     * to determine the position in the tree, but rather the position in the places:PlaceGroup field to 
	     * determine the position.  The first place in 'places' is the root of the tree, the next two its children, 
	     * and so on.  For collective operations that specify a root, the tree will use that root's position in 
	     * 'places' as the tree root, swapping it with the place in places(0), which would otherwise be root
	     * 
	     * A return value of Place.INVALID_PLACE means that the parent/child does not exist.
	     */
	    private def getParentId(root:Place):Place {
	        if (here == root) return Place.INVALID_PLACE;
	        rootPosition:Long = places.indexOf(root);
	        if (rootPosition == -1) return Place.INVALID_PLACE;
	        if (myPosition == -1) myPosition = places.indexOf(Runtime.hereLong());
	        if (myPosition == 0) return places((rootPosition-1)/2);
	        
	        parentPosition:Long = (myPosition-1)/2;
	        if (parentPosition == 0)
	        	return root;
	        else if (parentPosition == rootPosition)
		        return places(0); // swap root with index 0
	        else
		        return places(parentPosition);
	    }
	    
	    private def getChildIds(root:Place):Pair[Place,Place] {
	    	rootPosition:Long = places.indexOf(root);
	        if (rootPosition == -1) return Pair[Place,Place](Place.INVALID_PLACE, Place.INVALID_PLACE); // invalid root specified
	        val childPosition:Long;
	        if (here == root)
	        	childPosition = 1;
	        else {
	        	if (myPosition == -1) myPosition = places.indexOf(Runtime.hereLong());
	        	if (myPosition == 0)
	        		childPosition = (rootPosition*2)+1;
	        	else
	        		childPosition = (myPosition*2)+1;
	        }
	        
	        if (childPosition >= places.numPlaces())
	        	return Pair[Place,Place](Place.INVALID_PLACE, Place.INVALID_PLACE); // no children
	        else if (childPosition+1 >= places.numPlaces())
	        	return Pair[Place,Place]((childPosition==rootPosition)?places(0):places(childPosition), Place.INVALID_PLACE); // one child only
	        else
	        	return Pair[Place,Place]((childPosition==rootPosition)?places(0):places(childPosition), (childPosition+1==rootPosition)?places(0):places(childPosition+1)); // two children
	    }
	    
	    
	    /* Collective operations, to be executed on this team */

	    
	    
	    // This is an internal barrier which can be called at the end of team creation.  The regular
	    // barrier method assumes that the team is already in place.  This method adds some pre-checks
	    // to ensure that the state information for the entire team is in place before running the 
	    // regular barrier, which does not have these checks.
	    
	    // implementation note: There are several instances of "System.sleep(..)" in the code
	    // below, where we are waiting for a remote entity to update the state.  I have tried 
	    // using nohting in that position, as well as Runtime.probe(), and both of those options
	    // lead to deadlocks.  System.sleep(..), which internally increases then decreases 
	    // parallelism, seems to do the trick.
	    private def init() {
	        if (DEBUGINTERNALS) Runtime.println(here + " entering init phase");
		    parent:Place = getParentId(places(0));
	        if (DEBUGINTERNALS) Runtime.println(here+":team"+this.teamid+", has parent "+parent);
	    	val teamidcopy = this.teamid; // needed to prevent serializing "this"
		    if (parent != Place.INVALID_PLACE) {
			    @Pragma(Pragma.FINISH_ASYNC) finish at (parent) async {
			        while (Team.state.size() <= teamidcopy) {
			    	    System.sleep(10);
			}   }   }
		    if (DEBUGINTERNALS) Runtime.println(here+":team"+this.teamid+", moving on to init barrier");
		    collective_impl[Int](COLL_BARRIER, Place.FIRST_PLACE, null, 0, null, 0, 0, 0n); // barrier
		    if (DEBUGINTERNALS) Runtime.println(here + " leaving init phase");
		}

	    /*
	     * This method contains the implementation for all collectives.  Some arguments are only valid
	     * for specific collectives.
	     */
	    private def collective_impl[T](collType:Int, root:Place, src:Rail[T], src_off:Long, dst:Rail[T], dst_off:Long, count:Long, operation:Int):void {
	        if (DEBUGINTERNALS) Runtime.println(here+":team"+teamid+" entered "+getCollName(collType)+" (phase="+phase.get()+", root="+root+")");
	    	// block if some other collective is in progress.
	    	while (!this.phase.compareAndSet(PHASE_IDLE, PHASE_GATHER1))
	    		System.sleep(10);
	    
	        // make my local data arrays visible to other places
	    	local_src = src;
	    	local_src_off = src_off;
	        local_dst = dst;
	        local_dst_off = dst_off;
	        local_count = count;
	        
	    	//Runtime.println(here+":team"+teamid+" entered "+getCollName(collType)+" PHASE_GATHER1");
	    	parent:Place = getParentId(root);
	        children:Pair[Place,Place] = getChildIds(root);
	    	val teamidcopy = this.teamid; // needed to prevent serializing "this" in at() statements	    	
	        if (DEBUGINTERNALS) Runtime.println(here+":team"+teamidcopy+", root="+root+" has parent "+parent);
	        if (DEBUGINTERNALS) Runtime.println(here+":team"+teamidcopy+", root="+root+" has children "+children);

	    	
	    	// Start out waiting for all children to update our state 
	    	if (children.first == Place.INVALID_PLACE) // no children to wait for
	    		this.phase.compareAndSet(PHASE_GATHER1, PHASE_SCATTER);
	    	else if (children.second == Place.INVALID_PLACE) { // only one child, so skip a phase waiting for the second child.
	    		if (!this.phase.compareAndSet(PHASE_GATHER1, PHASE_GATHER2)) 
	    			this.phase.compareAndSet(PHASE_GATHER2, PHASE_SCATTER); 
	    	}
	        if (DEBUGINTERNALS) Runtime.println(here+":team"+teamidcopy+" waiting for children");
	    	while (this.phase.get() != PHASE_SCATTER) // wait for updates from children, not already skipped
	    		System.sleep(10);
	        if (DEBUGINTERNALS) Runtime.println(here+":team"+teamidcopy+" released by children");
	    
	        // all children have checked in.  Update our parent, and then wait for the parent to update us 
	    	if (parent == Place.INVALID_PLACE) { // this is the root
	    		// copy data locally from src to dst if needed
	    		if (collType == COLL_BROADCAST) {
	                if (DEBUGINTERNALS) Runtime.println(here+ " broadcasting data locally from "+src+" to "+dst);
	    			Rail.copy(src, src_off, dst, dst_off, count);
	                if (DEBUGINTERNALS) Runtime.println(here+ " dst now contains "+dst);
	    		}
	    		else if (collType == COLL_SCATTER) {
	                if (DEBUGINTERNALS) Runtime.println(here+ " copying data locally from "+src+" to "+dst);
			    	Rail.copy(src, src_off+(count*myPosition), dst, dst_off, count);
			        if (DEBUGINTERNALS) Runtime.println(here+ " dst now contains "+dst);
			    }
	    
	    		this.phase.set(PHASE_IDLE); // the root node has no parent, and can skip ahead
	    	}
	    	else {
	            @Pragma(Pragma.FINISH_ASYNC) finish at (parent) async { // increment the phase of the parent
	    			while(!Team.state(teamidcopy).phase.compareAndSet(PHASE_GATHER1, PHASE_GATHER2) && 
	    					!Team.state(teamidcopy).phase.compareAndSet(PHASE_GATHER2, PHASE_SCATTER))
	    				System.sleep(10);
	                if (DEBUGINTERNALS) Runtime.println(here+" has been set to phase "+Team.state(teamidcopy).phase.get());
	    		}
	            if (DEBUGINTERNALS) Runtime.println(here+ " waiting for parent "+parent+":team"+teamidcopy+" to release us from phase "+phase.get());
			    while (this.phase.get() != PHASE_IDLE) // wait for parent to set us free
			    	System.sleep(10);
			    if (DEBUGINTERNALS) Runtime.println(here+ " released by parent");
	    	}

	    	// move data from parent to children
	    	if (children.first != Place.INVALID_PLACE) {
		    	if (collType == COLL_BROADCAST) {
		    		val notnulldst:Rail[T]{self!=null} = dst as Rail[T]{self!=null};
		    		gr:GlobalRail[T] = new GlobalRail[T](notnulldst);
		    		finish {
		    			at (children.first) {
		                    if (DEBUGINTERNALS) Runtime.println(here+ " pulling data from "+gr+" into "+(local_dst as Rail[T]));
		    				Rail.asyncCopy(gr, dst_off, Team.state(teamidcopy).local_dst as Rail[T], Team.state(teamidcopy).local_dst_off, Team.state(teamidcopy).local_count);
		    			}
		    			if (children.second != Place.INVALID_PLACE) {
		    				at (children.second) {
		                        if (DEBUGINTERNALS) Runtime.println(here+ " pulling data from "+gr+" into "+(local_dst as Rail[T]));
		    					Rail.asyncCopy(gr, src_off, Team.state(teamidcopy).local_dst as Rail[T], Team.state(teamidcopy).local_dst_off, Team.state(teamidcopy).local_count);
		    				}
		    			}
		    		}
		            if (DEBUGINTERNALS) Runtime.println(here+ " finished moving data to children");
		    	}
		    	else if (collType == COLL_SCATTER) {
		    		//Runtime.println(here+ " pulling in data from "+root);
		    		val notnulldst:Rail[T]{self!=null} = dst as Rail[T]{self!=null};
		    		gr:GlobalRail[T] = new GlobalRail[T](notnulldst);
		    		val offset:Long = src_off*count*myPosition;
		            @Pragma(Pragma.FINISH_ASYNC_AND_BACK) finish { at (root) {
		    			Rail.asyncCopy(Team.state(teamidcopy).local_src as Rail[T], offset, gr, dst_off, Team.state(teamidcopy).local_count);
		    		}}
		    	}
	    	}
	    
	    	// our parent has updated us - update any children, and leave the barrier
	        if (children.first != Place.INVALID_PLACE) { // free the first child, if it exists
	            @Pragma(Pragma.FINISH_ASYNC) finish at (children.first) async {
	    			if (!Team.state(teamidcopy).phase.compareAndSet(PHASE_SCATTER, PHASE_IDLE))
	    				Runtime.println("ERROR root setting the first child "+here+":team"+teamidcopy+" to PHASE_IDLE");
	    			//else Runtime.println("set the first child "+here+":team"+teamidcopy+" to PHASE_IDLE");
	    		}
	    		if (children.second != Place.INVALID_PLACE) {
	                @Pragma(Pragma.FINISH_ASYNC) finish at (children.second) async {
	    				if (!Team.state(teamidcopy).phase.compareAndSet(PHASE_SCATTER, PHASE_IDLE))
	    					Runtime.println("ERROR root setting the second child "+here+":team"+teamidcopy+" to PHASE_IDLE");
	    				//else Runtime.println("set the second child "+here+":team"+teamidcopy+" to PHASE_IDLE");
	    			}
	    		}
	    	}
	        
	        local_src = null;
	        local_dst = null;
	        myPosition = -1; // Place.INVALID_PLACE.id();
	        // done!
	        if (DEBUGINTERNALS) Runtime.println(here+":team"+teamidcopy+" leaving "+getCollName(collType));
	    }
	}
}

// vim: shiftwidth=4:tabstop=4:expandtab
