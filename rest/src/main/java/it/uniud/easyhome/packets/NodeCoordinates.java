package it.uniud.easyhome.packets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

/** 
 * Immutable class for absolute coordinates of a module across the EasyHome network. 
 */
public class NodeCoordinates implements Serializable {

	private static final long serialVersionUID = 458835586884747577L;
    
    // Gateway (and consequently subnetwork) identifier (>0)
    private byte gid;
    // Node unique id (global address, like a IEEE MAC address, fixed for a node)
    private long nuid;
    // Address within the network (>0)
    private short address;
    
    public byte getGatewayId() {
        return gid;
    }
    
    public long getNuid() {
    	return nuid;
    }
    
    public short getAddress() {
        return address;
    }
    
    public NodeCoordinates(byte gid, long nuid, short address) {
        this.gid = gid;
        this.nuid = nuid;
        this.address = address;
    }
    
    
    public String toString() {
    	StringBuilder strb = new StringBuilder();
    	
    	strb.append("{G: ")
    		.append(gid)
    		.append("; N: ")
    		.append(nuid)
    		.append("; A:")
    		.append(address)
    		.append("}");
    	
    	return strb.toString();
    }
    
    @Override
    public boolean equals(Object other) {
        
        if (!(other instanceof NodeCoordinates))
            return false;
        
        NodeCoordinates otherCoords = (NodeCoordinates) other;
        
        if (otherCoords.getGatewayId() != this.getGatewayId())
            return false;
        if (otherCoords.getNuid() != this.getNuid())
            return false;
        if (otherCoords.getAddress() != this.getAddress())
            return false;
        
        return true;
    }
    
    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 31 + gid;
        hash = (int)(hash * 31 + nuid);
        hash = hash * 31 + address;
        return hash;
    }
    
}
