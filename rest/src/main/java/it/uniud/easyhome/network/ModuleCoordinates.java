package it.uniud.easyhome.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ModuleCoordinates {

    public static final int OCTETS = 5;
    
    // Gateway (and consequently subnetwork) identifier (>0, =0 for broadcast, =1 for EasyHome TCP/IP)
    private int gid;
    // Address within the network (>0, =0 if broadcast to a specific network, irrelevant if broadcast to all subnetworks)
    private int address;
    // Port of the interested module (>0, no broadcast is supported)
    private int port;
    
    public int getGatewayId() {
        return gid;
    }
    
    public int getAddress() {
        return address;
    }
    
    public int getPort() {
        return port;
    }
    
    public ModuleCoordinates(int gid, int address, int port) {
        this.gid = gid;
        this.address = address;
        this.port = port;
    }
    
    public ModuleCoordinates(ByteArrayInputStream bais) {
        
        gid = bais.read();
        
        int highAddr = bais.read();
        address = highAddr*256+bais.read();
        
        int highPort = bais.read();
        port = highPort*256+bais.read();
    }
    
    public void writeBytes(ByteArrayOutputStream baos) {
        
        baos.write(gid & 0xFF);
        baos.write((address >>> 8) & 0xFF);
        baos.write(address & 0xFF);
        baos.write((port >>> 8) & 0xFF);
        baos.write(port & 0xFF);
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
        if (otherCoords.getPort() != this.getPort())
            return false;
        
        return true;
    }
    
    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 31 + gid;
        hash = hash * 31 + address;
        hash = hash * 31 + port;
        return hash;
    }
    
}
