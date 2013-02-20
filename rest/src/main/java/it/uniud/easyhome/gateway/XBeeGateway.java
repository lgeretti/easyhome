package it.uniud.easyhome.gateway;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.LogLevel;
import it.uniud.easyhome.exceptions.ChecksumException;
import it.uniud.easyhome.exceptions.IllegalBroadcastPortException;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.exceptions.NoBytesAvailableException;
import it.uniud.easyhome.exceptions.RoutingEntryMissingException;
import it.uniud.easyhome.network.ModuleCoordinates;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.Operation;
import it.uniud.easyhome.packets.natives.NativePacket;
import it.uniud.easyhome.packets.xbee.XBeePacketToNode;
import it.uniud.easyhome.packets.xbee.XBeePacketFromNode;

import java.io.*;

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
        // if the destination port is actually a management port
        if (receiveOptions == 0x02) {
        	if (xpkt.getProfileId() == Domain.MANAGEMENT.getCode() && (dstEndpoint == 0x00 || dstEndpoint == (byte)0xEA)) {        		
	        	dstCoords = new ModuleCoordinates((byte)0,0xFFFFL,(short)0xFFFE,dstEndpoint);
	        	log(LogLevel.DEBUG, "Setting destination as broadcast");
        	} else {
        		throw new IllegalBroadcastPortException();
        	}
        } else {
	        
        	dstCoords = new ModuleCoordinates((byte)1,0x0L,(short)0x0,dstEndpoint);
    		log(LogLevel.DEBUG, "Setting destination as domotic controller");
        	
        	// If this is the implicit EasyHome controller endpoint
        	/*
        	if (xpkt.getProfileId() == Domain.MANAGEMENT.getCode() && (dstEndpoint == 0x00 || dstEndpoint == (byte)0xEA)) {
        		dstCoords = new ModuleCoordinates((byte)1,0x0L,(short)0x0,dstEndpoint);
        		log(LogLevel.DEBUG, "Setting destination as domotic controller");
        	} else {
        		dstCoords = getCoordinatesFor(dstEndpoint);
        	}
        	
	        if (dstCoords == null) {
		        log(LogLevel.INFO, "Could not find coordinates for mapped endpoint " + dstEndpoint);
		        throw new RoutingEntryMissingException();
	        }
	        */
	    }
        
        Operation op = new Operation(xpkt.getTransactionSeqNumber(),xpkt.getProfileId(),
        		xpkt.getClusterId(),(byte)0x0/*Frame control*/,xpkt.getCommand(),xpkt.getApsPayload());
        
        return new NativePacket(srcCoords,dstCoords,op);
    }
    
    @Override
    protected final void start() {

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
    		
    		log(LogLevel.DEBUG, "Read: " + ByteUtils.printBytes(readBytes));
    		
    		buffer.write(readBytes);
    	}
    	
    	byte[] originalBuffer = buffer.toByteArray();
    	
    	log(LogLevel.DEBUG, "Buffer: " + ByteUtils.printBytes(originalBuffer));
    	
    	int readBytes = 0;
    	
    	try {
    	
    		readBytes = xbeePkt.read(new ByteArrayInputStream(originalBuffer));

    	} catch (ChecksumException ex) {

    		readBytes = 4 + (originalBuffer[1]*256) + originalBuffer[2];
    		log(LogLevel.INFO, "Discarding checksum-failing packet of " + readBytes + " bytes");
    		
    		throw ex;
    	} catch (InvalidPacketTypeException ex) {

    		readBytes = 4 + (originalBuffer[1]*256) + originalBuffer[2];
    		log(LogLevel.INFO, "Discarding invalid-type packet of " + readBytes + " bytes");
    		
    		throw ex;
    	} finally {

    		// Reduces the buffer to the original amount minus the bytes consumed by the packet
       		buffer.reset();
        	if (readBytes < originalBuffer.length)
        		buffer.write(originalBuffer, readBytes, originalBuffer.length-readBytes);
    	}
    	
    	log(LogLevel.DEBUG, "XBee packet: " + ByteUtils.printBytes(xbeePkt.getBytes()));
    	
   		result = convertFrom(xbeePkt);
    	
    	return result;
    }
    
    @Override
    final protected void write(NativePacket pkt, OutputStream os) throws IOException {
    	
    	// NOTE: we do not need to remap back the endpoint of the source, since the gateway will become the source
    	// We instead need to rewrite the endpoints in the APS payload, where present, thus depending on the packet type
		XBeePacketFromNode xbeePkt = new XBeePacketFromNode(pkt);
		// We remap the endpoint of the destination, since the native network uses 0 for all cases
		// FIXME: depends on the actual node manufacturer and the fact that this is a management command like
		// endpoint list, simple descriptors and bind/unbind commands
		//if (xbeePkt.getDstEndpoint() == 0)
		//	xbeePkt.setDstEndpoint((byte)0x01);
		xbeePkt.write(os);		
    }
  
}