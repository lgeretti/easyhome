package it.uniud.easyhome.processing;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.natives.NativePacket;
import it.uniud.easyhome.packets.natives.NodeAnncePacket;

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
	        	
	        	println("Packet bytes: " + ByteUtils.printBytes(pkt.getBytes()));
	        	println("OpData bytes: " + ByteUtils.printBytes(pkt.getOperation().getData()));
	        	try {
	        		NodeAnncePacket announce = new NodeAnncePacket(pkt);
	        		
	        		long nuid = announce.getAnnouncedNuid();
	        		byte gatewayId = announce.getSrcCoords().getGatewayId();
	        		short address = announce.getAnnouncedAddress();
	        		byte capability = announce.getAnnouncedCapability();
	        		
	                Node.Builder nodeBuilder = new Node.Builder(nuid);
	                Node node = nodeBuilder.setGatewayId(gatewayId)
	                					   .setAddress(address)
	                					   .setCapability(capability)
	                					   .build();
	                
	                ClientResponse response = restResource.path("network")
	                		.type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node);
	                
	                if (response.getClientResponseStatus() == Status.CREATED) {
	                	
	                	NetworkEvent event = new NetworkEvent(NetworkEvent.EventKind.NODE_ADDED, gatewayId, nuid);
	                    try {
	                        ObjectMessage eventMessage = jmsSession.createObjectMessage(event);
	                        networkEventsProducer.send(eventMessage);
	                        println("Node " + ByteUtils.printBytes(nuid) + " at " + ByteUtils.printBytes(address) +
	                        		" announcement registered and event dispatched");
	                    } catch (Exception e) {
	                    	println("Message could not be dispatched to inbound packets topic");
	                    }
	                	
	                } else if (response.getClientResponseStatus() == Status.OK) {
	                	
	                	NetworkEvent event = new NetworkEvent(NetworkEvent.EventKind.NODE_ADDED, gatewayId, nuid);
	                    try {
	                        ObjectMessage eventMessage = jmsSession.createObjectMessage(event);
	                        networkEventsProducer.send(eventMessage);
	                        println("Node " + ByteUtils.printBytes(nuid) + 
	                        		" announcement re-registered (" + ByteUtils.printBytes(address) + ") and event dispatched");
	                    } catch (Exception e) {
	                    	println("Message could not be dispatched to inbound packets topic");
	                    }
	                	
	                } else
	                	println("Node announcement registration failed");
	                
	                
	        	} catch (InvalidPacketTypeException ex) {
	        		ex.printStackTrace();
	        		return;
	        	}
        	}
    	}
    }
    
}