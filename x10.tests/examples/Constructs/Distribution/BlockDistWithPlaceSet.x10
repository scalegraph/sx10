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

import x10.util.Set;
import x10.util.HashSet;

/**
 * Testing block dist.
 *
 * Randomly generate block dists and check
 * index-to-place mapping for conformance with x10 0.41 spec
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
public class BlockDistWithPlaceSet extends x10Test {

	public static P = Dist.makeUnique();
	public static COUNT = 200;
	public static L = 5;

	public def run(): boolean = {
		for (val [tries]: Point in 1..COUNT) {
			val lb1: int = ranInt(-L, L);
			val lb2: int = ranInt(-L, L);
			val ub1: int = ranInt(lb1, L);
			val ub2: int = ranInt(lb2, L);
			val R = (lb1..ub1) * (lb2..ub2);
			val totalPoints = (ub1-lb1+1)*(ub2-lb2+1);
			val r = createRandPlaceSet();
			val np = r.np;
			val placeNums = r.placeNums;
			val placeSet  = r.placeSet;

			val DBlock = Dist.makeBlock(R, 0, placeSet);
			val p: int = totalPoints/np;
			val q: int = totalPoints%np;
			var offsWithinPlace: int = 0;
			var pn: int = 0;
			//x10.io.Console.OUT.println("np = " + np + " lb1 = "+lb1+" ub1 = "+ub1+" lb2 = "+lb2+" ub2 = "+ub2+" totalPoints = "+totalPoints+" p = "+p+" q = "+q);

			for (val [i,j]: Point(2) in R) {
				//x10.io.Console.OUT.println("placeNum = "+placeNums[pn]+" offsWithinPlace = "+offsWithinPlace+" i = "+i+" j = "+j+" DBlock[i,j] = "+DBlock[i,j].id);
				chk(DBlock(i, j) == P(placeNums(pn)));
				chk(P(placeNums(pn)).id == placeNums(pn));
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
	 * emulating multiple return values
	 */
	static class randPlaceSet {
		val np: int;
		val placeSet: Set[Place];
		val placeNums: Rail[Int];
		def this(n: int, a: Rail[Int], s: Set[Place]): randPlaceSet = {
			np = n;
			placeNums = a;
			placeSet = s;
		}
	}

	/**
	 * Create a random, non-empty subset of the places
	 */
	def createRandPlaceSet(): randPlaceSet = {
		val placeSet: Set[Place] = new HashSet[Place]();
		var np: int;
		val placeNums = Rail.make[int](Place.MAX_PLACES);
		do {
			np = 0;
			val THRESH: int = ranInt(10, 90);
			for (val [i]: Point(1) in P) {
				val x: int = ranInt(0, 99);
				if (x >= THRESH) {
					placeSet.add(P(i));
					placeNums(np++) = i;
				}
			}
		} while (np == 0);
		return new randPlaceSet(np, placeNums, placeSet);
	}

	public static def main(var args: Array[String](1)): void = {
		new BlockDistWithPlaceSet().execute();
	}
}
