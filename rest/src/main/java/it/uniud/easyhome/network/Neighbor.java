package it.uniud.easyhome.network;

import java.io.Serializable;

import javax.persistence.Embeddable;

@Embeddable
public class Neighbor implements Serializable {
    
	private static final long serialVersionUID = -7424774896258284764L;

    // Node unique id (global address, like a IEEE MAC address, fixed for a node)
    private long nuid;
    // Address within the network (!=0)
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
    
    public void setNuid(long nuid) {
    	this.nuid = nuid;
    }
    
    public void setAddress(short address) {
    	this.address = address;
    }
    
    public String toString() {
    	StringBuilder strb = new StringBuilder();
    	
    	strb.append("(")
    		.append(Long.toHexString(nuid))
    		.append(":")
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
        hash = (int)(hash * 31 + nuid);
        hash = hash * 31 + address;
        return hash;
    }
    
}
