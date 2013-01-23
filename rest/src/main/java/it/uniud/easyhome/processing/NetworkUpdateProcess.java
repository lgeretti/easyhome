package it.uniud.easyhome.processing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.NodeCompactCoordinates;
import it.uniud.easyhome.packets.natives.NodeDescrReqPacket;
import it.uniud.easyhome.packets.natives.SimpleDescrReqPacket;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Topic;
import javax.naming.NamingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.sun.jersey.api.client.ClientResponse;

public class NetworkUpdateProcess extends Process {
	
	private MessageConsumer networkEventsConsumer = null;
	
    public NetworkUpdateProcess(int pid, UriInfo uriInfo,ProcessKind kind) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind);
        Topic networkEventsTopic = (Topic) jndiContext.lookup(JMSConstants.NETWORK_EVENTS_TOPIC);
        networkEventsConsumer = registerConsumerFor(networkEventsTopic);
    }
    
    @Override
	protected void process() throws JMSException, NamingException {

    	ObjectMessage msg = (ObjectMessage) networkEventsConsumer.receive();
    	if (msg != null) {
    		NetworkEvent event = (NetworkEvent) msg.getObject();
    		if (event != null && event.getKind() == NetworkEvent.EventKind.NODE_NEIGHBORS_CHANGED) {

    	        try {
    	        	
    	        	restResource.path("network").path("prune").type(MediaType.APPLICATION_JSON).post();
    	        	
    	        	ClientResponse missingNodesCoordsResponse = restResource.path("network").path("missingcoords").accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	                List<NodeCompactCoordinates> missingNodesCoords = JsonUtils.getListFrom(missingNodesCoordsResponse, NodeCompactCoordinates.class);
	                
	                println("Missing nodes: " + Arrays.toString(missingNodesCoords.toArray()));
	                
	                for (NodeCompactCoordinates missingNodeCoord : missingNodesCoords) {
	                	
	                	
	                }
	                
    	        } catch (Exception e) {
    	        	e.printStackTrace();
    	        }
    		}
       	}
    }
    
}