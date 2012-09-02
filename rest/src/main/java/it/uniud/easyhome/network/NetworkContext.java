package it.uniud.easyhome.network;

import it.uniud.easyhome.gateway.Gateway;
import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.gateway.XBeeGateway;
import it.uniud.easyhome.network.exceptions.PortAlreadyBoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/***
 * Provides the context related to the hub and gateways.
 * 
 * @author Luca Geretti
 *
 */
public class NetworkContext {

    private static final NetworkContext INSTANCE = new NetworkContext();
    
    private final List<Gateway> gateways = new ArrayList<Gateway>();
    
    private final Map<ModuleCoordinates,Integer> routingTable = new HashMap<ModuleCoordinates,Integer>();
    
    private final Map<Integer,Integer> gatewayPortCounts = new HashMap<Integer,Integer>();
    
    // Identifiers are guaranteed as unique, hence we cannot rely on the gateways size
    private int gidCount = 0;
    
    public List<Gateway> getGateways() {
        return gateways;
    }
    
    public Map<ModuleCoordinates,Integer> getRoutingTable() {
        return routingTable;
    }
    
    public int addRoutingEntry(ModuleCoordinates coords) {
        
        int portCount = gatewayPortCounts.get(coords.getGatewayId());
        portCount++;
        gatewayPortCounts.put(coords.getGatewayId(), portCount);
        routingTable.put(coords, portCount);
        
        return portCount;
    }
    
    public void removeRoutingEntry(ModuleCoordinates coords) {
        routingTable.remove(coords);
    }    
    
    public boolean hasRoutingEntry(ModuleCoordinates coords) {
        return routingTable.containsKey(coords);
    }
    
    public Integer getPortFor(ModuleCoordinates coords) {
        return routingTable.get(coords);
    }
    
    private NetworkContext() {}
    
    public static NetworkContext getInstance() {
     
        return INSTANCE;
    }
    
    /** 
     * Adds a new gateway.
     * 
     * @param protocol The protocol used by the gateway
     * @param port The port for the gateway (must not be already bound)
     * @return The gid of the created gateway
     */
    public int addGateway(ProtocolType protocol, int port) {
        
        for (Gateway gw : gateways)
            if (gw.getPort() == port)
                throw new PortAlreadyBoundException();
        
        int gid = ++gidCount;
        
        Gateway gw = null;
        
        switch (protocol) {
        
            case XBEE:  
        
                gw = new XBeeGateway(gid,port,this);
                break;
                
            case ZIGBEE:
                
                break;
                
            case EHS:    
        
                break;
        }
        
        gw.open();
        
        gateways.add(gw);
        gatewayPortCounts.put(gid, 0);
        
        return gid;
    }
    
    public boolean hasGateway(int gid) {
        for (Gateway gw : gateways)
            if (gw.getId() == gid)
                return true;
        
        return false;
    }
    
    public void removeGateway(int gid) {
        for (int i=0; i<gateways.size(); i++) 
            if (gateways.get(i).getId() == gid) {
                gateways.get(i).close();
                gateways.remove(i);
                gatewayPortCounts.remove(gid);
                break;
            }
    }
    
    public void removeAllGateways() {
        for (Gateway gw : gateways)
            gw.close();
        gateways.clear();
        gatewayPortCounts.clear();
    }
}
