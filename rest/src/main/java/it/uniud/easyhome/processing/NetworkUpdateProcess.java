package it.uniud.easyhome.processing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.GlobalCoordinates;
import it.uniud.easyhome.packets.natives.NodeDescrReqPacket;
import it.uniud.easyhome.packets.natives.SimpleDescrReqPacket;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Topic;
import javax.naming.NamingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class NetworkUpdateProcess extends Process {
	
	public static long KEEP_LINK_ALIVE_MS = 6*NodeDiscoveryRequestProcess.DISCOVERY_REQUEST_PERIOD_MS;
	
    public NetworkUpdateProcess(int pid, UriInfo uriInfo,ProcessKind kind) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind);
    }
    
    @Override
	protected void process() throws JMSException, NamingException {

        try {
        	
            ClientResponse networkCleanupResponse = restResource.path("network").path("cleanup").accept(MediaType.APPLICATION_JSON).post(ClientResponse.class);
            List<Node> cleanedNodes = JsonUtils.getListFrom(networkCleanupResponse, Node.class);
            
            for (Node cleanedNode : cleanedNodes)
            	println("Node " + cleanedNode + " removed due to no links being present");
            
            Thread.sleep(NodeDiscoveryRequestProcess.DISCOVERY_REQUEST_PERIOD_MS/4);
            
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }
    
}