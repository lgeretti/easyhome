package it.uniud.easyhome.processing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.natives.NodeDescrReqPacket;
import it.uniud.easyhome.packets.natives.SimpleDescrReqPacket;

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
	
    public SimpleDescrRequestProcess(int pid, UriInfo uriInfo,ProcessKind kind) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind);
        Topic networkEventsTopic = (Topic) jndiContext.lookup(JMSConstants.NETWORK_EVENTS_TOPIC);
        networkEventsConsumer = registerConsumerFor(networkEventsTopic);
    }
    
    @Override
	protected void process() throws JMSException, NamingException {

    	ObjectMessage msg = (ObjectMessage) networkEventsConsumer.receive();
    	if (msg != null) {
    		NetworkEvent event = (NetworkEvent) msg.getObject();
    		if (event != null && event.getKind() == NetworkEvent.EventKind.NODE_ENDPOINTS_ACQUIRED) {

    	        try {
        	        ClientResponse getResponse = restResource.path("network")
        	        								.path(Byte.toString(event.getGatewayId())).path(Short.toString(event.getAddress()))
        	        								.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        	        
    	        	Node node = JsonUtils.getFrom(getResponse, Node.class);
    	        	List<Short> endpoints = node.getEndpoints();
    	        	for (int i=0; i<endpoints.size(); i++) {
    	        		try {
    	    	        	SimpleDescrReqPacket packet = new SimpleDescrReqPacket(node.getCoordinates(),node.getEndpoints().get(i).byteValue(),++sequenceNumber);
    	    	            ObjectMessage outboundMessage = jmsSession.createObjectMessage(packet);
    	    	            getOutboundPacketsProducer().send(outboundMessage);    
    	    	            println("Simple descriptor for endpoint " + endpoints.get(i) + " of node " + node.getName() + " request dispatched");	
    	        			Thread.sleep(WAIT_TIME_BETWEEN_REQUESTS_MILLIS);
    	        		} catch (Exception e) {
    	    	        	e.printStackTrace();
    	    	        	i--;
    	    	        	println("Simple descriptor request for endpoint " + endpoints.get(i) + " of node " + node.getName() +  
    	    	        			" could not be dispatched, retrying");
    	    	        }
    	        	}
    	        } catch (Exception e) {
    	        	e.printStackTrace();
    	        	println("Simple descriptors for node cannot be recovered: issue when getting node " 
    	        			+ Byte.toString(event.getGatewayId()) + ":" + Integer.toHexString(0xFFFF & event.getAddress()));
    	        }
    		}
       	}
    }
    
}