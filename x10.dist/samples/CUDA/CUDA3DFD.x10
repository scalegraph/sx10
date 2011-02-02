import x10.io.Console;
import x10.util.Random;

import x10.util.CUDAUtilities;
import x10.compiler.CUDA;
import x10.compiler.CUDADirectParams;
import x10.compiler.NoInline;

public class CUDA3DFD {
    public static def init_data(data:Array[Float](1){rail}, dimx:Int, dimy:Int, dimz:Int)
    {
        var off:Int = 0;
        for(var iz:Int=0; iz<dimz; iz++)
            for(var iy:Int=0; iy<dimy; iy++)
                for(var ix:Int=0; ix<dimx; ix++)
                {
                    data(off++) = iz as Float;
                }
    }

    public static def random_data(data:Array[Float](1){rail}, dimx:Int, dimy:Int, dimz:Int, lower_bound:Int, upper_bound:Int)
    {
        val rnd = new Random(0);

        var off:Int = 0;
        for(var iz:Int=0; iz<dimz; iz++)
            for(var iy:Int=0; iy<dimy; iy++)
                for(var ix:Int=0; ix<dimx; ix++)
                {
                    //data(off++) = (lower_bound + (rnd.nextInt() % (upper_bound - lower_bound))) as Float;
                    data(off++) = iz as Float; //(iy*dimx + ix) as Float;
                }
    }

    // note that this CPU implemenation is extremely naive and slow, NOT to be used for performance comparisons
    public static def reference_3D(output:Array[Float](1){rail}, input:Array[Float](1){rail}, coeff:Array[Float](1){rail}, dimx:Int, dimy:Int, dimz:Int)
    { reference_3D(output, input, coeff, dimx, dimy, dimz, 4); }
    
    public static def reference_3D(output:Array[Float](1){rail}, input:Array[Float](1){rail}, coeff:Array[Float](1){rail}, dimx:Int, dimy:Int, dimz:Int, radius:Int)
    {
        val dimxy = dimx*dimy;

        var output_off:Int=0, input_off:Int=0;
        for(var iz:Int=0; iz<dimz; iz++)
        {
            for(var iy:Int=0; iy<dimy; iy++)
            {
                for(var ix:Int=0; ix<dimx; ix++)
                {
                    if( ix>=radius && ix<(dimx-radius) && iy>=radius && iy<(dimy-radius) && iz>=radius && iz<(dimz-radius) )
                    {
                        var valu:Float = input(input_off)*coeff(0);

                        for(var ir:Int=1; ir<=radius; ir++)
                        {
                            valu += coeff(ir) * (input(input_off+ir) + input(input_off-ir));                // horizontal
                            valu += coeff(ir) * (input(input_off+ir*dimx) + input(input_off-ir*dimx));        // vertical
                            valu += coeff(ir) * (input(input_off+ir*dimxy) + input(input_off-ir*dimxy));    // in front / behind
                        }

                        output(output_off) = valu;
                    }

                    ++output_off;
                    ++input_off;
                }
            }
        }
    }

    public static def within_epsilon(output:Array[Float](1){rail}, reference:Array[Float](1){rail}, dimx:Int, dimy:Int, dimz:Int)
    { return within_epsilon(output, reference, dimx, dimy, dimz, 4); }
    public static def within_epsilon(output:Array[Float](1){rail}, reference:Array[Float](1){rail}, dimx:Int, dimy:Int, dimz:Int, radius:Int)
    { return within_epsilon(output, reference, dimx, dimy, dimz, radius, -1); }
    public static def within_epsilon(output:Array[Float](1){rail}, reference:Array[Float](1){rail}, dimx:Int, dimy:Int, dimz:Int, radius:Int, zadjust:Int)
    { return within_epsilon(output, reference, dimx, dimy, dimz, radius, zadjust, 0.0001f); }
    public static def within_epsilon(output:Array[Float](1){rail}, reference:Array[Float](1){rail}, dimx:Int, dimy:Int, dimz:Int, radius:Int, zadjust:Int, delta:Float )
    {
        var retval:Boolean = true;

        var output_off:Int=0, ref_off:Int=0;
        for(var iz:Int=0; iz<dimz; iz++)
        {
            for(var iy:Int=0; iy<dimy; iy++)
            {
                for(var ix:Int=0; ix<dimx; ix++)
                {
                    if( ix>=radius && ix<(dimx-radius) && iy>=radius && iy<(dimy-radius) && iz>=radius && iz<(dimz-radius+zadjust) )
                    {
                        val difference = Math.abs( reference(ref_off) - output(output_off));

                        if( difference > delta )
                        {
                            retval = false;
                            Console.OUT.println(String.format(" ERROR: (%d,%d,%d)\t%.2f instead of %.2f", [ix,iy,iz, output(output_off), reference(ref_off)]));

                            return false;
                        }
                        //Console.OUT.println(ix+" "+iy+" "+difference);
                    }

                    ++output_off;
                    ++ref_off;
                }
            }
        }

        return retval;
    }


    public static def main (args : Array[String](1){rail}) {

        //cudaDeviceProp properties;

        //cudaGetDeviceProperties(&properties, device);
        //printf("3DFD running on: %s\n", properties.name);
        //if (properties.totalGlobalMem >= 1024*1024*1024) {
        //    printf("Total GPU Memory: %.4f GB\n", properties.totalGlobalMem/(1024.f*1024.f*1024.f) );
        //} else {
        //    printf("Total GPU Memory: %.4f MB\n", properties.totalGlobalMem/(1024.f*1024.f) );
        //}

        /////////////////////////////////////////////
        // process command-line arguments,
        // set execution parameters
        //
        var nreps:Int = 1;              // number of time-steps, over which performance is averaged
        var check_correctness:Boolean = true;  // 1=check correcness, 0-don't.  Note that CPU code is very
                                    //   naive and not optimized, so many steps will take a 
                                    //   long time on CPU
        var pad:Int  = 0;
        var dimx_:Int = 480+pad;
        var dimy_:Int = 480;
        var dimz_:Int = 400;
        
        //if( 2.2*nbytes > properties.totalGlobalMem )    // adjust the volume size if it exceeds available
        //{                                               // global memory (allowing for some memory use
        //                                                // by the driver
        //    dimz = properties.totalGlobalMem / (2.2*dimx*dimy*sizeof(float));
        //    nbytes= dimx*dimy*dimz*sizeof(float);
        //}

        if( args.size >= 3 )
        {
            dimx_   = Int.parse(args(0));
            dimy_   = Int.parse(args(1));
            dimz_   = Int.parse(args(2));
        }
        if( args.size >= 4)
            nreps = Int.parse(args(3));
        if( args.size >= 5)
            check_correctness = Boolean.parse(args(4));
        val dimx=dimx_, dimy=dimy_, dimz=dimz_;
        val nelements = dimx*dimy*dimz;

        Console.OUT.println(String.format("%dx%dx%d", [dimx as Any, dimy, dimz]));


        /////////////////////////////////////////////
        // setup data
        //
        
        // initialize data
        val h_data = new Array[Float](nelements);
        val h_reference = new Array[Float](nelements);
        random_data(h_data, dimx,dimy,dimz, 1, 5 );

        // allocate CPU and GPU memory
        val gpu = here.children().size==0 ? here : here.child(0);
        
        val d_input = CUDAUtilities.makeRemoteArray[Float](gpu,nelements,h_data); // allocate 
        val d_output = CUDAUtilities.makeRemoteArray[Float](gpu,nelements,(Int)=>0.0 as Float); // allocate 

        Console.OUT.println(String.format("allocated %.6f MB on device", [((2.f*nelements*4)/(1024.f*1024.f)) as Any]));

        val RADIUS = 4;

        // setup coefficients
        val h_coeff_symmetric = new Array[Float](RADIUS+1, 1);

        // kernel launch configuration

        /////////////////////////////////////////////
        // kernel execution
        //
        var start_time : Long = System.currentTimeMillis();
        for(var i:Int=0; i<nreps; i++) {
            val BLOCK_DIMX = 16;
            val BLOCK_DIMY = BLOCK_DIMX;
            val THREADS = BLOCK_DIMX*BLOCK_DIMY, BLOCKS_X=dimx/BLOCK_DIMX, BLOCKS_Y=dimy/BLOCK_DIMY;
            val S_DATA_STRIDE = BLOCK_DIMX+2*RADIUS;
            finish async at (gpu) @CUDA @CUDADirectParams {
                val c_coeff = h_coeff_symmetric.sequence();
                finish for ([block] in 0..(BLOCKS_X*BLOCKS_Y-1)) async {
                    val s_data = new Array[Float]((BLOCK_DIMY+2*RADIUS)*S_DATA_STRIDE, 0);
                    clocked finish for ([thread] in 0..(THREADS-1)) clocked async {
                        val blockidx = block%BLOCKS_X;
                        val blockidy = block/BLOCKS_X;
                        val threadidx = thread%BLOCK_DIMX;
                        val threadidy = thread/BLOCK_DIMX;
                        val ix = blockidx * BLOCK_DIMX + threadidx;
                        val iy = blockidy * BLOCK_DIMY + threadidy;
                        var in_idx:Int=iy*dimx + ix;
                        var out_idx:Int = 0;
                        val stride  = dimx*dimy;

                        var infront1:Float, infront2:Float, infront3:Float, infront4:Float;
                        var behind1:Float, behind2:Float, behind3:Float, behind4:Float;
                        var current:Float;

                        val tx = threadidx + RADIUS;
                        val ty = threadidy + RADIUS;

                        // fill the "in-front" and "behind" data
                        behind3  = d_input(in_idx);    in_idx += stride;
                        behind2  = d_input(in_idx);    in_idx += stride;
                        behind1  = d_input(in_idx);    in_idx += stride;

                        current  = d_input(in_idx);    out_idx = in_idx; in_idx += stride;

                        infront1 = d_input(in_idx);    in_idx += stride;
                        infront2 = d_input(in_idx);    in_idx += stride;
                        infront3 = d_input(in_idx);    in_idx += stride;
                        infront4 = d_input(in_idx);    in_idx += stride;

                        for(var i:Int=RADIUS; i<dimz-RADIUS; i++)
                        {
                            //////////////////////////////////////////
                            // advance the slice (move the thread-front)
                            behind4  = behind3;
                            behind3  = behind2;
                            behind2  = behind1;
                            behind1  = current;
                            current  = infront1;
                            infront1 = infront2;
                            infront2 = infront3;
                            infront3 = infront4;
                            infront4 = d_input(in_idx);

                            in_idx  += stride;
                            out_idx += stride;
                            next;

                            /////////////////////////////////////////
                            // update the data slice in smem

                            if(threadidy<RADIUS)    // halo above/below
                            {
                                s_data(threadidy*S_DATA_STRIDE + tx)                     = d_input(out_idx-RADIUS*dimx);
                                s_data((threadidy+BLOCK_DIMY+RADIUS)*S_DATA_STRIDE + tx) = d_input(out_idx+BLOCK_DIMY*dimx);
                            }

                            if(threadidx<RADIUS)    // halo left/right
                            {
                                s_data(ty*S_DATA_STRIDE + threadidx)                   = d_input(out_idx-RADIUS);
                                s_data(ty*S_DATA_STRIDE + threadidx+BLOCK_DIMX+RADIUS) = d_input(out_idx+BLOCK_DIMX);
                            }

                            // update the slice in smem
                            s_data((ty)*S_DATA_STRIDE + tx) = current;
                            next;

                            /////////////////////////////////////////
                            // compute the output value
                            var valu:Float  = @NoInline c_coeff(0) * current;
                            val sd1a = @NoInline s_data((ty-1)*S_DATA_STRIDE + tx);
                            val sd1b = @NoInline s_data((ty+1)*S_DATA_STRIDE + tx);
                            val sd1c = @NoInline s_data(ty*S_DATA_STRIDE + tx-1);
                            val sd1d = @NoInline s_data(ty*S_DATA_STRIDE + tx+1);
                            val sd2a = @NoInline s_data((ty-2)*S_DATA_STRIDE + tx);
                            val sd2b = @NoInline s_data((ty+2)*S_DATA_STRIDE + tx);
                            val sd2c = @NoInline s_data(ty*S_DATA_STRIDE + tx-2);
                            val sd2d = @NoInline s_data(ty*S_DATA_STRIDE + tx+2);
                            val sd3a = @NoInline s_data((ty-3)*S_DATA_STRIDE + tx);
                            val sd3b = @NoInline s_data((ty+3)*S_DATA_STRIDE + tx);
                            val sd3c = @NoInline s_data(ty*S_DATA_STRIDE + tx-3);
                            val sd3d = @NoInline s_data(ty*S_DATA_STRIDE + tx+3);
                            val sd4a = @NoInline s_data((ty-4)*S_DATA_STRIDE + tx);
                            val sd4b = @NoInline s_data((ty+4)*S_DATA_STRIDE + tx);
                            val sd4c = @NoInline s_data(ty*S_DATA_STRIDE + tx-4);
                            val sd4d = @NoInline s_data(ty*S_DATA_STRIDE + tx+4);
                            valu += @NoInline c_coeff(1)*( infront1 + behind1 + sd1a + sd1b + sd1c + sd1d );
                            valu += @NoInline c_coeff(2)*( infront2 + behind2 + sd2a + sd2b + sd2c + sd2d );
                            valu += @NoInline c_coeff(3)*( infront3 + behind3 + sd3a + sd3b + sd3c + sd3d );
                            valu += @NoInline c_coeff(4)*( infront4 + behind4 + sd4a + sd4b + sd4c + sd4d );
                            d_output(out_idx) = valu;
                        }
                    }
                }
            }
        }
        var elapsed_time_ms:Long = System.currentTimeMillis() - start_time;

        elapsed_time_ms /= nreps;
        val throughput_mpoints = (dimx*dimy*(dimz-2*RADIUS))/(elapsed_time_ms*1e3f);

        Console.OUT.println("-------------------------------");
        Console.OUT.println("time:       "+elapsed_time_ms+" ms");
        Console.OUT.println("throughput: "+throughput_mpoints+" MPoints/s");
        //printf("CUDA: %s\n", cudaGetErrorString(cudaGetLastError()) );


        /////////////////////////////////////////////
        // check the correctness
        //
        if( check_correctness)
        {
            Console.OUT.println("-------------------------------\n");
            Console.OUT.println("comparing to CPU result...\n");
            reference_3D( h_reference, h_data, h_coeff_symmetric, dimx,dimy,dimz, RADIUS );
            finish Array.asyncCopy(d_output, 0, h_data, 0, nelements);
            if( within_epsilon( h_data, h_reference, dimx,dimy,dimz, RADIUS*nreps, 0 ) ) 
            {
                Console.OUT.println("  Result within epsilon\n");
                Console.OUT.println("  TEST PASSED!\n");
            }
            else
            {
                Console.OUT.println("  Incorrect result\n");    
                Console.OUT.println("  TEST FAILED!\n");
            }
        }

        CUDAUtilities.deleteRemoteArray(d_input);
        CUDAUtilities.deleteRemoteArray(d_output);
    }

}

// vim: shiftwidth=4:tabstop=4:expandtab
