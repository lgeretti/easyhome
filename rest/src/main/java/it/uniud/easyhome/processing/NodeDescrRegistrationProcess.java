package it.uniud.easyhome.processing;

import java.util.List;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.exceptions.InvalidNodeDescException;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.natives.NativePacket;
import it.uniud.easyhome.packets.natives.NodeAnncePacket;
import it.uniud.easyhome.packets.natives.NodeDescrRspPacket;

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
	        		
	        		byte gid = descr.getSrcCoords().getGatewayId();
	        		short address = descr.getAddrOfInterest();
	        		
	        		ClientResponse nodesListResponse = restResource.path("network")
	                		.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	        		List<Node> nodes = JsonUtils.getListFrom(nodesListResponse, Node.class);
	        		
	        		for (Node node: nodes) {
	        			if (node.getGatewayId() == gid && node.getAddress() == address) {
	        				node.setLogicalType(descr.getLogicalType());
	        				node.setManufacturer(descr.getManufacturerCode());

	    	                ClientResponse updateResponse = restResource.path("network")
	    	                		.type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node);
	    	                
	    	                if (updateResponse.getClientResponseStatus() == Status.OK) {
	    	                	
	    		                MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
	    		                formData.add("type",NetworkJobType.NODE_DESCR_REQUEST.toString());
	    		                formData.add("gid",String.valueOf(gid));
	    		                formData.add("nuid",String.valueOf(node.getId()));
	    		                formData.add("address",String.valueOf(address));
	    		                
	    		                /*
	    		                println("Deleting NODE_DESCR_REQUEST job for " + gid + ", " + node.getId() + ", " + address);
	    		                
	    		                restResource.path("network").path("jobs").path("delete")
	    		                		.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	                	*/
	    	                	NetworkEvent event = new NetworkEvent(NetworkEvent.EventKind.NODE_DESCR_ACQUIRED, node.getGatewayId(), node.getId());
	    	                    try {
	    	                        ObjectMessage eventMessage = jmsSession.createObjectMessage(event);
	    	                        networkEventsProducer.send(eventMessage);
	    	                    } catch (JMSException ex) { }
	    	                    
	    	                	println("Node updated with logical type information " + descr.getLogicalType() + " and manufacturer " + descr.getManufacturerCode());
	    	                } else
	    	                	println("Node logical type information and manufacturer update failed");
	    	                
	    	                break;
	        			}
	        		}
	                

	        	} catch (InvalidPacketTypeException e) {
	        		e.printStackTrace();
	        	} catch (InvalidNodeDescException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
        	} else {
        		println("The packet does not validate");
        	}
    	}
    }
    
}