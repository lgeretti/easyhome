package it.uniud.easyhome.processing;

import java.util.Arrays;
import java.util.List;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.Node;
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

public class NodeNeighRegistrationProcess extends Process {
	
	private MessageProducer networkEventsProducer = null;
	
    public NodeNeighRegistrationProcess(int pid, UriInfo uriInfo,ProcessKind kind) throws NamingException, JMSException {
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
        	
        	if (NodeNeighRspPacket.validates(pkt)) {
	        	println("NodeNeighRspPacket received from " + pkt.getSrcCoords());
	        	
	        	try {
	        		NodeNeighRspPacket neighPkt = new NodeNeighRspPacket(pkt);
	        		
	        		if (neighPkt.isSuccessful()) {
		        		List<Short> newNeighborAddresses = neighPkt.getNeighborAddresses();
		        			
		        		Node node;
		        		ClientResponse updateResponse;
		        		
		        		synchronized(nodesLock) {
			        		// FIXME : source coordinates are useless, must find another way
			        		node = restResource.path("network")
			        							.path(Byte.toString(neighPkt.getSrcCoords().getGatewayId())).path(Short.toString(neighPkt.getSrcCoords().getAddress()))
			        							.accept(MediaType.APPLICATION_JSON).get(Node.class);
			        		
			        		if (neighborsChanged(node, newNeighborAddresses)) {
			        			
			        			node.setNeighbors(newNeighborAddresses);
				        		
				                updateResponse = restResource.path("network").path("update")
				                		.type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node);
				                
			                	NetworkEvent event = new NetworkEvent(NetworkEvent.EventKind.NODE_NEIGHBORS_CHANGED, node.getGatewayId(), node.getAddress());
			                    try {
			                        ObjectMessage eventMessage = jmsSession.createObjectMessage(event);
			                        networkEventsProducer.send(eventMessage);
			                    } catch (JMSException ex) { }
				                
				                if (updateResponse.getClientResponseStatus() == Status.OK)
				                	println("Node '" + node.getName() + "' updated with neighbors information (" + Arrays.toString(newNeighborAddresses.toArray()) + ")");
				                else
				                	println("Node '" + node.getName() + "' neighbors information update failed");
			        		} else {
			        			println("Node '" + node.getName() + "' has unchanged neighbor information");
			        		}
		        		}
	        		}
	        	} catch (InvalidPacketTypeException e) {
	        		e.printStackTrace();
	        	}
        	}
    	}
    }
    
    private boolean neighborsChanged(Node node, List<Short> newNeighborAddresses) {
    	
    	List<Short> oldNeighborAddresses = node.getNeighborAddresses();
    	
    	if (oldNeighborAddresses.size() != newNeighborAddresses.size())
    		return true;
    	
    	for (Short oldNeighborAddress : oldNeighborAddresses) {
    		boolean found = false;
    		for (Short newNeighborAddress : newNeighborAddresses) {
    			if (newNeighborAddress.equals(oldNeighborAddress)) {
    				found = true;
    				break;
    			}
    		}
    		if (!found)
    			return true;
    	}
    	
    	return false;
    }
    
}