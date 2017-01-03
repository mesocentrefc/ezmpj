package mesofc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class DefaultMessageCodec implements EZMessageCodec {

    public byte[] serialize(Object obj) throws IOException {

	final ByteArrayOutputStream bao = new ByteArrayOutputStream();
	final ObjectOutputStream oos = new ObjectOutputStream(bao);

	oos.writeObject(obj);
	oos.flush();
	oos.close();

	return bao.toByteArray();

    }

    public Object deserialize(byte[] buffer) throws IOException, ClassNotFoundException {

	final ByteArrayInputStream bin = new ByteArrayInputStream(buffer);
	final ObjectInputStream ois = new ObjectInputStream(bin);

	Object obj = ois.readObject();
	ois.close();
	
	return obj;

    }
}