package it.uniud.easyhome.processing;

import java.util.Arrays;
import java.util.List;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.NetworkJob;
import it.uniud.easyhome.network.NetworkJobType;
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
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.xml.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

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
	        		
	        		byte gatewayId = activeEpPkt.getSrcCoords().getGatewayId();
	        		short address = activeEpPkt.getAddrOfInterest();
	                
	        		Node node = restResource.path("network").path(Byte.toString(gatewayId)).path(Short.toString(address))
	        								.accept(MediaType.APPLICATION_JSON).get(Node.class);
	        		node.setEndpoints(activeEps);

	                ClientResponse updateResponse = restResource.path("network").path("update")
	                		.type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node);
	                
	                if (updateResponse.getClientResponseStatus() == Status.OK) {
	                	
		                MultivaluedMap<String,String> queryData = new MultivaluedMapImpl();
		                queryData.add("type",NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST.toString());
		                queryData.add("gid",String.valueOf(gatewayId));
		                queryData.add("address",String.valueOf(address));
		                
		                println("Deleting NODE_ACTIVE_ENDPOINTS_REQUEST job for " + node.getName());
		                
		                restResource.path("network").path("jobs").queryParams(queryData).delete(ClientResponse.class);
		                
	                	NetworkEvent event = new NetworkEvent(NetworkEvent.EventKind.NODE_ENDPOINTS_ACQUIRED, gatewayId, address);
	                    try {
	                        ObjectMessage eventMessage = jmsSession.createObjectMessage(event);
	                        networkEventsProducer.send(eventMessage);
	                        println("Node " + node.getName() + " updated with endpoints information (" + Arrays.toString(activeEps.toArray()) + ")");
	                    } catch (Exception e) {
	                    	println("Node active endpoints registration message could not be dispatched to inbound packets topic");
	                    }
	                } else
	                	println("Node active endpoints information update failed");
	                
	        		
	        	} catch (Exception e) {
	        		e.printStackTrace();
	        	}
        	}
    	}
    }
    
}