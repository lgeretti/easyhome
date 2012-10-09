package it.uniud.easyhome.gateway;

import it.uniud.easyhome.network.ModuleCoordinates;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Gateway implements Runnable {
    
	@XmlElement(name="id")
    protected byte id;
	
	@XmlElement(name="protocolType")
    private ProtocolType protocolType;
	
	@XmlElement(name="port")
    protected int port;
    
    private final Map<ModuleCoordinates,Integer> routingTable = new HashMap<ModuleCoordinates,Integer>();
    
    private int mappedEndpointCounter = 0;
    
    protected ServerSocket server = null;
    
	protected volatile boolean disconnected = false;
	
    @SuppressWarnings("unused")
    private Gateway() { }
    
    protected Gateway(byte id, ProtocolType protocolType, int port) {
    	this.id = id;
    	this.protocolType = protocolType;
    	this.port = port;
    }
    
    public byte getId() {
        return id;
    }
    
    public ProtocolType getProtocolType() {
        return protocolType;
    }
    
    public int getPort() {
        return port;
    }
    
    public Map<ModuleCoordinates,Integer> getRoutingTable() {
        return routingTable;
    }
    
    public int addRoutingEntry(ModuleCoordinates coords) {
        
        println("Putting routing entry (endpoint " + (mappedEndpointCounter+1) + ") for " + coords);
    	
        routingTable.put(coords, ++mappedEndpointCounter);
        
        return mappedEndpointCounter;
    }
    
    public void removeRoutingEntry(ModuleCoordinates coords) {
        routingTable.remove(coords);
    }
    
    public void removeRoutingEntriesForGateway(int gid) {
        
        Iterator<Map.Entry<ModuleCoordinates,Integer>> it = routingTable.entrySet().iterator();
        while (it.hasNext())
            if (it.next().getKey().getGatewayId() == gid)
                it.remove();
    }
    
    public Integer getEndpointFor(ModuleCoordinates coords) {
        return routingTable.get(coords);
    }
    
    protected ModuleCoordinates getCoordinatesFor(int endpoint) {
        ModuleCoordinates coords = null;

        for (Entry<ModuleCoordinates,Integer> pair : routingTable.entrySet()) 
            if (pair.getValue() == endpoint) {
                coords = pair.getKey();
                break;
            }
        
        return coords;
    }
    
    public void open() { 
    	// To be overridden
    }
    
    public void close() {
        try {
        	disconnect();
            server.close();
        } catch (IOException ex) {
            // We swallow any IO error
        }
    }
    
    /** Drop any existing connection */
    public void disconnect() {
    	disconnected = true;
    }
    
    protected void println(String msg) {
    	System.out.println("Gw #" + id + ": " + msg);
    }

	@Override
	public void run() {
		
	}
}
