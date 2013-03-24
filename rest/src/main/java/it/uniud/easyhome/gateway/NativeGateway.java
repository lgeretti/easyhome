package it.uniud.easyhome.gateway;

import it.uniud.easyhome.common.LogLevel;
import it.uniud.easyhome.exceptions.IncompletePacketException;
import it.uniud.easyhome.packets.natives.NativePacket;

import java.io.*;

import javax.jms.MessageProducer;
import javax.jms.Session;

public class NativeGateway extends Gateway {
	
    public NativeGateway(byte id, int port, LogLevel logLevel) {
    	super(id,ProtocolType.NATIVE,port,logLevel);
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
    final protected void write(NativePacket pkt, OutputStream os, Session jmsSession, MessageProducer producer) throws IOException {
    	
		pkt.write(os);
    }
  
}