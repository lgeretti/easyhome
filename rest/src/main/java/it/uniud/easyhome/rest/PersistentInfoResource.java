package it.uniud.easyhome.rest;

import it.uniud.easyhome.exceptions.MultipleLinkException;
import it.uniud.easyhome.network.*;

import java.util.List;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.*;
import javax.ws.rs.*;

/** Handles the access to the network of nodes */
@Path("/persistentinfo")
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
    public List<NodePersistentInfo> getNodePersistentInfos() {
        
        return resEjb.getPersistentInfos();
    }
    
    @GET
    @Path("{gatewayId}/{nuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public NodePersistentInfo getNodePersistentInfo(@PathParam("gatewayId") byte gatewayId, @PathParam("nuid") long nuid) {
    	
        NodePersistentInfo info = resEjb.getPersistentInfo(gatewayId,nuid);
        
        if (info == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        return info;
    }
    
    // curl -X POST http://localhost:8080/easyhome/rest/persistentinfo/2/0 -H "Content-Type: application/x-www-form-urlencoded" --data-binary "locationName=Salotto&locationType=LIVINGROOM" 
    @POST
    @Path("{gatewayId}/{nuid}")
    public Response insertOrUpdatePersistentInfo(@PathParam("gatewayId") byte gatewayId, 
    											@PathParam("nuid") long nuid, 
    								  			@FormParam("name") String name, 
    								  			@FormParam("locationName") String locationName,
    								  			@FormParam("locationType") LocationType locationType) {
    	
    	synchronized(infoLock) {
    		
    		Location location = null;
    		if (locationName != null && locationType != null)
    			location = new Location(locationName,locationType);
    		
    		NodePersistentInfo info = resEjb.getPersistentInfo(gatewayId,nuid);
    		
    		if (info == null) {
    			
    			resEjb.insertPersistentInfo(new NodePersistentInfo(++nodePersistentInfoId,gatewayId,nuid,name,location));
    			return Response.created(
                        	uriInfo.getAbsolutePathBuilder()
                        	.path(Byte.toString(gatewayId))
                        	.path(Long.toString(nuid))
                        	.build())
                        .build();
    		}
    		
    		if (name != null)
    			info.setName(name);
    		if (location != null)
    			info.setLocation(location);
    			
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
