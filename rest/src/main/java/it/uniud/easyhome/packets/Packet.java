package it.uniud.easyhome.packets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Packet {

	public byte[] getBytes();
	
	/** @return The number of bytes read to obtain the packet */
	public int read(InputStream is) throws IOException;
	
	public void write(OutputStream os) throws IOException;
}
