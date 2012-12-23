package it.uniud.easyhome.packets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

public class Operation implements Serializable {

    private static final long serialVersionUID = -5552129172588857832L;

    public final static int FIXED_OCTETS = 7;
        
    private short domain;
    private short context;
    private byte flags;
    private byte sequenceNumber;
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
    
    public byte getFlags() {
    	return flags;
    }
    
    public byte getSequenceNumber() {
    	return sequenceNumber;
    }
    
    /** The command is actually relevant only if the operation is not addressed to the management domain */
    public byte getCommand() {
        return command;
    }
    
    public byte[] getData() {
        return data;
    }
    
    public Operation(byte sequenceNumber, short domain, short context, byte flags, byte command, byte[] data) {
        
    	this.sequenceNumber = sequenceNumber;
        this.domain = domain;
        this.context = context;
        this.flags = flags;
        this.command = command;
        this.data = data;
    }
    
    public Operation(InputStream is, int dataSize) throws IOException {

    	sequenceNumber = (byte)is.read();
    	
        int highDomain = is.read();
        domain = (short)(highDomain*256+is.read());
        
        int highContext = is.read();
        context = (short)(highContext*256+is.read());
        
        flags = (byte)is.read();
        
        command = (byte)is.read();
        
        data = new byte[dataSize];
        is.read(data, 0, dataSize);
    }
    
    public void write(OutputStream os) throws IOException {

        os.write(sequenceNumber);
        os.write((domain >>> 8) & 0xFF);
        os.write(domain & 0xFF);
        os.write((context >>> 8) & 0xFF);
        os.write(context & 0xFF);
        os.write(flags);
        os.write(command);

        os.write(data);
    }
    
    public String toString() {
    	StringBuilder strb = new StringBuilder();
    	
    	strb.append("{D: ")
    		.append(domain)
    		.append("; Cx: ")
    		.append(context);
    	
    	if (Domain.isManagement(domain))
    		strb.append("; Cm: ").append(command);
    	
    	strb.append("}");
    	
    	return strb.toString();    	
    }
}
