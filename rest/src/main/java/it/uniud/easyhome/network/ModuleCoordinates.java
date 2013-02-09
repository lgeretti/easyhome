package it.uniud.easyhome.network;

import it.uniud.easyhome.common.ByteUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

/** 
 * Immutable class for absolute coordinates of a module across the EasyHome network. 
 */
public class ModuleCoordinates implements Serializable {

	private static final long serialVersionUID = -4456393211251496309L;

	public static final int OCTETS = 12;
    
    // The coordinates
    private GlobalCoordinates nodeCoordinates;
    // Endpoint of the interested module (0 addresses the management endpoint)
    private byte endpoint;
    
    public byte getGatewayId() {
        return nodeCoordinates.getGatewayId();
    }
    
    public long getNuid() {
    	return nodeCoordinates.getNuid();
    }
    
    public short getAddress() {
        return nodeCoordinates.getAddress();
    }
    
    public GlobalCoordinates getNodeCoordinates() {
    	return nodeCoordinates;
    }
    
    public byte getEndpoint() {
        return endpoint;
    }
    
    public ModuleCoordinates(byte gatewayId, long nuid, short address, byte endpoint) {
        this.nodeCoordinates = new GlobalCoordinates(gatewayId,nuid,address);
        this.endpoint = endpoint;
    }
    
    public ModuleCoordinates(GlobalCoordinates nodeCoords, byte endpoint) {
        this.nodeCoordinates = nodeCoords;
        this.endpoint = endpoint;
    }
    
    public ModuleCoordinates(InputStream is) throws IOException {

        byte gatewayId = (byte)is.read();
    	long nuid = (((long)is.read()) << 56) + 
    		   (((long)is.read()) << 48) + 
    		   (((long)is.read()) << 40) + 
    		   (((long)is.read()) << 32) +
			   (((long)is.read()) << 24) + 
			   (((long)is.read()) << 16) + 
			   (((long)is.read()) << 8) + 
			   (long)is.read();
        short address = (short)((is.read()<<8)+is.read());
        this.nodeCoordinates = new GlobalCoordinates(gatewayId,nuid,address);
        endpoint = (byte)is.read();
    }
    
    public void write(OutputStream os) throws IOException {
        
    	byte gatewayId = nodeCoordinates.getGatewayId();
    	long nuid = nodeCoordinates.getNuid();
    	short address = nodeCoordinates.getAddress();
    	
        os.write(gatewayId & 0xFF);
        os.write((int)((nuid >>> 56) & 0xFF)); 
        os.write((int)((nuid >>> 48) & 0xFF));
        os.write((int)((nuid >>> 40) & 0xFF));
        os.write((int)((nuid >>> 32) & 0xFF));
        os.write((int)((nuid >>> 24) & 0xFF)); 
        os.write((int)((nuid >>> 16) & 0xFF));
        os.write((int)((nuid >>> 8) & 0xFF));
        os.write((int)(nuid & 0xFF));
        
        os.write((address >>> 8) & 0xFF);
        os.write(address & 0xFF);
        os.write(endpoint & 0xFF);
    }
    
    public String toString() {
    	StringBuilder strb = new StringBuilder();
    	
    	strb.append("(")
    		.append(nodeCoordinates.getGatewayId())
    		.append(":")
    		.append(Long.toHexString(nodeCoordinates.getNuid()))
    		.append(":")
    		.append(Integer.toHexString(0xFFFF & nodeCoordinates.getAddress()))
    		.append(":")
    		.append(Integer.toHexString(0xFF & endpoint))
    		.append(")");
    	
    	return strb.toString();
    }
    
    @Override
    public boolean equals(Object other) {
        
        if (!(other instanceof ModuleCoordinates))
            return false;
        
        ModuleCoordinates otherCoords = (ModuleCoordinates) other;
        
        if (!otherCoords.getNodeCoordinates().equals(otherCoords.getNodeCoordinates()))
            return false;
        if (otherCoords.getEndpoint() != this.getEndpoint())
            return false;
        
        return true;
    }
    
    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 31 + nodeCoordinates.hashCode();
        hash = hash * 31 + endpoint;
        return hash;
    }
    
}
