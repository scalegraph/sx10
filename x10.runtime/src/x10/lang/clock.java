/**
 * The representation of clocks in X10.

 * In addition to the methods supported below, clocks are also used in
 * the following statements:
 *     next;
 *     clocked (c1,..., cn) Stm
 *     now (c) Stm
 * Handtranslated from the X10 source in x10/lang/clock.x10 by vj
 * @author Christoph von Praun
 * @author vj
 */
 
package x10.lang;

abstract public class clock extends Object {
	
	protected clock() {}
	
	abstract static public class factory {
		abstract public clock clock();
	}
	
	public static final factory factory = Runtime.factory.getClockFactory();
	
	/** An activity calls this method to tell the clock that is done 
	 with whatever it intended to do during this phase of the clock.
	 The activity will not post any other statement for execution
	 in this phase of the clock.  */
	abstract public void resume();
	
	/** An activity calls this method to tell the clock that it is no
	 * longer interested in interacting with the clock. It will no
	 * longer call now or resume on this clock. The activity is
	 * considered de-registered from the clock after this method
	 * returns.
	 */
	abstract public void drop();
	
	
	/** An activity may call this method to determine whether it is
	 * registered with this clock or not.
	 */
	abstract public boolean dropped();
	
}
