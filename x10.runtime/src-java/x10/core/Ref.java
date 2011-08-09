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

package x10.core;

import x10.rtt.NamedType;
import x10.rtt.RuntimeType;
import x10.rtt.Type;
import x10.rtt.Types;
import x10.x10rt.X10JavaSerializable;
import x10.x10rt.X10JavaSerializer;

import java.io.IOException;


// Base class of all X10 ref objects -- should be generated, but we need this class to get Box to compile.
public class Ref implements RefI {
    
    private static final long serialVersionUID = 1L;

    // N.B. this is called implicitly by all subclasses of Ref
    public Ref() {}

    // constructor just for allocation
    public Ref(java.lang.System[] $dummy) {}

    // constructor for non-virtual call
    public final Ref x10$lang$Object$$init$S() {return this;}
    
    public Ref $init(){return x10$lang$Object$$init$S();}
	
    public static Ref $make() { return new Ref(); }

    /* TODO to be removed
    public void $init(Object out$){}
    
    // XTENLANG-1858: every Java class that could be an (non-static) inner class must have constructors with the outer instance parameter
    public Ref(Object out$) {}
    */

    public static final RuntimeType<Ref> $RTT = new NamedType<Ref>("x10.lang.Object", Ref.class);
    public RuntimeType<?> $getRTT() {return $RTT;}
    public Type<?> $getParam(int i) {return null;}

    @Override
    public java.lang.String toString() {
        return Types.typeName(this) + "@" + Integer.toHexString(System.identityHashCode(this));
    }

    public void $_serialize(X10JavaSerializer serializer) throws IOException { }

    public short $_get_serialization_id() { return 0; }
}
