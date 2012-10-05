package it.uniud.easyhome.network;

import java.io.IOException;
import java.io.InputStream;

public interface ReceivedPacket {

	public void read(InputStream os) throws IOException;
}
