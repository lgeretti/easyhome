package it.uniud.easyhome.network;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "Link")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Link implements Serializable {

	private static final long serialVersionUID = 7233524750252380817L;
	
	@Id
	private long id;
	@Column(nullable = false)
	private byte gatewayId;
	@Column(nullable = false)
    private short sourceAddress;
    @Column(nullable = false)
    private short destinationAddress;
	@Column(nullable = false)
	private long timestamp;
    
    @SuppressWarnings("unused")
	private Link() { }
    
    public Link(long id, byte gatewayId, short sourceAddress, short destinationAddress) {
        this.id = id;
        this.gatewayId = gatewayId;
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.timestamp = System.currentTimeMillis();
    }
    
	public long getId() {
		return id;
	}
	
	public byte getGatewayId() {
		return gatewayId;
	}
	
    public short getSourceAddress() {
    	return sourceAddress;
    }
    
    public short getDestinationAddress() {
        return destinationAddress;
    }
	
	public Date getDate() {
		return new Date(timestamp);
	}
    
    public String toString() {
    	StringBuilder strb = new StringBuilder();
    	
    	strb.append("(")
    		.append(Long.toHexString(gatewayId))
    		.append(":")
    		.append(Integer.toHexString(0xFFFF & sourceAddress))
    		.append("->")
    		.append(Integer.toHexString(0xFFFF & destinationAddress));
    	
    	return strb.toString();
    }
    
    @Override
    public boolean equals(Object other) {
        
        if (!(other instanceof Link))
            return false;
        
        Link otherCoords = (Link) other;

        if (otherCoords.getGatewayId() != this.getGatewayId())
            return false;
        if (otherCoords.getSourceAddress() != this.getSourceAddress())
            return false;
        if (otherCoords.getDestinationAddress() != this.getDestinationAddress())
            return false;
        
        return true;
    }
    
    @Override
    public int hashCode() {
        int hash = 1;
        hash = (int)(hash * 31 + id);
        hash = hash * 31 + gatewayId;
        hash = hash * 31 + sourceAddress;
        hash = hash * 31 + destinationAddress;
        return hash;
    }
    
}
