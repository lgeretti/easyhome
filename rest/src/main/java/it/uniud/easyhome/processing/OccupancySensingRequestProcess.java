package it.uniud.easyhome.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.common.LogLevel;
import it.uniud.easyhome.devices.HomeAutomationDevice;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.packets.natives.NodeDiscoveryReqPacket;
import it.uniud.easyhome.packets.natives.NodeNeighReqPacket;
import it.uniud.easyhome.packets.natives.OccupancyAttributeReqPacket;
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

public class OccupancySensingRequestProcess extends Process {
	
	public static long REQUEST_PERIOD_MS = 5000;
	
	private int nodeIdx = 0;
	
	private byte sequenceNumber = 0;
	
    public OccupancySensingRequestProcess(int pid, UriInfo uriInfo,ProcessKind kind, LogLevel logLevel) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind,logLevel);
    }
    
    @Override
	protected void process() throws JMSException, NamingException {

    	try {
	    	ClientResponse getResponse = restResource.path(RestPaths.NODES).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	        	        
	    	List<Node> nodes = JsonUtils.getListFrom(getResponse, Node.class);
	    
	    	List<NodeEndpoint> occupancyEndpoints = getOccupancyEndpoints(nodes);
	    	
	    	if (!occupancyEndpoints.isEmpty()) {
		    	nodeIdx = ((nodeIdx+1) >= occupancyEndpoints.size()  ? 0 : nodeIdx+1);
		
		    	Node node = occupancyEndpoints.get(nodeIdx).node;
		    		
	    		sequenceNumber++;
	    		
	    		OccupancyAttributeReqPacket packet = new OccupancyAttributeReqPacket(node.getCoordinates(),occupancyEndpoints.get(nodeIdx).endpoint,sequenceNumber);
		 	    ObjectMessage outboundMessage = jmsSession.createObjectMessage(packet);
		    	getOutboundPacketsProducer().send(outboundMessage);    
		    	log(LogLevel.INFO, "Occupancy attribute request for " + node + " dispatched");
		    	Thread.sleep(REQUEST_PERIOD_MS/nodes.size());
	    	}
			Thread.sleep(REQUEST_PERIOD_MS);
	    	
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }
    
    private List<NodeEndpoint> getOccupancyEndpoints(List<Node> nodes) {
 
    	List<NodeEndpoint> result = new ArrayList<NodeEndpoint>();
    	
    	for (Node node : nodes) {
	    	Map<Byte,HomeAutomationDevice> devices = node.getMappedDevices();
	    	
	    	for (Entry<Byte,HomeAutomationDevice> device : devices.entrySet()) {
	    		if (device.getValue() == HomeAutomationDevice.OCCUPANCY_SENSOR) {
	    			result.add(new NodeEndpoint(node,device.getKey()));
	    			break;
	    		}
	    	}
    	}
    	
    	return result;
    }
    
    private class NodeEndpoint {
    	
    	public Node node;
    	public byte endpoint;
    	
    	public NodeEndpoint(Node node, byte endpoint) {
    		this.node = node;
    		this.endpoint = endpoint;
    	}
    	
    }
    
}