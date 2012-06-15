package x10.io;

@x10.core.X10Generated public class FileNotFoundException extends x10.io.IOException implements x10.x10rt.X10JavaSerializable
{
    private static final long serialVersionUID = 1L;
    private static final short $_serialization_id = x10.x10rt.DeserializationDispatcher.addDispatcher(x10.x10rt.DeserializationDispatcher.ClosureKind.CLOSURE_KIND_NOT_ASYNC, FileNotFoundException.class);
    
    public static final x10.rtt.RuntimeType<FileNotFoundException> $RTT = x10.rtt.NamedType.<FileNotFoundException> make(
    "x10.io.FileNotFoundException", /* base class */FileNotFoundException.class
    , /* parents */ new x10.rtt.Type[] {x10.io.IOException.$RTT}
    );
    public x10.rtt.RuntimeType<?> $getRTT() {return $RTT;}
    
    
    private void writeObject(java.io.ObjectOutputStream oos) throws java.io.IOException { if (x10.runtime.impl.java.Runtime.TRACE_SER) { java.lang.System.out.println("Serializer: writeObject(ObjectOutputStream) of " + this + " calling"); } oos.defaultWriteObject(); }
    public static x10.x10rt.X10JavaSerializable $_deserialize_body(FileNotFoundException $_obj , x10.x10rt.X10JavaDeserializer $deserializer) throws java.io.IOException { 
    
        if (x10.runtime.impl.java.Runtime.TRACE_SER) { x10.runtime.impl.java.Runtime.printTraceMessage("X10JavaSerializable: $_deserialize_body() of " + FileNotFoundException.class + " calling"); } 
        x10.io.IOException.$_deserialize_body($_obj, $deserializer);
        return $_obj;
        
    }
    
    public static x10.x10rt.X10JavaSerializable $_deserializer(x10.x10rt.X10JavaDeserializer $deserializer) throws java.io.IOException { 
    
        FileNotFoundException $_obj = new FileNotFoundException((java.lang.System[]) null);
        $deserializer.record_reference($_obj);
        return $_deserialize_body($_obj, $deserializer);
        
    }
    
    public short $_get_serialization_id() {
    
         return $_serialization_id;
        
    }
    
    public void $_serialize(x10.x10rt.X10JavaSerializer $serializer) throws java.io.IOException {
    
        super.$_serialize($serializer);
        
    }
    
    // constructor just for allocation
    public FileNotFoundException(final java.lang.System[] $dummy) { 
    super($dummy);
    }
    
        
        
//#line 15 "/home/lshadare/x10-constraints/x10.runtime/src-x10/x10/io/FileNotFoundException.x10"
public FileNotFoundException() {super();
                                                                                                                                               {
                                                                                                                                                  
//#line 15 "/home/lshadare/x10-constraints/x10.runtime/src-x10/x10/io/FileNotFoundException.x10"

                                                                                                                                              }}
        
        
//#line 16 "/home/lshadare/x10-constraints/x10.runtime/src-x10/x10/io/FileNotFoundException.x10"
public FileNotFoundException(final java.lang.String message) {super(((java.lang.String)(message)));
                                                                                                                                                                             {
                                                                                                                                                                                
//#line 16 "/home/lshadare/x10-constraints/x10.runtime/src-x10/x10/io/FileNotFoundException.x10"

                                                                                                                                                                            }}
        
        
//#line 14 "/home/lshadare/x10-constraints/x10.runtime/src-x10/x10/io/FileNotFoundException.x10"
final public x10.io.FileNotFoundException
                                                                                                            x10$io$FileNotFoundException$$x10$io$FileNotFoundException$this(
                                                                                                            ){
            
//#line 14 "/home/lshadare/x10-constraints/x10.runtime/src-x10/x10/io/FileNotFoundException.x10"
return x10.io.FileNotFoundException.this;
        }
        
        }
        