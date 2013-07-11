package x10.util;

public class ParallelSerialization {
    @native("c++", "count_ser_size(shared_map, data , data_offset, data_count)") {}
    private static native def count_ser_size[T](shared_map :addr_map, data :IndexedMemoryChunk[T], data_offset :Int, data_count :Int): Int;
    @native("c++", "write_ser_data(shared_map, data, data_offset, data_count, ser_data, ser_off)")
    private static native def write_ser_data[T](shared_map :addr_map, data :IndexedMemoryChunk[T], data_offset :Int, data_count :Int, ser_data :IndexedMemoryChunk[Byte], ser_off :Int): void;
    @native("c++", "read_deser_data(shared_map, data, data_offset, data_count, deser_data, deser_off)")
    private static native def read_deser_data[T](shared_map :addr_map, data :IndexedMemoryChunk[Byte], data_offset :Int, data_count :Int, deser_data :IndexedMemoryChunk[T], deser_off :Int): void;
    @native("c++", )
    private static native def resolve_addr_map(): void;

    public static def serialize[T](data :IndexedMemoryChunk[T], count :IndexedMemoryChunk[Int], offset :IndexedMemoryChunk[Int], threads :Int):IndexedMemoryChunk[Byte]
    {
        val places = count.size / threads;
        val addr_map_etc = new IndexedMemoryChunk[addr_map](places, new addr_map());
        finish for (p in 0..(places-1)) async {
            for (th in 0..(threads-1)) async {
                offset(p*threads+th+1) = count_ser_size(addr_map_etc(p), data, data_offset(p*threads+th), count(p*threads+th));
            }
        }

        offset(0) = 0;
        for (i in 0..(places*threads-1)) offset(i+1) += offset(i);
        val ser_data = IndexedMemoryChunk.allocateUniinitialized[Byte](offset(places*threads));
        finish for (p in 0..(places-1)) async {
            for (th in 0..(threads-1)) async {
                write_ser_data(addr_map_etc(p), data, data_offset(p*threads+th), data_count(p*threads+th), ser_data, offset(p*threads+th));
            }
        }
        delete addr_map_etc;
        return imc;
    }

    public static def deserialize[T](stm :IndexedMemoryChunk[T], count :IndexedMemoryChunk[Int], offset :IndexedMemoryChunk[Int], threads :Int) :void
    {
        val places = count.size / threads;
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
    }
}

