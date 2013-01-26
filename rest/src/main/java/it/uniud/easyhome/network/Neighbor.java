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
public class Neighbor implements Serializable {
    
	private static final long serialVersionUID = -7424774896258284764L;

    // Node unique id (global address, like a IEEE MAC address, fixed for a node)
	@Column(nullable = false)
    private long nuid;
    // Address within the network (!=0)
	@Column(nullable = false)
    private short address;
    
    public long getNuid() {
    	return nuid;
    }
    
    public short getAddress() {
        return address;
    }
    
    @SuppressWarnings("unused")
	private Neighbor() { }
    
    public Neighbor(long nuid, short address) {
        this.nuid = nuid;
        this.address = address;
    }
    
    public String toString() {
    	StringBuilder strb = new StringBuilder();
    	
    	strb.append("(")
    		.append(Long.toHexString(nuid))
    		.append(".")
    		.append(Integer.toHexString(0xFFFF & address))
    		.append(")");
    	
    	return strb.toString();
    }
    
    @Override
    public boolean equals(Object other) {
        
        if (!(other instanceof Neighbor))
            return false;
        
        Neighbor otherCoords = (Neighbor) other;

        if (otherCoords.getNuid() != this.getNuid())
            return false;
        if (otherCoords.getAddress() != this.getAddress())
            return false;
        
        return true;
    }
    
    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 31 + (int)(0xFFFFFFFF & nuid);
        hash = hash * 31 + (int)((nuid >>> 32) & 0xFFFFFFFF);
        hash = hash * 31 + address;
        return hash;
    }
    
}
