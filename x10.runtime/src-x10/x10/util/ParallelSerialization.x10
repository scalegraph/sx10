package x10.util;

import x10.compiler.Native;
import x10.compiler.NativeRep;

public class ParallelSerialization {
	
    @Native("c++", "x10aux::count_ser_size<#T >((#data)->raw() , #data_offset, #data_count)")
    private static native def count_ser_size[T](place :Int, data :Rail[T], data_offset :Int, data_count :Int): Int;
    
    @Native("c++", "x10aux::write_ser_data<#T >((#data)->raw() , #data_offset, #data_count, (#ser_data)->raw(), #ser_off, #ser_count)")
    private static native def write_ser_data[T](place :Int, data :Rail[T], data_offset :Int, data_count :Int, ser_data :Rail[Byte], ser_off :Int, ser_count :Int): void;

    @Native("c++", "x10aux::read_deser_data<#T >((#data)->raw(), #data_offset, #data_count, (#deser_data)->raw(), #deser_off, #deser_count)")
    private static native def read_deser_data[T](place :Int, data :Rail[T], data_offset :Int, data_count :Int, deser_data :Rail[Byte], deser_off :Int, deser_count :Int): void;


    public static def serialize[T](data :Rail[T], data_offset :Int, data_count :Int): Rail[Byte]
    {
        val ser_count = count_ser_size(0n, data, data_offset, data_count);
        val ser_data = new Rail[Byte](ser_count);//Rail.allocateUninitialized[Byte](ser_count);
        write_ser_data(0n, data, data_offset, data_count, ser_data, 0n, ser_count);

        return ser_data;
    }

    public static def serialize[T](data :Rail[T], data_offsets :Rail[Int], data_counts :Rail[Int], ser_offsets :Rail[Int], ser_counts :Rail[Int]): Rail[Byte]
    {
        val places = data_counts.size;
        var ser_size: Int = 0n;
        
        finish for (p in 0..(places-1)) async {
            ser_counts(p) = count_ser_size(p as Int, data, data_offsets(p), data_counts(p));
            ser_size += ser_counts(p);
        }

        ser_offsets(0) = 0n;
        for (i in 0..(places-2)) ser_offsets(i+1) = ser_offsets(i) + ser_counts(i);
        val ser_data = new Rail[Byte](ser_size);//Rail.allocateUninitialized[Byte](ser_size);
        finish for (p in 0..(places-1)) async {
            write_ser_data(p as Int, data, data_offsets(p), data_counts(p), ser_data, ser_offsets(p), ser_counts(p));
        }
        
        return ser_data;
    }

    public static def deserialize[T](data :Rail[T], data_offset :Int, data_count :Int, deser_data :Rail[Byte], deser_offset :Int, deser_count :Int) :void
    {
        read_deser_data(0n, data, data_offset, data_count, deser_data, deser_offset, deser_count);
    }

    public static def deserialize[T](data :Rail[T], data_offsets :Rail[Int], data_counts :Rail[Int], deser_data :Rail[Byte], deser_offsets :Rail[Int], deser_counts :Rail[Int]) :void
    {
        val places = data_counts.size;
        finish for (p in 0..(places-1)) async {
            read_deser_data(p as Int, data, data_offsets(p), data_counts(p), deser_data, deser_offsets(p), deser_counts(p));
        }
    }
}

