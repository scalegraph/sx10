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
import x10.util.Timer;

/**
 * Converted to 2.1.
 */
class LocalTable {

    val a: Array[long](1);
    val mask: int;

    def this(size:int) {
        mask = size-1;
        a = new Array[long](size, (i:int)=>i as long);
    }

    public def update(ran:long) {
        //a((ran&mask) as int) ^= ran;
        val index = (ran&mask) as int;
        a(index) = a(index) ^ ran;
    }
}


class FRASimpleDist {

    static POLY = 0x0000000000000007L;
    static PERIOD = 1317624576693539401L;

    // Utility routine to start random number generator at Nth step
    static def HPCC_starts(var n:long): long {
        var i:int;
        var j:int;
        val m2 = new Array[long](64);
        while (n < 0L) n += PERIOD;
        while (n > PERIOD) n -= PERIOD;
        if (n == 0L) return 0x1L;
        var temp:long = 0x1L;
        for (i=0; i<64; i++) {
            m2(i) = temp;
            temp = (temp << 1) ^ (temp < 0L ? POLY : 0L);
            temp = (temp << 1) ^ (temp < 0L ? POLY : 0L);
        }
        for (i=62; i>=0; i--) if (((n >> i) & 1) != 0L) break;
        var ran:long = 0x2L;
        while (i > 0) {
            temp = 0L;
            for (j=0; j<64; j++) if (((ran >> j) & 1L) != 0L) temp ^= m2(j);
            ran = temp;
            i -= 1;
            if (((n >> i) & 1L) != 0L)
                ran = (ran << 1) ^ (ran < 0L ? POLY : 0L);
        }
        return ran;
    }

    static def randomAccessUpdate(
        num_updates: long,
        logLocalTableSize: int,
        tables: PlaceLocalHandle[LocalTable]
    ) {
        finish for (var p:int=0; p<Place.MAX_PLACES; p++) {
            val valp = p;
            at(Place.place(p)) async {
                var ran:long = HPCC_starts(valp*(num_updates/Place.MAX_PLACES));
                for (var i:long=0; i<num_updates/Place.MAX_PLACES; i++) {
                    val placeId = ((ran>>logLocalTableSize) & (Place.MAX_PLACES-1)) as int;
                    val ran2=ran;
                    async at (Place.place(placeId)) {
                        tables().update(ran2);
                    }
                    ran = (ran << 1) ^ (ran<0L ? POLY : 0L);
                }
            }
        }
    }


    public static def main(args: Array[String](1)) {
        if ((Place.MAX_PLACES & (Place.MAX_PLACES-1)) > 0) {
            println("The number of places must be a power of 2.");
            return;
        }

        // calculate the size of update array (must be a power of 2)
        val logLocalTableSize = args.size > 1 && args(0).equals("-m") ? int.parse(args(1)) : 12;
        val localTableSize = 1<<logLocalTableSize;
        val tableSize = localTableSize*Place.MAX_PLACES;
        val num_updates = 4*tableSize;

        // create local tables
        val tables = PlaceLocalHandle.make[LocalTable](PlaceGroup.WORLD, () => new LocalTable(localTableSize));

        // print some info
        println("Main table size   = 2^" +logLocalTableSize + "*" + Place.MAX_PLACES+" = " + tableSize+ " words");
        println("Number of places = " + Place.MAX_PLACES);
        println("Number of updates = " + num_updates);

        // time it
        var cpuTime:double = -now();
        randomAccessUpdate(num_updates, logLocalTableSize, tables);
        cpuTime += now();

        // print statistics
        val GUPs = (cpuTime > 0.0 ? 1.0 / cpuTime : -1.0) * num_updates / 1e9;
        Console.OUT.println("CPU time used  = "+cpuTime+" seconds");
        Console.OUT.println("" + GUPs+" Billion(10^9) Updates per second (GUP/s)");

        // repeat for testing.
        randomAccessUpdate(num_updates, logLocalTableSize, tables);
        for (var i:int=0; i<Place.MAX_PLACES; i++) {
            at (Place.place(i)) async {
                val table = tables();
                var err:int = 0;
                for ([j] in table.a) 
                    if (table.a(j) != j as Long) err++;
                println("Found " + err + " errors.");
            }
        }
    }

    static def now() = Timer.nanoTime() * 1e-9D;

    static def println(s:String) { Console.OUT.println(s); }
}
