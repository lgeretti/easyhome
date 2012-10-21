package it.uniud.easyhome.gateway;

import it.uniud.easyhome.exceptions.IncompletePacketException;
import it.uniud.easyhome.packets.NativePacket;

import java.io.*;

public class NativeGateway extends Gateway {
	
    public NativeGateway(byte id, int port) {
    	super(id,ProtocolType.NATIVE,port);
    	MAX_CONNECTIONS = 32;
    }
    
    @Override
    public final void open() {

        Thread thr = new Thread(this);
        thr.start();
    }
    
    @Override
    final protected NativePacket readFrom(InputStream is, ByteArrayOutputStream buffer) throws IOException {
    	
    	NativePacket result = null;
    	try {
	    	byte readByte = (byte)is.read();
	    	if (buffer.size() == 0 && readByte != NativePacket.DELIMITER)
	    		throw new IncompletePacketException();
	    	
	    	buffer.write(readByte);
	    	if (is.available() > 0) {
	    		byte[] readBytes = new byte[is.available()];
	    		is.read(readBytes);
	    		buffer.write(readBytes);
	    	}
	    		
	    	result = new NativePacket(new ByteArrayInputStream(buffer.toByteArray()));
    	} catch (Exception ex) {
    		if (ex instanceof IOException)
    			throw ex;
    		
    		ex.printStackTrace();
    	}
    	
    	return result;
    }
    
    @Override
    final protected void write(NativePacket pkt, OutputStream os) throws IOException {
    	
		pkt.write(os);
    }
  
}