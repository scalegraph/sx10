import x10.compiler.FinishAsync;
import x10.util.Timer;
public class ManyLocalFinish1 {
    public static def main(args: Array[String](1)) //throws Exception
    {
            val start = Timer.milliTime();
            finish{
	           var i:int = 0;
	           for(i=0;i<1000;i++){
		       val p = Place.place(i % Place.MAX_PLACES);
	    	   async at (p){
            		@FinishAsync(1,1,true,1)
            		finish{
				       for(var j:int = 0; j<50; j++){
                          async{}
				    }
               }
	    	}
 	    }
	    }
	    val end = Timer.milliTime();
	    Console.OUT.println("time = "+(end-start) + " milliseconds");
     }
}
