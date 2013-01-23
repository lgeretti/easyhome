package it.uniud.easyhome.processing;

import java.util.Arrays;
import java.util.List;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.Neighbor;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.NetworkJob;
import it.uniud.easyhome.network.NetworkJobType;
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
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jettison.json.JSONException;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.core.util.MultivaluedMapImpl;

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
		        		List<Neighbor> newNeighbors = neighPkt.getNeighbors();
		        		
		        		byte tsn = neighPkt.getOperation().getSequenceNumber();	
		        		
		                MultivaluedMap<String,String> queryData = new MultivaluedMapImpl();
		                queryData.add("type",NetworkJobType.NODE_NEIGH_REQUEST.toString());
		                queryData.add("tsn",Byte.toString(tsn));
		                
		                ClientResponse jobResponse = restResource.path("network").path("jobs").queryParams(queryData)
		                							.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		                List<NetworkJob> jobs = JsonUtils.getListFrom(jobResponse, NetworkJob.class);
		                
		                if (!jobs.isEmpty()) {
		                	
		                	NetworkJob job = jobs.get(0);

			                byte gatewayId = job.getGatewayId();
			                short address = job.getAddress();
		                	
			                queryData = new MultivaluedMapImpl();
			                queryData.add("type",NetworkJobType.NODE_NEIGH_REQUEST.toString());
			                queryData.add("gid",String.valueOf(gatewayId));
			                queryData.add("address",String.valueOf(address));
			                
			                restResource.path("network").path("jobs").queryParams(queryData).delete(ClientResponse.class);
			        		
			        		Node node;
			        		ClientResponse updateResponse;
			        		
			        		synchronized(nodesLock) {
				        		node = restResource.path("network")
				        							.path(Byte.toString(gatewayId)).path(Short.toString(address))
				        							.accept(MediaType.APPLICATION_JSON).get(Node.class);
				        		
				        		if (neighborsChanged(node, newNeighbors)) {
				        			
				        			node.setNeighbors(newNeighbors);
					        		
					                updateResponse = restResource.path("network").path("update")
					                		.type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node);
					                
				                	NetworkEvent event = new NetworkEvent(NetworkEvent.EventKind.NODE_NEIGHBORS_CHANGED, node.getGatewayId(), node.getAddress());
				                    try {
				                        ObjectMessage eventMessage = jmsSession.createObjectMessage(event);
				                        networkEventsProducer.send(eventMessage);
				                    } catch (JMSException ex) { }
					                
					                if (updateResponse.getClientResponseStatus() == Status.OK)
					                	println("Node '" + node.getName() + "' updated with neighbors information");
					                else
					                	println("Node '" + node.getName() + "' neighbors information update failed");
				        		} else {
				        			println("Node '" + node.getName() + "' has unchanged neighbor information");
				        		}
			        		}
		                }
	        		}
	        	} catch (InvalidPacketTypeException e) {
	        		e.printStackTrace();
	        	} catch (JSONException e) {
					e.printStackTrace();
				}
        	}
    	}
    }
    
    private boolean neighborsChanged(Node node, List<Neighbor> newNeighbors) {
    	
    	List<Neighbor> oldNeighbors = node.getNeighbors();
    	
    	if (oldNeighbors.size() != newNeighbors.size())
    		return true;
    	
    	for (Neighbor oldNeighbor : oldNeighbors) {
    		boolean found = false;
    		for (Neighbor newNeighborAddress : newNeighbors) {
    			if (newNeighborAddress.equals(oldNeighbor)) {
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