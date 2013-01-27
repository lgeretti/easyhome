package it.uniud.easyhome.network;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/** 
 * Immutable class for global coordinates of a node across the EasyHome network. 
 */
@Embeddable
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class GlobalCoordinates implements Serializable {

	private static final long serialVersionUID = 5341871780590147681L;
	
	// Gateway (and consequently subnetwork) identifier (0 for broadcast, 1 for the native TCP/IP subnetwork)
	@Column(nullable = false)
    private byte gatewayId;
    // Node unique id (global address, like a IEEE MAC address, fixed for a node) (0x0 for a gateway node if gid!=1, or the domotic controller if gid==1, 
    // 0x000000000000FFFF for a broadcast)
	@Column(nullable = false)
    private long nuid;
    // Address within the network (0x0000 for the gateway, 0xFFFE if broadcast or unknown)
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
	private GlobalCoordinates() { }
    
    public GlobalCoordinates(byte gatewayId, long nuid, short address) {
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
        
        if (!(other instanceof GlobalCoordinates))
            return false;
        
        GlobalCoordinates otherCoords = (GlobalCoordinates) other;
        
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
