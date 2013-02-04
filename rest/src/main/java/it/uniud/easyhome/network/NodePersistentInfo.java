package it.uniud.easyhome.network;

import javax.persistence.*;
import javax.xml.bind.annotation.*;

@Entity
@Table(name = "NodePersistentInfo")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class NodePersistentInfo {
	
	@Id
	private long id;
    @Column(nullable = false)
    private byte gatewayId;
    @Column(nullable = false)
    private long nuid;
    @Column
    private String name;
    @Column
    private String location;

    @SuppressWarnings("unused")
	private NodePersistentInfo() {}
    
    public NodePersistentInfo(long id, byte gatewayId, long nuid, String name, String location) {
    	this.id = id;
    	this.gatewayId = gatewayId;
    	this.nuid = nuid;
    	this.name = name;
    	this.location = location;
    }

	public void setName(String name) {
		this.name = name;
	}
	
	public void setLocation(String location) {
		this.location = location;
	}
    
	public byte getGatewayId() {
		return gatewayId;
	}
	
	public long getNuid() {
		return nuid;
	}
	
    public String getName() {
        return this.name;
    }
    
	public String getLocation() {
		return this.location;
	}

    @Override
    public boolean equals(Object other) {
        
        if (!(other instanceof NodePersistentInfo))
            throw new IllegalArgumentException();
        NodePersistentInfo otherNode = (NodePersistentInfo) other;
        
        if (this.gatewayId != otherNode.gatewayId) return false;
        if (this.nuid != otherNode.nuid) return false;
        
        return true;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;

        long result = 1;
        result = prime * result + gatewayId;
        result = result * prime + (int)(0xFFFFFFFF & nuid);
        result = result * prime + (int)((nuid >>> 32) & 0xFFFFFFFF);
        return (int)result;
    }
}
