package it.uniud.easyhome.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;

/** 
 * Immutable class for absolute coordinates of a module across the EasyHome network. 
 */
public class ModuleCoordinates implements Serializable {

    private static final long serialVersionUID = -5009839141486612459L;

    public static final int OCTETS = 5;
    
    // Gateway (and consequently subnetwork) identifier (>0, =0 for broadcast, =1 for EasyHome TCP/IP subnetwork)
    private int gid;
    // Unit unique id (global address, like a IEEE MAC address, fixed for a unit)
    private long uuid;
    // Address within the network (>0, =0 if broadcast)
    private int address;
    // Endpoint of the interested module (>=0, =0 addresses the device endpoint of the EH subnetwork)
    private int endpoint;
    
    public int getGatewayId() {
        return gid;
    }
    
    public int getAddress() {
        return address;
    }
    
    public int getEndpoint() {
        return endpoint;
    }
    
    public ModuleCoordinates(int gid, int address, int endpoint) {
        this.gid = gid;
        this.address = address;
        this.endpoint = endpoint;
    }
    
    public ModuleCoordinates(ByteArrayInputStream bais) {
        
        gid = bais.read();
        
        int highAddr = bais.read();
        address = highAddr*256+bais.read();
        
        int highPort = bais.read();
        endpoint = highPort*256+bais.read();
    }
    
    public void writeBytes(ByteArrayOutputStream baos) {
        
        baos.write(gid & 0xFF);
        baos.write((address >>> 8) & 0xFF);
        baos.write(address & 0xFF);
        baos.write((endpoint >>> 8) & 0xFF);
        baos.write(endpoint & 0xFF);
    }
    
    public String toString() {
    	StringBuilder strb = new StringBuilder();
    	
    	strb.append("{G: ")
    		.append(gid)
    		.append("; A:")
    		.append(address)
    		.append("; E: ")
    		.append(endpoint)
    		.append("}");
    	
    	return strb.toString();
    }
    
    @Override
    public boolean equals(Object other) {
        
        if (!(other instanceof ModuleCoordinates))
            return false;
        
        ModuleCoordinates otherCoords = (ModuleCoordinates) other;
        
        if (otherCoords.getGatewayId() != this.getGatewayId())
            return false;
        if (otherCoords.getAddress() != this.getAddress())
            return false;
        if (otherCoords.getEndpoint() != this.getEndpoint())
            return false;
        
        return true;
    }
    
    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 31 + gid;
        hash = hash * 31 + address;
        hash = hash * 31 + endpoint;
        return hash;
    }
    
}
