package it.uniud.easyhome.processing;

import java.util.Arrays;
import java.util.List;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.NetworkJob;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.ResponseStatus;
import it.uniud.easyhome.packets.natives.ActiveEndpointsRspPacket;
import it.uniud.easyhome.packets.natives.NativePacket;
import it.uniud.easyhome.packets.natives.NodeNeighRspPacket;
import it.uniud.easyhome.packets.natives.NodePowerLevelRspPacket;

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

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.xml.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

public class NodePowerLevelRegistrationProcess extends Process {
	
    public NodePowerLevelRegistrationProcess(int pid, UriInfo uriInfo,ProcessKind kind) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind);
    }
    
    @Override
	protected void process() throws JMSException, NamingException {
    	
    	MessageConsumer inboundPacketsConsumer = getInboundPacketsConsumer();

    	ObjectMessage msg = (ObjectMessage) inboundPacketsConsumer.receive();
    	if (msg != null) {
        	NativePacket pkt = (NativePacket) msg.getObject();
        	
        	if (NodePowerLevelRspPacket.validates(pkt)) {
	        	println("NodePowerLevelRspPacket received from " + pkt.getSrcCoords());
	        	
	        	try {
	        		NodePowerLevelRspPacket plPkt = new NodePowerLevelRspPacket(pkt);
	        		
	        		if (plPkt.getStatus() == ResponseStatus.SUCCESS) {
	        			
	        			byte gatewayId = plPkt.getSrcCoords().getGatewayId();
	        			short address = plPkt.getAddrOfInterest();
	        			byte powerLevel = plPkt.getPowerLevel();
	        			
		        		Node node;
		        		ClientResponse updateResponse;
		        		
		        		synchronized(nodesLock) {
			        		ClientResponse getResponse = restResource.path("network").path(Byte.toString(gatewayId)).path(Short.toString(address))
			        												 .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
			        		
			        		if (getResponse.getClientResponseStatus() == ClientResponse.Status.OK) {

			        			node = JsonUtils.getFrom(getResponse, Node.class);
				                MultivaluedMap<String,String> formParams = new MultivaluedMapImpl();
				                formParams.add("powerLevel",Byte.toString(powerLevel));
			        			updateResponse = restResource.path("network").path(Byte.toString(gatewayId)).path(Short.toString(address))
				                		.type(MediaType.APPLICATION_FORM_URLENCODED).post(ClientResponse.class,formParams);
			        		
			        			
				                if (updateResponse.getClientResponseStatus() == Status.OK) {
				                	
					                MultivaluedMap<String,String> queryData = new MultivaluedMapImpl();
					                queryData.add("type",NetworkJobType.NODE_POWER_LEVEL_REQUEST.toString());
					                queryData.add("gid",String.valueOf(gatewayId));
					                queryData.add("address",String.valueOf(address));
					                
					                restResource.path("network").path("jobs").queryParams(queryData).delete(ClientResponse.class);
				                	
				                    println("Node " + node.getName() + " updated with power level (" + powerLevel + ")");
				                } else
				                	println("Node " + node.getName() + " power level information insertion failed");	
				        
				        	}
		        		
		        		}
	        		}
	        		
	        	} catch (Exception e) {
	        		e.printStackTrace();
	        	}
        	}
    	}
    }
    
}