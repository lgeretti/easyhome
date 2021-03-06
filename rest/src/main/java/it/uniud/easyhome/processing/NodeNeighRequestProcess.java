package it.uniud.easyhome.processing;

import java.util.List;

import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.common.LogLevel;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.packets.natives.NodeNeighReqPacket;
import it.uniud.easyhome.rest.RestPaths;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.naming.NamingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class NodeNeighRequestProcess extends Process {
	
	public static long NEIGH_REQUEST_PERIOD_MS = 7000;
	
	private int nodeIdx = 0;
	
	private byte sequenceNumber = 0;
	
    public NodeNeighRequestProcess(int pid, UriInfo uriInfo,ProcessKind kind, LogLevel logLevel) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind,logLevel);
    }
    
    @Override
	protected void process() throws JMSException, NamingException {

    	try {
	    	ClientResponse getResponse = restResource.path(RestPaths.NODES).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	        	        
	    	List<Node> nodes = JsonUtils.getListFrom(getResponse, Node.class);
	    
	    	if (!nodes.isEmpty()) {
		    	nodeIdx = ((nodeIdx+1) >= nodes.size()  ? 0 : nodeIdx+1);
		
		    	Node node = nodes.get(nodeIdx);
		    	
		    	if (node.getLogicalType() == NodeLogicalType.ROUTER || node.getLogicalType() == NodeLogicalType.COORDINATOR) {
		    		
		    		sequenceNumber++;
		    		
		            MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
		            formData.add("type",NetworkJobType.NODE_NEIGH_REQUEST.toString());
		            formData.add("gatewayId",Byte.toString(node.getCoordinates().getGatewayId()));
		            formData.add("address",Short.toString(node.getCoordinates().getAddress()));
		            formData.add("tsn",Byte.toString(sequenceNumber));
		            
		            restResource.path(RestPaths.JOBS).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
		    		
			    	NodeNeighReqPacket packet = new NodeNeighReqPacket(node.getCoordinates(),sequenceNumber);
			 	    ObjectMessage outboundMessage = jmsSession.createObjectMessage(packet);
			    	getOutboundPacketsProducer().send(outboundMessage);    
			    	log(LogLevel.INFO, "Node '" + node.getName() + "' neighbours request dispatched");
		    	} 
	    	}
			Thread.sleep(NEIGH_REQUEST_PERIOD_MS);
	    	
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }
    
}