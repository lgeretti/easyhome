package it.uniud.easyhome.packets.xbee;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.exceptions.ChecksumException;
import it.uniud.easyhome.exceptions.IncompletePacketException;
import it.uniud.easyhome.exceptions.InvalidDelimiterException;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.Packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class XBeePacket implements Packet {

	protected byte srcEndpoint;
	protected byte dstEndpoint;
	protected short clusterId;
	protected short profileId;
	protected byte transactionSeqNumber = 0x00;
	protected byte command = 0x00;
	protected byte[] apsPayload = new byte[0];

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
	public int read(InputStream is) throws IOException, InvalidDelimiterException, 
											InvalidPacketTypeException, ChecksumException {

    	int octet = is.read();
    	
    	if (octet == -1)
    		throw new IncompletePacketException();
    	
        if (octet != XBeeConstants.START_DELIMITER)
        	throw new InvalidDelimiterException();
        	
        int highLength = is.read();
        int length = highLength*256 + is.read();
        
        if (length < 0)
        	throw new IncompletePacketException();
        
        waitForAvailability(is, length);
        
        byte[] packetPayload = new byte[length];
        
        int sum = 0;
        for (int i=0; i<length; i++) {
            int readValue = is.read();
            packetPayload[i] = (byte)readValue;
            sum += readValue;
        }
        sum += is.read();
             
        if (0xFF != (sum & 0xFF))
        	throw new ChecksumException();
        	
        handlePacketPayload(new ByteArrayInputStream(packetPayload), length);
        
        return 4+length;
	}
	
	@Override
	public void write(OutputStream os) throws IOException {
		os.write(getBytes());
		os.flush();
	}
	
	private void waitForAvailability(InputStream is, int length) throws IOException {
		
		int i = 0;
        for (; i < 100; i++) {
        	
        	if (is.available() > length)
        		break;
        	try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				throw new IncompletePacketException();
			}
        }
        if (i == 100)
        	throw new IncompletePacketException();
	}
	
    public String printBytes() {
        
        StringBuilder strb = new StringBuilder();
        for (byte b: getBytes()) {
            if ((0xFF & b) < 0x10)
                strb.append("0");
            strb.append(Integer.toHexString(0xFF & b).toUpperCase()).append(" ");
        }
        
        return strb.toString();
    }

	protected abstract void handlePacketPayload(ByteArrayInputStream is, int packetLength);
}
