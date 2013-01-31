package it.uniud.easyhome.processing;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.packets.natives.NativePacket;
import it.uniud.easyhome.packets.natives.NodeAnncePacket;

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

public class NodeAnnceRegistrationProcess extends Process {
	
	private MessageProducer networkEventsProducer = null;
	
    public NodeAnnceRegistrationProcess(int pid, UriInfo uriInfo, ProcessKind kind) throws NamingException, JMSException {
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
        	if (NodeAnncePacket.validates(pkt)) {
	        	println("NodeAnncePacket received from " + pkt.getSrcCoords());
	        	
	        	try {
	        		NodeAnncePacket announce = new NodeAnncePacket(pkt);
	        		
	        		byte gatewayId = announce.getSrcCoords().getGatewayId();
	        		long nuid = announce.getAnnouncedNuid();
	        		short address = announce.getAnnouncedAddress();

	                MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
	                formData.add("gid",Byte.toString(gatewayId));
	                formData.add("nuid",Long.toString(nuid));
	                formData.add("address",Short.toString(address));
	                
	                if (address == 0) {
	                	formData.add("logicalType", NodeLogicalType.COORDINATOR.toString());
	                }
	                
	                ClientResponse nodeInsertionResponse = restResource.path("network").path("insert")
	                		.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	                
	                formData = new MultivaluedMapImpl();
	                formData.add("type",NetworkJobType.NODE_DESCR_REQUEST.toString());
	                formData.add("gid",Byte.toString(gatewayId));
	                formData.add("address",Short.toString(address));
	                
	                ClientResponse jobInsertionResponse = restResource.path("network").path("jobs")
	                		.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	                
	                boolean jobInsertionsSuccessful = (jobInsertionResponse.getClientResponseStatus() == Status.CREATED);
	                
	                if (nodeInsertionResponse.getClientResponseStatus() == Status.CREATED && jobInsertionsSuccessful) {
	                	
	                	NetworkEvent event = new NetworkEvent(NetworkEvent.EventKind.NODE_ADDED, gatewayId, address);
	                    try {
	                        ObjectMessage eventMessage = jmsSession.createObjectMessage(event);
	                        networkEventsProducer.send(eventMessage);
	                        println("Node " + gatewayId + ":" + Integer.toHexString(0xFFFF & address) + " announcement registered and event dispatched");
	                    } catch (Exception e) {
	                    	println("Message could not be dispatched to inbound packets topic");
	                    }
	                	
	                } else if (nodeInsertionResponse.getClientResponseStatus() == Status.OK && jobInsertionsSuccessful) {
	                	
	                	NetworkEvent event = new NetworkEvent(NetworkEvent.EventKind.NODE_ADDED, gatewayId, address);
	                    try {
	                        ObjectMessage eventMessage = jmsSession.createObjectMessage(event);
	                        networkEventsProducer.send(eventMessage);
	                        println("Node " + gatewayId + ":" + Integer.toHexString(0xFFFF & address) + " announcement re-registered and event dispatched");
	                    } catch (Exception e) {
	                    	println("Message could not be dispatched to inbound packets topic");
	                    }
	                	
	                } else
	                	println("Node " + gatewayId + ":" + Integer.toHexString(0xFFFF & address) + " announcement registration failed");
	                
	                
	        	} catch (Exception ex) {
	        		ex.printStackTrace();
	        		return;
	        	}
        	}
    	}
    }
    
}