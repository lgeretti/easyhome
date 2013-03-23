package it.uniud.easyhome.rest;

import it.uniud.easyhome.ejb.LocationEJB;
import it.uniud.easyhome.network.*;
import it.uniud.easyhome.processing.NetworkUpdateProcess;
import it.uniud.easyhome.processing.NodeDiscoveryRequestProcess;

import java.util.List;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.*;
import javax.ws.rs.*;

/** Handles the locations available for the building */
@Path(RestPaths.LOCATIONS)
public final class LocationResource {
	
    private LocationEJB resEjb;
    
    private static int locationId = 0;
    private static Object locationLock = new Object();

    public LocationResource() throws NamingException {
    	resEjb = (LocationEJB) new InitialContext().lookup("java:global/easyhome/" + LocationEJB.class.getSimpleName());
    }
    
    @Context
    private UriInfo uriInfo;
    
    @POST
    public Response insertLocation(@FormParam("name") String name, 
    						   @FormParam("type") LocationType type,
    						   @FormParam("imgPath") String imgPath) {
    	
    	int thisLocId;
    	
    	synchronized(locationLock) {
    		
    		Location loc = resEjb.findLocation(name); 
    			
    		if (loc == null) {
	    		thisLocId = ++locationId;
	    		
	    		resEjb.insertLocation(thisLocId,name,type,imgPath);
	    		
	            return Response.created(
                        uriInfo.getAbsolutePathBuilder()
                               .path(Long.toString(thisLocId))
                               .build())
                      .build();
    		} else {
    			return Response.ok().build();
    		}
    	}
    }

	@GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Location> getLocations() {
        
        return resEjb.getLocations();
    }
    
	@GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Location getLocation(@PathParam("id") int id) {
        
        Location loc = resEjb.findLocationById(id);
        
        if (loc == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        return loc;
    }
	
	@POST
    @Path("{id}/occupied")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setOccupied(@PathParam("id") int id) {
        
        Location loc = resEjb.findLocationById(id);
        
        if (loc == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        loc.setOccupied(true);
        
        resEjb.updateUnmanaged(loc);
        
        return Response.ok().build();
    }
	
	@POST
    @Path("{id}/unoccupied")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setUnoccupied(@PathParam("id") int id) {
        
        Location loc = resEjb.findLocationById(id);
        
        if (loc == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        loc.setOccupied(false);
        
        resEjb.updateUnmanaged(loc);
        
        return Response.ok().build();
    }
    
    @DELETE
    @Path("{id}")
    public Response deleteLocation(@PathParam("id") int id) {
    	
    	boolean existed;
    	
    	synchronized(locationLock) {
        	existed = resEjb.removeLocation(id);
    	}
    	
        if (!existed) {
        	throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        return Response.ok().build();
    }
    
    @DELETE
    public Response deleteLocations() {
        
    	synchronized(locationLock) {
    		resEjb.removeAllLocations();
    	}
        
        return Response.ok().build();
    }
}
