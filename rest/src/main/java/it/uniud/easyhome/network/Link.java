package it.uniud.easyhome.network;

import java.util.Date;

import javax.persistence.AttributeOverrides;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Embedded;
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
public class Link {
	
	@Id
	private long id;
	@Column(nullable = false)
	private byte gatewayId;
	@Embedded
	@AttributeOverrides({@AttributeOverride(name="nuid", column=@Column(name="SRC_NUID")),@AttributeOverride(name="address", column=@Column(name="SRC_ADDRESS"))})
	private LocalCoordinates source;
	@Embedded
	@AttributeOverrides({@AttributeOverride(name="nuid", column=@Column(name="DST_NUID")),@AttributeOverride(name="address", column=@Column(name="DST_ADDRESS"))})
	private LocalCoordinates destination;
	
	@Column(nullable = false)
	private long timestamp;
    
    @SuppressWarnings("unused")
	private Link() { }
    
    public Link(long id, byte gatewayId, LocalCoordinates source, LocalCoordinates destination) {
        this.id = id;
        this.gatewayId = gatewayId;
        this.source = source;
        this.destination = destination;
        this.timestamp = System.currentTimeMillis();
    }
    
	public long getId() {
		return id;
	}
	
	public byte getGatewayId() {
		return gatewayId;
	}

    public LocalCoordinates getSource() {
    	return source;
    }

    public LocalCoordinates getDestination() {
    	return destination;
    }
    	
	public Date getDate() {
		return new Date(timestamp);
	}
	
	public void update() {
		this.timestamp = System.currentTimeMillis();
	}
    
    public String toString() {
    	StringBuilder strb = new StringBuilder();
    	
    	strb.append("(")
    		.append(Long.toHexString(gatewayId))
    		.append(": ")
    		.append(source.toString())
    		.append("->")
    		.append(destination.toString())
    	    .append(")");
    	
    	return strb.toString();
    }
    
    @Override
    public boolean equals(Object other) {
        
        if (!(other instanceof Link))
            return false;
        
        Link otherLink = (Link) other;

        if (otherLink.getGatewayId() != this.getGatewayId())
            return false;
        if (!otherLink.getSource().equals(this.getSource()))
            return false;
        if (!otherLink.getDestination().equals(this.getDestination()))
            return false;
        
        return true;
    }
    
    @Override
    public int hashCode() {
        int hash = 1;
        hash = (int)(hash * 31 + id);
        hash = hash * 31 + gatewayId;
        hash = hash * 31 + source.hashCode();
        hash = hash * 31 + destination.hashCode();
        return hash;
    }
    
}
