package it.uniud.easyhome.rest;

import it.uniud.easyhome.network.*;

import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.*;
import javax.ws.rs.*;

@Path(RestPaths.PERSISTENTINFO)
public final class PersistentInfoResource {
	
    private PersistentInfoEJB resEjb;
    private static Object infoLock = new Object();
 
    private static int nodePersistentInfoId = 0;

    public PersistentInfoResource() throws NamingException {
    	resEjb = (PersistentInfoEJB) new InitialContext().lookup("java:global/easyhome/" + PersistentInfoEJB.class.getSimpleName());
    }
    
    @Context
    private UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<PersistentInfo> getNodePersistentInfos() {
        
        return resEjb.getPersistentInfos();
    }
    
    @GET
    @Path("{gatewayId}/{nuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public PersistentInfo getNodePersistentInfo(@PathParam("gatewayId") byte gatewayId, @PathParam("nuid") long nuid) {
    	
        PersistentInfo info = resEjb.getPersistentInfo(gatewayId,nuid);
        
        if (info == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        return info;
    }
    
    // curl -X POST http://localhost:8080/easyhome/rest/persistentinfo/2/0 -H "Content-Type: application/x-www-form-urlencoded" --data-binary "locationName=Salotto&locationType=LIVINGROOM&imgPath='img/livingroom.svg'" 
    @POST
    @Path("{gatewayId}/{nuid}")
    public Response insertOrUpdatePersistentInfo(@PathParam("gatewayId") byte gatewayId, 
    											@PathParam("nuid") long nuid, 
    								  			@FormParam("name") String name, 
    								  			@FormParam("locationName") String locationName,
    								  			@FormParam("deviceType") FunctionalityContainerType funcContainerType,
    								  			@FormParam("imgPath") String imgPath,
    								  			@FormParam("help") String help) {
    	
    	if (funcContainerType == null)
    		funcContainerType = FunctionalityContainerType.NONE;
    	
    	synchronized(infoLock) {
    		
    		PersistentInfo info = resEjb.getPersistentInfo(gatewayId,nuid);
    		
    		Location loc = null;
    		if (locationName != null) {
    			loc = resEjb.getLocation(locationName);
    		
	    		if (loc == null)
	    			throw new WebApplicationException(Response.Status.BAD_REQUEST);
    		}
    		
    		if (info == null) {
    			
    			resEjb.insertPersistentInfo(new PersistentInfo(++nodePersistentInfoId,gatewayId,nuid,name,loc,funcContainerType,imgPath,help));
    			return Response.created(
                        	uriInfo.getAbsolutePathBuilder()
                        	.path(Byte.toString(gatewayId))
                        	.path(Long.toString(nuid))
                        	.build())
                        .build();
    		}
    		
    		if (name != null)
    			info.setName(name);
    		if (loc != null)
    			info.setLocation(loc);
    			
    		resEjb.updatedUnmanaged(info);
    	}
        
    	return Response.ok().build();
    }
    
    @POST
    @Path("{gatewayId}/{nuid}/changeLocation")
    public Response changeLocation(@PathParam("gatewayId") byte gatewayId, 
								   @PathParam("nuid") long nuid,
								   @FormParam("locationName") String locationName) {
	
    	synchronized(infoLock) {
    		
    		PersistentInfo info = resEjb.getPersistentInfo(gatewayId,nuid);
    		
    		Location loc = null;
    		if (locationName != null) {
    			loc = resEjb.getLocation(locationName);
    		
	    		if (loc == null)
	    			throw new WebApplicationException(Response.Status.BAD_REQUEST);
    		}
    		
    		info.setLocation(loc);
    			
    		resEjb.updatedUnmanaged(info);
    	}
        
    	return Response.ok().build();
    }
    
    @DELETE
    @Path("{gatewayId}/{nuid}")
    public Response deleteInfo(@PathParam("gatewayId") byte gatewayId, @PathParam("nuid") long nuid) {
    	
        boolean found = resEjb.removeInfo(gatewayId, nuid);
        
        if (!found) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);
      
        return Response.ok().build();
    }
       
    @DELETE
    public Response deleteAll() {
    	
    	synchronized(infoLock) {
    		resEjb.removeAllPersistedInfos();
    	}
    	
    	return Response.ok().build();
    }
}
