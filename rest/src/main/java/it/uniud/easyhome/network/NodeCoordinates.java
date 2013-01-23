package it.uniud.easyhome.network;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/** 
 * Immutable class for absolute coordinates of a module across the EasyHome network. 
 */
@XmlRootElement
public class NodeCoordinates implements Serializable {

	private static final long serialVersionUID = 458835586884747577L;
    
    // Gateway (and consequently subnetwork) identifier (!=0)
	@XmlElement(name="gatewayId")
    private byte gatewayId;
    // Node unique id (global address, like a IEEE MAC address, fixed for a node)
	@XmlElement(name="nuid")
    private long nuid;
    // Address within the network (!=0)
    @XmlElement(name="address")
    private short address;
    
    public byte getGatewayId() {
        return gatewayId;
    }
    
    public long getNuid() {
    	return nuid;
    }
    
    public short getAddress() {
        return address;
    }
    
    @SuppressWarnings("unused")
	private NodeCoordinates() { }
    
    public NodeCoordinates(byte gatewayId, long nuid, short address) {
        this.gatewayId = gatewayId;
        this.nuid = nuid;
        this.address = address;
    }
    
    public String toString() {
    	StringBuilder strb = new StringBuilder();
    	
    	strb.append("(")
    		.append(gatewayId)
    		.append(":")
    		.append(Long.toHexString(nuid))
    		.append(":")
    		.append(Integer.toHexString(0xFFFF & address))
    		.append(")");
    	
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
        hash = hash * 31 + gatewayId;
        hash = (int)(hash * 31 + nuid);
        hash = hash * 31 + address;
        return hash;
    }
    
}
