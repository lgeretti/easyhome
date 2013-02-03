package it.uniud.easyhome.processing;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
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
	
	private static final long MAXIMIZATION_PERIOD_MS = 10000;
	private static final long MINIMIZATION_PERIOD_MS = 10000;
	private static final short MAX_PL_SET_ISS_RETRIES = 2;
	private static final short MAX_PL_SET_ACK_RETRIES = 3;
	private static final long ACKNOWLEDGE_WAIT_GRANULARITY_MS = 1000;
	
	private MessageConsumer networkEventsConsumer = null;
	
    public NetworkGraphMinimizationProcess(int pid, UriInfo uriInfo,ProcessKind kind) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind);
        Topic networkEventsTopic = (Topic) jndiContext.lookup(JMSConstants.NETWORK_EVENTS_TOPIC);
        networkEventsConsumer = registerConsumerFor(networkEventsTopic);
    }
    
    private void requestPowerLevelChange(byte gatewayId, short address, byte powerLevel) throws JMSException, JSONException, InterruptedException, NodeNotFoundException {
    	
    	short issueRetryCount = 0;
    	
    	Node node = null;
    	
		println("Trying to set power level " + powerLevel + " for node " + gatewayId + ":" + Integer.toHexString(0xFFFF & address));
    	
    	while (issueRetryCount < MAX_PL_SET_ISS_RETRIES) {
	        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
	        formData.add("gid",Byte.toString(gatewayId));
	        formData.add("address",Short.toString(address));
	        formData.add("powerLevel",Byte.toString(powerLevel));
	        ClientResponse changeIssueResponse = restResource.path("ui").path("changePower").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        
        	if (changeIssueResponse.getClientResponseStatus() == ClientResponse.Status.NOT_FOUND)
        		throw new NodeNotFoundException();

	        short acknowledgeRetryCount = 0;
	        
	        while (acknowledgeRetryCount < MAX_PL_SET_ACK_RETRIES) {
	        	
	        	Thread.sleep(ACKNOWLEDGE_WAIT_GRANULARITY_MS);
	        	
	        	ClientResponse nodeResponse = restResource.path("network").path(Byte.toString(gatewayId)).path(Short.toString(address))
	        									.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	        	if (nodeResponse.getClientResponseStatus() == ClientResponse.Status.NOT_FOUND)
	        		throw new NodeNotFoundException();
	        	
	        	node = JsonUtils.getFrom(nodeResponse, Node.class);
	        	if (node.getPowerLevel() == powerLevel) {
	        		println("Successfully set power level for node " + node.getName());
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
        	println("Network graph minimization interrupted");
        } catch (NodeNotFoundException e) {
        	println("Network graph minimization aborted due to a missing node during maximization");
        } catch (Exception e) {
        	e.printStackTrace();
        	println("Network graph minimization failure");
        }
    	
		
    }
    
    private void maximizeNetworkGraph() throws InterruptedException, JMSException, NodeNotFoundException, JSONException {

    	println("Network graph maximization phase started");
    	
    	while(true) {
    		
        	ClientResponse getResponse = restResource.path("network").path("infrastructural").accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
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
    	
    	println("Network graph maximization phase completed");
    }
    
    private void minimizeNetworkGraph() throws InterruptedException, JSONException {
    	
    	println("Network graph minimization phase started");
    	
    	
    	println("Network graph minimization phase completed");
    }
    
}