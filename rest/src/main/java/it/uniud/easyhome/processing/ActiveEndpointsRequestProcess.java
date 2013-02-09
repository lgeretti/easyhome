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

public class ActiveEndpointsRequestProcess extends Process {
	
	private byte sequenceNumber = 0;
	
	private MessageConsumer networkEventsConsumer = null;
	
    public ActiveEndpointsRequestProcess(int pid, UriInfo uriInfo,ProcessKind kind) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind);
        Topic networkEventsTopic = (Topic) jndiContext.lookup(JMSConstants.NETWORK_EVENTS_TOPIC);
        networkEventsConsumer = registerConsumerFor(networkEventsTopic);
    }
    
    private void doRequest(byte gatewayId, short address, boolean isRepeated) throws JMSException, JSONException {
    	
        ClientResponse getNodeResponse = restResource.path(RestPaths.NODES)
				 .path(Byte.toString(gatewayId)).path(Short.toString(address))
				 .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

        if (getNodeResponse.getClientResponseStatus() == ClientResponse.Status.OK) {
        
	        Node node = JsonUtils.getFrom(getNodeResponse, Node.class);
	        
	        if (node.getLogicalType() == NodeLogicalType.END_DEVICE) {
	    	
		    	byte tsn = ++sequenceNumber;
		    	
		        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
		        formData.add("type",NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST.toString());
		        formData.add("gid",Byte.toString(gatewayId));
		        formData.add("address",Short.toString(address));
		        formData.add("tsn",Byte.toString(tsn));
		        
		        restResource.path(RestPaths.NODES).path(RestPaths.JOBS).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
		    	
		    	ActiveEndpointsReqPacket packet = new ActiveEndpointsReqPacket(node.getCoordinates(),tsn);
		        ObjectMessage outboundMessage = jmsSession.createObjectMessage(packet);
		        getOutboundPacketsProducer().send(outboundMessage);    
		        println("Node " + node + " active endpoints request " + (isRepeated ? "re-" : "") + "dispatched");
	        }
        } else {
        	println("Node " + Node.nameFor(gatewayId, address) + " not found, ignoring");
        }
    }
    
    
    @Override
	protected void process() throws JMSException, NamingException {
    	
    	
		ClientResponse jobListResponse = restResource.path(RestPaths.NODES).path(RestPaths.JOBS).queryParam("type", NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST.toString())
        		.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		try {
			List<NetworkJob> jobs = JsonUtils.getListFrom(jobListResponse, NetworkJob.class);
	    	
	    	if (jobs.isEmpty()) {
	        	ObjectMessage msg = (ObjectMessage) networkEventsConsumer.receive();
	        	if (msg != null) {
	        		NetworkEvent event = (NetworkEvent) msg.getObject();
	        		if (event != null && event.getKind() == NetworkEvent.EventKind.NODE_DESCR_ACQUIRED) {
	        			
	        			doRequest(event.getGatewayId(),event.getAddress(),false);
	        		}
	           	}    		
	    	} else {
	    		
	    		Date fiveSecBeforeNow = new Date(System.currentTimeMillis()-JOB_TIMEOUT_MILLIS); // Does not need to be accurate
	    		
	    		for (NetworkJob job : jobs) {
	    			
	    			Date jobDate = job.getDate();
	    			if (jobDate.before(fiveSecBeforeNow) || job.isFirst()) {
	    				doRequest(job.getGatewayId(),job.getAddress(),true);
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