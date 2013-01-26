package it.uniud.easyhome.processing;

import java.util.List;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.packets.natives.NodeDescrReqPacket;
import it.uniud.easyhome.packets.natives.NodeDiscoveryReqPacket;
import it.uniud.easyhome.packets.natives.NodeNeighReqPacket;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Topic;
import javax.naming.NamingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class NodeDiscoveryRequestProcess extends Process {
	
	public static long DISCOVERY_REQUEST_PERIOD_MS = 10000;
	
	private int nodeIdx = 0;
	
	private byte sequenceNumber = 0;
	
    public NodeDiscoveryRequestProcess(int pid, UriInfo uriInfo,ProcessKind kind) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind);
    }
    
    @Override
	protected void process() throws JMSException, NamingException {

    	try {
    		
	    	ClientResponse getResponse = restResource.path("network").path("infrastructural").accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	        	        
	    	List<Node> nodes = JsonUtils.getListFrom(getResponse, Node.class);
	    
	    	if (!nodes.isEmpty()) {
		    	nodeIdx = ((nodeIdx+1) >= nodes.size()  ? 0 : nodeIdx+1);
		
		    	Node node = nodes.get(nodeIdx);
		    	
	    		sequenceNumber++;
	    		
	    		NodeDiscoveryReqPacket packet = new NodeDiscoveryReqPacket(node,sequenceNumber);
		 	    ObjectMessage outboundMessage = jmsSession.createObjectMessage(packet);
		    	getOutboundPacketsProducer().send(outboundMessage);    
		    	println("Node " + node.getName() + " discovery request dispatched");
		    	Thread.sleep(DISCOVERY_REQUEST_PERIOD_MS/nodes.size());
	    	} else {
	    		Thread.sleep(1000);
	    	}
	    	
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }
    
}