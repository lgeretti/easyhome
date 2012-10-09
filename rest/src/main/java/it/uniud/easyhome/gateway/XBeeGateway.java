package it.uniud.easyhome.gateway;

import it.uniud.easyhome.network.NativePacket;
import it.uniud.easyhome.network.ModuleCoordinates;
import it.uniud.easyhome.network.Operation;
import it.uniud.easyhome.network.xbee.XBeeReceivedPacket;
import it.uniud.easyhome.network.xbee.XBeeTransmittedPacket;
import it.uniud.easyhome.network.exceptions.IllegalBroadcastPortException;
import it.uniud.easyhome.network.exceptions.RoutingEntryMissingException;

import java.io.*;

public class XBeeGateway extends Gateway {
    
    public XBeeGateway(byte id, int port) {
    	super(id,ProtocolType.XBEE,port);
    }
    
    private NativePacket convertFrom(XBeeReceivedPacket xpkt) throws RoutingEntryMissingException {
        
        ModuleCoordinates srcCoords = new ModuleCoordinates(
        		id,xpkt.get64BitSrcAddr(),xpkt.get16BitSrcAddr(),xpkt.getSrcEndpoint());
        
        byte receiveOptions = xpkt.getReceiveOptions();
        byte dstEndpoint = xpkt.getDstEndpoint();
        
        ModuleCoordinates dstCoords = null;
        
        // If a broadcast, we use the broadcast format for the destination coordinates, but only
        // if the destination port is actually the administration port
        if (receiveOptions == 0x02) {
        	if (dstEndpoint == 0x00) {        		
	        	dstCoords = new ModuleCoordinates((byte)0,(short)0xFFFF,(short)0xFFFE,(byte)0);
	        	println("Setting destination as broadcast");
        	} else {
        		throw new IllegalBroadcastPortException();
        	}
        } else {
	        
	        dstCoords = getCoordinatesFor(dstEndpoint);
	        
	        if (dstCoords == null) {
		        println("Could not find coordinates for mapped endpoint " + dstEndpoint);
		        throw new RoutingEntryMissingException();
	        }
	    }
        
        Operation op = new Operation(xpkt.getTransactionSeqNumber(),xpkt.getProfileId(),
        		xpkt.getClusterId(),xpkt.getFrameControl(),xpkt.getCommand(),xpkt.getApsPayload());
        
        return new NativePacket(srcCoords,dstCoords,op);
    }
    
    @Override
    public final void open() {

        Thread thr = new Thread(this);
        thr.start();
    }
    
    @Override
    final protected NativePacket readFrom(InputStream is) throws IOException {

    	NativePacket result = null;
    	try {
	    	XBeeReceivedPacket xbeePkt = new XBeeReceivedPacket();
	    	xbeePkt.read(is);
	    	result = convertFrom(xbeePkt);
    	} catch (Exception ex) {
    		if (ex instanceof IOException)
    			throw ex;
    		
    		ex.printStackTrace();
    	}
    	
    	return result;
    }
    
    @Override
    final protected void write(NativePacket pkt, OutputStream os) throws IOException {
    	
		XBeeTransmittedPacket xbeePkt = new XBeeTransmittedPacket(pkt);
		xbeePkt.write(os);
    }
  
}