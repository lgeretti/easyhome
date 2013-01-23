package it.uniud.easyhome.processing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.NodeCoordinates;
import it.uniud.easyhome.packets.natives.NodeDescrReqPacket;
import it.uniud.easyhome.packets.natives.SimpleDescrReqPacket;

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

public class NetworkUpdateProcess extends Process {
	
	private MessageConsumer networkEventsConsumer = null;
	private MessageProducer networkEventsProducer = null;
	
    public NetworkUpdateProcess(int pid, UriInfo uriInfo,ProcessKind kind) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind);
        Topic networkEventsTopic = (Topic) jndiContext.lookup(JMSConstants.NETWORK_EVENTS_TOPIC);
        networkEventsConsumer = registerConsumerFor(networkEventsTopic);
        networkEventsProducer = registerProducerFor(networkEventsTopic);
    }
    
    @Override
	protected void process() throws JMSException, NamingException {

    	ObjectMessage msg = (ObjectMessage) networkEventsConsumer.receive();
    	if (msg != null) {
    		NetworkEvent event = (NetworkEvent) msg.getObject();
    		if (event != null && event.getKind() == NetworkEvent.EventKind.NODE_NEIGHBORS_CHANGED) {

    	        try {
    	        	
    	        	restResource.path("network").path("prune").type(MediaType.APPLICATION_JSON).post();
    	        	//restResource.path("network").path("relocalize").type(MediaType.APPLICATION_JSON).post();
    	        	
    	        	ClientResponse missingNodesCoordsResponse = restResource.path("network").path("missingcoords").accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	                List<NodeCoordinates> missingNodesCoords = JsonUtils.getListFrom(missingNodesCoordsResponse, NodeCoordinates.class);
	                
	                println("Missing nodes: " + Arrays.toString(missingNodesCoords.toArray()));
	                
	                for (NodeCoordinates missingNodeCoord : missingNodesCoords) {
	                	
		        		byte gatewayId = missingNodeCoord.getGatewayId();
		        		long nuid = missingNodeCoord.getNuid();
		        		short address = missingNodeCoord.getAddress();
	                	
		                MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
		                formData.add("gid",Byte.toString(gatewayId));
		                formData.add("nuid",Long.toString(nuid));
		                formData.add("address",Short.toString(address));
		                
		                restResource.path("network").path("insert").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
		                
		                formData = new MultivaluedMapImpl();
		                formData.add("type",NetworkJobType.NODE_DESCR_REQUEST.toString());
		                formData.add("gid",Byte.toString(gatewayId));
		                formData.add("address",Short.toString(address));
		                
		                restResource.path("network").path("jobs").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	                }
		                
		            if (!missingNodesCoords.isEmpty()) {
		            	
		            	byte gatewayId = missingNodesCoords.get(0).getGatewayId();
		        		short address = missingNodesCoords.get(0).getAddress();
		        		
	                	NetworkEvent outboundEvent = new NetworkEvent(NetworkEvent.EventKind.NODE_ADDED, gatewayId, address);
	                    try {
	                        ObjectMessage eventMessage = jmsSession.createObjectMessage(outboundEvent);
	                        networkEventsProducer.send(eventMessage);
	                        println(missingNodesCoords.size() + " missing nodes registered and event dispatched");
	                    } catch (Exception e) {
	                    	println("Message could not be dispatched to inbound packets topic");
	                    }	                	
	                }
	                
    	        } catch (Exception e) {
    	        	e.printStackTrace();
    	        }
    		}
       	}
    }
    
}