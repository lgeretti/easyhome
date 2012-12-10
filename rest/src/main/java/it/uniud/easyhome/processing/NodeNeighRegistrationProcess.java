package it.uniud.easyhome.processing;

import java.util.List;

import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.natives.NativePacket;
import it.uniud.easyhome.packets.natives.NodeNeighRspPacket;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.naming.NamingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;

public class NodeNeighRegistrationProcess extends Process {
	
    public NodeNeighRegistrationProcess(int pid, UriInfo uriInfo,ProcessKind kind) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind);
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
	        		NodeNeighRspPacket neigh = new NodeNeighRspPacket(pkt);
	        		
	        		List<Long> neighborIds = neigh.getNeighborIds();
	        		
	        		for (Long id: neighborIds) {
	        			
		        		Node neighbor = restResource.path("network/"+id)
		                		.accept(MediaType.APPLICATION_JSON).get(Node.class);
		        		neighbor.setNeighbors(neighborIds);

    	                ClientResponse updateResponse = restResource.path("network")
    	                		.type(MediaType.APPLICATION_JSON).post(ClientResponse.class,neighbor);
    	                
    	                if (updateResponse.getClientResponseStatus() == Status.OK) {
    	                	println("Node updated with neighbors information");
    	                } else
    	                	println("Node neighbors information update failed");
    	                
    	                break;
	        		}
	        		
	        	} catch (InvalidPacketTypeException e) {
	        		e.printStackTrace();
	        	}
        	}
    	}
    }
    
}