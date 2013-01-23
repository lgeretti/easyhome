package it.uniud.easyhome.network;

import java.io.Serializable;

/** 
 * Immutable class for compact coordinates of a node across the EasyHome network. 
 */
public class NodeCompactCoordinates implements Serializable {
    
	private static final long serialVersionUID = 1859073539716463156L;
	
	// Gateway (and consequently subnetwork) identifier (!=0)
    private byte gid;
    // Address within the network (!=0)
    private short address;
    
    public byte getGatewayId() {
        return gid;
    }
    
    public short getAddress() {
        return address;
    }
    
    @SuppressWarnings("unused")
	private NodeCompactCoordinates() { }
    
    public NodeCompactCoordinates(byte gid, short address) {
        this.gid = gid;
        this.address = address;
    }
    
    
    public String toString() {
    	StringBuilder strb = new StringBuilder();
    	
    	strb.append("(")
    		.append(gid)
    		.append(":")
    		.append(Integer.toHexString(0xFFFF & address))
    		.append(")");
    	
    	return strb.toString();
    }
    
    @Override
    public boolean equals(Object other) {
        
        if (!(other instanceof NodeCompactCoordinates))
            return false;
        
        NodeCompactCoordinates otherCoords = (NodeCompactCoordinates) other;
        
        if (otherCoords.getGatewayId() != this.getGatewayId())
            return false;
        if (otherCoords.getAddress() != this.getAddress())
            return false;
        
        return true;
    }
    
    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 31 + gid;
        hash = hash * 31 + address;
        return hash;
    }
    
}
