package it.uniud.easyhome.gateway;

import it.uniud.easyhome.exceptions.GatewayIdentifierAlreadyPresentException;
import it.uniud.easyhome.exceptions.PortAlreadyBoundException;

import java.util.ArrayList;
import java.util.List;

/***
 * Provides the context related to the hub and gateways.
 * 
 * @author Luca Geretti
 *
 */
public class HubContext {
	
    private static final HubContext INSTANCE = new HubContext();
    
    private final List<Gateway> gateways = new ArrayList<Gateway>();
    
    public List<Gateway> getGateways() {
        return gateways;
    }  
    
    private HubContext() {
    }
    
    public static HubContext getInstance() {
     
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
     * @param id The gateway id
     * @param protocol The protocol used by the gateway
     * @param port The port for the gateway (must not be already bound)
     * @return The gid of the created gateway
     */
    public void addGateway(byte id, ProtocolType protocol, int port) {
        
        for (Gateway gw : gateways) {
            if (gw.getPort() == port)
                throw new PortAlreadyBoundException();
            if (gw.getId() == id)
            	throw new GatewayIdentifierAlreadyPresentException();
        }
        
        Gateway gw = null;
        
        switch (protocol) {
        
            case XBEE:  
        
                gw = new XBeeGateway(id,port);
                break;
            
            case NATIVE:
            	
            	throw new RuntimeException("Cannot register another native gateway");
        }
        
        gw.open();
        
        gateways.add(gw);
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
    
    public void disconnectAllGateways() {
        for (Gateway gw : gateways)
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
