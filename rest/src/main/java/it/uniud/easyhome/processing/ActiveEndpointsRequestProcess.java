package it.uniud.easyhome.processing;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.network.NetworkEvent;
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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.sun.jersey.api.client.ClientResponse;

public class ActiveEndpointsRequestProcess extends Process {
	
	private byte sequenceNumber = 0;
	
	private MessageConsumer networkEventsConsumer = null;
	
    public ActiveEndpointsRequestProcess(int pid, UriInfo uriInfo,ProcessKind kind) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind);
        Topic networkEventsTopic = (Topic) jndiContext.lookup(JMSConstants.NETWORK_EVENTS_TOPIC);
        networkEventsConsumer = registerConsumerFor(networkEventsTopic);
    }
    
    @Override
	protected void process() throws JMSException, NamingException {

    	ObjectMessage msg = (ObjectMessage) networkEventsConsumer.receive();
    	if (msg != null) {
    		NetworkEvent event = (NetworkEvent) msg.getObject();
    		if (event != null && event.getKind() == NetworkEvent.EventKind.NODE_ADDED) {

    	        try {
        	        ClientResponse getResponse = restResource.path("network").path(String.valueOf(event.getNuid()))
  						  .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        	        
    	        	Node node = JsonUtils.getFrom(getResponse, Node.class);
    	        	ActiveEndpointsReqPacket packet = new ActiveEndpointsReqPacket(node,++sequenceNumber);

    	            ObjectMessage outboundMessage = jmsSession.createObjectMessage(packet);
    	            getOutboundPacketsProducer().send(outboundMessage);    
    	            println("Active endpoints request dispatched");
    	        } catch (Exception e) {
    	        	e.printStackTrace();
    	        	println("Active endpoints request could not be dispatched to outbound packets topic");
    	        }
    		}
       	}
    }
    
}