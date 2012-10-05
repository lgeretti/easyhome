package it.uniud.easyhome.network;

import java.io.IOException;
import java.io.OutputStream;

public interface TransmittedPacket {

	public void write(OutputStream os) throws IOException;
}
