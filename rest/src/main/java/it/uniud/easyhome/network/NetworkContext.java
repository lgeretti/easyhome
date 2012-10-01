package it.uniud.easyhome.network;

import it.uniud.easyhome.gateway.Gateway;
import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.gateway.XBeeGateway;
import it.uniud.easyhome.network.exceptions.PortAlreadyBoundException;

import java.util.ArrayList;
import java.util.List;

/***
 * Provides the context related to the hub and gateways.
 * 
 * @author Luca Geretti
 *
 */
public class NetworkContext {

    private static final NetworkContext INSTANCE = new NetworkContext();
    
    private final List<Gateway> gateways = new ArrayList<Gateway>();
    
    // Identifiers are guaranteed as unique, hence we cannot rely on the gateways size
    // gid = 0 for broadcast
    // gid = 1 for the EasyHome network
    // hence actual gateways start from 2 onwards
    private int gidCount = 1;
    
    public List<Gateway> getGateways() {
        return gateways;
    }  
    
    public int getGidCount() {
    	return gidCount;
    }
    
    private NetworkContext() {}
    
    public static NetworkContext getInstance() {
     
        return INSTANCE;
    }
    
    public Gateway getGatewayForId(int gid) {
        
        for (Gateway gw : gateways)
            if (gw.getId() == gid)
                return gw;

        return null;
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
            if (gw.getTCPPort() == port)
                throw new PortAlreadyBoundException();
        
        int gid = ++gidCount;
        
        Gateway gw = null;
        
        switch (protocol) {
        
            case XBEE:  
        
                gw = new XBeeGateway(gid,port);
                break;
                
            case ZIGBEE:
                
                break;
                
            case EHS:    
        
                break;
        }
        
        gw.open();
        
        gateways.add(gw);
        
        return gid;
    }
    
    public boolean hasGateway(int gid) {
        for (Gateway gw : gateways)
            if (gw.getId() == gid)
                return true;
        
        return false;
    }
    
    public void disconnectGateway(int gid) {
        for (Gateway gw : gateways)
            if (gw.getId() == gid)
                gw.disconnect();    	
    }
    
    public void removeGateway(int gid) {
        for (int i=0; i<gateways.size(); i++) 
            if (gateways.get(i).getId() == gid) {
                gateways.get(i).close();
                gateways.remove(i);
                
                for (Gateway gw: gateways) {
                    gw.removeRoutingEntriesForGateway(gid);
                }
                
                break;
            }
    }
    
    public void removeAllGateways() {
        for (Gateway gw : gateways)
            gw.close();
        gateways.clear();
    }
}
