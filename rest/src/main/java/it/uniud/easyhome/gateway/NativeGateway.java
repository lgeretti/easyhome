package it.uniud.easyhome.gateway;

import it.uniud.easyhome.network.NativePacket;

import java.io.*;

public class NativeGateway extends Gateway {
	
	private static final int PORT = 5001;
	
    public NativeGateway() {
    	super((byte)1,ProtocolType.NATIVE,PORT);
    	MAX_CONNECTIONS = 32;
    }
    
    @Override
    public final void open() {

        Thread thr = new Thread(this);
        thr.start();
    }
    
    @Override
    final protected NativePacket readFrom(InputStream is) throws IOException {

    	return new NativePacket(is);
    }
    
    @Override
    final protected void write(NativePacket pkt, OutputStream os) throws IOException {
    	
		pkt.write(os);
    }
  
}