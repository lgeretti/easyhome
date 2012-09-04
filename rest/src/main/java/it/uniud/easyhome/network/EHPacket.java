package it.uniud.easyhome.network;

import it.uniud.easyhome.network.exceptions.ChecksumException;
import it.uniud.easyhome.network.exceptions.InvalidDelimiterException;
import it.uniud.easyhome.network.exceptions.InvalidPacketLengthException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class EHPacket {

    private ModuleCoordinates srcCoords;
    private ModuleCoordinates dstCoords;
    private Operation operation;
    
    // Cached field
    private int checksum;
    
    public ModuleCoordinates getSrcCoords() {
        return srcCoords;
    }

    public ModuleCoordinates getDstCoords() {
        return dstCoords;
    }    
    
    public Operation getOperation() {
        return operation;
    }
    
    public EHPacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
        this.srcCoords = srcCoords;
        this.dstCoords = dstCoords;
        this.operation = op;
    }
    
    public EHPacket(byte[] bytes) {
        
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        
        int delimiter = bais.read();
        
        if (delimiter != 0xEA)
            throw new InvalidDelimiterException();
        
        int highLength = bais.read();
        int length = highLength*256+bais.read();
        
        if (length != bytes.length-4)
            throw new InvalidPacketLengthException();
        
        int sum = 0;
        for (int i=3; i<bytes.length; i++)
            sum += bytes[i];
        
        if (0xFF != (sum & 0xFF))
            throw new ChecksumException();
        
        checksum = bytes[bytes.length-1];
        
        srcCoords = new ModuleCoordinates(bais);
        dstCoords = new ModuleCoordinates(bais);
        operation = new Operation(bais,length-2*ModuleCoordinates.OCTETS-Operation.FIXED_OCTETS);
    }
    
    public byte[] getBytes() {
        
        int payloadLength = 2*ModuleCoordinates.OCTETS+Operation.FIXED_OCTETS+operation.getData().length;
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream(payloadLength+4); 
        
        baos.write((byte)0xEA);
        baos.write((payloadLength >>> 8) & 0xFF);
        baos.write(payloadLength & 0xFF);
        srcCoords.writeBytes(baos);
        dstCoords.writeBytes(baos);
        operation.writeBytes(baos);
        baos.write(checksum);
        
        return baos.toByteArray();
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