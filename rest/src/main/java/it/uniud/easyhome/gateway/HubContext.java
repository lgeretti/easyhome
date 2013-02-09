package it.uniud.easyhome.gateway;

import it.uniud.easyhome.common.LogLevel;
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
    
    public Gateway getGatewayForId(byte gatewayId) {
        
        for (Gateway gw : gateways)
            if (gw.getId() == gatewayId)
                return gw;

        return null;
    }
    
    /** 
     * Adds a new gateway.
     * 
     * @param id The gateway id
     * @param protocol The protocol used by the gateway
     * @param port The port for the gateway (must not be already bound)
     */
    public void addGateway(byte id, ProtocolType protocol, int port) {
        
    	if (id < 1)
    		throw new RuntimeException("Gateway id must be greater than zero");
    	
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

    public void setLogLevel(LogLevel logLevel) {
        for (Gateway gw : gateways)
            gw.setLogLevel(logLevel);
    }
    
    public void setLogLevel(byte gatewayId, LogLevel logLevel) {
        for (Gateway gw : gateways)
            if (gw.getId() == gatewayId) {
            	gw.setLogLevel(logLevel);
            	break;
            }
    }

    public void openGateway(byte gatewayId) {
        for (Gateway gw : gateways)
            if (gw.getId() == gatewayId) {
            	gw.open();
            	break;
            }
    }
    
    public void closeGateway(byte gatewayId) {
        for (Gateway gw : gateways)
            if (gw.getId() == gatewayId) {
            	gw.close();
            	break;
            }
    }
    
    public boolean hasGateway(int gatewayId) {
        for (Gateway gw : gateways)
            if (gw.getId() == gatewayId)
                return true;
        
        return false;
    }
    
    public void removeGateway(byte gatewayId) {
        for (int i=0; i<gateways.size(); i++) 
            if (gateways.get(i).getId() == gatewayId) {
                gateways.get(i).close();
                gateways.remove(i);
                
                for (Gateway gw: gateways) {
                    gw.removeRoutingEntriesForGateway(gatewayId);
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
