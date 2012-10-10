package it.uniud.easyhome.xbee;

import it.uniud.easyhome.exceptions.ChecksumException;
import it.uniud.easyhome.exceptions.InvalidDelimiterException;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.packets.ReceivedPacket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class XBeeReceivedPacket implements ReceivedPacket {

	private long srcAddr64;
	private short srcAddr16;
	private byte srcEndpoint;
	private byte dstEndpoint;
	private short clusterId;
	private short profileId;
	private byte receiveOptions = 0x00;
	private byte frameControl = 0x00;
	private byte transactionSeqNumber = 0x00;
	private byte command = 0x00;
	private byte[] apsPayload = new byte[0];
	
	public XBeeReceivedPacket() {
	}
	
	public long get64BitSrcAddr() {
		return srcAddr64;
	}
	public void set64BitSrcAddr(long srcAddr64) {
		this.srcAddr64 = srcAddr64;
	}

	public short get16BitSrcAddr() {
		return srcAddr16;
	}	
	public void set16BitSrcAddr(short srcAddr16) {
		this.srcAddr16 = srcAddr16;
	}

	public byte getSrcEndpoint() {
		return srcEndpoint;
	}
	public void setSrcEndpoint(byte srcEndpoint) {
		this.srcEndpoint = srcEndpoint;
	}

	public byte getDstEndpoint() {
		return dstEndpoint;
	}
	public void setDstEndpoint(byte dstEndpoint) {
		this.dstEndpoint = dstEndpoint;
	}
	
	public short getClusterId() {
		return clusterId;
	}
	public void setClusterId(short clusterId) {
		this.clusterId = clusterId;
	}
	
	public short getProfileId() {
		return profileId;
	}
	public void setProfileId(short profileId) {
		this.profileId = profileId;
	}
	
	public byte getReceiveOptions() {
		return receiveOptions;
	}
	public void setReceiveOptions(byte receiveOptions) {
		this.receiveOptions = receiveOptions;
	}
	
	public byte getFrameControl() {
		return frameControl;
	}
	public void setFrameControl(byte frameControl) {
		this.frameControl = frameControl;
	}
	
	public byte getTransactionSeqNumber() {
		return transactionSeqNumber;
	}
	public void setTransactionSeqNumber(byte transactionSeqNumber) {
		this.transactionSeqNumber = transactionSeqNumber;
	}
	
	public byte getCommand() {
		return command;
	}
	public void setCommand(byte command) {
		this.command = command;
	}
	
	public byte[] getApsPayload() {
		return apsPayload;
	}
	public void setApsPayload(byte[] apsPayload) {
		this.apsPayload = apsPayload;
	}
	@Override
	public void read(InputStream is) throws IOException, InvalidDelimiterException, 
											InvalidPacketTypeException, ChecksumException {

    	int octet = is.read();
    	
    	if (octet == -1)
    		throw new IOException();
    	
        if (octet != XBeeConstants.START_DELIMITER)
        	throw new InvalidDelimiterException();
        	
        int highLength = is.read();
        // (-1 because the frame type is not stored)
        int length = highLength*256 + is.read() - 1;
        
        byte[] packetPayload = new byte[length];
        
        int sum = XBeeConstants.EXPLICIT_RX_INDICATOR_FRAME_TYPE;
        byte frameType = (byte)is.read();
        if (frameType != XBeeConstants.EXPLICIT_RX_INDICATOR_FRAME_TYPE) 
        	throw new InvalidPacketTypeException();
                
        for (int i=0; i<length; i++) {
            int readValue = is.read();
            packetPayload[i] = (byte)readValue;
            sum += readValue;
        }
        sum += is.read();
             
        if (0xFF != (sum & 0xFF)) 
        	throw new ChecksumException();
        
        handlePacketPayload(new ByteArrayInputStream(packetPayload), length-20);
	}

	private void handlePacketPayload(ByteArrayInputStream is, int apsPayloadLength) {
		
		srcAddr64 = (((long)is.read()) << 56) + 
			       (((long)is.read()) << 48) + 
			       (((long)is.read()) << 40) + 
			       (((long)is.read()) << 32) +
			       (((long)is.read()) << 24) + 
			       (((long)is.read()) << 16) + 
			       (((long)is.read()) << 8) + 
			       (long)is.read();
	    srcAddr16 = (short)((is.read() << 8) + is.read());
		srcEndpoint = (byte)is.read(); 
		dstEndpoint = (byte)is.read();
		         
		clusterId = (short)((is.read() << 8) + is.read());
		profileId = (short)((is.read() << 8) + is.read());
		 
		receiveOptions = (byte)is.read();
		 
		frameControl = (byte)is.read();
		 
		transactionSeqNumber = (byte)is.read();
		 
		command = (byte)is.read();
		 
		apsPayload = new byte[apsPayloadLength];
		
		for (int i=0; i<apsPayloadLength; i++)
			apsPayload[i] = (byte)is.read();
	}
}
