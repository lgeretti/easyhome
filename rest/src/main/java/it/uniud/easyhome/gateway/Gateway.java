package it.uniud.easyhome.gateway;

import it.uniud.easyhome.network.ModuleCoordinates;

import java.util.Map;

public interface Gateway {
    
    public int getId();
    
    public ProtocolType getProtocolType();
    
    public int getTCPPort();
    
    public Map<ModuleCoordinates,Integer> getRoutingTable();
    
    public int addRoutingEntry(ModuleCoordinates coords);
    
    public void removeRoutingEntry(ModuleCoordinates coords);
    
    public void removeRoutingEntriesForGateway(int gid);
    
    public Integer getPortFor(ModuleCoordinates coords);
    
    public void open();
    
    public void close();
    
    /** Drop any existing connection */ 
    public void disconnect();
}
