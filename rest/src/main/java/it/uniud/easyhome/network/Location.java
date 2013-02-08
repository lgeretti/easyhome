package it.uniud.easyhome.network;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@Embeddable
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Location implements Serializable {

	private static final long serialVersionUID = -3972393244808127322L;

	@Column(nullable = false)
	private String name;
	@Column(nullable = false)
	private LocationType type;
	
    @SuppressWarnings("unused")
	private Location() { }
    
    public Location(String name, LocationType type) {
    	this.name = name;
    	this.type = type;
    }
    
    public String getName() {
    	return name;
    }
    
    public LocationType getType() {
    	return type;
    }
    
    @Override
    public boolean equals(Object other) {
        
        if (!(other instanceof Location))
            return false;
        
        Location otherLocation = (Location) other;
        
        if (!otherLocation.getName().equals(this.name))
            return false;
        if (otherLocation.getType() != this.type)
            return false;
        
        return true;
    }
    
    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 31 + name.hashCode();
        hash = hash * 31 + type.hashCode();
        return hash;
    }
}
