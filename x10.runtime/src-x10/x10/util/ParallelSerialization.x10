package x10.util;

import x10.compiler.Native;
import x10.compiler.NativeRep;

public class ParallelSerialization {
	
	@NativeRep("c++", "x10aux::addr_map*", "x10aux::addr_map*", null)
	private static struct AddressMap { }
	
	@Native("c++", "new x10aux::addr_map[#places]()")
	private static native def createAddressMap(places :Int) :AddressMap;
	
	@Native("c++", "delete [] #addressMap")
	private static native def deallocAddressMap(addressMap :AddressMap) :void;


    @Native("c++", "x10aux::count_ser_size[#T ](&#addressMap[#place], (#data)->raw() , #data_offset, #data_count)")
    private static native def count_ser_size[T](addressMap :AddressMap, place :Int,
    	data :IndexedMemoryChunk[T], data_offset :Int, data_count :Int): Int;
    
    @Native("c++", "x10aux::write_ser_data[#T ](&#addressMap[#place], (#data)->raw() , #data_offset, #data_count, (#ser_data)->raw(), #ser_off)")
    private static native def write_ser_data[T](addressMap :AddressMap, place :Int,
    	data :IndexedMemoryChunk[T], data_offset :Int, data_count :Int, ser_data :IndexedMemoryChunk[Byte], ser_off :Int): void;
/*
    @Native("c++", "read_deser_data(shared_map, data, data_offset, data_count, deser_data, deser_off)")
    private static native def read_deser_data[T](shared_map :addr_map, data :IndexedMemoryChunk[Byte], data_offset :Int, data_count :Int, deser_data :IndexedMemoryChunk[T], deser_off :Int): void;
    
    @Native("c++", )
    private static native def resolve_addr_map(): void;
*/
    public static def serialize[T](data :IndexedMemoryChunk[T], count :IndexedMemoryChunk[Int], offset :IndexedMemoryChunk[Int], threads :Int):IndexedMemoryChunk[Byte]
    {
    	val element_count = count;
        val places = element_count.length() / threads;
        val element_offset = IndexedMemoryChunk.allocateUninitialized[Int](element_count.length() + 1);
        val addressMap = createAddressMap(places);
        
        element_offset(0) = 0;
        for(i in 0..(count.length()-1)) element_offset(i + 1) = element_offset(i) + element_count(i);
        finish for (p in 0..(places-1)) async {
            for (th in 0..(threads-1)) async {
                offset(p*threads+th+1) = count_ser_size(addressMap, p, data, element_offset(p*threads+th), element_count(p*threads+th));
            }
        }

        offset(0) = 0;
        for (i in 0..(places*threads-1)) offset(i+1) += offset(i);
        val ser_data = IndexedMemoryChunk.allocateUninitialized[Byte](offset(places*threads));
        finish for (p in 0..(places-1)) async {
            for (th in 0..(threads-1)) async {
                write_ser_data(addressMap, p, data, element_offset(p*threads+th), element_count(p*threads+th), ser_data, offset(p*threads+th));
            }
        }
        
        deallocAddressMap(addressMap);
        element_offset.deallocate();
        return ser_data;
    }

    public static def deserialize[T](stm :IndexedMemoryChunk[T], count :IndexedMemoryChunk[Int], offset :IndexedMemoryChunk[Int], threads :Int) :void
    {
/*        val places = count.length() / threads;
        val addr_map_etc = new IndexedMemoryChunk[addr_map](places, new addr_map());
        finish for (p in 0..(places-1)) async {
            for (th in 0..(threads-1)) async {
                offset(p*threads+th+1) = count_ser_size(addr_map_etc(p), data, data_offset, count(p*threads+th));
            }
        }

        offset(0) = 0;
        for (i in 0..(places*threads-1)) offset(i+1) += offset(i);
        val imc = IndexedMemoryChunk.allocateUniinitialized[Byte](offset(places*threads));
        finish for (p in 0..(places-1)) async {
            for (th in 0..(threads-1)) async {
                write_ser_data(addr_map_etc(p), data, data_offset, count(p*threads+th), imc, offset(p*threads+th));
            }
        }
*/    }
}

