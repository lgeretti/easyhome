package it.uniud.easyhome.processing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.common.LogLevel;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.natives.NodeDescrReqPacket;
import it.uniud.easyhome.packets.natives.SimpleDescrReqPacket;
import it.uniud.easyhome.rest.RestPaths;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Topic;
import javax.naming.NamingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.sun.jersey.api.client.ClientResponse;

public class SimpleDescrRequestProcess extends Process {
	
	private byte sequenceNumber = 0;
	
	private static long WAIT_TIME_BETWEEN_REQUESTS_MILLIS = 500;
	
	private MessageConsumer networkEventsConsumer = null;
	
    public SimpleDescrRequestProcess(int pid, UriInfo uriInfo,ProcessKind kind, LogLevel logLevel) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind,logLevel);
        Topic networkEventsTopic = (Topic) jndiContext.lookup(JMSConstants.NETWORK_EVENTS_TOPIC);
        networkEventsConsumer = registerConsumerFor(networkEventsTopic);
    }
    
    @Override
	protected void process() throws JMSException, NamingException {

    	ObjectMessage msg = (ObjectMessage) networkEventsConsumer.receive();
    	if (msg != null) {
    		NetworkEvent event = (NetworkEvent) msg.getObject();
    		if (event != null && event.getKind() == NetworkEvent.EventKind.NODE_ENDPOINTS_ACQUIRED) {

    			byte gatewayId = event.getGatewayId();
	        	short address = event.getAddress();
    			
    	        try {
    	        	
        	        ClientResponse getResponse = restResource.path(RestPaths.NODES)
        	        								.path(Byte.toString(gatewayId)).path(Short.toString(address))
        	        								.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        	        
        	        if (getResponse.getClientResponseStatus() == ClientResponse.Status.OK) {
	    	        	Node node = JsonUtils.getFrom(getResponse, Node.class);
	    	        	List<Byte> endpoints = node.getEndpoints();
	    	        	for (int i=0; i<endpoints.size(); i++) {
	    	        		try {
	    	    	        	SimpleDescrReqPacket packet = new SimpleDescrReqPacket(node.getCoordinates(),node.getEndpoints().get(i).byteValue(),++sequenceNumber);
	    	    	            ObjectMessage outboundMessage = jmsSession.createObjectMessage(packet);
	    	    	            getOutboundPacketsProducer().send(outboundMessage);    
	    	    	            log(LogLevel.INFO, "Simple descriptor for endpoint " + endpoints.get(i) + " of node " + node + " request dispatched");	
	    	        			Thread.sleep(WAIT_TIME_BETWEEN_REQUESTS_MILLIS);
	    	        		} catch (Exception e) {
	    	    	        	e.printStackTrace();
	    	    	        	i--;
	    	    	        	log(LogLevel.DEBUG, "Simple descriptor request for endpoint " + endpoints.get(i) + " of node " + node +  
	    	    	        			" could not be dispatched, retrying");
	    	    	        }
	    	        	}
        	        } else
        	        	log(LogLevel.DEBUG, "Node " + Node.nameFor(gatewayId, address) + " not found, ignoring");
    	        } catch (Exception e) {
    	        	e.printStackTrace();
    	        	log(LogLevel.DEBUG, "Simple descriptors cannot be recovered: issue when getting node " + Node.nameFor(gatewayId, address));
    	        }
    		}
       	}
    }
    
}