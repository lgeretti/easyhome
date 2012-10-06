package it.uniud.easyhome.network.xbee;

import it.uniud.easyhome.network.EHPacket;
import it.uniud.easyhome.network.TransmittedPacket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class XBeeTransmittedPacket implements TransmittedPacket {
	
    private static final byte START_DELIMITER = 0x7E;
    private static final byte EXPLICIT_ADDRESSING_COMMAND_FRAME_TYPE = 0x11;
	
	private byte frameId = 0x00;
	private long dstAddr64;
	private short dstAddr16;
	private byte srcEndpoint;
	private byte dstEndpoint;
	private short clusterId;
	private short profileId;
	private byte broadcastRadius = 0x00;
	private byte transmitOptions = 0x00;
	private byte frameControl = 0x00;
	private byte transactionSeqNumber = 0x00;
	private byte command = 0x00;
	private byte[] apsPayload = new byte[0];
	
	public XBeeTransmittedPacket() {
	}
	
	public XBeeTransmittedPacket(EHPacket ehp) {
		
		dstAddr64 = ehp.getDstCoords().getUuid();
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
	public void write(OutputStream os) throws IOException {
    	
		os.write(START_DELIMITER);
		
		int length = 23 + apsPayload.length;
		
		// High and low lengths
		os.write((length >>> 8) & 0xFF);
		os.write(length & 0xFF);
		
		int sum = 0;
		
		// Frame type
		byte frameType = EXPLICIT_ADDRESSING_COMMAND_FRAME_TYPE; 
		os.write(frameType);
		sum += frameType;
		// Frame Id
		os.write(frameId);
		sum += frameId;
		// 64 bit destination address
		byte[] ieeeDestAddr = new byte[8];
		ieeeDestAddr[0] = (byte)((dstAddr64 >>> 56) & 0xFF);
		ieeeDestAddr[1] = (byte)((dstAddr64 >>> 48) & 0xFF);
		ieeeDestAddr[2] = (byte)((dstAddr64 >>> 40) & 0xFF);
		ieeeDestAddr[3] = (byte)((dstAddr64 >>> 32) & 0xFF);
		ieeeDestAddr[4] = (byte)((dstAddr64 >>> 24) & 0xFF);
		ieeeDestAddr[5] = (byte)((dstAddr64 >>> 16) & 0xFF);
		ieeeDestAddr[6] = (byte)((dstAddr64 >>> 8) & 0xFF);
		ieeeDestAddr[7] = (byte)(dstAddr64 & 0xFF); 
		for (byte b: ieeeDestAddr) {
			os.write(b);
			sum += b;
		}
		// 16 bit destination address
		byte highNwkDestAddr = (byte)((dstAddr16 >>> 8) & 0xFF);
		byte lowNwkDestAddr = (byte)(dstAddr16 & 0xFF);
		os.write(highNwkDestAddr);
		sum += highNwkDestAddr;
		os.write(lowNwkDestAddr);
		sum += lowNwkDestAddr;
		// Source endpoint;
		os.write(srcEndpoint);
		sum += srcEndpoint;
		// Destination endpoint
		os.write(dstEndpoint);
		sum += dstEndpoint;
		// Cluster ID
		byte highClusterId = (byte)((clusterId >>> 8) & 0xFF);
		byte lowClusterId = (byte)(clusterId & 0xFF);
		os.write(highClusterId);
		sum += highClusterId;
		os.write(lowClusterId);
		sum += lowClusterId;
		// Profile ID
		byte highProfileId = (byte)((profileId >>> 8) & 0xFF);
		byte lowProfileId = (byte)(profileId & 0xFF);
		os.write(highProfileId);
		sum += highProfileId;
		os.write(lowProfileId);
		sum += lowProfileId;
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
		os.write(command);
		sum += command;
		// Aps payload
		for (byte b: apsPayload) {
			os.write(b);
			sum += b;
		}
		// Checksum
		os.write(0xFF - (sum & 0xFF));
		os.flush();
	}
}
