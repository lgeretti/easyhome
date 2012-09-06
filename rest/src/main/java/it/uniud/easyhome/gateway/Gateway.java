package it.uniud.easyhome.gateway;

public interface Gateway {
    
    public int getId();
    
    public ProtocolType getProtocolType();
    
    public int getTCPPort();
    
    public int getNewMappedPort();
    
    public void open();
    
    public void close();
}
