package it.uniud.easyhome.packets.xbee;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.natives.NativePacket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class XBeePacketFromNode extends XBeePacket {
	
	protected long dstAddr64;
	protected short dstAddr16;
	protected byte frameId = 0x00;
	protected byte broadcastRadius = 0x00;
	protected byte transmitOptions = 0x00;
	
	public XBeePacketFromNode() {
	}
	
	public XBeePacketFromNode(NativePacket ehp) {
		
		dstAddr64 = ehp.getDstCoords().getNuid();
		dstAddr16 = ehp.getDstCoords().getAddress();
		srcEndpoint = ehp.getSrcCoords().getEndpoint();
		dstEndpoint = ehp.getDstCoords().getEndpoint();
		clusterId = ehp.getOperation().getContext();
		profileId = ehp.getOperation().getDomain();
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
	
	public boolean isBroadcast() {
		return (dstAddr64 == 0xFFFFL);
	}
	
	@Override
	public byte[] getBytes() {
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		os.write(XBeeConstants.START_DELIMITER);
		
		// If not using the management profile, the command byte is not present
		int length = 21 + apsPayload.length + (Domain.isManagement(profileId) ? 0 : 1);
		
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
		// Source endpoint
		byte srcEndpointToWrite = srcEndpoint;
		os.write(srcEndpointToWrite);
		sum += srcEndpointToWrite;
		// Destination endpoint
		byte dstEndpointToWrite = dstEndpoint;
		os.write(dstEndpointToWrite);
		sum += dstEndpointToWrite;
		// Cluster ID
		for (int j=8; j>=0; j-=8) {
			byte val = (byte)((clusterId >>> j) & 0xFF);
			os.write(val);
			sum += val;
		}			
		// Profile ID
		for (int j=8; j>=0; j-=8) {
			byte val = (byte)((profileId >>> j) & 0xFF);
			os.write(val);
			sum += val;
		}			
		// Broadcast radius
		os.write(broadcastRadius);
		sum += broadcastRadius;
		// Transmit options
		os.write(transmitOptions);
		sum += transmitOptions;
		// Transaction sequence number
		os.write(transactionSeqNumber);
		sum += transactionSeqNumber;
		if (!Domain.isManagement(profileId)) {
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
        
        frameId = (byte)is.read();

		dstAddr64 = ByteUtils.getLong(is, Endianness.BIG_ENDIAN);
        
	    dstAddr16 = ByteUtils.getShort(is, Endianness.BIG_ENDIAN);
	     
		srcEndpoint = (byte)is.read();
		dstEndpoint = (byte)is.read();
		         
		clusterId = ByteUtils.getShort(is, Endianness.BIG_ENDIAN);
		
		profileId = ByteUtils.getShort(is, Endianness.BIG_ENDIAN);
		
		broadcastRadius = (byte)is.read();
		
		transmitOptions = (byte)is.read();
		 
		transactionSeqNumber = (byte)is.read();
		 
		int apsPayloadLength = 0;
		
		if (Domain.isManagement(profileId)) {
			apsPayloadLength = packetLength - 21;
			command = 0x00;
		} else {
			apsPayloadLength = packetLength - 22;
			command = (byte)is.read();
		}
		
		apsPayload = new byte[apsPayloadLength];
		
		for (int i=0; i<apsPayloadLength; i++)
			apsPayload[i] = (byte)is.read();
		
	}
	
}
