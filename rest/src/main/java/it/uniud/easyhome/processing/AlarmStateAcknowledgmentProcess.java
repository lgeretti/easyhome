package it.uniud.easyhome.processing;

import java.util.Arrays;
import java.util.List;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.common.LogLevel;
import it.uniud.easyhome.devices.DeviceType;
import it.uniud.easyhome.devices.FunctionalityType;
import it.uniud.easyhome.devices.states.ColoredAlarm;
import it.uniud.easyhome.devices.states.FridgeCode;
import it.uniud.easyhome.devices.states.LampState;
import it.uniud.easyhome.devices.states.PresenceSensorState;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.NetworkJob;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.ResponseStatus;
import it.uniud.easyhome.packets.natives.ActiveEndpointsRspPacket;
import it.uniud.easyhome.packets.natives.AlarmStateRspPacket;
import it.uniud.easyhome.packets.natives.NativePacket;
import it.uniud.easyhome.packets.natives.NodeNeighRspPacket;
import it.uniud.easyhome.packets.natives.NodePowerLevelRspPacket;
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

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.xml.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

public class AlarmStateAcknowledgmentProcess extends Process {
	
    public AlarmStateAcknowledgmentProcess(int pid, UriInfo uriInfo,ProcessKind kind,LogLevel logLevel) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind,logLevel);
    }
    
    @Override
	protected void process() throws JMSException, NamingException {
    	
    	MessageConsumer inboundPacketsConsumer = getInboundPacketsConsumer();

    	ObjectMessage msg = (ObjectMessage) inboundPacketsConsumer.receive();
    	if (msg != null) {
        	NativePacket pkt = (NativePacket) msg.getObject();
        	
        	if (AlarmStateRspPacket.validates(pkt)) {
	        	log(LogLevel.DEBUG, "AlarmStateRspPacket received from " + pkt.getSrcCoords());
	        	
	        	try {
	        		AlarmStateRspPacket alarmPkt = new AlarmStateRspPacket(pkt);
	        		
	        		if (alarmPkt.getStatus() == ResponseStatus.SUCCESS) {
	        			
	        			byte gatewayId = alarmPkt.getSrcCoords().getGatewayId();
	        			short address = alarmPkt.getAddrOfInterest();
	        			FridgeCode alarmCode = FridgeCode.fromCode(alarmPkt.getAlarmCode());
	        			
	        			// (NOTE: for simplicity, we assume that only one source of alarms exists)
	        			
		        		synchronized(nodesLock) {
		        			
		            		MultivaluedMap<String,String> params = new MultivaluedMapImpl();
		                    params.add("deviceType",DeviceType.COLORED_LAMP.toString());
		            		ClientResponse nodesResponse = restResource.path(RestPaths.NODES).queryParams(params).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		            		
			        		if (nodesResponse.getClientResponseStatus() == ClientResponse.Status.OK) {

			        			List<Node> nodes = JsonUtils.getListFrom(nodesResponse, Node.class);

			        			for (Node node : nodes) {
			        				
				            		ClientResponse presenceStateResponse = restResource.path(RestPaths.STATES).path("sensors/presence").path(Long.toString(node.getId()))
				            												   .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
				            		PresenceSensorState presenceState = JsonUtils.getFrom(presenceStateResponse, PresenceSensorState.class);
				            		
				            		MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
				            		ColoredAlarm alarmToIssue = null;
				            		switch (alarmCode) {
					            		case ALARM1:
					            			if (presenceState.isOccupied())
					            				alarmToIssue = ColoredAlarm.RED_BLINK;
					            			else
					            				alarmToIssue = ColoredAlarm.NONE;
					            			break;
					            		case ALARM2:
					            			if (presenceState.isOccupied())
					            				alarmToIssue = ColoredAlarm.BLUE_BLINK;
					            			else
					            				alarmToIssue = ColoredAlarm.NONE;
					            			break;
					            		default:
					            			alarmToIssue = ColoredAlarm.NONE;
				            		}
				            		formData.add("value",alarmToIssue.toString());
					                restResource.path(RestPaths.STATES).path("lamps").path(Long.toString(node.getId())).path("alarm")
		                						.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
					                
					                log(LogLevel.INFO, "Alarm set for lamp id " + node.getId() + " (" + (presenceState.isOccupied()? "occupied":"unoccupied") 
					                					+ "): " + alarmToIssue);
			        			}
			        			
				        	} else
				        		log(LogLevel.DEBUG, "No lamp actuators are available for alarms, ignoring");
		        		
		        		}
	        		}
	        		
	        	} catch (Exception e) {
	        		e.printStackTrace();
	        	}
        	}
    	}
    }
    
}