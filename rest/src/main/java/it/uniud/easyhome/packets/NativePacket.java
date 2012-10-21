package it.uniud.easyhome.packets;

import it.uniud.easyhome.exceptions.InvalidDelimiterException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

public class NativePacket implements Serializable {

    private static final long serialVersionUID = -6680743462870235932L;
    public static final byte DELIMITER = (byte)0xEA;
    
    private ModuleCoordinates srcCoords;
    private ModuleCoordinates dstCoords;
    private Operation operation;
    
    public ModuleCoordinates getSrcCoords() {
        return srcCoords;
    }

    public ModuleCoordinates getDstCoords() {
        return dstCoords;
    }    
    
    public Operation getOperation() {
        return operation;
    }
    
    public NativePacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
        this.srcCoords = srcCoords;
        this.dstCoords = dstCoords;
        this.operation = op;
    }
    
    public NativePacket(InputStream is) {
        
    	try {
	        byte delimiter = (byte)is.read();
	        
	        if (delimiter != DELIMITER)
	            throw new InvalidDelimiterException();
	        
	        int highLength = is.read();
	        int length = highLength*256+is.read();
	        
	        srcCoords = new ModuleCoordinates(is);
	        dstCoords = new ModuleCoordinates(is);
	        operation = new Operation(is,length-2*ModuleCoordinates.OCTETS-Operation.FIXED_OCTETS);
        
    	} catch (IOException ex) {
    		ex.printStackTrace();
    	}
    }
    
    public byte[] getBytes() {
        
        int payloadLength = 2*ModuleCoordinates.OCTETS+Operation.FIXED_OCTETS+operation.getData().length;
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream(payloadLength+4); 
        
        baos.write((byte)0xEA);
        baos.write((payloadLength >>> 8) & 0xFF);
        baos.write(payloadLength & 0xFF);
        try {
	        srcCoords.write(baos);
	        dstCoords.write(baos);
	        operation.write(baos);
        } catch (IOException ex) {
        	ex.printStackTrace();
        }
        
        return baos.toByteArray();
    }
    
    public void write(OutputStream os) {
    	try {
	    	os.write(getBytes());
	    	os.flush();
    	} catch (IOException ex) {
    		ex.printStackTrace();
    	}
    }
    
    public String printBytes() {
        
        StringBuilder strb = new StringBuilder();
        for (byte b: getBytes()) {
            if ((0xFF & b) < 0x10)
                strb.append("0");
            strb.append(Integer.toHexString(0xFF & b).toUpperCase()).append(" ");
        }
        
        return strb.toString();
    }
}