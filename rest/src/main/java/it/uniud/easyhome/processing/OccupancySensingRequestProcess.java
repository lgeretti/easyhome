package it.uniud.easyhome.processing;

import java.util.List;

import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.common.LogLevel;
import it.uniud.easyhome.devices.Functionality;
import it.uniud.easyhome.devices.FunctionalityType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.natives.OccupancyAttributeReqPacket;
import it.uniud.easyhome.rest.RestPaths;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.naming.NamingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class OccupancySensingRequestProcess extends Process {
	
	public static long REQUEST_PERIOD_MS = 1000;
	
	private byte sequenceNumber = 0;
	
    public OccupancySensingRequestProcess(int pid, UriInfo uriInfo,ProcessKind kind, LogLevel logLevel) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind,logLevel);
    }
    
    @Override
	protected void process() throws JMSException, NamingException {

    	try {
	    	ClientResponse getResponse = restResource.path(RestPaths.NODES).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	        	        
	    	List<Node> nodes = JsonUtils.getListFrom(getResponse, Node.class);
	    	
	    	for (Node node : nodes) {

	    		MultivaluedMap<String,String> params = new MultivaluedMapImpl();
	            params.add("infoId",Long.toString(node.getInfo().getId()));
	            params.add("funcType",FunctionalityType.OCCUPATION_SENSING.toString());
        		ClientResponse functionalitiesResponse = restResource.path(RestPaths.FUNCTIONALITIES).queryParams(params).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        		List<Functionality> functionalities = JsonUtils.getListFrom(functionalitiesResponse, Functionality.class);

	    		if (functionalities.size() == 1) {
	    			OccupancyAttributeReqPacket packet = new OccupancyAttributeReqPacket(node.getCoordinates(),(byte)1,++sequenceNumber);
			 	    ObjectMessage outboundMessage = jmsSession.createObjectMessage(packet);
			    	getOutboundPacketsProducer().send(outboundMessage);    
			    	log(LogLevel.DEBUG, "Occupancy attribute request for " + node + " dispatched");
	    		}
	    	}
			Thread.sleep(REQUEST_PERIOD_MS);
	    	
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }   
}