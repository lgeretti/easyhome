package it.uniud.easyhome.network;

public interface Packet {

    public byte[] getHeader();
    
    public byte[] getPayload();
    
    public int length();
}
