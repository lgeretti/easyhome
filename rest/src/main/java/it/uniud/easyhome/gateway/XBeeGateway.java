package it.uniud.easyhome.gateway;

import it.uniud.easyhome.exceptions.IllegalBroadcastPortException;
import it.uniud.easyhome.exceptions.IncompletePacketException;
import it.uniud.easyhome.exceptions.NoBytesAvailableException;
import it.uniud.easyhome.exceptions.RoutingEntryMissingException;
import it.uniud.easyhome.packets.ModuleCoordinates;
import it.uniud.easyhome.packets.Operation;
import it.uniud.easyhome.packets.natives.NativePacket;
import it.uniud.easyhome.packets.xbee.XBeeConstants;
import it.uniud.easyhome.packets.xbee.XBeeInboundPacket;
import it.uniud.easyhome.packets.xbee.XBeeOutboundPacket;

import java.io.*;

public class XBeeGateway extends Gateway {
    
    public XBeeGateway(byte id, int port) {
    	super(id,ProtocolType.XBEE,port);
    }
    
    private NativePacket convertFrom(XBeeInboundPacket xpkt) throws RoutingEntryMissingException {
        
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
    final protected NativePacket readFrom(InputStream is, ByteArrayOutputStream buffer) throws IOException {
    	
    	NativePacket result = null;
    	
    	XBeeInboundPacket xbeePkt = new XBeeInboundPacket();
    	
    	if (is.available() == 0)
    		throw new NoBytesAvailableException();
    	
    	int readByte = is.read();
    	if (buffer.size() == 0 && ((byte)readByte) != XBeeConstants.START_DELIMITER)
    		throw new IncompletePacketException();
    	
    	buffer.write(readByte);
    	if (is.available() > 0) {
    		byte[] readBytes = new byte[is.available()];
    		is.read(readBytes);
    		
    		buffer.write(readBytes);
    	}
    	
    	xbeePkt.read(new ByteArrayInputStream(buffer.toByteArray()));
    	result = convertFrom(xbeePkt);
    	
    	return result;
    }
    
    @Override
    final protected void write(NativePacket pkt, OutputStream os) throws IOException {
    	
		XBeeOutboundPacket xbeePkt = new XBeeOutboundPacket(pkt);
		xbeePkt.write(os);
    }
  
}