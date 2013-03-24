package it.uniud.easyhome.rest;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.devices.PersistentInfo;
import it.uniud.easyhome.devices.states.*;
import it.uniud.easyhome.ejb.StateEJB;
import it.uniud.easyhome.exceptions.MultipleLinkException;
import it.uniud.easyhome.network.*;

import java.util.List;
import java.util.Set;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.*;
import javax.ws.rs.*;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

@Path(RestPaths.STATES)
public final class StateResource {
	
    private StateEJB resEjb;
    
	private static final String TARGET = "http://localhost:8080/easyhome/rest/";
	private static Client client = Client.create();
    
	private Connection jmsConnection = null;
	protected javax.naming.Context jndiContext = null;
	protected Session jmsSession = null;
    
    private static Object statesLock = new Object();

    public StateResource() throws NamingException {
    	resEjb = (StateEJB) new InitialContext().lookup("java:global/easyhome/" + StateEJB.class.getSimpleName());
    }
    
    @Context
    private UriInfo uriInfo;
    
    @GET
    @Path("lamps")
    @Produces(MediaType.APPLICATION_JSON)
    public List<LampState> getLampStates() {
    	
    	return resEjb.getStatesOfClass(LampState.class);
    }
    
    @GET
    @Path("lamps/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public LampState getLampStateByInfoId(@PathParam("id") long id) {
    	
    	return resEjb.findStateByInfoId(id,LampState.class);
    }    
    
    @GET
    @Path("fridges")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FridgeState> getFridgeStates() {
    	return resEjb.getStatesOfClass(FridgeState.class);
    }
    
    @GET
    @Path("fridges/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public FridgeState getFridgeStateByInfoId(@PathParam("id") long id) {
    	
    	return resEjb.findStateByInfoId(id,FridgeState.class);
    }   
    
    @GET
    @Path("sensors/presence")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PresenceSensorState> getPresenceSensorStates() {
    	return resEjb.getStatesOfClass(PresenceSensorState.class);
    }
    
    @GET
    @Path("sensors/presence/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public PresenceSensorState getPresenceSensorStateByInfoId(@PathParam("id") long id) {
    	
    	return resEjb.findStateByInfoId(id,PresenceSensorState.class);
    }  
    
    @PUT
    @Path("lamps/{id}")
    public Response insertLampState(@PathParam("id") long infoId) {
    		
    	boolean infoFound = resEjb.insertLampStateFrom(infoId);
    	
    	if (!infoFound)
        	throw new WebApplicationException(Response.Status.BAD_REQUEST);
    	
        return Response.ok().build();
    } 
    
    @PUT
    @Path("fridges/{id}")
    public Response insertFridgeState(@PathParam("id") long infoId) {
    		
    	boolean infoFound = resEjb.insertFridgeStateFrom(infoId);
    	
    	if (!infoFound)
        	throw new WebApplicationException(Response.Status.BAD_REQUEST);
    	
        return Response.ok().build();
    } 
    
    @PUT
    @Path("sensors/presence/{id}")
    public Response insertPresenceSensorState(@PathParam("id") long infoId) {
    		
    	boolean infoFound = resEjb.insertPresenceSensorStateFrom(infoId);
    	
    	if (!infoFound)
        	throw new WebApplicationException(Response.Status.BAD_REQUEST);
    	
        return Response.ok().build();
    } 
    
    
    @POST
    @Path("lamps/{id}")
    public Response updateLampState(@PathParam("id") long id,
    								@FormParam("on") boolean on,
    								@FormParam("red") byte red,
    								@FormParam("green") byte green,
    								@FormParam("blue") byte blue,
    								@FormParam("white") byte white,
    								@FormParam("alarm") ColoredAlarm alarm) {
    		
    	LampState thisLamp = resEjb.findStateByInfoId(id,LampState.class);
    	
    	if (thisLamp == null)
    		throw new WebApplicationException(Response.Status.NOT_FOUND);
    	
    	thisLamp.setOnline(on)
    			.setRed(red)
    			.setGreen(green)
    			.setBlue(blue)
    			.setWhite(white)
    			.setAlarm(alarm);
    	
    	resEjb.updateManagedState(thisLamp);
    	
    	return Response.ok().build();
    }    
    
    @POST
    @Path("lamps/{id}/red")
    public Response updateLampRedColor(@PathParam("id") long id,
    								@FormParam("value") byte value) {
    		
    	LampState thisLamp = resEjb.findStateByInfoId(id,LampState.class);
    	
    	if (thisLamp == null)
    		throw new WebApplicationException(Response.Status.NOT_FOUND);
    	
    	thisLamp.setRed(value);
    	
    	resEjb.updateManagedState(thisLamp);
    	
    	return Response.ok().build();
    }   
    
    @POST
    @Path("lamps/{id}/green")
    public Response updateLampGreenColor(@PathParam("id") long id,
    								@FormParam("value") byte value) {
    		
    	LampState thisLamp = resEjb.findStateByInfoId(id,LampState.class);
    	
    	if (thisLamp == null)
    		throw new WebApplicationException(Response.Status.NOT_FOUND);
    	
    	thisLamp.setGreen(value);
    	
    	resEjb.updateManagedState(thisLamp);
    	
    	return Response.ok().build();
    }   
    
    @POST
    @Path("lamps/{id}/blue")
    public Response updateLampBlueColor(@PathParam("id") long id,
    								@FormParam("value") byte value) {
    		
    	LampState thisLamp = resEjb.findStateByInfoId(id,LampState.class);
    	
    	if (thisLamp == null)
    		throw new WebApplicationException(Response.Status.NOT_FOUND);
    	
    	thisLamp.setBlue(value);
    	
    	resEjb.updateManagedState(thisLamp);
    	
    	return Response.ok().build();
    }   
    
    @POST
    @Path("lamps/{id}/white")
    public Response updateLampLuminosity(@PathParam("id") long id,
    								@FormParam("value") byte value) {
    		
    	LampState thisLamp = resEjb.findStateByInfoId(id,LampState.class);
    	
    	if (thisLamp == null)
    		throw new WebApplicationException(Response.Status.NOT_FOUND);
    	
    	thisLamp.setWhite(value);
    	
    	resEjb.updateManagedState(thisLamp);
    	
    	return Response.ok().build();
    }   
    
    @POST
    @Path("fridges/{id}")
    public Response updateFridgeLastCode(@PathParam("id") long id,
    								@FormParam("lastCode") FridgeCode lastCode) {
    		
    	FridgeState thisFridge = resEjb.findStateByInfoId(id,FridgeState.class);
    	
    	if (thisFridge == null)
    		throw new WebApplicationException(Response.Status.NOT_FOUND);
    	
    	thisFridge.setLastCode(lastCode);
    	
    	resEjb.updateManagedState(thisFridge);
    	
    	return Response.ok().build();
    }
    
    @PUT
    @Path("sensors/presence/{id}/occupied")
    public Response updatePresenceSensorOccupied(@PathParam("id") long id) {
    		
    	PresenceSensorState thisSensor = resEjb.findStateByInfoId(id,PresenceSensorState.class);
    	
    	if (thisSensor == null)
    		throw new WebApplicationException(Response.Status.NOT_FOUND);
    	
    	thisSensor.setOccupied(true);
    	
    	resEjb.updateManagedState(thisSensor);
    	
    	return Response.ok().build();
    }
    
    @PUT
    @Path("sensors/presence/{id}/unoccupied")
    public Response updatePresenceSensorUnoccupied(@PathParam("id") long id) {
    		
    	PresenceSensorState thisSensor = resEjb.findStateByInfoId(id,PresenceSensorState.class);
    	
    	if (thisSensor == null)
    		throw new WebApplicationException(Response.Status.NOT_FOUND);
    	
    	thisSensor.setOccupied(false);
    	
    	resEjb.updateManagedState(thisSensor);
    	
    	return Response.ok().build();
    }
    
    @DELETE
    public Response deleteStates() {
    	
    	synchronized(statesLock) {
    		resEjb.removeAllStates();
    	}
        
        return Response.ok().build();
    }
    
    private void sendLampStateUpdateMessage(LampState state, byte gatewayId, short address, int level) {
    	
    	try {
	    	jndiContext = new InitialContext();
	        ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup(JMSConstants.CONNECTION_FACTORY);
	        
	        Topic networkEventsTopic = (Topic) jndiContext.lookup(JMSConstants.NETWORK_EVENTS_TOPIC);
	        
	        jmsConnection = connectionFactory.createConnection();
	        jmsSession = jmsConnection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
	        MessageProducer producer = jmsSession.createProducer(networkEventsTopic);
	            
	        jmsConnection.start();
	        
	        PersistentInfo info = state.getDevice();
	        
	    	
			NetworkEvent event = new NetworkEvent(NetworkEvent.EventKind.LEVEL_CONTROL_VARIATION, 
					gatewayId, address,new byte[]{(byte)0,(byte)(level & 0x0FF)});
        
            ObjectMessage eventMessage = jmsSession.createObjectMessage(event);
            producer.send(eventMessage);
        } catch (JMSException ex) { 
        	
        } catch (NamingException e) {
        	
		} finally {
        	
        	try {
				jmsConnection.close();
			} catch (JMSException e) {
			}
        }
        
    }
}
