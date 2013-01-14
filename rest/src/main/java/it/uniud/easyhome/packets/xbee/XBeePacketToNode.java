package it.uniud.easyhome.packets.xbee;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.packets.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class XBeePacketToNode extends XBeePacket {

	private long srcAddr64;
	private short srcAddr16;
	private byte receiveOptions = 0x00;
	
	public XBeePacketToNode() {
	}
	
	public XBeePacketToNode(XBeePacketFromNode pkt, long srcAddr64, short srcAddr16) {
		
		this.srcAddr64 = srcAddr64;
		this.srcAddr16 = srcAddr16;
		
		receiveOptions = (pkt.isBroadcast() ? (byte)0x02 : 0x00);
		
		srcEndpoint = pkt.getSrcEndpoint();
		dstEndpoint = pkt.getDstEndpoint();
		clusterId = pkt.getClusterId();
		profileId = pkt.getProfileId();
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
		int length = 19 + getApsPayload().length + (Domain.isManagement(getProfileId()) ? 0 : 1);
		
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
		baos.write(srcEndpoint);
		sum += srcEndpoint;
		// Destination endpoint
		baos.write(dstEndpoint);
		sum += dstEndpoint;
		// Cluster ID
		byte[] clusterBytes = ByteUtils.getBytes(clusterId, Endianness.BIG_ENDIAN);
		for (int j=0; j<2; j++) {
			baos.write(clusterBytes[j]);
			sum += clusterBytes[j];
		}			
		// Profile ID
		byte[] profileBytes = ByteUtils.getBytes(profileId, Endianness.BIG_ENDIAN);
		for (int j=0; j<2; j++) {
			baos.write(profileBytes[j]);
			sum += profileBytes[j];
		}			
		// Receive options
		baos.write(receiveOptions);
		sum += receiveOptions;
		// Transaction sequence number
		baos.write(transactionSeqNumber);
		sum += transactionSeqNumber;
		if (!Domain.isManagement(profileId)) {
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
		
		srcAddr64 = ByteUtils.getLong(is, Endianness.BIG_ENDIAN);
	    srcAddr16 = ByteUtils.getShort(is, Endianness.BIG_ENDIAN);
	    
		srcEndpoint = (byte)is.read();
		dstEndpoint = (byte)is.read();
		         
		clusterId = ByteUtils.getShort(is, Endianness.BIG_ENDIAN);
		
		profileId = ByteUtils.getShort(is, Endianness.BIG_ENDIAN);
		
		receiveOptions = (byte)is.read();
		 
		transactionSeqNumber = (byte)is.read();
		 
		int apsPayloadLength = 0;
		
		if (Domain.isManagement(profileId)) {
			apsPayloadLength = packetLength - 19;
			command = 0x00;
		} else {
			apsPayloadLength = packetLength - 20;
			command = (byte)is.read();
		}
		
		apsPayload = new byte[apsPayloadLength];
		
		for (int i=0; i<apsPayloadLength; i++)
			apsPayload[i] = (byte)is.read();
	}

}
