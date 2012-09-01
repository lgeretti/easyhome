package it.uniud.easyhome.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ModuleCoordinates {

    public static final int OCTETS = 5;
    
    private int network;
    private int address;
    private int port;
    
    public int getNetwork() { 
        return network;
    }
    
    public int getAddress() {
        return address;
    }
    
    public int getPort() {
        return port;
    }
    
    public ModuleCoordinates(ByteArrayInputStream bais) {
        
        network = bais.read();
        
        int highAddr = bais.read();
        address = highAddr*256+bais.read();
        
        int highPort = bais.read();
        port = highPort*256+bais.read();
    }
    
    public void writeBytes(ByteArrayOutputStream baos) {
        
        baos.write(network & 0xFF);
        baos.write((address >>> 8) & 0xFF);
        baos.write(address & 0xFF);
        baos.write((port >>> 8) & 0xFF);
        baos.write(port & 0xFF);
    }
    
}
