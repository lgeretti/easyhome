package it.uniud.easyhome.processing;

import java.util.List;

import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.common.LogLevel;
import it.uniud.easyhome.devices.DeviceType;
import it.uniud.easyhome.devices.Functionality;
import it.uniud.easyhome.devices.FunctionalityType;
import it.uniud.easyhome.devices.states.LampState;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.natives.LampStateSetPacket;
import it.uniud.easyhome.packets.natives.OccupancyAttributeReqPacket;
import it.uniud.easyhome.rest.RestPaths;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.naming.NamingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class LampStateUpdateProcess extends Process {
	
	public static long REQUEST_PERIOD_MS = 1000;
	
    public LampStateUpdateProcess(int pid, UriInfo uriInfo,ProcessKind kind, LogLevel logLevel) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind,logLevel);
    }
    
    @Override
	protected void process() throws JMSException, NamingException {

    	try {
    		MultivaluedMap<String,String> params = new MultivaluedMapImpl();
            params.add("deviceType",DeviceType.COLORED_LAMP.toString());
	    	ClientResponse getResponse = restResource.path(RestPaths.NODES).queryParams(params).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	        	        
	    	List<Node> nodes = JsonUtils.getListFrom(getResponse, Node.class);
    		
	    	for (Node node : nodes) {

	            params.add("funcType",FunctionalityType.OCCUPATION_SENSING.toString());
	    		ClientResponse lampStateResponse = restResource.path(RestPaths.STATES).path("lamps").path(Long.toString(node.getInfo().getId())).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	    		LampState lampState = JsonUtils.getFrom(lampStateResponse, LampState.class);
	    		
				LampStateSetPacket packet = new LampStateSetPacket(lampState,node.getCoordinates());
		        
	            ObjectMessage changeMessage = jmsSession.createObjectMessage(packet);
	            getOutboundPacketsProducer().send(changeMessage); 
	            
	            log(LogLevel.FINE,"State update sent to lamp with address 0x"+Long.toHexString(node.getCoordinates().getNuid()));
	    	}
	    	Thread.sleep(REQUEST_PERIOD_MS/nodes.size());
	    	
        } catch (ArithmeticException e) {
        	try {
        		log(LogLevel.DEBUG,"No lamps to send state updates to");
        		Thread.sleep(REQUEST_PERIOD_MS);
        	} catch (InterruptedException ex) { }
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }   
}