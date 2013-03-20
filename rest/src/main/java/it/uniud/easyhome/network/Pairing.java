package it.uniud.easyhome.network;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "Pairing")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Pairing {
	
	@Id
	private long id;
	@Column(nullable = false)
	private NodePersistentInfo source;
	@Column(nullable = false)
	private NodePersistentInfo destination;
	
    @SuppressWarnings("unused")
	private Pairing() { }
    
    public Pairing(long id, NodePersistentInfo source, NodePersistentInfo destination) {
        this.id = id;
        this.source = source;
        this.destination = destination;
    }
    
	public long getId() {
		return id;
	}

    public NodePersistentInfo getSource() {
    	return source;
    }

    public NodePersistentInfo getDestination() {
    	return destination;
    }
    
    public String toString() {
    	StringBuilder strb = new StringBuilder();
    	
    	strb.append("(")
    		.append(source.toString())
    		.append("->")
    		.append(destination.toString())
    		.append(")");
    	
    	return strb.toString();
    }
    
    @Override
    public boolean equals(Object other) {
        
        if (!(other instanceof Pairing))
            return false;
        
        Pairing otherPairing = (Pairing) other;

        if (!otherPairing.getSource().equals(this.getSource()))
            return false;
        if (!otherPairing.getDestination().equals(this.getDestination()))
            return false;
        
        return true;
    }
    
    @Override
    public int hashCode() {
        int hash = 1;
        hash = (int)(hash * 31 + id);
        hash = hash * 31 + source.hashCode();
        hash = hash * 31 + destination.hashCode();
        return hash;
    }
    
}
