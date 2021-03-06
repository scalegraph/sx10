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

/**
 * This class represents a real-world problem in graphics engines --
 * determining which objects in a large sprawling world are close enough to the
 * camera to be considered for rendering.  The naive implementation (GCSpheres) 
 * produces a lot of objects and is thus a good benchmark for 
 * garbage collection in X10.
 * This version uses structs to significantly reduce heap allocation.
 *
 * @Author Dave Cunningham
 * @Author Vijay Saraswat
 */
class StructSpheres {

    static type Real = Float;


    static struct Vector3(x:Real, y:Real, z:Real) {
        public def getX () = x; 
        public def getY () = y;
        public def getZ () = z;

        public def add (other:Vector3)
            = Vector3(this.x+other.x, this.y+other.y, this.z+other.z);

        public def neg () = Vector3(-this.x, -this.y, -this.z);

        public def sub (other:Vector3) = add(other.neg());

        public def length () = Math.sqrtf(length2());

        public def length2 () = x*x + y*y + z*z;
    }


    static struct WorldObject {

        def this (x:Real, y:Real, z:Real, r:Real) {
            pos = Vector3(x,y,z);
            renderingDistance = r;
        }

        public def intersects (home:Vector3)
            = home.sub(pos).length2() < renderingDistance*renderingDistance;

        protected val pos:Vector3;
        protected val renderingDistance:Real;
    }


    public static def compute():boolean {

        val reps = 7500;

        // The following correspond to a modern out-door computer game:
        val num_objects = 50000;
        val world_size = 6000;
        val obj_max_size = 400;

        val ran = new Random(0);

        // the array can go on the heap
        // but the elements ought to be /*inlined*/ in the array
        val spheres =
            new Array[WorldObject](num_objects, (i:int) => {
                val x = (ran.nextDouble()*world_size) as Real;
                val y = (ran.nextDouble()*world_size) as Real;
                val z = (ran.nextDouble()*world_size) as Real;
                val r = (ran.nextDouble()*obj_max_size) as Real;
                return WorldObject(x,y,z,r);
            });

        val time_start = System.nanoTime();

        var counter : Long = 0;

        // HOT LOOP BEGINS
        for (c in 1..reps) {

            val x = (ran.nextDouble()*world_size) as Real;
            val y = (ran.nextDouble()*world_size) as Real;
            val z = (ran.nextDouble()*world_size) as Real;

            val pos = Vector3(x,y,z);

            for (i in 0..(spheres.size-1)) {
                if (spheres(i).intersects(pos)) {
                    counter++;
                }
            }
        }
        // HOT LOOP ENDS

        val time_taken = System.nanoTime() - time_start;
        Console.OUT.println("Total time: "+time_taken/1E9);

        val expected = 108996L;
	val ok = counter == expected;
        if (!ok) {
            Console.ERR.println("number of intersections: "+counter
                                +" (expected "+expected+")");
        }
        return ok;
    }

    public static def main (Array[String]) {
        compute();
    }

}
