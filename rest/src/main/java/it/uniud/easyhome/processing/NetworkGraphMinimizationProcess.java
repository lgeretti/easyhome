package it.uniud.easyhome.processing;

import java.util.Date;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.common.LogLevel;
import it.uniud.easyhome.exceptions.NodeNotFoundException;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.NetworkJob;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.packets.natives.ActiveEndpointsReqPacket;
import it.uniud.easyhome.packets.natives.NodeDescrReqPacket;
import it.uniud.easyhome.packets.natives.NodePowerLevelReqPacket;
import it.uniud.easyhome.packets.natives.NodePowerLevelSetIssuePacket;
import it.uniud.easyhome.packets.natives.SimpleDescrReqPacket;
import it.uniud.easyhome.rest.RestPaths;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Topic;
import javax.naming.NamingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jettison.json.JSONException;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class NetworkGraphMinimizationProcess extends Process {
	
	private static final long MAXIMIZATION_PERIOD_MS = 20000;
	private static final long MINIMIZATION_PERIOD_MS = 2*NetworkUpdateProcess.KEEP_LINK_ALIVE_MS;
	private static final short MAX_PL_SET_ISS_RETRIES = 3;
	private static final short MAX_PL_SET_ACK_RETRIES = 3;
	private static final long ACKNOWLEDGE_WAIT_GRANULARITY_MS = 1000;
	
	private MessageConsumer networkEventsConsumer = null;
	
    public NetworkGraphMinimizationProcess(int pid, UriInfo uriInfo,ProcessKind kind,LogLevel logLevel) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind,logLevel);
        Topic networkEventsTopic = (Topic) jndiContext.lookup(JMSConstants.NETWORK_EVENTS_TOPIC);
        networkEventsConsumer = registerConsumerFor(networkEventsTopic);
    }
    
    private void requestPowerLevelChange(byte gatewayId, short address, byte powerLevel) throws JMSException, JSONException, InterruptedException, NodeNotFoundException {
    	
    	short issueRetryCount = 0;
    	
    	Node node = null;
    	
		log(LogLevel.FINE, "Trying to set power level " + powerLevel + " for node " + Node.nameFor(gatewayId, address));
    	
    	while (issueRetryCount < MAX_PL_SET_ISS_RETRIES) {
	        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
	        formData.add("gatewayId",Byte.toString(gatewayId));
	        formData.add("address",Short.toString(address));
	        formData.add("powerLevel",Byte.toString(powerLevel));
	        ClientResponse changeIssueResponse = restResource.path("admin").path("changePower").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        
        	if (changeIssueResponse.getClientResponseStatus() == ClientResponse.Status.NOT_FOUND)
        		throw new NodeNotFoundException();

	        short acknowledgeRetryCount = 0;
	        
	        while (acknowledgeRetryCount < MAX_PL_SET_ACK_RETRIES) {
	        	
	        	Thread.sleep(ACKNOWLEDGE_WAIT_GRANULARITY_MS);
	        	
	        	ClientResponse nodeResponse = restResource.path(RestPaths.NODES).path(Byte.toString(gatewayId)).path(Short.toString(address))
	        									.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	        	if (nodeResponse.getClientResponseStatus() == ClientResponse.Status.NOT_FOUND)
	        		throw new NodeNotFoundException();
	        	
	        	node = JsonUtils.getFrom(nodeResponse, Node.class);
	        	if (node.getPowerLevel() == powerLevel) {
	        		log(LogLevel.FINE, "Successfully set power level for node " + node);
	        		break;
	        	}
	        	
	        	acknowledgeRetryCount++;
	        }
	        
	        if (node != null && node.getPowerLevel() == powerLevel)
	        	break;
	        
	        issueRetryCount++;
    	}
    }
    
    
    @Override
	protected void process() throws JMSException, NamingException {
    	
		try {
        	ObjectMessage msg = (ObjectMessage) networkEventsConsumer.receive();
        	if (msg != null) {
        		NetworkEvent event = (NetworkEvent) msg.getObject();
        		if (event != null && event.getKind() == NetworkEvent.EventKind.NETWORK_GRAPH_MINIMIZATION) {
        			
        			maximizeNetworkGraph();
        			minimizeNetworkGraph();
        		}
           	}   
        } catch (InterruptedException e) {
        	log(LogLevel.FINE, "Network graph minimization interrupted");
        } catch (NodeNotFoundException e) {
        	log(LogLevel.FINE, "Network graph minimization aborted due to a missing node during the procedure");
        } catch (Exception e) {
        	e.printStackTrace();
        	log(LogLevel.FINE, "Network graph minimization failure");
        }
    	
		
    }
    
    private void maximizeNetworkGraph() throws InterruptedException, JMSException, NodeNotFoundException, JSONException {

    	log(LogLevel.INFO, "Network graph maximization phase started");
    	
    	while(true) {
    		
        	ClientResponse getResponse = restResource.path(RestPaths.NODES).path("infrastructural").accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        	List<Node> nodes = JsonUtils.getListFrom(getResponse, Node.class);
    	
    		Node nodeToMaximize = null;
    		for (Node node: nodes) {
    			if (node.getPowerLevel() != 4) {
    				nodeToMaximize = node; 
    				break;
    			}
    		}
    		
    		if (nodeToMaximize == null)
    			break;
    		
        	requestPowerLevelChange(nodeToMaximize.getCoordinates().getGatewayId(),nodeToMaximize.getCoordinates().getAddress(),(byte)4);

    		Thread.sleep(MAXIMIZATION_PERIOD_MS);
    	
    	};
    	
    	log(LogLevel.INFO, "Network graph maximization phase completed");
    }
    
    private void minimizeNetworkGraph() throws InterruptedException, JMSException, NodeNotFoundException, JSONException {
    	
    	log(LogLevel.INFO, "Network graph minimization phase started");
    	
    	ClientResponse getResponse = restResource.path(RestPaths.NODES).path("infrastructural").accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
    	List<Node> nodes = JsonUtils.getListFrom(getResponse, Node.class);
    	
    	int numNodes = nodes.size();
    	
    	Queue<MinimizationEntry> entries = new ConcurrentLinkedQueue<MinimizationEntry>();
    	for (Node node : nodes)
    		entries.add(new MinimizationEntry(node.getCoordinates().getGatewayId(),node.getCoordinates().getAddress(),node.getPowerLevel()));
    	
    	while (!entries.isEmpty()) {
    		
    		MinimizationEntry entry = entries.poll();
    		
    		log(LogLevel.FINE, "Trying to reduce " + entry);
    		
    		entry.reducePowerLevel();
    		requestPowerLevelChange(entry.getGatewayId(),entry.getAddress(),entry.getPowerLevel());
    		
    		Thread.sleep(MINIMIZATION_PERIOD_MS);
    		
        	getResponse = restResource.path(RestPaths.NODES).path("infrastructural").accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        	nodes = JsonUtils.getListFrom(getResponse, Node.class);
        	
        	if (nodes.size() < numNodes) {
        		
        		log(LogLevel.FINE, "Reduction resulted in reduced graph size, thus restoring power level and discarding the node");
        		entry.increasePowerLevel();
        		requestPowerLevelChange(entry.getGatewayId(),entry.getAddress(),entry.getPowerLevel());
        		
        	} else {

            	if (entry.getPowerLevel() > 0) {
            		entries.offer(entry);
            		log(LogLevel.FINE, "Reduced successfully");
            	} else
            		log(LogLevel.FINE, "Reduced to zero successfully, hence discarding the node");
        	}
    	}
    	
    	log(LogLevel.INFO, "Network graph minimization phase completed");
    }
    
    private class MinimizationEntry {
    	
    	private byte gatewayId;
    	private short address;
    	private byte powerLevel;
    	
    	public MinimizationEntry(byte gatewayId, short address, byte powerLevel) {
    		this.gatewayId = gatewayId;
    		this.address = address;
    		this.powerLevel = powerLevel;
    	}
    	
    	public byte getGatewayId() {
    		return gatewayId;
    	}
    	
    	public short getAddress() {
    		return address;
    	}
    	
    	public byte getPowerLevel() {
    		return powerLevel;
    	}
    	
    	public void reducePowerLevel() {
    		--powerLevel;
    	}

    	public void increasePowerLevel() {
    		--powerLevel;
    	}
    	
    	public String toString() {
    		
    		StringBuilder strb = new StringBuilder();
    		
    		strb.append(gatewayId)
    			.append(":")
    			.append(Integer.toHexString(0xFFFF & address))
    			.append("@")
    			.append(powerLevel);
    		
    		return strb.toString();
    	}

    }
    
}