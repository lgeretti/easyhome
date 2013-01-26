package it.uniud.easyhome.network;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/** 
 * Immutable class for absolute coordinates of a module across the EasyHome network. 
 */
@Embeddable
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class NodeCoordinates {
    
    // Gateway (and consequently subnetwork) identifier (!=0)
	@Column(nullable = false)
    private byte gatewayId;
    // Node unique id (global address, like a IEEE MAC address, fixed for a node)
	@Column(nullable = false)
    private long nuid;
    // Address within the network (!=0)
	@Column(nullable = false)
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
