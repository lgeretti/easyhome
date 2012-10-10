package it.uniud.easyhome.packets;

import java.io.IOException;
import java.io.InputStream;

public interface ReceivedPacket {

	public void read(InputStream os) throws IOException;
}
