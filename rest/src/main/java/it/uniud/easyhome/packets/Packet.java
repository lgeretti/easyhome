package it.uniud.easyhome.packets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Packet {

	public byte[] getBytes();
	
	public void read(InputStream is) throws IOException;
	
	public void write(OutputStream os) throws IOException;
}
