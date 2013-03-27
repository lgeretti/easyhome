package it.uniud.easyhome.processing;

import java.util.Date;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.common.LogLevel;
import it.uniud.easyhome.devices.Pairing;
import it.uniud.easyhome.devices.states.LampState;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.ResponseStatus;
import it.uniud.easyhome.packets.natives.LevelControlRspPacket;
import it.uniud.easyhome.packets.natives.NativePacket;
import it.uniud.easyhome.rest.RestPaths;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.naming.NamingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class LightLevelControlProcess extends Process {

	private static int LOCK_TIME_BETWEEN_UPDATES_IN_SECONDS = 10;
	
    public LightLevelControlProcess(int pid, UriInfo uriInfo,ProcessKind kind, LogLevel logLevel) throws NamingException, JMSException {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]),kind,logLevel);
    }
    
    @Override
	protected void process() throws JMSException, NamingException {
    	
    	MessageConsumer inboundPacketsConsumer = getInboundPacketsConsumer();

    	ObjectMessage msg = (ObjectMessage) inboundPacketsConsumer.receive();
    	if (msg != null) {
        	NativePacket pkt = (NativePacket) msg.getObject();
        	
        	if (LevelControlRspPacket.validates(pkt)) {
	        	log(LogLevel.FINE, "LevelControlRspPacket received from " + pkt.getSrcCoords());
	        	
	        	try {
	        		LevelControlRspPacket levelControlPkt = new LevelControlRspPacket(pkt);
	        		
	        		if (levelControlPkt.getStatus() == ResponseStatus.SUCCESS) {
	        			byte gatewayId = levelControlPkt.getSrcCoords().getGatewayId();
	        			short address = levelControlPkt.getAddrOfInterest();
	        			int levelPercentage = levelControlPkt.getLevelPercentage();
	        			log(LogLevel.FINE, "Level percentage " + levelPercentage + "% event recognized");
	        			
		        		ClientResponse getNodeResponse = restResource.path(RestPaths.NODES).path(Byte.toString(gatewayId)).path(Short.toString(address))
								 .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

						if (getNodeResponse.getClientResponseStatus() == ClientResponse.Status.OK) {
							
			        		Node node = JsonUtils.getFrom(getNodeResponse, Node.class);
			        		
			        		ClientResponse getPairingResponse = restResource.path(RestPaths.PAIRINGS).path(Long.toString(node.getInfo().getId()))
															.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
			        		
			        		if (getPairingResponse.getClientResponseStatus() == ClientResponse.Status.OK) {
			        		
			        			Pairing pairing = JsonUtils.getFrom(getPairingResponse, Pairing.class);
			        			
			        			long lampId = pairing.getDestination().getId();
			        			
				        		ClientResponse getLampStateResponse = restResource.path(RestPaths.STATES).path("lamps").path(Long.toString(lampId))
																.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
				        		LampState state = JsonUtils.getFrom(getLampStateResponse, LampState.class);
				        		
				        		//if (state.isOnline()) {
				        			
					        		Date lastStateUpdatePlusLockTime = new Date(state.getLastWhiteUpdate()+1000*LOCK_TIME_BETWEEN_UPDATES_IN_SECONDS);
					        		Date currentDate = new Date(System.currentTimeMillis());
					        		
					        		if (currentDate.after(lastStateUpdatePlusLockTime)) {
			
						        		byte previousVal = state.getWhite();
						        		int newVal = Math.max(0, Math.min(100, previousVal +levelPercentage));
									
						        		if (previousVal != newVal) {
										if (previousVal != 0)
											newVal = 0;
										else
											newVal = 50;
							        		log(LogLevel.INFO, "Level modified from " + previousVal + " to " + newVal);
							        		
							        		
							        		MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
							                formData.add("value",Byte.toString((byte)(newVal & 0xFF)));
							                
							                restResource.path(RestPaths.STATES).path("lamps").path(Long.toString(lampId)).path("white")
							                			.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
						        		} else
						        			log(LogLevel.FINE, "Lamp state has not been changed by control: discarding the event");
					                
					        		} else
					        			log(LogLevel.FINE, "Lamp state updated too recently: discarding the event");				        			
				        			
				        		//} else 
				        		//	log(LogLevel.FINE, "Lamp is offline: discarding the event");
			                
			        		} else if (getPairingResponse.getClientResponseStatus() == ClientResponse.Status.NOT_FOUND) 
			                	log(LogLevel.FINE, "Pairing not present for this controller");
			        		
						} else
					    	log(LogLevel.FINE, "Node " + Node.nameFor(gatewayId, address) + " not found, ignoring");
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
