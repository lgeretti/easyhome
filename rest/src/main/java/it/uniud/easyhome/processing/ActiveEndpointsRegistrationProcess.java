package it.uniud.easyhome.processing;

import java.util.Arrays;
import java.util.List;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.common.LogLevel;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.NetworkJob;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.ResponseStatus;
import it.uniud.easyhome.packets.natives.ActiveEndpointsRspPacket;
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

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.xml.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

public class ActiveEndpointsRegistrationProcess extends Process {
	
	private MessageProducer networkEventsProducer = null;
	
    public ActiveEndpointsRegistrationProcess(int pid, UriInfo uriInfo,ProcessKind kind, LogLevel logLevel) throws NamingException, JMSException {
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
        	
        	if (ActiveEndpointsRspPacket.validates(pkt)) {
	        	log(LogLevel.FINE,"ActiveEndpointsRspPacket received from " + pkt.getSrcCoords());
	        	
	        	try {
	        		ActiveEndpointsRspPacket activeEpPkt = new ActiveEndpointsRspPacket(pkt);
	        		
	        		if (activeEpPkt.getStatus() == ResponseStatus.SUCCESS) {
	        			
		        		List<Byte> activeEps = activeEpPkt.getActiveEndpoints();
		        		
		        		byte gatewayId = activeEpPkt.getSrcCoords().getGatewayId();
		        		short address = activeEpPkt.getAddrOfInterest();
		                
		        		Node node = null;
		        		ClientResponse nodeResponse = null;
		        		ClientResponse updateResponse = null;
		        		
		        		synchronized(nodesLock) {
			        		nodeResponse = restResource.path(RestPaths.NODES).path(Byte.toString(gatewayId)).path(Short.toString(address))
			        										.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
			        		if (nodeResponse.getClientResponseStatus() == ClientResponse.Status.OK) {
			        			
			        			node = JsonUtils.getFrom(nodeResponse, Node.class);
				        		node.setEndpoints(activeEps);
				        		
				                updateResponse = restResource.path(RestPaths.NODES).path("update")
				                		.type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node);		
			        		}
		        		}
		                if (nodeResponse.getClientResponseStatus() == ClientResponse.Status.OK) {
		                	if (updateResponse.getClientResponseStatus() == Status.OK) {
		                	
				                MultivaluedMap<String,String> queryData = new MultivaluedMapImpl();
				                queryData.add("type",NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST.toString());
				                queryData.add("gatewayId",String.valueOf(gatewayId));
				                queryData.add("address",String.valueOf(address));
				                
				                restResource.path(RestPaths.JOBS).queryParams(queryData).delete(ClientResponse.class);
			                	
			                    try {
			                    	if (activeEps.size() > 0) {
			                    		NetworkEvent event = new NetworkEvent(NetworkEvent.EventKind.NODE_ENDPOINTS_ACQUIRED, gatewayId, address);
				                        ObjectMessage eventMessage = jmsSession.createObjectMessage(event);
				                        networkEventsProducer.send(eventMessage);
			                    	}
			                        log(LogLevel.INFO,node + " updated with endpoints information (" + Arrays.toString(activeEps.toArray()) + ")");
			                    } catch (Exception e) {
			                    	log(LogLevel.FINE,"Active endpoints registration message for " + node + " could not be dispatched to inbound packets topic");
			                    }
			                } else 
			                	log(LogLevel.FINE,"Active endpoints information update for " + node + " failed");
		                } else
		                	log(LogLevel.FINE,"Node " + Node.nameFor(gatewayId, address) + " not found, ignoring");
	        		}
	        		
	        	} catch (Exception e) {
	        		e.printStackTrace();
	        	}
        	}
    	}
    }
    
}