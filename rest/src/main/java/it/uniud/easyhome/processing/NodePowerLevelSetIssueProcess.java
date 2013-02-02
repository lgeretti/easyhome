package it.uniud.easyhome.processing;

import java.util.Date;
import java.util.List;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
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

public class NodePowerLevelSetIssueProcess extends Process {
	
	private byte sequenceNumber = 0;
	
	private MessageConsumer networkEventsConsumer = null;
	
    public NodePowerLevelSetIssueProcess(int pid, UriInfo uriInfo,ProcessKind kind) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind);
        Topic networkEventsTopic = (Topic) jndiContext.lookup(JMSConstants.NETWORK_EVENTS_TOPIC);
        networkEventsConsumer = registerConsumerFor(networkEventsTopic);
    }
    
    private void doRequest(byte gatewayId, short address, byte powerLevel, boolean isRepeated) throws JMSException, JSONException {
    	
        ClientResponse getNodeResponse = restResource.path("network")
				 .path(Byte.toString(gatewayId)).path(Short.toString(address))
				 .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

        Node node = JsonUtils.getFrom(getNodeResponse, Node.class);
        
        if (node.getLogicalType() == NodeLogicalType.ROUTER || node.getLogicalType() == NodeLogicalType.COORDINATOR) {
    	
	    	byte tsn = ++sequenceNumber;
	    	
	        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
	        formData.add("type",NetworkJobType.NODE_POWER_LEVEL_SET_ISSUE.toString());
	        formData.add("gid",Byte.toString(gatewayId));
	        formData.add("address",Short.toString(address));
	        formData.add("tsn",Byte.toString(tsn));
	        formData.add("payload",Byte.toString(powerLevel));
	        
	        restResource.path("network").path("jobs").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	
	        NodePowerLevelSetIssuePacket packet = new NodePowerLevelSetIssuePacket(node.getCoordinates(),powerLevel,sequenceNumber);
	 	    ObjectMessage outboundMessage = jmsSession.createObjectMessage(packet);
	    	getOutboundPacketsProducer().send(outboundMessage);    
	        println("Node " + node.getName() + " power level set issue " + (isRepeated ? "re-" : "") + "dispatched");
        }
    }
    
    
    @Override
	protected void process() throws JMSException, NamingException {
    	
    	
		ClientResponse jobListResponse = restResource.path("network").path("jobs").queryParam("type", NetworkJobType.NODE_POWER_LEVEL_SET_ISSUE.toString())
        		.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		try {
			List<NetworkJob> jobs = JsonUtils.getListFrom(jobListResponse, NetworkJob.class);
	    	
	    	if (jobs.isEmpty()) {
	        	ObjectMessage msg = (ObjectMessage) networkEventsConsumer.receive();
	        	if (msg != null) {
	        		NetworkEvent event = (NetworkEvent) msg.getObject();
	        		if (event != null && event.getKind() == NetworkEvent.EventKind.NODE_POWER_LEVEL_SET_ISSUE) {
	        			
	        			doRequest(event.getGatewayId(),event.getAddress(),event.getPayload()[0],false);
	        		}
	           	}    		
	    	} else {
	    		
	    		Date fiveSecBeforeNow = new Date(System.currentTimeMillis()-JOB_TIMEOUT_MILLIS); // Does not need to be accurate
	    		
	    		for (NetworkJob job : jobs) {
	    			
	    			Date jobDate = job.getDate();
	    			if (jobDate.before(fiveSecBeforeNow) || job.isFirst()) {
	    				
	    				doRequest(job.getGatewayId(),job.getAddress(),job.getPayload(),true);
	    			}
	    		}
	    		
	    		Thread.sleep(JOB_POLLING_TIME_MILLIS);
	    	}
        } catch (InterruptedException e) {
        	// Nothing to be done
        } catch (Exception e) {
        	e.printStackTrace();
        	println("Node power level set issue could not be dispatched to outbound packets topic");
        	try {
				Thread.sleep(JOB_POLLING_TIME_MILLIS);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        }
    	
		
    }
    
}