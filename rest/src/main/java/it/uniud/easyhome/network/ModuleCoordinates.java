package it.uniud.easyhome.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;

/** 
 * Immutable class for absolute coordinates of a module across the EasyHome network. 
 */
public class ModuleCoordinates implements Serializable {

    private static final long serialVersionUID = -5009839141486612459L;

    public static final int OCTETS = 13;
    
    // Gateway (and consequently subnetwork) identifier (>=0, =0 for broadcast, =1 for EasyHome TCP/IP subnetwork)
    private byte gid;
    // Unit unique id (global address, like a IEEE MAC address, fixed for a unit) (0x0 for the gateway, 0xFFFF for a broadcast)
    private long uuid;
    // Address within the network (>=0, 0xFFFE if broadcast or unknown)
    private short address;
    // Endpoint of the interested module (>=0, =0 addresses the device endpoint of the EH subnetwork)
    private byte endpoint;
    
    public byte getGatewayId() {
        return gid;
    }
    
    public long getUuid() {
    	return uuid;
    }
    
    public short getAddress() {
        return address;
    }
    
    public byte getEndpoint() {
        return endpoint;
    }
    
    public ModuleCoordinates(byte gid, long uuid, short address, byte endpoint) {
        this.gid = gid;
        this.uuid = uuid;
        this.address = address;
        this.endpoint = endpoint;
    }
    
    public ModuleCoordinates(ByteArrayInputStream bais) {
        
        gid = (byte)bais.read();
    	uuid = (((long)bais.read()) << 56) + 
    		   (((long)bais.read()) << 48) + 
    		   (((long)bais.read()) << 40) + 
    		   (((long)bais.read()) << 32) +
			   (((long)bais.read()) << 24) + 
			   (((long)bais.read()) << 16) + 
			   (((long)bais.read()) << 8) + 
			   (long)bais.read();
        address = (short)((bais.read()<<8)+bais.read());
        endpoint = (byte)((bais.read()<<8)+bais.read());
    }
    
    public void writeBytes(ByteArrayOutputStream baos) {
        
        baos.write(gid & 0xFF);
        baos.write((int)((uuid >>> 56) & 0xFF)); 
        baos.write((int)((uuid >>> 48) & 0xFF));
        baos.write((int)((uuid >>> 40) & 0xFF));
        baos.write((int)((uuid >>> 32) & 0xFF));
        baos.write((int)((uuid >>> 24) & 0xFF)); 
        baos.write((int)((uuid >>> 16) & 0xFF));
        baos.write((int)((uuid >>> 8) & 0xFF));
        baos.write((int)(uuid & 0xFF));
        
        baos.write((address >>> 8) & 0xFF);
        baos.write(address & 0xFF);
        baos.write((endpoint >>> 8) & 0xFF);
        baos.write(endpoint & 0xFF);
    }
    
    public String toString() {
    	StringBuilder strb = new StringBuilder();
    	
    	strb.append("{G: ")
    		.append(gid)
    		.append("; U: ")
    		.append(uuid)
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
        if (otherCoords.getUuid() != this.getUuid())
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
        hash = (int)(hash * 31 + uuid);
        hash = hash * 31 + address;
        hash = hash * 31 + endpoint;
        return hash;
    }
    
}
