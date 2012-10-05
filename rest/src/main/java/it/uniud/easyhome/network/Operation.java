package it.uniud.easyhome.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

public class Operation implements Serializable {

    private static final long serialVersionUID = -5552129172588857832L;

    public final static int FIXED_OCTETS = 6;
    
    private byte flags;
    private short domain;
    private short context;
    private byte command;
    private byte[] data;
    
    /** A domain is equivalent to the Profile of ZigBee */
    public short getDomain() {
        return domain;
    }
    
    /** A context is equivalent to the Cluster of ZigBee */
    public short getContext() {
        return context;
    }
    
    public boolean isContextSpecific() {
        return ((flags & 0x01) == 1);
    }
    
    public byte getCommand() {
        return command;
    }
    
    public byte getFlags() {
    	return flags;
    }
    
    public byte[] getData() {
        return data;
    }
    
    public Operation(byte flags, short domain, short context, byte command, byte[] data) {
        
        this.flags = flags;
        this.domain = domain;
        this.context = context;
        this.command = command;
        this.data = data;
    }
    
    public Operation(ByteArrayInputStream bais, int dataSize) {
        
        flags = (byte)bais.read();
        
        int highDomain = bais.read();
        domain = (short)(highDomain*256+bais.read());
        
        int highContext = bais.read();
        context = (short)(highContext*256+bais.read());
        
        command = (byte)bais.read();
        
        data = new byte[dataSize];
        bais.read(data, 0, dataSize);
    }
    
    public void writeBytes(ByteArrayOutputStream baos) {
        
        baos.write(flags & 0xFF);
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
