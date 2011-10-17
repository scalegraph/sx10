package x10.lang;

import x10.util.Ordered;

/**
 * The interface that must be implemented by reduction operations.
 * 
 * Implementations of Reducible[T] must ensure that the operator(T,T) method is associative
 * and commutative and stateless, and that zero() is an identity.
 */
public interface Reducible[T]  {
	
   /**
    * The identity for this reducer operation. It must be the case
    * that operator(zero(),f)=f.
    */
    def zero():T;
    
    operator this(T,T):T;

    public static struct AndReducer implements Reducible[Boolean] {
        public def zero():Boolean = true;
        public operator this(a:Boolean, b:Boolean):Boolean = a && b;
    }
    
    public static struct OrReducer implements Reducible[Boolean] {
        public def zero():Boolean = false;
        public operator this(a:Boolean, b:Boolean):Boolean = a || b;
    }
    
    public static struct SumReducer[T] {T <: Arithmetic[T], T haszero} implements Reducible[T] {
        public def zero():T = Zero.get[T]();
        public operator this(a:T, b:T):T = a+b; 
    }

    public static struct MinReducer[T] {T <: Ordered[T]} implements Reducible[T] {
        private val zeroVal:T;
        public def this(maxValue:T) { zeroVal = maxValue; }
        public def zero():T = zeroVal;
        public operator this(a:T, b:T):T = a < b ? a : b;
    }

    public static struct MaxReducer[T] {T <: Ordered[T]} implements Reducible[T] {
        private val zeroVal:T;
        public def this(minValue:T) { zeroVal = minValue; }
        public def zero():T = zeroVal;
        public operator this(a:T, b:T):T = a >= b ? a : b;
    }

}
