package it.uniud.easyhome.processing;

import java.util.Arrays;
import java.util.List;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.exceptions.InvalidNodeLogicalTypeException;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.Manufacturer;
import it.uniud.easyhome.network.LocalCoordinates;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.NetworkJob;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.network.NodePersistentInfo;
import it.uniud.easyhome.packets.ResponseStatus;
import it.uniud.easyhome.packets.natives.NativePacket;
import it.uniud.easyhome.packets.natives.NodeDiscoveryRspPacket;
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

public class NodeDiscoveryRegistrationProcess extends Process {
	
	private MessageProducer networkEventsProducer = null;
	
    public NodeDiscoveryRegistrationProcess(int pid, UriInfo uriInfo,ProcessKind kind) throws NamingException, JMSException {
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
        	
        	if (NodeDiscoveryRspPacket.validates(pkt)) {
	        	println("NodeDiscoveryRspPacket received from " + pkt.getSrcCoords());
	        	
	        	try {
	        		NodeDiscoveryRspPacket discPkt = new NodeDiscoveryRspPacket(pkt);
	        		
	        		if (discPkt.getStatus() == ResponseStatus.SUCCESS) {
	        			byte gatewayId = discPkt.getSrcCoords().getGatewayId();
	        			short senderAddress = discPkt.getSenderAddress();
		        		long discNuid = discPkt.getNuid();
		        		short discAddress = discPkt.getAddrOfInterest();
		        		NodeLogicalType discLogicalType = discPkt.getLogicalType();
		        		Manufacturer discManufacturer = discPkt.getManufacturer();

		        		ClientResponse senderRetrievalResponse = restResource.path(RestPaths.NODES).path(Byte.toString(gatewayId)).path(Short.toString(senderAddress))
		        						.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		        		
		        		if (senderRetrievalResponse.getClientResponseStatus() == ClientResponse.Status.OK) {
		                
		        			Node sender = JsonUtils.getFrom(senderRetrievalResponse, Node.class);
		        			
			        		MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
			                formData.add("gid",Byte.toString(gatewayId));
			                formData.add("nuid",Long.toString(discNuid));
			                formData.add("address",Short.toString(discAddress));
			                formData.add("logicalType",discLogicalType.toString());
			                formData.add("manufacturer",discManufacturer.toString());
			                
			                if (discLogicalType == NodeLogicalType.END_DEVICE && sender.getLocation() != null) {
			                	formData.add("locationName",sender.getLocation().getName());
			                	formData.add("locationType",sender.getLocation().getType().toString());
			                }
			                
			                ClientResponse insertionResponse = restResource.path(RestPaths.NODES).path("insert")
			                									.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
			                
			                String insertionString = (ClientResponse.Status.CREATED == insertionResponse.getClientResponseStatus() ? " (inserted)" : " (updated)");
			                
			                if (ClientResponse.Status.CREATED == insertionResponse.getClientResponseStatus()) {
			                	
			                	if (discLogicalType == NodeLogicalType.END_DEVICE) {
					                formData = new MultivaluedMapImpl();
					                formData.add("type",NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST.toString());
					                formData.add("gid",Byte.toString(gatewayId));
					                formData.add("address",Short.toString(discAddress));
					                restResource.path(RestPaths.NODES).path(RestPaths.JOBS).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
					            } else if (discLogicalType == NodeLogicalType.ROUTER || discLogicalType == NodeLogicalType.COORDINATOR) {
					                formData = new MultivaluedMapImpl();
					                formData.add("type",NetworkJobType.NODE_POWER_LEVEL_REQUEST.toString());
					                formData.add("gid",Byte.toString(gatewayId));
					                formData.add("address",Short.toString(discAddress));                
					                restResource.path(RestPaths.NODES).path(RestPaths.JOBS).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
					            }
					                
			                	NetworkEvent event = new NetworkEvent(NetworkEvent.EventKind.NODE_DESCR_ACQUIRED,gatewayId, discAddress);
			                    try {
			                        ObjectMessage eventMessage = jmsSession.createObjectMessage(event);
			                        networkEventsProducer.send(eventMessage);
			                    } catch (JMSException ex) { }
			                	
			                }
			        		
			                println(sender + " discovered " 
			                		+ Long.toHexString(discNuid) + ":" + Integer.toHexString(0xFFFF & discAddress) + " of type " + discLogicalType + insertionString);
			                
			                formData = new MultivaluedMapImpl();
			                formData.add("gatewayId",Byte.toString(gatewayId));
			                formData.add("sourceNuid",Long.toString(sender.getCoordinates().getNuid()));
			                formData.add("sourceAddress",Short.toString(sender.getCoordinates().getAddress()));
			                formData.add("destinationNuid",Long.toString(discNuid));
			                formData.add("destinationAddress",Short.toString(discAddress));
			                
			                restResource.path(RestPaths.NODES).path(RestPaths.LINKS).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
			                
		        		} else 
		        			println("Sender node " + Node.nameFor(gatewayId, senderAddress) + " not found, hence discarding discovered node");
	        		}
	        	} catch (InvalidPacketTypeException e) {
	        		e.printStackTrace();
	        	} catch (InvalidNodeLogicalTypeException e) {
	        		// Never going to happen anyway
	        	} catch (Exception e) {
					e.printStackTrace();
				}
        	}	
    	}
    }

}