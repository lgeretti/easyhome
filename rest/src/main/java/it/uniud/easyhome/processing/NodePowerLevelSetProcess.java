package it.uniud.easyhome.processing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.network.GlobalCoordinates;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.NetworkJob;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.natives.NodeDescrReqPacket;
import it.uniud.easyhome.packets.natives.NodePowerLevelReqPacket;
import it.uniud.easyhome.packets.natives.NodePowerLevelSetPacket;
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

public class NodePowerLevelSetProcess extends Process {
	
	private byte sequenceNumber = 0;
	
	private MessageConsumer networkEventsConsumer = null;
	
    public NodePowerLevelSetProcess(int pid, UriInfo uriInfo,ProcessKind kind) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind);
        Topic networkEventsTopic = (Topic) jndiContext.lookup(JMSConstants.NETWORK_EVENTS_TOPIC);
        networkEventsConsumer = registerConsumerFor(networkEventsTopic);
    }
    
    @Override
	protected void process() throws JMSException, NamingException {

    	ObjectMessage msg = (ObjectMessage) networkEventsConsumer.receive();
    	if (msg != null) {
    		NetworkEvent event = (NetworkEvent) msg.getObject();
    		if (event != null && event.getKind() == NetworkEvent.EventKind.NODE_POWER_LEVEL_SET) {

    			byte powerLevel = event.getPayload()[0];
	        	long nuid = ByteUtils.getLong(event.getPayload(), 1, Endianness.BIG_ENDIAN);
	        	GlobalCoordinates destinationCoords = new GlobalCoordinates(event.getGatewayId(),nuid,event.getAddress());
    			
    	        try {
    	        	
    	    		NodePowerLevelSetPacket packet = new NodePowerLevelSetPacket(destinationCoords,powerLevel,++sequenceNumber);
    		 	    ObjectMessage outboundMessage = jmsSession.createObjectMessage(packet);
    		    	getOutboundPacketsProducer().send(outboundMessage);    
    		    	println("Node " + destinationCoords + " power level request dispatched");
    		    	
    	        } catch (Exception e) {
    	        	e.printStackTrace();
    	        	println("Node power level set for node "+ destinationCoords + " could not be issued");
    	        }
    		}
       	}
    }
    
}