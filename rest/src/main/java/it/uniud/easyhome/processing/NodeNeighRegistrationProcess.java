package it.uniud.easyhome.processing;

import java.util.List;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.common.LogLevel;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.LocalCoordinates;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.NetworkJob;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.natives.NativePacket;
import it.uniud.easyhome.packets.natives.NodeNeighRspPacket;
import it.uniud.easyhome.rest.RestPaths;

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
	
    public NodeNeighRegistrationProcess(int pid, UriInfo uriInfo,ProcessKind kind, LogLevel logLevel) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind,logLevel);
        
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
	        	log(LogLevel.DEBUG, "NodeNeighRspPacket received from " + pkt.getSrcCoords());
	        	
	        	try {
	        		NodeNeighRspPacket neighPkt = new NodeNeighRspPacket(pkt);
	        		
	        		if (neighPkt.isSuccessful()) {
		        		List<LocalCoordinates> newNeighbors = neighPkt.getNeighbors();
		        		
		        		byte tsn = neighPkt.getOperation().getSequenceNumber();	
		        		
		                MultivaluedMap<String,String> queryData = new MultivaluedMapImpl();
		                queryData.add("type",NetworkJobType.NODE_NEIGH_REQUEST.toString());
		                queryData.add("tsn",Byte.toString(tsn));
		                
		                ClientResponse jobResponse = restResource.path(RestPaths.JOBS).queryParams(queryData)
		                							.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		                List<NetworkJob> jobs = JsonUtils.getListFrom(jobResponse, NetworkJob.class);
		                
		                if (!jobs.isEmpty()) {
		                	
		                	NetworkJob job = jobs.get(0);

			                byte gatewayId = job.getGatewayId();
			                short address = job.getAddress();
		                	
			                queryData = new MultivaluedMapImpl();
			                queryData.add("type",NetworkJobType.NODE_NEIGH_REQUEST.toString());
			                queryData.add("gatewayId",String.valueOf(gatewayId));
			                queryData.add("address",String.valueOf(address));
			                
			                restResource.path(RestPaths.JOBS).queryParams(queryData).delete(ClientResponse.class);
			        		
			        		Node node;
			        		ClientResponse updateResponse;
			        		
			        		synchronized(nodesLock) {
				        		node = restResource.path(RestPaths.NODES)
				        							.path(Byte.toString(gatewayId)).path(Short.toString(address))
				        							.accept(MediaType.APPLICATION_JSON).get(Node.class);
				        		
				        		if (neighborsChanged(node, newNeighbors)) {
				        			
				        			node.setNeighbors(newNeighbors);
					        		
					                updateResponse = restResource.path(RestPaths.NODES).path("update")
					                		.type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node);
					                
				                	NetworkEvent event = new NetworkEvent(NetworkEvent.EventKind.NODE_NEIGHBORS_CHANGED, 
				                								node.getCoordinates().getGatewayId(), node.getCoordinates().getAddress());
				                    try {
				                        ObjectMessage eventMessage = jmsSession.createObjectMessage(event);
				                        networkEventsProducer.send(eventMessage);
				                    } catch (JMSException ex) { }
					                
					                if (updateResponse.getClientResponseStatus() == Status.OK)
					                	log(LogLevel.INFO, node + " updated with neighbors information (#" + newNeighbors.size() + ")");
					                else
					                	log(LogLevel.DEBUG, node + " neighbors information update failed");
				        		} else {
				        			log(LogLevel.INFO, node + " has unchanged neighbor information (#" + newNeighbors.size() + ")");
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
    
    private boolean neighborsChanged(Node node, List<LocalCoordinates> newNeighbors) {
    	
    	List<LocalCoordinates> oldNeighbors = node.getNeighbors();
    	
    	if (oldNeighbors.size() != newNeighbors.size())
    		return true;
    	
    	for (LocalCoordinates oldNeighbor : oldNeighbors) {
    		boolean found = false;
    		for (LocalCoordinates newNeighborAddress : newNeighbors) {
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