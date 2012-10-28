package it.uniud.easyhome.xbee;

import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.packets.Domains;
import it.uniud.easyhome.packets.NativePacket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class XBeeTransmittedPacket extends XBeePacket {
	
	private long dstAddr64;
	private short dstAddr16;
	private byte frameId = 0x00;
	private byte broadcastRadius = 0x00;
	private byte transmitOptions = 0x00;
	
	public XBeeTransmittedPacket() {
	}
	
	public XBeeTransmittedPacket(NativePacket ehp) {
		
		dstAddr64 = ehp.getDstCoords().getNuid();
		dstAddr16 = ehp.getDstCoords().getAddress();
		srcEndpoint = ehp.getSrcCoords().getEndpoint();
		dstEndpoint = ehp.getDstCoords().getEndpoint();
		clusterId = ehp.getOperation().getContext();
		profileId = ehp.getOperation().getDomain();
		frameControl = ehp.getOperation().getFlags();
		transactionSeqNumber = ehp.getOperation().getSequenceNumber();
		command = ehp.getOperation().getFlags();
		apsPayload = ehp.getOperation().getData();
	}
	
	public byte getFrameId() {
		return frameId;
	}
	public void setFrameId(byte frameId) {
		this.frameId = frameId;
	}
	
	public long get64BitDstAddr() {
		return dstAddr64;
	}
	public void set64BitDstAddr(long dstAddr64) {
		this.dstAddr64 = dstAddr64;
	}

	public short get16BitDstAddr() {
		return dstAddr16;
	}	
	public void set16BitDstAddr(short dstAddr16) {
		this.dstAddr16 = dstAddr16;
	}
	
	public byte getBroadcastRadius() {
		return broadcastRadius;
	}
	public void setBroadcastRadius(byte broadcastRadius) {
		this.broadcastRadius = broadcastRadius;
	}
	
	public byte getTransmitOptions() {
		return transmitOptions;
	}
	public void setTransmitOptions(byte transmitOptions) {
		this.transmitOptions = transmitOptions;
	}
	
	@Override
	public byte[] getBytes() {
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		os.write(XBeeConstants.START_DELIMITER);
		
		// If not using the management profile, the command byte is not present
		int length = 22 + apsPayload.length + (Domains.isManagement(profileId) ? 0 : 1);
		
		// High and low lengths
		os.write((length >>> 8) & 0xFF);
		os.write(length & 0xFF);
		
		int sum = 0;
		
		// Frame type
		byte frameType = XBeeConstants.EXPLICIT_ADDRESSING_COMMAND_FRAME_TYPE; 
		os.write(frameType);
		sum += frameType;
		// Frame Id
		os.write(frameId);
		sum += frameId;
		// 64 bit destination address
		for (int j=56; j>=0; j-=8) {
			byte val = (byte)((dstAddr64 >>> j) & 0xFF);
			os.write(val);
			sum += val;
		}
		// 16 bit destination address
		for (int j=8; j>=0; j-=8) {
			byte val = (byte)((dstAddr16 >>> j) & 0xFF);
			os.write(val);
			sum += val;
		}		
		// Source endpoint;
		
		byte srcEndpointToWrite = (srcEndpoint == 0 ? 1 : srcEndpoint);
		os.write(srcEndpointToWrite);
		sum += srcEndpointToWrite;
		// Destination endpoint
		byte dstEndpointToWrite = (dstEndpoint == 0 ? 1 : dstEndpoint);
		os.write(dstEndpointToWrite);
		sum += dstEndpointToWrite;
		// Cluster ID
		for (int j=8; j>=0; j-=8) {
			byte val = (byte)((clusterId >>> j) & 0xFF);
			os.write(val);
			sum += val;
		}			
		// Profile ID
		short profileIdToWrite = (profileId == 0 ? Domains.EASYHOME_MANAGEMENT.getCode() : profileId);
		for (int j=8; j>=0; j-=8) {
			byte val = (byte)((profileIdToWrite >>> j) & 0xFF);
			os.write(val);
			sum += val;
		}			
		// Broadcast radius
		os.write(broadcastRadius);
		sum += broadcastRadius;
		// Transmit options
		os.write(transmitOptions);
		sum += transmitOptions;
		// Frame control
		os.write(frameControl);
		sum += frameControl;
		// Transaction sequence number
		os.write(transactionSeqNumber);
		sum += transactionSeqNumber;
		if (!Domains.isManagement(profileId)) {
			os.write(command);
			sum += command;
		}
		// Aps payload
		for (byte b: apsPayload) {
			os.write(b);
			sum += b;
		}
		// Checksum
		os.write(0xFF - (sum & 0xFF));
		
		return os.toByteArray();
	}

	@Override
	protected void handlePacketPayload(ByteArrayInputStream is, int packetLength) {

        byte frameType = (byte)is.read();
        if (frameType != XBeeConstants.EXPLICIT_ADDRESSING_COMMAND_FRAME_TYPE) 
        	throw new InvalidPacketTypeException();
		
		dstAddr64 = (((long)is.read()) << 56) + 
			       (((long)is.read()) << 48) + 
			       (((long)is.read()) << 40) + 
			       (((long)is.read()) << 32) +
			       (((long)is.read()) << 24) + 
			       (((long)is.read()) << 16) + 
			       (((long)is.read()) << 8) + 
			       (long)is.read();
	    dstAddr16 = (short)((is.read() << 8) + is.read());
	    
	    byte readSrcEndpoint = (byte)is.read(); 
		srcEndpoint = (readSrcEndpoint == 1 ? 0 : readSrcEndpoint);
		byte readDstEndpoint = (byte)is.read();
		dstEndpoint = (readDstEndpoint == 1 ? 0 : readSrcEndpoint);
		         
		clusterId = (short)((is.read() << 8) + is.read());
		
		short readProfile = (short)((is.read() << 8) + is.read());
		profileId = (Domains.isManagement(readProfile) ? 0 : readProfile);
		 
		broadcastRadius = (byte)is.read();
		
		transmitOptions = (byte)is.read();
		 
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
