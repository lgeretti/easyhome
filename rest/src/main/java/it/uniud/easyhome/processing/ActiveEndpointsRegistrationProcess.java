package it.uniud.easyhome.processing;

import java.util.Arrays;
import java.util.List;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.natives.ActiveEndpointsRspPacket;
import it.uniud.easyhome.packets.natives.NativePacket;
import it.uniud.easyhome.packets.natives.NodeNeighRspPacket;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Topic;
import javax.naming.NamingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;

public class ActiveEndpointsRegistrationProcess extends Process {
	
	private MessageProducer networkEventsProducer = null;
	
    public ActiveEndpointsRegistrationProcess(int pid, UriInfo uriInfo,ProcessKind kind) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind);
        
        Topic networkEventsTopic = (Topic) jndiContext.lookup(JMSConstants.NETWORK_EVENTS_TOPIC);
        networkEventsProducer = registerProducerFor(networkEventsTopic);
    }
    
    @Override
	protected void process() throws JMSException, NamingException {
    	
    	MessageConsumer inboundPacketsConsumer = getInboundPacketsConsumer();

    	ObjectMessage msg = (ObjectMessage) inboundPacketsConsumer.receive();
    	if (msg != null) {
        	NativePacket pkt = (NativePacket) msg.getObject();
        	
        	if (ActiveEndpointsRspPacket.validates(pkt)) {
	        	println("ActiveEndpointsRspPacket received from " + pkt.getSrcCoords());
	        	
	        	try {
	        		ActiveEndpointsRspPacket activeEpPkt = new ActiveEndpointsRspPacket(pkt);
	        		
	        		List<Short> activeEps = activeEpPkt.getActiveEndpoints();
	        			
	        		Node node = restResource.path("network/"+pkt.getSrcCoords().getNuid())
	                		.accept(MediaType.APPLICATION_JSON).get(Node.class);
	        		node.setEndpoints(activeEps);

	                ClientResponse updateResponse = restResource.path("network")
	                		.type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node);
	                
	                if (updateResponse.getClientResponseStatus() == Status.OK) {
	                	
	                	NetworkEvent event = new NetworkEvent(NetworkEvent.EventKind.NODE_ENDPOINTS_ACQUIRED, node.getGatewayId(), node.getId());
	                    try {
	                        ObjectMessage eventMessage = jmsSession.createObjectMessage(event);
	                        networkEventsProducer.send(eventMessage);
	                        println("Node " + pkt.getSrcCoords().getNuid() + " updated with endpoints information (#" + activeEps.size() + ")");
	                    } catch (Exception e) {
	                    	println("Node endpoints registration message could not be dispatched to inbound packets topic");
	                    }
	                } else
	                	println("Node endpoints information update failed");
	        		
	        	} catch (InvalidPacketTypeException e) {
	        		e.printStackTrace();
	        	}
        	}
    	}
    }
    
}