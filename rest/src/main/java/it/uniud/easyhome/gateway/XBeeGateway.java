package it.uniud.easyhome.gateway;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.exceptions.IllegalBroadcastPortException;
import it.uniud.easyhome.exceptions.IncompletePacketException;
import it.uniud.easyhome.exceptions.NoBytesAvailableException;
import it.uniud.easyhome.exceptions.RoutingEntryMissingException;
import it.uniud.easyhome.packets.ModuleCoordinates;
import it.uniud.easyhome.packets.Operation;
import it.uniud.easyhome.packets.natives.NativePacket;
import it.uniud.easyhome.packets.xbee.XBeeConstants;
import it.uniud.easyhome.packets.xbee.XBeePacketToNode;
import it.uniud.easyhome.packets.xbee.XBeePacketFromNode;

import java.io.*;
import java.util.Map;

public class XBeeGateway extends Gateway {
    
    public XBeeGateway(byte id, int port) {
    	super(id,ProtocolType.XBEE,port);
    }
    
    private NativePacket convertFrom(XBeePacketToNode xpkt) throws RoutingEntryMissingException {
        
        ModuleCoordinates srcCoords = new ModuleCoordinates(
        		id,xpkt.get64BitSrcAddr(),xpkt.get16BitSrcAddr(),xpkt.getSrcEndpoint());
        
        byte receiveOptions = xpkt.getReceiveOptions();
        byte dstEndpoint = xpkt.getDstEndpoint();
        
        ModuleCoordinates dstCoords = null;
        
        // If a broadcast, we use the broadcast format for the destination coordinates, but only
        // if the destination port is actually an administration port
        if (receiveOptions == 0x02) {
        	if (dstEndpoint == 0x00 || dstEndpoint == 0x01) {        		
	        	dstCoords = new ModuleCoordinates((byte)0,0xFFFFL,(short)0xFFFE,(byte)0);
	        	println("Setting destination as broadcast");
        	} else {
        		throw new IllegalBroadcastPortException();
        	}
        } else {
	        
        	// If this is the implicit EasyHome controller endpoint
        	if (dstEndpoint == 0x00 || dstEndpoint == 0x01) {
        		dstCoords = new ModuleCoordinates((byte)1,0x0L,(short)0x0,(byte)0);
        		println("Setting destination as domotic controller");
        	} else {
        		dstCoords = getCoordinatesFor(dstEndpoint);
        	}
	        
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
    final protected NativePacket readFrom(InputStream is, ByteArrayOutputStream buffer) throws IOException {
    	
    	NativePacket result = null;
    	
    	XBeePacketToNode xbeePkt = new XBeePacketToNode();
    	
    	if (is.available() == 0 && buffer.size() == 0)
    		throw new NoBytesAvailableException();
    	
    	if (is.available() > 0) {
    		byte[] readBytes = new byte[is.available()];
    		is.read(readBytes);
    		
    		buffer.write(readBytes);
    	}
    	
    	byte[] originalBuffer = buffer.toByteArray();
    	
    	int readBytes = xbeePkt.read(new ByteArrayInputStream(originalBuffer));
    	
    	result = convertFrom(xbeePkt);

    	buffer.reset();
    	if (readBytes < originalBuffer.length)
    		buffer.write(originalBuffer, readBytes, originalBuffer.length-readBytes);
    	
    	return result;
    }
    
    @Override
    final protected void write(NativePacket pkt, OutputStream os) throws IOException {
    	
    	// NOTE: we do not need to remap back the endpoint of the source, since the gateway will become the source
    	// We instead need to rewrite the endpoints in the APS payload, where present, thus depending on the packet type
		XBeePacketFromNode xbeePkt = new XBeePacketFromNode(pkt);
		// We remap the endpoint of the destination, since the native network uses 0 for all cases
		// FIXME: depends on the actual node manufacturer
		if (xbeePkt.getDstEndpoint() == 0)
			xbeePkt.setDstEndpoint((byte)0x01);
		xbeePkt.write(os);		
    }
  
}