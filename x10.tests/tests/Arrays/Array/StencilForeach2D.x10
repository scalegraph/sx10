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
import x10.regionarray.*;

/**
 * Implementing a 5-point stencil operation using a for ... async loop
 * @author Tong 11/29/2006
 */

public class StencilForeach2D extends x10Test {
    
    public def run(): boolean = {
        val R = Region.make(-1..256, -1..256);
        val r = Region.make(0..255, 0..255);
        val north = [0, 1] as Point;
        val south = [0, -1] as Point;
        val west = [-1, 0] as Point;
        val east  = [1, 0] as Point;
        val A = new Array[double](R, (Point)=>0.0D);
        val h  = 0.1;
            
        finish for (val p: Point(2) in r)  async
            A(p)=(A(p+north)+A(p+south)+A(p+west)+A(p+east)-4*A(p))*h;
            
        return true;
    }
    
    public static def main(var args: Rail[String]): void = {
        new StencilForeach2D().execute();
    }

}
