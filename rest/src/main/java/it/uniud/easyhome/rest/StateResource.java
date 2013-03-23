package it.uniud.easyhome.rest;

import it.uniud.easyhome.devices.states.*;
import it.uniud.easyhome.ejb.StateEJB;
import it.uniud.easyhome.exceptions.MultipleLinkException;
import it.uniud.easyhome.network.*;

import java.util.List;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.*;
import javax.ws.rs.*;

@Path(RestPaths.STATES)
public final class StateResource {
	
    private StateEJB resEjb;
    
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
    
    @POST
    @Path("sensors/presence/{id}")
    public Response updatePresenceSensorOccupation(@PathParam("id") long id,
    								@FormParam("occupied") boolean occupied) {
    		
    	PresenceSensorState thisSensor = resEjb.findStateByInfoId(id,PresenceSensorState.class);
    	
    	if (thisSensor == null)
    		throw new WebApplicationException(Response.Status.NOT_FOUND);
    	
    	thisSensor.setOccupied(occupied);
    	
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
}