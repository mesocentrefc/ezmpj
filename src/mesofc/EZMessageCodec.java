package mesofc;

import java.io.IOException;

public interface EZMessageCodec {
    
    public  byte[] serialize(Object obj) throws IOException;
    
    public  Object deserialize(byte[] buffer) throws IOException, ClassNotFoundException ;

}
