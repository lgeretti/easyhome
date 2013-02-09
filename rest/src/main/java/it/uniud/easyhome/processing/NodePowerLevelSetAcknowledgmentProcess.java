package it.uniud.easyhome.processing;

import java.util.Arrays;
import java.util.List;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.exceptions.PowerLevelSetIssueJobNotPresentException;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.NetworkJob;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.ResponseStatus;
import it.uniud.easyhome.packets.natives.ActiveEndpointsRspPacket;
import it.uniud.easyhome.packets.natives.NativePacket;
import it.uniud.easyhome.packets.natives.NodeNeighRspPacket;
import it.uniud.easyhome.packets.natives.NodePowerLevelRspPacket;
import it.uniud.easyhome.packets.natives.NodePowerLevelSetAcknowledgmentPacket;
import it.uniud.easyhome.rest.RestPaths;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Topic;
import javax.naming.NamingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jettison.json.JSONException;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.xml.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

public class NodePowerLevelSetAcknowledgmentProcess extends Process {
	
    public NodePowerLevelSetAcknowledgmentProcess(int pid, UriInfo uriInfo,ProcessKind kind) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind);
    }
    
    @Override
	protected void process() throws JMSException, NamingException {
    	
    	MessageConsumer inboundPacketsConsumer = getInboundPacketsConsumer();

    	ObjectMessage msg = (ObjectMessage) inboundPacketsConsumer.receive();
    	if (msg != null) {
        	NativePacket pkt = (NativePacket) msg.getObject();
        	
        	if (NodePowerLevelSetAcknowledgmentPacket.validates(pkt)) {
	        	println("NodePowerLevelSetAcknowledgmentPacket received from " + pkt.getSrcCoords());
	        	
	        	try {
	        		NodePowerLevelSetAcknowledgmentPacket ackPkt = new NodePowerLevelSetAcknowledgmentPacket(pkt);
	        		
	        		if (ackPkt.getStatus() == ResponseStatus.SUCCESS) {
	        			
	        			byte gatewayId = ackPkt.getSrcCoords().getGatewayId();
	        			short address = ackPkt.getAddrOfInterest();
	        			byte powerLevel = getPowerLevel(gatewayId,address);
		        		
		        		synchronized(nodesLock) {
			        		ClientResponse getResponse = restResource.path(RestPaths.NODES).path(Byte.toString(gatewayId)).path(Short.toString(address))
			        												 .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
			        		
			        		if (getResponse.getClientResponseStatus() == ClientResponse.Status.OK) {

			        			Node node = JsonUtils.getFrom(getResponse, Node.class);
			        			node.setPowerLevel(powerLevel);
			        			
			        			ClientResponse updateResponse = restResource.path(RestPaths.NODES).path("update").type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node);
			        			
				                if (updateResponse.getClientResponseStatus() == Status.OK) {
				                	
				                	cleanJobs(gatewayId,address);
				                	
				                    println(node + " updated with power level (" + powerLevel + ")");
				                } else
				                	println(node + " power level information insertion failed");
				        	} else
				        		println("Node" + Node.nameFor(gatewayId, address) + " not found, ignoring");
		        		}
	        		}
	        		
	        	} catch (PowerLevelSetIssueJobNotPresentException e) {
	        		
	        		println("No corresponding power level set issue job found: acknowledgment ignored");
	        		
	        	} catch (Exception e) {
	        		e.printStackTrace();
	        	}
        	}
    	}
    }
    
    private byte getPowerLevel(byte gatewayId, short address) throws JSONException {
    	
    	MultivaluedMap<String,String> queryData = new MultivaluedMapImpl();
        queryData.add("type",NetworkJobType.NODE_POWER_LEVEL_SET_ISSUE.toString());
        queryData.add("gid",String.valueOf(gatewayId));
        queryData.add("address",String.valueOf(address));	        			
		ClientResponse jobListResponse = restResource.path(RestPaths.JOBS).queryParams(queryData)
										.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		List<NetworkJob> jobs = JsonUtils.getListFrom(jobListResponse, NetworkJob.class);

		if (jobs.isEmpty())
			throw new PowerLevelSetIssueJobNotPresentException();
		
		return jobs.get(0).getPayload();
    }
    
    private void cleanJobs(byte gatewayId, short address) throws JSONException {
    	
        MultivaluedMap<String,String> queryData = new MultivaluedMapImpl();
        queryData.add("type",NetworkJobType.NODE_POWER_LEVEL_SET_ISSUE.toString());
        queryData.add("gid",String.valueOf(gatewayId));
        queryData.add("address",String.valueOf(address));
        
        restResource.path(RestPaths.JOBS).queryParams(queryData).delete(ClientResponse.class);
    }
}