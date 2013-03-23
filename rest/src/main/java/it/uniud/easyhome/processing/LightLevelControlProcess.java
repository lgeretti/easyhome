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
import it.uniud.easyhome.packets.natives.LevelControlRspPacket;
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

public class LightLevelControlProcess extends Process {

	
	private MessageProducer networkEventsProducer = null;
	
    public LightLevelControlProcess(int pid, UriInfo uriInfo,ProcessKind kind, LogLevel logLevel) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind,logLevel);
        
        Topic networkEventsTopic = (Topic) jndiContext.lookup(JMSConstants.NETWORK_EVENTS_TOPIC);
        networkEventsProducer = registerProducerFor(networkEventsTopic);
    }
    
    @Override
	protected void process() throws JMSException, NamingException {
    	
    	MessageConsumer inboundPacketsConsumer = getInboundPacketsConsumer();

    	ObjectMessage msg = (ObjectMessage) inboundPacketsConsumer.receive();
    	if (msg != null) {
        	NativePacket pkt = (NativePacket) msg.getObject();
        	
        	if (LevelControlRspPacket.validates(pkt)) {
	        	log(LogLevel.DEBUG, "LevelControlRspPacket received from " + pkt.getSrcCoords());
	        	
	        	try {
	        		LevelControlRspPacket levelControlPkt = new LevelControlRspPacket(pkt);
	        		
	        		if (levelControlPkt.getStatus() == ResponseStatus.SUCCESS) {
	        			byte gatewayId = levelControlPkt.getSrcCoords().getGatewayId();
	        			short address = levelControlPkt.getAddrOfInterest();
	        			int levelPercentage = levelControlPkt.getLevelPercentage();
	        			log(LogLevel.DEBUG, "Level percentage " + levelPercentage + "% event recognized");
	        			
		        		ClientResponse getResponse = restResource.path(RestPaths.NODES).path(Byte.toString(gatewayId)).path(Short.toString(address))
								 .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

						if (getResponse.getClientResponseStatus() == ClientResponse.Status.OK) {
							
							NetworkEvent event = new NetworkEvent(NetworkEvent.EventKind.LEVEL_CONTROL_VARIATION, 
    								gatewayId, address,new byte[]{(byte)(levelPercentage & 0x0FF)});
					        try {
					            ObjectMessage eventMessage = jmsSession.createObjectMessage(event);
					            networkEventsProducer.send(eventMessage);
					        } catch (JMSException ex) { }
						
						} else
					    	log(LogLevel.DEBUG, "Node " + Node.nameFor(gatewayId, address) + " not found, ignoring");
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