package it.uniud.easyhome.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Operation {

    public final static int FIXED_OCTETS = 4;
    
    private int context;
    private int method;
    private byte[] data;
    
    public int getContext() {
        return context;
    }
    
    public int getMethod() {
        return method;
    }
    
    public byte[] getData() {
        return data;
    }
    
    public Operation(ByteArrayInputStream bais, int dataSize) {
        
        int highContext = bais.read();
        context = highContext*256+bais.read();
        
        int highMethod = bais.read();
        method = highMethod*256+bais.read();
        
        data = new byte[dataSize];
        bais.read(data, 0, dataSize);
    }
    
    public void writeBytes(ByteArrayOutputStream baos) {
        
        baos.write((context >>> 8) & 0xFF);
        baos.write(context & 0xFF);
        baos.write((method >>> 8) & 0xFF);
        baos.write(method & 0xFF);
        
        try {
            baos.write(data);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected error when writing operation data bytes");
        }
    }
}
