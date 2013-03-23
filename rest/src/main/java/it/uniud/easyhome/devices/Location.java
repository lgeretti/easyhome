package it.uniud.easyhome.devices;


import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "Location")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Location implements Serializable {

	private static final long serialVersionUID = -3972393244808127322L;

	@Id
	private int id;
	@Column(nullable = false)
	private String name;
	@Column(nullable = false)
	private LocationType type;
	@Column(nullable = false)
	private boolean occupied;
	@Column(nullable = false)
	private String imgPath;
	
    @SuppressWarnings("unused")
	private Location() { }
    
    public Location(int id, String name, LocationType type, String imgPath) {
    	this.id = id;
    	this.name = name;
    	this.type = type;
    	this.occupied = false;
    	this.imgPath = imgPath;
    }
    
    public int getId() {
    	return id;
    }
    
    public String getName() {
    	return name;
    }
    
    public LocationType getType() {
    	return type;
    }
    
    public boolean isOccupied() {
    	return occupied;
    }
    
    public String getImgPath() {
    	return imgPath;
    }
    
    public void setOccupied(boolean occupied) {
    	this.occupied = occupied;
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
    
    public String toString() {
    	return name;
    }
}
