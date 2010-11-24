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

package x10.lang;


// Members of the class x10.lang._ are imported automatically.
public class _ {
    public static type void = Void;
    public static type boolean = Boolean;
    public static type byte = Byte;
    public static type short = Short;
    public static type char = Char;
    public static type int = Int;
    public static type long = Long;
    public static type float = Float;
    public static type double = Double;

    public static type boolean(b:Boolean) = Boolean{self==b};
    public static type byte(b:Byte) = Byte{self==b};
    public static type short(b:Short) = Short{self==b};
    public static type char(b:Char) = Char{self==b};
    public static type int(b:Int) = Int{self==b};
    public static type long(b:Long) = Long{self==b};
    public static type float(b:Float) = Float{self==b};
    public static type double(b:Double) = Double{self==b};
    public static type string(s:String) = String{self==s};
    
    public static type Boolean(b:Boolean) = Boolean{self==b};
    public static type Byte(b:Byte) = Byte{self==b};
    public static type Short(b:Short) = Short{self==b};
    public static type Char(b:Char) = Char{self==b};
    public static type Int(b:Int) = Int{self==b};
    public static type Long(b:Long) = Long{self==b};
    public static type Float(b:Float) = Float{self==b};
    public static type Double(b:Double) = Double{self==b};
    public static type String(s:String) = String{self==s};
    public static type Any(x:Any) = Any{self==x};
   

    public static type signed = Int;
    public static type unsigned = UInt;

    public static type ubyte = UByte;
    public static type uint8 = UByte;
    public static type nat8 = UByte;

    public static type ushort = UShort;
    public static type uint16 = UShort;
    public static type nat16 = UShort;

    public static type uint = UInt;
    public static type uint32 = UInt;
    public static type nat32 = UInt;

    public static type ulong = ULong;
    public static type uint64 = ULong;
    public static type nat64 = ULong;
 
    public static type int8 = byte;
    public static type int16 = short;
    public static type int32 = int;
    public static type int64 = long;
    
    public static type GlobalRef[T](p:Place) {T<:Object} = GlobalRef[T]{self.home==p};
    public static type Point(r: Int) = Point{self.rank==r};
    public static type Place(id:Int) = Place{self.id==id};
    public static type Place(p:Place) = Place{self==p};
    
    public static type Region(r:Int) = Region{self.rank==r};
    public static type Region(r:Region) = Region{self==r};
    public static type RectRegion(r:Int) = RectRegion{self.rect && self.rank==r};
    public static type Range = Region{self.rect && self.rank==1};
    
    public static type Dist(r:Int)   = Dist{self.rank==r};
    public static type Dist(r:Region) = Dist{self.region==r};
    public static type Dist(d:Dist) = Dist{self==d};
    
    public static type Array[T](r:Int) = Array[T]{self.rank==r};
    public static type Array[T](r:Region) = Array[T]{self.region==r};
    public static type Array[T](a:Array[T]) = Array[T]{self==a};

    public static type DistArray[T](r:Int) = DistArray[T]{self.rank==r};
    public static type DistArray[T](r:Region) = DistArray[T]{self.region==r};
    public static type DistArray[T](d:Dist) = DistArray[T]{self.dist==d};
    public static type DistArray[T](a:DistArray[T]) = DistArray[T]{self==a};

    public static type Rail[T](n:Int) = Rail[T]{self.length==n};

    public static type Console = x10.io.Console;
}
