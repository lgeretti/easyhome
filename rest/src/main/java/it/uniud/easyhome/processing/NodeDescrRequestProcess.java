package it.uniud.easyhome.processing;

import java.util.Date;
import java.util.List;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.common.LogLevel;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.NetworkJob;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.Packet;
import it.uniud.easyhome.packets.natives.NodeDescrReqPacket;
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

public class NodeDescrRequestProcess extends Process {
	
	private byte sequenceNumber = 0;
	
	private MessageConsumer networkEventsConsumer = null;
	
    public NodeDescrRequestProcess(int pid, UriInfo uriInfo,ProcessKind kind, LogLevel logLevel) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind,logLevel);
        Topic networkEventsTopic = (Topic) jndiContext.lookup(JMSConstants.NETWORK_EVENTS_TOPIC);
        networkEventsConsumer = registerConsumerFor(networkEventsTopic);
    }
    
    private void doRequest(byte gatewayId, short address, boolean isRepeated) throws JMSException, JSONException {
    	        
        ClientResponse getNodeResponse = restResource.path(RestPaths.NODES)
        								 .path(Byte.toString(gatewayId)).path(Short.toString(address))
        								 .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        
        if (getNodeResponse.getClientResponseStatus() == ClientResponse.Status.OK) {
        	
        	byte tsn = ++sequenceNumber;
        	
            MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
            formData.add("type",NetworkJobType.NODE_DESCR_REQUEST.toString());
            formData.add("gatewayId",Byte.toString(gatewayId));
            formData.add("address",Short.toString(address));
            formData.add("tsn",Byte.toString(tsn));
            
            restResource.path(RestPaths.JOBS).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
        	
	    	Node node = JsonUtils.getFrom(getNodeResponse, Node.class);
	    	
	    	NodeDescrReqPacket packet = new NodeDescrReqPacket(node.getCoordinates(),tsn);
	        ObjectMessage outboundMessage = jmsSession.createObjectMessage(packet);
	        getOutboundPacketsProducer().send(outboundMessage);    
	        log(LogLevel.INFO, "Node " + node + " descriptor request " + (isRepeated ? "re-" : "") + "dispatched");
        } else
        	log(LogLevel.FINE, "Node " + Node.nameFor(gatewayId, address) + " not found, ignoring");
    }
    
    @Override
	protected void process() throws JMSException, NamingException {
    	
		ClientResponse jobListResponse = restResource.path(RestPaths.JOBS).queryParam("type", NetworkJobType.NODE_DESCR_REQUEST.toString())
        		.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		try {
			List<NetworkJob> jobs = JsonUtils.getListFrom(jobListResponse, NetworkJob.class);
	    	
	    	if (jobs.size() == 0) {
	        	ObjectMessage msg = (ObjectMessage) networkEventsConsumer.receive();
	        	if (msg != null) {
	        		NetworkEvent event = (NetworkEvent) msg.getObject();
	        		if (event != null && event.getKind() == NetworkEvent.EventKind.NODE_ADDED) {
	        			doRequest(event.getGatewayId(),event.getAddress(),false);
	        		}
	           	}    		
	    	} else {
	    		
	    		Date timeoutSecondsBeforeNow = new Date(System.currentTimeMillis()-JOB_TIMEOUT_MILLIS); // Does not need to be accurate
	    		
	    		for (NetworkJob job : jobs) {
	    			
	    			Date jobDate = job.getDate();
	    			if (jobDate.before(timeoutSecondsBeforeNow) || job.isFirst()) {
	    				doRequest(job.getGatewayId(),job.getAddress(),true);
	    			}
	    		}
	    		
	    		Thread.sleep(JOB_POLLING_TIME_MILLIS);
	    	}
        } catch (InterruptedException e) {
        	// Nothing to be done
        } catch (Exception e) {
        	e.printStackTrace();
        	log(LogLevel.FINE, "Node descriptor request could not be dispatched to outbound packets topic");
        }
    }
    
}