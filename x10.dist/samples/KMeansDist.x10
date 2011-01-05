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

import x10.io.Console;
import x10.util.Random;

public class KMeansDist {

    static val DIM=2, CLUSTERS=4, POINTS=2000, ITERATIONS=50;

    static val points_region = 0..(POINTS-1)*0..(DIM-1);

    public static def main (Array[String]) {
        val rnd = PlaceLocalHandle.make[Random](Dist.makeUnique(), () => new Random(0));
        val local_curr_clusters = PlaceLocalHandle.make[Array[Float](1)](Dist.makeUnique(), 
                                                                            () => new Array[Float](CLUSTERS*DIM));
        val local_new_clusters = PlaceLocalHandle.make[Array[Float](1)](Dist.makeUnique(),
							                   () =>  new Array[Float](CLUSTERS*DIM));
        val local_cluster_counts = PlaceLocalHandle.make[Array[Int](1)](Dist.makeUnique(), 
                                                                           ()=> new Array[Int](CLUSTERS));

        val points_dist = Dist.makeBlock(points_region, 0);
        val points = DistArray.make[Float](points_dist, (p:Point)=>rnd().nextFloat());

        val central_clusters = new Array[Float](CLUSTERS*DIM, (i:int) => {
            val p = Point.make([i/DIM, i%DIM]);
            return at (points_dist(p)) points(p);
        });

	val old_central_clusters = new Array[Float](CLUSTERS*DIM);

        val central_cluster_counts = new Array[Int](CLUSTERS);

        for (i in 1..ITERATIONS) {

            Console.OUT.println("Iteration: "+i);

            for (var j:Int=0 ; j<CLUSTERS ; ++j) {
                local_cluster_counts()(j) = 0;
            }

            finish {
                // reset state
                for (d in points_dist.places()) async at(d) {
                    for (var j:Int=0 ; j<DIM*CLUSTERS ; ++j) {
                        local_curr_clusters()(j) = central_clusters(j);
                        local_new_clusters()(j) = 0;
                    }
                    for (var j:Int=0 ; j<CLUSTERS ; ++j) {
                        local_cluster_counts()(j) = 0;
                    }
                }
            }

            finish {
                // compute new clusters and counters
                for (var p_:Int=0 ; p_<POINTS ; ++p_) {
                    val p = p_;
                    async at(points_dist(p,0)) {
                        var closest:Int = -1;
                        var closest_dist:Float = Float.MAX_VALUE;
                        for (var k:Int=0 ; k<CLUSTERS ; ++k) { 
                            var dist : Float = 0;
                            for (var d:Int=0 ; d<DIM ; ++d) { 
                                val tmp = points(Point.make(p,d)) - local_curr_clusters()(k*DIM+d);
                                dist += tmp * tmp;
                            }
                            if (dist < closest_dist) {
                                closest_dist = dist;
                                closest = k;
                            }
                        }
                        for (var d:Int=0 ; d<DIM ; ++d) { 
                            local_new_clusters()(closest*DIM+d) += points(Point.make(p,d));
                        }
                        local_cluster_counts()(closest)++;
                    }
                }
            }


            for (var j:Int=0 ; j<DIM*CLUSTERS ; ++j) {
                old_central_clusters(j) = central_clusters(j);
                central_clusters(j) = 0;
            }

            for (var j:Int=0 ; j<CLUSTERS ; ++j) {
                central_cluster_counts(j) = 0;
            }

            finish {
                val central_clusters_gr = GlobalRef(central_clusters);
                val central_cluster_counts_gr = GlobalRef(central_cluster_counts);
                val there = here;
                for (d in points_dist.places()) async {
                    // access PlaceLocalHandles 'here' and then data will be captured by at and transfered to 'there' for accumulation
                    val tmp_new_clusters = local_new_clusters();
                    val tmp_cluster_counts = local_cluster_counts();
                    at (there) atomic {
                        for (var j:Int=0 ; j<DIM*CLUSTERS ; ++j) {
                            central_clusters_gr()(j) += tmp_new_clusters(j);
                        }
                        for (var j:Int=0 ; j<CLUSTERS ; ++j) {
                            central_cluster_counts_gr()(j) += tmp_cluster_counts(j);
                        }
                    }
                }
            }

            for (var k:Int=0 ; k<CLUSTERS ; ++k) { 
                for (var d:Int=0 ; d<DIM ; ++d) { 
                    central_clusters(k*DIM+d) /= central_cluster_counts(k);
                }
            }

            // TEST FOR CONVERGENCE
            var b:Boolean = true;
            for (var j:Int=0 ; j<CLUSTERS*DIM ; ++j) { 
                if (Math.abs(old_central_clusters(j)-central_clusters(j))>0.0001) {
                    b = false;
                    break;
                }
            }
            if (b) break;

        }

        for (var d:Int=0 ; d<DIM ; ++d) { 
            for (var k:Int=0 ; k<CLUSTERS ; ++k) { 
                if (k>0)
                    Console.OUT.print(" ");
                Console.OUT.print(central_clusters(k*DIM+d));
            }
            Console.OUT.println();
        }
    }
}

// vim: shiftwidth=4:tabstop=4:expandtab
