package it.uniud.easyhome.processing;

import java.util.Arrays;
import java.util.List;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.exceptions.InvalidNodeLogicalTypeException;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.Manufacturer;
import it.uniud.easyhome.network.Neighbor;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.NetworkJob;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.packets.natives.NativePacket;
import it.uniud.easyhome.packets.natives.NodeDiscoveryRspPacket;
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
	        		
	        		if (discPkt.isSuccessful()) {
		        		long discNuid = discPkt.getNuid();
		        		short discAddress = discPkt.getAddrOfInterest();
		        		byte gatewayId = discPkt.getSrcCoords().getGatewayId();
		        		NodeLogicalType discLogicalType = discPkt.getLogicalType();
		        		Manufacturer discManufacturer = discPkt.getManufacturer();
		        		
		        		Node discoveredNode = new Node.Builder(50,discNuid).setGatewayId(gatewayId).setAddress(discAddress).setLogicalType(discLogicalType).setManufacturer(discManufacturer).build();
		        		
		        		short senderAddress = discPkt.getSenderAddress();
		        		
		                println("Node " + gatewayId + ":" + Integer.toHexString(0xFFFFFFFF & senderAddress) + " discovered node " 
		                		+ Long.toHexString(discNuid) + ":" + Integer.toHexString(0xFFFFFFFF & discAddress) + " of type " + discLogicalType + " and manufacturer " + discManufacturer);
	        		}
	        	} catch (InvalidPacketTypeException e) {
	        		e.printStackTrace();
	        	} catch (InvalidNodeLogicalTypeException e) {
	        		
	        	}
	        		/*catch (JSONException e) {
	        	}
					e.printStackTrace();
				}*/
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