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

package x10.lang;

import x10.compiler.*;

import x10.util.Pair;
import x10.util.GrowableRail;
import x10.util.concurrent.AtomicLong;
import x10.util.concurrent.AtomicBoolean;
import x10.util.concurrent.SimpleLatch;

class ResilientStorePlaceZero {

    static me = new ResilientStorePlaceZero();

    // Turn this on to debug deadlocks within the finish implementation
    // moved VERBOSE flag to x10.lang.FinishState
    //static VERBOSE = false;


    /** Simply utility function to send a message to place zero at the x10rt level.
      * Propagates back the exception, if thrown.
      */
    private static def lowLevelAt(cl:()=>void) {
        if (here.id == 0l) {
            cl();
        } else {
            val exc = new GlobalRef(new Cell[Exception](null));
            val c = new GlobalRef(new AtomicBoolean());
            Runtime.x10rtSendMessage(0, () => @RemoteInvocation("low_level_at_out") {
                try {
                    cl();
                } catch (t:Exception) {
                    Runtime.x10rtSendMessage(c.home.id, () => @RemoteInvocation("low_level_at_back_exc") {
                        // [DC] assume that the write barrier on c is enough to see update on exc
                        exc.getLocalOrCopy()(t);
                        c.getLocalOrCopy().getAndSet(true);
                    }, null);
                }
                Runtime.x10rtSendMessage(c.home.id, () => @RemoteInvocation("low_level_at_back") {
                    c.getLocalOrCopy().getAndSet(true);
                }, null);
            }, null);
            while (!c().get()) Runtime.probe();
            if (exc()() != null) throw exc()();
        }
    }

    /** Simply utility function to send a message to place zero, that returns an Int (-1 used internally), at the x10rt level. */
    private static def lowLevelAtExprLong(cl:()=>Long) : Long {
        if (here.id == 0l) {
            return cl();
        } else {
            val c = new GlobalRef(new AtomicLong(-1l));
            Runtime.x10rtSendMessage(0, () => @RemoteInvocation("low_level_at_int_out") {
                val r = cl();
                Runtime.x10rtSendMessage(c.home.id, () => @RemoteInvocation("low_level_at_int_back") {
                    c.getLocalOrCopy().set(r);
                }, null);
            }, null);
            while (c().get()==-1l) Runtime.probe();
            return c().get();
        }
    }

    private static class State {

        val id : Long;
        val parent : State;
        val transit : Rail[Int];
        val live : Rail[Int];
        val transitAdopted : Rail[Int];
        val liveAdopted : Rail[Int];
        val homeId : Long;
        var adopted : Boolean;
        var adoptedParent : Long;
        var multipleExceptions : GrowableRail[Exception] = null;
        val latch : SimpleLatch;

        private def ensureMultipleExceptions() {
            if (multipleExceptions == null) multipleExceptions = new GrowableRail[Exception]();
            return multipleExceptions;
        }

        public def this(pfs:State, homeId:Long, id:Long, latch:SimpleLatch) {
            this.id = id;
            this.parent = pfs;
            this.transit = new Rail[Int](Place.MAX_PLACES * Place.MAX_PLACES, 0n);
            this.live = new Rail[Int](Place.MAX_PLACES, 0n);
            this.transitAdopted = new Rail[Int](Place.MAX_PLACES * Place.MAX_PLACES, 0n);
            this.liveAdopted = new Rail[Int](Place.MAX_PLACES, 0n);
            this.live(homeId) = 1n;
            if (FinishState.VERBOSE) Runtime.println("    initial live("+homeId+") == 1");
            this.homeId = homeId;
            this.adopted = false;
            this.latch = latch;
        }

        def findFirstNonDeadParent() : State {
            if (!Place.isDead(parent.homeId)) return parent;
            return parent.findFirstNonDeadParent();
        }

        def adopt(child:State) : void {
            for (i in 0..(Place.MAX_PLACES-1)) {
                liveAdopted(i) += child.live(i);
                liveAdopted(i) += child.liveAdopted(i);
                for (j in 0..(Place.MAX_PLACES-1)) {
                    transitAdopted(j + i*Place.MAX_PLACES) += child.transit(j + i*Place.MAX_PLACES);
                    transitAdopted(j + i*Place.MAX_PLACES) += child.transitAdopted(j + i*Place.MAX_PLACES);
                }
            }
            child.adopted = true;
            child.adoptedParent = id;
        }

        def addDeadPlaceException(p:Place) {
            val e = new DeadPlaceException(p);
            e.fillInStackTrace();
            ensureMultipleExceptions().add(e);
        }

    }

    // TODO: freelist to reuse ids (maybe also states)
    private val states = new GrowableRail[State]();

    private var numDead : Long = 0;


    static def make(homeId:Long, parentId:Long, latch:SimpleLatch) : Long {
        return lowLevelAtExprLong(() => {
            atomic {
                val pfs = parentId==-1l ? null : me.states(parentId);
                val id = me.states.size();
                if (FinishState.VERBOSE) Runtime.println("make("+parentId+","+id+") @ "+homeId);
                val fs = new State(pfs, homeId, id, latch);
                me.states.add(fs);
                return fs.id;
            }
        });
    }

    static def getStateAccountingForAdoption(id:Long) {
        var fs:State = me.states(id);
        var adopted : Boolean = false;
        while (fs.adopted) {
            adopted = true;
            fs = me.states(fs.adoptedParent);
        }
        return Pair(fs, adopted);
    }

    static def notifySubActivitySpawn(id:Long, srcId:Long, dstId:Long) {
        lowLevelAt(() => { atomic {
            if (FinishState.VERBOSE) Runtime.println("notifySubActivitySpawn("+id+", "+srcId+", "+dstId+")");
            val pair = getStateAccountingForAdoption(id);
            val fs = pair.first;
            val adopted = pair.second;
            if (adopted) {
                fs.transitAdopted(srcId + dstId*Place.MAX_PLACES)++;
            } else {
                fs.transit(srcId + dstId*Place.MAX_PLACES)++;
            }
            if (FinishState.VERBOSE) Runtime.println("    transit("+srcId+","+dstId+") == "+fs.transit(srcId + dstId*Place.MAX_PLACES));
        } });
    }

    static def notifyActivityCreation(id:Long, srcId:Long, dstId:Long) {
        return 1l==lowLevelAtExprLong(() => { atomic {
            if (FinishState.VERBOSE) Runtime.println("notifyActivityCreation("+id+", "+srcId+", "+dstId+")");
            if (Place(srcId).isDead()) return 0l;
            val pair = getStateAccountingForAdoption(id);
            val fs = pair.first;
            val adopted = pair.second;
            if (adopted) {
                fs.liveAdopted(dstId)++;
                fs.transitAdopted(srcId + dstId*Place.MAX_PLACES)--;
            } else {
                fs.live(dstId)++;
                fs.transit(srcId + dstId*Place.MAX_PLACES)--;
            }
            if (FinishState.VERBOSE) Runtime.println("    live("+dstId+") == "+fs.live(dstId));
            if (FinishState.VERBOSE) Runtime.println("    transit("+srcId+","+dstId+") == "+fs.transit(srcId + dstId*Place.MAX_PLACES));
            return 1l;
        } });
    }

    static def notifyActivityTermination(id:Long, dstId:Long) {
        lowLevelAt(() => { atomic {
            if (FinishState.VERBOSE) Runtime.println("notifyActivityTermination("+id+", "+dstId+")");
            val pair = getStateAccountingForAdoption(id);
            val fs = pair.first;
            val adopted = pair.second;
            if (adopted) {
                fs.liveAdopted(dstId)--;
            } else {
                fs.live(dstId)--;
            }
            if (FinishState.VERBOSE) Runtime.println("    live("+dstId+") == "+fs.live(dstId));
            if (fs.latch != null && me.quiescent(fs)) {
                if (FinishState.VERBOSE) Runtime.println("    Releasing latch...");
                fs.latch.release();
            }
        } });
    }

    static def notifyPlaceDeath(root_id:Long) {
        assert here == Place.FIRST_PLACE;
        me.pushUp();
        atomic {
            if (FinishState.VERBOSE) Runtime.println("Checking if root finished has quiesced after place death...");
            val root_fs = me.states(root_id);
            if (root_fs.latch != null && me.quiescent(root_fs)) {
                if (FinishState.VERBOSE) Runtime.println("    Releasing latch on root...");
                root_fs.latch.release();
            }
        }
    }

    static def pushException(id:Long, t:Exception) {
        lowLevelAt(() => { atomic {
            val fs = me.states(id);
            if (fs.adopted) {
                // ignoring exception since finish is dead
                if (FinishState.VERBOSE) Runtime.println("pushException("+id+", "+t+") dropped due to dead finish");
            } else {
                if (FinishState.VERBOSE) Runtime.println("pushException("+id+", "+t+")");
                fs.ensureMultipleExceptions().add(t);
            }
        } });
    }

    def quiescent(fs:State) : Boolean {

        // There is actually a race condition here (despite quiescent being called in an atomic section
        // The Place.isDead() can go to false between the pushUp() and the code after it, causing
        // a finish to 
        // TODO: store dead places in an array, use the same data to drive pushUp() and DPE generation

        val nd = Place.numDead();
        if (nd != me.numDead) {
            numDead = nd;
            pushUp();
        }

        // overwrite counters with 0 if places have died, accumuluate exceptions
        for (i in 0..(Place.MAX_PLACES-1)) {
            if (Place.isDead(i)) {
                for (unused in 1..fs.live(i)) {
                    fs.addDeadPlaceException(Place(i));
                }
                fs.live(i) = 0n;
                fs.liveAdopted(i) = 0n;

                // kill horizontal and vertical lines in transit matrix
                for (j in 0..(Place.MAX_PLACES-1)) {
                    // do not generate DPEs for these guys, they were technically never sent!
                    //for (unused in 1..fs.transit(i + j*Place.MAX_PLACES)) {
                    //    fs.addDeadPlaceException(Place(i));
                    //}
                    fs.transit(i + j*Place.MAX_PLACES) = 0n;
                    fs.transitAdopted(i + j*Place.MAX_PLACES) = 0n;

                    for (unused in 1..fs.transit(j + i*Place.MAX_PLACES)) {
                        fs.addDeadPlaceException(Place(i));
                    }
                    fs.transit(j + i*Place.MAX_PLACES) = 0n;
                    fs.transitAdopted(j + i*Place.MAX_PLACES) = 0n;
                }
            }
        }

        // [DC] a previous version of this used != instead of >
        // however when I made the adjustment to allow resilient finish to be used
        // as the root finish implementation (outside of main)
        // this as no longer adequate since finishes used as root finish are used in a
        // quirky fashion
        if (FinishState.VERBOSE) Runtime.println("quiescent("+fs.id+")");
        for (i in 0..(Place.MAX_PLACES-1)) {
            if (fs.live(i)>0) {
                if (FinishState.VERBOSE) Runtime.println("    "+fs.id+" Live at "+i);
                return false;
            }
            for (j in 0..(Place.MAX_PLACES-1)) {
                if (fs.transit(i + j*Place.MAX_PLACES)>0) {
                    if (FinishState.VERBOSE) Runtime.println("    "+fs.id+" In transit from "+i+" -> "+j);
                    return false;
                }
            }
        }
        for (i in 0..(Place.MAX_PLACES-1)) {
            if (fs.liveAdopted(i)>0) {
                if (FinishState.VERBOSE) Runtime.println("    "+fs.id+" Live (adopted) at "+i);
                return false;
            }
            for (j in 0..(Place.MAX_PLACES-1)) {
                if (fs.transitAdopted(i + j*Place.MAX_PLACES)>0) {
                    if (FinishState.VERBOSE) Runtime.println("    "+fs.id+" In transit (adopted) from "+i+" -> "+j);
                    return false;
                }
            }
        }

        return true;
    }

    /** Grandfather activities under a dead finish into the nearest parent finish at a place that is still alive. */
    private def pushUp() : void {
        atomic {
            for (i in 0..(states.size()-1)) {
                val fs = states(i);
                if (fs.adopted) continue;
                if (Place.isDead(fs.homeId)) {
                    val pfs = fs.findFirstNonDeadParent();
                    if (FinishState.VERBOSE) Runtime.println("Finish has died ("+fs.id+"), adopting activities into ("+pfs.id+")");
                    pfs.adopt(fs);
                }
            }
        }
    }

    static def waitForFinish(id:Long) {
        lowLevelAt(() => {
            if (FinishState.VERBOSE) Runtime.println("waitForFinish("+id+")");
            val s : State;
            atomic {
                s = me.states(id);
            }
            notifyActivityTermination(id, s.homeId);
            when (s.adopted || me.quiescent(s)) { }
            if (!s.adopted) {
                if (s.multipleExceptions != null) {
                    if (FinishState.VERBOSE) Runtime.println("waitForFinish("+id+") done waiting (throwing exceptions)");
                    throw new MultipleExceptions(s.multipleExceptions);
                }
                if (FinishState.VERBOSE) Runtime.println("waitForFinish("+id+") done waiting");
            } else {
                if (FinishState.VERBOSE) Runtime.println("waitForFinish("+id+") done waiting, finish was dead (cleaning up)");
            }
        });
    }
}


