package it.uniud.easyhome.processing;

import java.util.Arrays;
import java.util.List;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.common.LogLevel;
import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.devices.Location;
import it.uniud.easyhome.devices.Manufacturer;
import it.uniud.easyhome.devices.PersistentInfo;
import it.uniud.easyhome.exceptions.InvalidNodeLogicalTypeException;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.LocalCoordinates;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.NetworkJob;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.packets.ResponseStatus;
import it.uniud.easyhome.packets.natives.NativePacket;
import it.uniud.easyhome.packets.natives.NodeDiscoveryRspPacket;
import it.uniud.easyhome.packets.natives.NodeNeighRspPacket;
import it.uniud.easyhome.packets.natives.OccupancyAttributeRspPacket;
import it.uniud.easyhome.rest.RestPaths;

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

import org.codehaus.jettison.json.JSONException;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class OccupancySensingRegistrationProcess extends Process {

    public OccupancySensingRegistrationProcess(int pid, UriInfo uriInfo,ProcessKind kind, LogLevel logLevel) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind,logLevel);
    }
    
    @Override
	protected void process() throws JMSException, NamingException {
    	
    	MessageConsumer inboundPacketsConsumer = getInboundPacketsConsumer();

    	ObjectMessage msg = (ObjectMessage) inboundPacketsConsumer.receive();
    	if (msg != null) {
        	NativePacket pkt = (NativePacket) msg.getObject();
        	
        	if (OccupancyAttributeRspPacket.validates(pkt)) {
	        	log(LogLevel.DEBUG, "OccupancyAttributeRspPacket received from " + pkt.getSrcCoords());
	        	
	        	try {
	        		OccupancyAttributeRspPacket occupancyPkt = new OccupancyAttributeRspPacket(pkt);
	        		
	        		if (occupancyPkt.getStatus() == ResponseStatus.SUCCESS) {
	        			byte gatewayId = occupancyPkt.getSrcCoords().getGatewayId();
	        			short senderAddress = occupancyPkt.getAddrOfInterest();
	        			boolean occupied = occupancyPkt.isOccupied();

		        		ClientResponse senderRetrievalResponse = restResource.path(RestPaths.NODES).path(Byte.toString(gatewayId)).path(Short.toString(senderAddress))
		        						.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		        		
		        		if (senderRetrievalResponse.getClientResponseStatus() == ClientResponse.Status.OK) {
		                
		        			Node sender = JsonUtils.getFrom(senderRetrievalResponse, Node.class);
		        			
			        		ClientResponse senderLocationRetrievalResponse = restResource.path(RestPaths.PERSISTENTINFO).path(Byte.toString(gatewayId))
			        				.path(Long.toString(sender.getCoordinates().getNuid())).path("location").accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);		        			
			        		Location location = JsonUtils.getFrom(senderLocationRetrievalResponse, Location.class);
			        		
		        			if (location != null) {
		        				
		        				boolean previouslyOccupied = location.isOccupied();
		        				location.setOccupied(occupied);
				                
				                if (previouslyOccupied != occupied) {
					                restResource.path(RestPaths.LOCATIONS).path(Integer.toString(location.getId())).path(occupied ? "occupied" : "unoccupied").put(ClientResponse.class);
					                restResource.path(RestPaths.STATES).path("sensors").path("presence").path(Long.toString(sender.getInfo().getId())).path(occupied ? "occupied" : "unoccupied").put(ClientResponse.class);
				                	log(LogLevel.INFO, location + " is now " + (occupied ? "occupied" : "unoccupied"));
				                } else
				                	log(LogLevel.DEBUG, "No change in occupancy state");
		        			}
			                
		        		} else 
		        			log(LogLevel.DEBUG, "Sender node " + Node.nameFor(gatewayId, senderAddress) + " not found, hence discarding occupancy information");
	        		}
	        	} catch (InvalidPacketTypeException e) {
	        		e.printStackTrace();
	        	} catch (Exception e) {
					e.printStackTrace();
				}
        	}
    	}
    }

}