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

import java.io.Serializable;

import x10.x10rt.ActiveMessage;
import x10.x10rt.MessageRegistry;
import x10.x10rt.X10RT;
import x10.x10rt.UnknownMessageException;

public class SerializationTest {
    public static class ObjectToSerialize implements Serializable {
        static private final long serialVersionUID = 42L;
        public String firstAttribute;
        public int secondAttribute;
        public int [] thirdAttribute;

        public ObjectToSerialize() {
            this.firstAttribute = "One string to rule them all";
            this.secondAttribute = 42;
            this.thirdAttribute = new int [] {111, 222, 333, 444};
        }
        
        public boolean isEqualTo(ObjectToSerialize o) {
            if(0 != this.firstAttribute.compareTo(o.firstAttribute)) {
                return false;
            }
            if(this.secondAttribute != o.secondAttribute) {
                return false;
            }
            if(java.util.Arrays.hashCode(this.thirdAttribute) != 
                    java.util.Arrays.hashCode(o.thirdAttribute)) {
                return false;
            }
            return true;
        }
        
        public void printFields() {
            System.out.println("fa = "+firstAttribute);
            System.out.println("sa = "+secondAttribute);
            System.out.printf("ta = [%d, %d, %d, %d]\n", thirdAttribute[0], thirdAttribute[1], thirdAttribute[2], thirdAttribute[3]);
        }
    }

    
    public static void main(String[] args) throws InterruptedException {
        X10RT.barrier();

        ActiveMessage msg = 
            MessageRegistry.register(SerializationTest.class, 
                "MessageHandler", 
                ObjectToSerialize.class);
        
        ActiveMessage msg2 = 
            MessageRegistry.register(ObjectToSerialize.class, 
                "printFields");

        X10RT.barrier();

        ObjectToSerialize obj = new ObjectToSerialize();

        X10RT.barrier();

        if(0 == X10RT.here().getId()) {
            msg.send(X10RT.getPlace(1), obj);
            
            msg2.send(X10RT.getPlace(1), obj);
        }

        X10RT.barrier();
    }

    public static void MessageHandler(ObjectToSerialize obj) 
        throws UnknownMessageException, IllegalArgumentException {

        ObjectToSerialize localObj = new ObjectToSerialize();
        if(localObj.isEqualTo(obj)) {
            System.out.println("OK");
        } else {
            System.err.println("FAIL");
        }
    }
}
