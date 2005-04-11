/**
 * Java Grande Forum MolDyn Benchmark 
 * Converted to an x10 test case
 * @author kemal
 */
/**************************************************************************
*                                                                         *
*             Java Grande Forum Benchmark Suite - Version 2.0             *
*                                                                         *
*                            produced by                                  *
*                                                                         *
*                  Java Grande Benchmarking Project                       *
*                                                                         *
*                                at                                       *
*                                                                         *
*                Edinburgh Parallel Computing Centre                      *
*                                                                         * 
*                email: epcc-javagrande@epcc.ed.ac.uk                     *
*                                                                         *
*                                                                         *
*      This version copyright (c) The University of Edinburgh, 1999.      *
*                         All rights reserved.                            *
*                                                                         *
**************************************************************************/


import moldyn.*;
import jgfutil.*;

public class JGFMolDynBenchSizeA{ 

  public boolean run(){

    JGFInstrumentor.printHeader(3,0);

    JGFMolDynBench mold = new JGFMolDynBench(); 
    mold.JGFrun(0);
    return true;
 
  }
  public static void main(String args[]) {
	boolean b= (new JGFMolDynBenchSizeA()).run();
	System.out.println("++++++ "+(b?"Test succeeded.":"Test failed."));
	System.exit(b?0:1);
  }
}
