package it.uniud.easyhome.packets.xbee;

import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.packets.Domains;
import it.uniud.easyhome.packets.ModuleCoordinates;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class XBeeInboundPacket extends XBeePacket {

	private long srcAddr64;
	private short srcAddr16;
	private byte receiveOptions = 0x00;
	
	public XBeeInboundPacket() {
	}
	
	public XBeeInboundPacket(XBeeOutboundPacket pkt, long srcAddr64, short srcAddr16) {
		
		this.srcAddr64 = srcAddr64;
		this.srcAddr16 = srcAddr16;
		
		receiveOptions = (pkt.isBroadcast() ? (byte)0x02 : 0x00);
		
		srcEndpoint = pkt.getSrcEndpoint();
		dstEndpoint = pkt.getDstEndpoint();
		clusterId = pkt.getClusterId();
		profileId = pkt.getProfileId();
		frameControl = pkt.getFrameControl();
		transactionSeqNumber = pkt.getTransactionSeqNumber();
		command = pkt.getCommand();
		apsPayload = pkt.getApsPayload();
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
	
	public byte getReceiveOptions() {
		return receiveOptions;
	}
	public void setReceiveOptions(byte receiveOptions) {
		this.receiveOptions = receiveOptions;
	}
	
	@Override
	public byte[] getBytes() {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		baos.write(XBeeConstants.START_DELIMITER);
		
		// If not using the management profile, the command byte is not present
		int length = 20 + getApsPayload().length + (Domains.isManagement(getProfileId()) ? 0 : 1);
		
		// High and low lengths
		baos.write((length >>> 8) & 0xFF);
		baos.write(length & 0xFF);
		
		int sum = 0;
		
		// Frame type
		byte frameType = XBeeConstants.EXPLICIT_RX_INDICATOR_FRAME_TYPE; 
		baos.write(frameType);
		sum += frameType;
		// 64 bit source address
		for (int j=56; j>=0; j-=8) {
			byte val = (byte)((srcAddr64 >>> j) & 0xFF);
			baos.write(val);
			sum += val;
		}
		// 16 bit source address
		for (int j=8; j>=0; j-=8) {
			byte val = (byte)((srcAddr16 >>> j) & 0xFF);
			baos.write(val);
			sum += val;
		}		
		// Source endpoint
		byte srcEndpointToWrite = (srcEndpoint == 0 ? 1 : srcEndpoint);
		baos.write(srcEndpointToWrite);
		sum += srcEndpointToWrite;
		// Destination endpoint
		byte dstEndpointToWrite = (dstEndpoint == 0 ? 1 : dstEndpoint);
		baos.write(dstEndpointToWrite);
		sum += dstEndpointToWrite;
		// Cluster ID
		for (int j=8; j>=0; j-=8) {
			byte val = (byte)((clusterId >>> j) & 0xFF);
			baos.write(val);
			sum += val;
		}			
		// Profile ID
		short profileIdToWrite = (profileId == 0 ? Domains.EASYHOME_MANAGEMENT.getCode() : profileId);
		for (int j=8; j>=0; j-=8) {
			byte val = (byte)((profileIdToWrite >>> j) & 0xFF);
			baos.write(val);
			sum += val;
		}			
		// Receive options
		baos.write(receiveOptions);
		sum += receiveOptions;
		// Frame control
		baos.write(frameControl);
		sum += frameControl;
		// Transaction sequence number
		baos.write(transactionSeqNumber);
		sum += transactionSeqNumber;
		if (!Domains.isManagement(profileId)) {
			baos.write(command);
			sum += command;
		}
		// Aps payload
		for (byte b: apsPayload) {
			baos.write(b);
			sum += b;
		}
		// Checksum
		baos.write(0xFF - (sum & 0xFF));
		
		return baos.toByteArray();
	}

	@Override
	final protected void handlePacketPayload(ByteArrayInputStream is, int packetLength) {
		
        byte frameType = (byte)is.read();
        if (frameType != XBeeConstants.EXPLICIT_RX_INDICATOR_FRAME_TYPE) 
        	throw new InvalidPacketTypeException();
		
		srcAddr64 = (((long)is.read()) << 56) + 
			       (((long)is.read()) << 48) + 
			       (((long)is.read()) << 40) + 
			       (((long)is.read()) << 32) +
			       (((long)is.read()) << 24) + 
			       (((long)is.read()) << 16) + 
			       (((long)is.read()) << 8) + 
			       (long)is.read();
	    srcAddr16 = (short)((is.read() << 8) + is.read());
	    
	    byte readSrcEndpoint = (byte)is.read(); 
		srcEndpoint = (readSrcEndpoint == 1 ? 0 : readSrcEndpoint);
		byte readDstEndpoint = (byte)is.read();
		dstEndpoint = (readDstEndpoint == 1 ? 0 : readDstEndpoint);
		         
		clusterId = (short)((is.read() << 8) + is.read());
		
		short readProfile = (short)((is.read() << 8) + is.read());
		profileId = (Domains.isManagement(readProfile) ? 0 : readProfile);
		 
		receiveOptions = (byte)is.read();
		 
		frameControl = (byte)is.read();
		 
		transactionSeqNumber = (byte)is.read();
		 
		int apsPayloadLength = 0;
		
		if (Domains.isManagement(profileId)) {
			apsPayloadLength = packetLength - 20;
			command = 0x00;
		} else {
			apsPayloadLength = packetLength - 21;
			command = (byte)is.read();
		}
		
		apsPayload = new byte[apsPayloadLength];
		
		for (int i=0; i<apsPayloadLength; i++)
			apsPayload[i] = (byte)is.read();
	}

}
