package it.uniud.easyhome.packets;

import it.uniud.easyhome.common.ByteUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

/** 
 * Immutable class for absolute coordinates of a module across the EasyHome network. 
 */
public class ModuleCoordinates implements Serializable {

    private static final long serialVersionUID = -5009839141486612459L;

    public static final int OCTETS = 12;
    
    // Gateway (and consequently subnetwork) identifier (0 for broadcast, 1 for the native TCP/IP subnetwork)
    private byte gid;
    // Node unique id (global address, like a IEEE MAC address, fixed for a node) (0x0 for a gateway node if gid!=1, or the domotic controller if gid==1, 
    // 0x000000000000FFFF for a broadcast)
    private long nuid;
    // Address within the network (0x0000 for the gateway, 0xFFFE if broadcast or unknown)
    private short address;
    // Endpoint of the interested module (0 addresses the management endpoint)
    private byte endpoint;
    
    public byte getGatewayId() {
        return gid;
    }
    
    public long getNuid() {
    	return nuid;
    }
    
    public short getAddress() {
        return address;
    }
    
    public byte getEndpoint() {
        return endpoint;
    }
    
    public ModuleCoordinates(byte gid, long nuid, short address, byte endpoint) {
        this.gid = gid;
        this.nuid = nuid;
        this.address = address;
        this.endpoint = endpoint;
    }
    
    public ModuleCoordinates(InputStream is) throws IOException {

        gid = (byte)is.read();
    	nuid = (((long)is.read()) << 56) + 
    		   (((long)is.read()) << 48) + 
    		   (((long)is.read()) << 40) + 
    		   (((long)is.read()) << 32) +
			   (((long)is.read()) << 24) + 
			   (((long)is.read()) << 16) + 
			   (((long)is.read()) << 8) + 
			   (long)is.read();
        address = (short)((is.read()<<8)+is.read());
        endpoint = (byte)is.read();
    }
    
    public void write(OutputStream os) throws IOException {
        
        os.write(gid & 0xFF);
        os.write((int)((nuid >>> 56) & 0xFF)); 
        os.write((int)((nuid >>> 48) & 0xFF));
        os.write((int)((nuid >>> 40) & 0xFF));
        os.write((int)((nuid >>> 32) & 0xFF));
        os.write((int)((nuid >>> 24) & 0xFF)); 
        os.write((int)((nuid >>> 16) & 0xFF));
        os.write((int)((nuid >>> 8) & 0xFF));
        os.write((int)(nuid & 0xFF));
        
        os.write((address >>> 8) & 0xFF);
        os.write(address & 0xFF);
        os.write(endpoint & 0xFF);
    }
    
    public String toString() {
    	StringBuilder strb = new StringBuilder();
    	
    	strb.append("{G: ")
    		.append(gid)
    		.append("; N: ")
    		.append(ByteUtils.printBytes(ByteUtils.getBytes(nuid)))
    		.append("; A: ")
    		.append(ByteUtils.printBytes(ByteUtils.getBytes(address)))
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
        if (otherCoords.getNuid() != this.getNuid())
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
        hash = (int)(hash * 31 + nuid);
        hash = hash * 31 + address;
        hash = hash * 31 + endpoint;
        return hash;
    }
    
}
