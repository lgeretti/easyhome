package it.uniud.easyhome.processing;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.devices.HomeAutomationDevice;
import it.uniud.easyhome.exceptions.InvalidNodeDescException;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.natives.NativePacket;
import it.uniud.easyhome.packets.natives.NodeAnncePacket;
import it.uniud.easyhome.packets.natives.NodeDescrRspPacket;
import it.uniud.easyhome.packets.natives.SimpleDescrRspPacket;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jettison.json.JSONException;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;

public class SimpleDescrRegistrationProcess extends Process {
	
	private MessageProducer networkEventsProducer = null;
	
    public SimpleDescrRegistrationProcess(int pid, UriInfo uriInfo, ProcessKind kind) throws NamingException, JMSException {
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
        	
        	if (SimpleDescrRspPacket.validates(pkt)) {
	        	println("SimpleDescrRspPacket received from " + pkt.getSrcCoords());
	        	
	        	try {
	        		SimpleDescrRspPacket descr = new SimpleDescrRspPacket(pkt);
	        		
	        		byte gid = descr.getSrcCoords().getGatewayId();
	        		short address = descr.getAddrOfInterest();
	        		byte endpoint = descr.getEndpoint();
	        		
	        		HomeAutomationDevice device = descr.getDevice();
	        		
	        		Node node;
	        		ClientResponse updateResponse;
	        		
	        		synchronized(nodesLock) {
		        		ClientResponse nodeResponse = restResource.path("network").path(Byte.toString(gid)).path(Short.toString(address))
		                		.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		        		node = JsonUtils.getFrom(nodeResponse, Node.class);

	    				node.addDevice(endpoint, device);
	    				
		                updateResponse = restResource.path("network").path("update")
		                		.type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node);	        			
	        		}
	                
	                if (updateResponse.getClientResponseStatus() == Status.OK) {
	                	
	                	NetworkEvent event = new NetworkEvent(NetworkEvent.EventKind.SIMPLE_DESCR_ACQUIRED, node.getGatewayId(), node.getAddress(), endpoint);
	                    try {
	                        ObjectMessage eventMessage = jmsSession.createObjectMessage(event);
	                        networkEventsProducer.send(eventMessage);
	                    } catch (JMSException ex) { }
	                	
	                	println("Node '" + node.getName() + "' updated with device information for endpoint " + endpoint);
	                } else
	                	println("Node '" + node.getName() + "' device information update failed for endpoint " + endpoint);

	        	} catch (InvalidPacketTypeException e) {
	        		e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
        	}
    	}
    }
    
}