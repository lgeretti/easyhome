package it.uniud.easyhome.network;

public interface Packet {
    
    public int length();
    
    public boolean isValid();
}
