package it.uniud.easyhome.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

public class Operation implements Serializable {

    private static final long serialVersionUID = -5552129172588857832L;

    public final static int FIXED_OCTETS = 6;
    
    private boolean isContextSpecific;
    private int domain;
    private int context;
    private int command;
    private byte[] data;
    
    public int getDomain() {
        return domain;
    }
    
    public int getContext() {
        return context;
    }
    
    public boolean isContextSpecific() {
        return isContextSpecific;
    }
    
    public int getCommand() {
        return command;
    }
    
    public byte[] getData() {
        return data;
    }
    
    public Operation(int flags, int domain, int context, int command, byte[] data) {
        
        isContextSpecific = ((flags & 0x01) == 1);
        
        this.domain = domain;
        this.context = context;
        this.command = command;
        this.data = data;
    }
    
    public Operation(ByteArrayInputStream bais, int dataSize) {
        
        int flags = bais.read();
        isContextSpecific = ((flags & 0x01) == 1);
        
        int highDomain = bais.read();
        domain = highDomain*256+bais.read();
        
        int highContext = bais.read();
        context = highContext*256+bais.read();
        
        command = bais.read();
        
        data = new byte[dataSize];
        bais.read(data, 0, dataSize);
    }
    
    public void writeBytes(ByteArrayOutputStream baos) {
        
        baos.write(isContextSpecific ? 0x01 : 0x00);
        baos.write((domain >>> 8) & 0xFF);
        baos.write(domain & 0xFF);
        baos.write((context >>> 8) & 0xFF);
        baos.write(context & 0xFF);
        baos.write(command & 0xFF);
        
        try {
            baos.write(data);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected error when writing operation data bytes");
        }
    }
}
