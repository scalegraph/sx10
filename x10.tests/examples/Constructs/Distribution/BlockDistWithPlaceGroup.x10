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

import harness.x10Test;
import x10.util.ArrayList;

/**
 * Testing block dist.
 *
 * Randomly generate block dists and check
 * index-to-place mapping for conformance with spec.
 *
 * The dist block(R, Q) distributes the elements of R (in
 * order) over the set of places Q in blocks as follows. Let p equal
 * |R| div N and q equal |R| mod N, where N is the size of Q. The first
 * q places get successive blocks of size (p + 1) and the remaining
 * places get blocks of size p.
 *
 * This tests the block distribution with a given random subset of places,
 * not all places
 *
 * @author kemal 5/2005
 */
public class BlockDistWithPlaceGroup extends x10Test {

    public static COUNT = 200;
    public static L = 5;

    public def run(): boolean = {
        for ([tries] in 1..COUNT) {
            val lb1: int = ranInt(-L, L);
            val lb2: int = ranInt(-L, L);
            val ub1: int = ranInt(lb1, L);
            val ub2: int = ranInt(lb2, L);
            val R = (lb1..ub1) * (lb2..ub2);
            val totalPoints = (ub1-lb1+1)*(ub2-lb2+1);
            val placeGroup = createRandPlaceGroup();
            val np = placeGroup.numPlaces();

            val DBlock = Dist.makeBlock(R, 0, placeGroup);
            val p: int = totalPoints/np;
            val q: int = totalPoints%np;
            var offsWithinPlace: int = 0;
            var pn: int = 0;
            Console.OUT.println("np = " + np + " lb1 = "+lb1+" ub1 = "+ub1+" lb2 = "+lb2+" ub2 = "+ub2+" totalPoints = "+totalPoints+" p = "+p+" q = "+q);

            for (val [i,j]: Point(2) in R) {
                Console.OUT.println("placeNum = "+placeGroup(pn)+" offsWithinPlace = "+offsWithinPlace+" i = "+i+" j = "+j+" DBlock[i,j] = "+DBlock(i,j).id);
                chk(DBlock(i, j).equals(placeGroup(pn)));
                offsWithinPlace++;
                if (offsWithinPlace == (p + (pn < q ? 1 : 0))) {
                    //time to go to next place
                    offsWithinPlace = 0;
                    pn++;
                }
            }
        }
        return true;
    }

    /**
     * Create a random, non-empty subset of the places
     */
    def createRandPlaceGroup():PlaceGroup {
        val places = new ArrayList[Place]();
        do {
            val THRESH: int = ranInt(10, 90);
            for (p in Place.places()) {
                val x:int = ranInt(0, 99);
                if (x >= THRESH) {
                    places.add(p);
                }
            }
        } while (places.size() == 0);
        return new SparsePlaceGroup(places.toArray().sequence());
    }

    public static def main(var args: Array[String](1)): void = {
        new BlockDistWithPlaceGroup().execute();
    }
}
