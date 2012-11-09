package it.uniud.easyhome.processing;

import java.util.List;

import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.exceptions.InvalidNodeDescException;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.NetworkEvent;
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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jettison.json.JSONException;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;

public class NodeDescrRegistrationProcess extends Process {
	
    public NodeDescrRegistrationProcess(int pid, UriInfo uriInfo, ProcessKind kind) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind);
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

	    	                ClientResponse updateResponse = restResource.path("network")
	    	                		.type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node);
	    	                
	    	                if (updateResponse.getClientResponseStatus() == Status.OK) {
	    	                	println("Node updated with logical type information");
	    	                } else
	    	                	println("Node logical type information update failed");
	    	                
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
        	}
    	}
    }
    
}