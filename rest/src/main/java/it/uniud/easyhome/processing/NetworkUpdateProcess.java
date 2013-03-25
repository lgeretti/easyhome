package it.uniud.easyhome.processing;

import java.util.List;

import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.common.LogLevel;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.rest.RestPaths;

import javax.jms.JMSException;
import javax.naming.NamingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.sun.jersey.api.client.ClientResponse;

public class NetworkUpdateProcess extends Process {
	
	public static long KEEP_LINK_ALIVE_MS = 6*NodeDiscoveryRequestProcess.DISCOVERY_REQUEST_PERIOD_MS;
	
    public NetworkUpdateProcess(int pid, UriInfo uriInfo,ProcessKind kind,LogLevel logLevel) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind,logLevel);
    }
    
    @Override
	protected void process() throws JMSException, NamingException {

        try {

        	// The order of cleanup is necessarily links first, then nodes (those that have no links), and finally jobs (those whose node does not exist)
        	
        	restResource.path(RestPaths.LINKS).path("cleanup").accept(MediaType.APPLICATION_JSON).post(ClientResponse.class);
        	
            ClientResponse nodesCleanupResponse = restResource.path(RestPaths.NODES).path("cleanup").accept(MediaType.APPLICATION_JSON).post(ClientResponse.class);
            List<Node> cleanedNodes = JsonUtils.getListFrom(nodesCleanupResponse, Node.class);
            for (Node cleanedNode : cleanedNodes)
            	log(LogLevel.FINE, "Node " + cleanedNode + " removed due to no links being present");
            
            restResource.path(RestPaths.JOBS).path("cleanup").accept(MediaType.APPLICATION_JSON).post(ClientResponse.class);

            Thread.sleep(NodeDiscoveryRequestProcess.DISCOVERY_REQUEST_PERIOD_MS/4);
            
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }
    
}