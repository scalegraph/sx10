import x10.lang.*;
import java.lang.reflect.*;
/**
 * Synthetic benchmark to time arary accesses
 */

public class Initialization {
	String _tests[] = {"testDouble"};
	static final int kArraySize=5000;
	double[.] _doubleArray1D;
	double[.] _doubleArray2D;
	double[] _javaArray;
	
	public Initialization(){
		long start,stop;
		int OneDSize = kArraySize * kArraySize;
		
		start = System.currentTimeMillis();
		System.out.println("creating java array size "+OneDSize);
		_javaArray = new double[OneDSize];
		stop = System.currentTimeMillis();
		System.out.println("Created array in "+((double)(stop-start)/1000)+" seconds");
		
		start = System.currentTimeMillis();
		System.out.println("creating array size "+OneDSize);
		region r = [0:OneDSize];
		distribution  D = distribution.factory.block(r);
		_doubleArray1D = new double[D];
		stop = System.currentTimeMillis();
		System.out.println("Created array in "+((double)(stop-start)/1000)+" seconds");
		
		System.out.println("creating array ["+kArraySize+","+kArraySize+"] ("+(kArraySize*kArraySize)+")");
		region r2 = [0:kArraySize,0:kArraySize];
		distribution  D2 = distribution.factory.block(r2);
		System.out.println("Start allocation...");
		start = System.currentTimeMillis();
		_doubleArray2D = new double[D2];
		stop = System.currentTimeMillis();
		System.out.println("Created array in "+((double)(stop-start)/1000)+" seconds");
		System.out.println("finished allocating");
	}
	public static void  main(String a[]){
		
		Initialization test = new Initialization();
		
	}
}