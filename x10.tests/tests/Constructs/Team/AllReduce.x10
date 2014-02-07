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

import harness.x10Test;
import x10.util.Team;

/**
 * Unit tests for all reduce functionality of teams
 */
public class AllReduce extends x10Test {

    def allReduceTest(team:Team, res:GlobalRef[Cell[Boolean]]) {
        val count = 113L;
        val src = new Rail[Double](count, (i:long)=>((here.id+1) as Double) * i * i);
        val dst = new Rail[Double](count, (i:long)=>-(i as Double));
        var success: boolean = true;
                
        {
            team.allreduce(src, 0L, dst, 0L, count, Team.ADD);

            val oracle_base = ((team.size()*team.size() + team.size())/2) as Double;
            for (i in 0..(count-1)) {
                val oracle:double = oracle_base * i * i;
                if (dst(i) != oracle) {
                    Console.OUT.printf("Team %d place %d received invalid sum %f at %d instead of %f\n",
                                       team.id(), here.id, dst(i), i, oracle);
                    success = false;
                }
            }
        }

        {
            team.allreduce(src, 0L, dst, 0L, count, Team.MAX);

            val oracle_base = (team.size()) as Double;
            for (i in 0..(count-1)) {
                val oracle:double = oracle_base * i * i;
                if (dst(i) != oracle) {
                    Console.OUT.printf("Team %d place %d received invalid max %f at %d instead of %f\n",
                                       team.id(), here.id, dst(i), i, oracle);
                    success = false;
                }
            }
        }

        val src2 = new Rail[Complex](count, (i:long)=>(Complex(here.id+1, 0) * i * i));
        val dst2 = new Rail[Complex](count, (i:long)=>Complex(-i, 0));
                
        {
            team.allreduce(src2, 0L, dst2, 0L, count, Team.ADD);

            val oracle_base = Complex((team.size()*team.size() + team.size())/2, 0);
            for (i in 0..(count-1)) {
                val oracle:Complex = oracle_base * i * i;
                if (dst2(i) != oracle) {
                    Console.OUT.printf("Team %d place %d received invalid sum (%f,%f) at %d instead of (%f,%f)\n",
                                       [team.id(), here.id, dst2(i).re, dst2(i).im, i, oracle.re, oracle.im]);
                    success = false;
                }
            }
        }



        {
            team.allreduce(src, 0L, dst, 0L, count, Team.MIN);

            val oracle_base = 1.0f;
            for (i in 0..(count-1)) {
                val oracle:double = oracle_base * i * i;
                if (dst(i) != oracle) {
                    Console.OUT.printf("Team %d place %d received invalid min %f at %d instead of %f\n",
                                       team.id(), here.id, dst(i), i, oracle);
                    success = false;
                }
            }
        }

        val reducedSuccess = team.allreduce(success ? 1 : 0, Team.AND);

        team.barrier();

        if (reducedSuccess != 1) {
            Console.OUT.println("Reduced Success value was "+reducedSuccess+" but expected 1");
        }

        success &= (reducedSuccess == 1);

        if (!success) at (res.home) res().set(false);

    }

    public def run(): boolean {
        Console.OUT.println("Doing all reduce for World ("+Place.numPlaces()+" places)");
        val res:Cell[Boolean] = new Cell[Boolean](true);
        val gr:GlobalRef[Cell[Boolean]] = GlobalRef[Cell[Boolean]](res);
        finish for (p in Place.places()) {
            async at(p) allReduceTest(Team.WORLD, gr);
        }

        return res();
    }

    public static def main(args: Rail[String]) {
        new AllReduce().execute();
    }

}
