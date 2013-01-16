package it.uniud.easyhome.processing;

import java.util.Date;
import java.util.List;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.NetworkJob;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.natives.ActiveEndpointsReqPacket;
import it.uniud.easyhome.packets.natives.NodeDescrReqPacket;
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

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class ActiveEndpointsRequestProcess extends Process {
	
	private byte sequenceNumber = 0;
	
	private MessageConsumer networkEventsConsumer = null;
	
    public ActiveEndpointsRequestProcess(int pid, UriInfo uriInfo,ProcessKind kind) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind);
        Topic networkEventsTopic = (Topic) jndiContext.lookup(JMSConstants.NETWORK_EVENTS_TOPIC);
        networkEventsConsumer = registerConsumerFor(networkEventsTopic);
    }
    
    private void doRequest(Node node, boolean isRepeated) throws JMSException {
    	
    	ActiveEndpointsReqPacket packet = new ActiveEndpointsReqPacket(node,++sequenceNumber);
        ObjectMessage outboundMessage = jmsSession.createObjectMessage(packet);
        getOutboundPacketsProducer().send(outboundMessage);    
        println("Node " + node.getName() + " active endpoints request " + (isRepeated ? "re-" : "") + "dispatched");
    }
    
    @Override
	protected void process() throws JMSException, NamingException {
    	
		ClientResponse jobListResponse = restResource.path("network").path("jobs").queryParam("type", NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST.toString())
        		.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		try {
			List<NetworkJob> jobs = JsonUtils.getListFrom(jobListResponse, NetworkJob.class);
	    	
	    	if (jobs.isEmpty()) {
	        	ObjectMessage msg = (ObjectMessage) networkEventsConsumer.receive();
	        	if (msg != null) {
	        		NetworkEvent event = (NetworkEvent) msg.getObject();
	        		if (event != null && event.getKind() == NetworkEvent.EventKind.NODE_ADDED) {
	
		                MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
		                formData.add("type",NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST.toString());
		                formData.add("gid",String.valueOf(event.getGid()));
		                formData.add("address",String.valueOf(event.getAddress()));
		                
		                restResource.path("network").path("jobs").path("reset").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        			
            	        ClientResponse getNodeResponse = restResource.path("network")
            	        									.path(Byte.toString(event.getGid())).path(Short.toString(event.getAddress()))
            	        									.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
            	        
        	        	Node node = JsonUtils.getFrom(getNodeResponse, Node.class);
        	        	doRequest(node,false);
	        		}
	           	}    		
	    	} else {
	    		
	    		Date fiveSecBeforeNow = new Date(System.currentTimeMillis()-JOB_TIMEOUT_MILLIS); // Does not need to be accurate
	    		
	    		for (NetworkJob job : jobs) {
	    			
	    			Date jobDate = job.getDate();
	    			if (jobDate.before(fiveSecBeforeNow)) {
	    				
	    				boolean isRepeated = true;
	    				
	    				restResource.path("network").path("jobs").path(String.valueOf(job.getId())).path("reset").type(MediaType.APPLICATION_JSON).post(ClientResponse.class);
	    				
	    				ClientResponse getNodeResponse = restResource.path("network")
	    						  .path(Byte.toString(job.getGatewayId())).path(Short.toString(job.getAddress()))
	      						  .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        	        	Node node = JsonUtils.getFrom(getNodeResponse, Node.class);
        	        	doRequest(node,isRepeated);
	    			}
	    		}
	    		
	    		Thread.sleep(JOB_POLLING_TIME_MILLIS);
	    	}
        } catch (InterruptedException e) {
        	// Nothing to be done
        } catch (Exception e) {
        	e.printStackTrace();
        	println("Node active endpoints request could not be dispatched to outbound packets topic");
        }
    }
    
}