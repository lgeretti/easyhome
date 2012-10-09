package it.uniud.easyhome.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

/** 
 * Immutable class for absolute coordinates of a module across the EasyHome network. 
 */
public class ModuleCoordinates implements Serializable {

    private static final long serialVersionUID = -5009839141486612459L;

    public static final int OCTETS = 13;
    
    // Gateway (and consequently subnetwork) identifier (>=0, =0 for broadcast, =1 for the native TCP/IP subnetwork)
    private byte gid;
    // Unit unique id (global address, like a IEEE MAC address, fixed for a unit) (0x0 for the gateway, 
    // 0x0000FFFF for a broadcast, 0xFFFFFFFF for unknown)
    private long uuid;
    // Address within the network (>=0, 0xFFFE if broadcast or unknown)
    private short address;
    // Endpoint of the interested module (>=0, =0 addresses the configuration endpoint)
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
    
    public ModuleCoordinates(InputStream is) throws IOException {

        gid = (byte)is.read();
    	uuid = (((long)is.read()) << 56) + 
    		   (((long)is.read()) << 48) + 
    		   (((long)is.read()) << 40) + 
    		   (((long)is.read()) << 32) +
			   (((long)is.read()) << 24) + 
			   (((long)is.read()) << 16) + 
			   (((long)is.read()) << 8) + 
			   (long)is.read();
        address = (short)((is.read()<<8)+is.read());
        endpoint = (byte)((is.read()<<8)+is.read());
    }
    
    public void write(OutputStream os) throws IOException {
        
        os.write(gid & 0xFF);
        os.write((int)((uuid >>> 56) & 0xFF)); 
        os.write((int)((uuid >>> 48) & 0xFF));
        os.write((int)((uuid >>> 40) & 0xFF));
        os.write((int)((uuid >>> 32) & 0xFF));
        os.write((int)((uuid >>> 24) & 0xFF)); 
        os.write((int)((uuid >>> 16) & 0xFF));
        os.write((int)((uuid >>> 8) & 0xFF));
        os.write((int)(uuid & 0xFF));
        
        os.write((address >>> 8) & 0xFF);
        os.write(address & 0xFF);
        os.write((endpoint >>> 8) & 0xFF);
        os.write(endpoint & 0xFF);
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
