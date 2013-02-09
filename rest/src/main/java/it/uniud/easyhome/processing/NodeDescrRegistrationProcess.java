package it.uniud.easyhome.processing;

import java.util.List;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.exceptions.InvalidNodeDescException;
import it.uniud.easyhome.exceptions.InvalidNodeLogicalTypeException;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.packets.natives.NativePacket;
import it.uniud.easyhome.packets.natives.NodeAnncePacket;
import it.uniud.easyhome.packets.natives.NodeDescrRspPacket;
import it.uniud.easyhome.rest.RestPaths;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jettison.json.JSONException;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class NodeDescrRegistrationProcess extends Process {
	
	private MessageProducer networkEventsProducer = null;
	
    public NodeDescrRegistrationProcess(int pid, UriInfo uriInfo, ProcessKind kind) throws NamingException, JMSException {
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
        	
        	if (NodeDescrRspPacket.validates(pkt)) {
	        	println("NodeDescrRspPacket received from " + pkt.getSrcCoords());
	        	
	        	try {
	        		NodeDescrRspPacket descr = new NodeDescrRspPacket(pkt);
	        		
	        		if (descr.isSuccessful()) {
		        		byte gatewayId = descr.getSrcCoords().getGatewayId();
		        		short address = descr.getAddrOfInterest();
		        		
		        		ClientResponse nodeResponse = null;
		        		ClientResponse updateResponse = null;
		        		Node node = null;
		        		synchronized(nodesLock) {
			        		nodeResponse = restResource.path(RestPaths.NODES).path(Byte.toString(gatewayId)).path(Short.toString(address))
			                											   .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
			        		
			        		if (nodeResponse.getClientResponseStatus() == ClientResponse.Status.OK) {
				        		node = JsonUtils.getFrom(nodeResponse, Node.class);
			
			    				node.setLogicalType(descr.getLogicalType());
			    				node.setManufacturer(descr.getManufacturerCode());
			
				                updateResponse = restResource.path(RestPaths.NODES).path("update")
				                		.type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node);
			        		}
		        		}
		                if (nodeResponse.getClientResponseStatus() == ClientResponse.Status.OK) { 
		                	if (updateResponse.getClientResponseStatus() == ClientResponse.Status.OK) {
		                		
				                MultivaluedMap<String,String> queryData = new MultivaluedMapImpl();
				                queryData.add("type",NetworkJobType.NODE_DESCR_REQUEST.toString());
				                queryData.add("gatewayId",String.valueOf(gatewayId));
				                queryData.add("address",String.valueOf(address));
				                
				                restResource.path(RestPaths.JOBS).queryParams(queryData).delete(ClientResponse.class);
				                
				                if (descr.getLogicalType() == NodeLogicalType.END_DEVICE) {
					                MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
					                formData = new MultivaluedMapImpl();
					                formData.add("type",NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST.toString());
					                formData.add("gatewayId",Byte.toString(gatewayId));
					                formData.add("address",Short.toString(address));                
		
					                restResource.path(RestPaths.JOBS).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
				                } else if (descr.getLogicalType() == NodeLogicalType.ROUTER || descr.getLogicalType() == NodeLogicalType.COORDINATOR) {
				                	MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
					                formData.add("type",NetworkJobType.NODE_POWER_LEVEL_REQUEST.toString());
					                formData.add("gatewayId",Byte.toString(gatewayId));
					                formData.add("address",Short.toString(address));                
					                restResource.path(RestPaths.JOBS).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
					            }
				                
			                	NetworkEvent event = new NetworkEvent(NetworkEvent.EventKind.NODE_DESCR_ACQUIRED, 
			                											node.getCoordinates().getGatewayId(), node.getCoordinates().getAddress());
			                    try {
			                        ObjectMessage eventMessage = jmsSession.createObjectMessage(event);
			                        networkEventsProducer.send(eventMessage);
			                    } catch (JMSException ex) { 
			                    	println("Descriptor acquisition event dispatch failed");
			                    }
			                    
			                	println("Node " + node + " updated with logical type information " + descr.getLogicalType() + " and manufacturer " + descr.getManufacturerCode());

		                	} else
			                	println("Node " + node + " logical type information and manufacturer update failed");
	        			} else 
		        			println("Node " + Node.nameFor(gatewayId, address) + " not found, ignoring");
	        		}
	    	       
	        	} catch (InvalidPacketTypeException e) {
	        		e.printStackTrace();
	        	} catch (InvalidNodeLogicalTypeException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
        	}
    	}
    }
    
}