package it.uniud.easyhome.packets;

import java.io.IOException;
import java.io.OutputStream;

public interface TransmittedPacket {

	public void write(OutputStream os) throws IOException;
}
