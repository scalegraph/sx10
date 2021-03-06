import harness.x10Test;

/**
 * Tests a Block,Block distribution of a three dimensional array over two axes.
 * @author milthorpe
 */
public class TestBlockBlockDist extends x10Test {
	public def run(): Boolean = {
        // array region is 40 * 50 * 60
        val r = Region.makeRectangular(0, 39);
        val gridRegion = r * 0..49 * 0..59;
        val gridDist1 = Dist.makeBlockBlock(gridRegion, 0, 1);
        checkDist(gridDist1);

        val myArray = DistArray.make[Double](gridDist1, (p : Point) => 0.0);
        finish ateach (p in myArray) {
            myArray(p) = myArray(p) + 2.0;
        }

        val gridDist2 = Dist.makeBlockBlock(gridRegion, 1, 2);
        checkDist(gridDist2);

        val gridDist3 = Dist.makeBlockBlock(gridRegion, 1, 0);
        checkDist(gridDist3);

        val gridDist4 = Dist.makeBlockBlock(0..5*0..6, 0, 1);
        checkDist(gridDist4);

        val region5 = (1..10)*(-2..10)*(1..10);
        val dist5 = Dist.makeBlockBlock(region5, 0, 1);
        val regionForZero = dist5(Place.place(0));
        chk(region5.minPoint().equals(regionForZero.minPoint()));

        return true;
    }

    /**
     * Checks that the number of points in this dist that are mapped to each
     * place is the same as the number of points in the place restriction of 
     * this dist. 
     */ 
    private def checkDist(dist:Dist) {
        val placeCounts = new Rail[Int](Place.MAX_PLACES);
        for (p in dist.region) {
            val place = dist(p);
            placeCounts(place.id)++;
        }
        for ([q] in 0..(placeCounts.size-1)) {
            chk(placeCounts(q) == dist(Place(q)).size());
        }
    } 

    public static def main(args:Array[String]) {
        new TestBlockBlockDist().execute();
    }
}
