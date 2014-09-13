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

import x10.util.Timer;

// NUM_PLACES: 4

/**
 * at(p) async is executed in parallel ?
 *
 * @author Murata 1/2014
 */
public class XTENLANG_3303 extends x10Test {

    public def run() {
        if (Place.MAX_PLACES != 4) return false;
        val place1 = here.next();
        val t = new Timer();
        val startTime = t.nanoTime();
        finish for (i in 1..4) {
            at(place1) async {
                Console.OUT.println("Activity " + i + " at " + here + ": sleep 5 sec");
                System.sleep(5000);
                Console.OUT.println("Activity " + i + " at " + here + ": woken up");
            }
        }
        val elapsedTime = t.nanoTime() - startTime;
        if (elapsedTime / 1000000 < 5500) return true;
        else return false;
    }

    public static def main(Rail[String]) {
        new XTENLANG_3303().execute();
    }
}
