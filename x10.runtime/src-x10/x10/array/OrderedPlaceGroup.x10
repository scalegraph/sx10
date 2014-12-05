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

package x10.array;

/**
 * <p>An implementation of PlaceGroup that simply uses a sorted Rail[Place] to
 * represent the Places in the group.  This implementation is only suitable
 * when the PlaceGroup contains a fairly small number of places.
 * This can happen either becase the group is very sparse, or because the total 
 * number of places being used by the program is small.  In either case, 
 * this PlaceGroup should have acceptable performance.</p>
 *
 * <p>Although the basic operations (contains, indexOf) could be asymptotically
 * improved from O(N) to O(log(N)) by using binary search instead of linear
 * search, the expected performance of this class would still be poor due to
 * the space overhead of explictly representing all of the Places in the group,
 * which in turn would yield O(size()) serialization costs.  Therefore, we have
 * decided to go with the lower constants and ignore the asymptotic analysis.</p>
 */
public final class OrderedPlaceGroup extends PlaceGroup {
 
  /**
   * The set of places.
   * Only places that are in the group are in the array.
   */
  private val places:Rail[Place];

  ///**
  // * Construct a OrderedPlaceGroup from a Sequence[Place].
  // */
  //public def this(ps:Sequence[Place]) {
  //  places = new Rail[Place](ps.size(), (i:int)=>ps(i));
  //}

  /**
   * Construct a OrderedPlaceGroup from a Rail[Place](1). 
   */
  public def this(pa:Rail[Place]) {
    places = new Rail[Place](pa.size, (i:Long)=>pa(i));
  }

  /**
   * Construct a OrderedPlaceGroup that contains a single place, p.
   * @param p the place 
   */
  public def this(p:Place) {
    places = [p as Place];
  }

  public operator this(i:Long):Place = places(i);

  public def iterator() = places.iterator();
  //public def iterator() = places.values().iterator();

  public def numPlaces() = places.size;

  public def contains(id:Long):Boolean {
	var cnt : Long = 0;
    for (i in places) {
        if (places(cnt++).id == id) return true;
    }
    return false;
  }

  public def indexOf(id:Long):Long {
	var cnt : Long = 0;
    for (i in places) {
        if (places(cnt).id == id) return cnt;
		cnt++;
    }
    return -1;
  }
}
 

