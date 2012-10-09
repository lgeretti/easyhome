package it.uniud.easyhome.gateway;

import it.uniud.easyhome.network.NativePacket;

import java.io.*;

public class NativeGateway extends Gateway {
    
    public NativeGateway(byte id, int port) {
    	super(id,ProtocolType.NATIVE,port);
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