package it.uniud.easyhome.network;

import javax.persistence.*;
import javax.xml.bind.annotation.*;

@Entity
@Table(name = "PersistentInfo")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class PersistentInfo {
	
	@Id
	private long id;
    @Column(nullable = false)
    private byte gatewayId;
    @Column(nullable = false)
    private long nuid;
    @Column
    private String name;
    @OneToOne
    private Location location;
    @Column
    private FunctionalityContainerType funcContainerType;
    @Column
    private String imgPath;
    @Column
    private String help;

    @SuppressWarnings("unused")
	private PersistentInfo() {}
    
    public PersistentInfo(long id, byte gatewayId, long nuid, String name, Location location, FunctionalityContainerType funcContainerType, String imgPath, String help) {
    	this.id = id;
    	this.gatewayId = gatewayId;
    	this.nuid = nuid;
    	this.name = name;
    	this.location = location;
    	this.funcContainerType = funcContainerType;
    	this.imgPath = imgPath;
    	this.help = help;
    }

	public void setName(String name) {
		this.name = name;
	}
	
	public void setLocation(Location location) {
		this.location = location;
	}
    
	public long getId() {
		return id;
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
    
	public Location getLocation() {
		return this.location;
	}
	
	public FunctionalityContainerType getDeviceType() {
		return this.funcContainerType;
	}
	
	public String getImgPath() {
		return this.imgPath;
	}
	
	public String getHelp() {
		return this.help;
	}

    @Override
    public boolean equals(Object other) {
        
        if (!(other instanceof PersistentInfo))
            throw new IllegalArgumentException();
        PersistentInfo otherNode = (PersistentInfo) other;
        
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
