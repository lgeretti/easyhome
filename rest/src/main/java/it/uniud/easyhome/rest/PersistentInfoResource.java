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
    	resEjb = (PersistentInfoEJB) new
                InitialContext().lookup("java:global/easyhome/PersistentInfoEJB");
    }
    
    @Context
    private UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<NodePersistentInfo> getNodePersistentInfos() {
        
        return resEjb.getPersistentInfos();
    }
    
    @GET
    @Path("{gid}/{exaNuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public NodePersistentInfo getNodePersistentInfo(@PathParam("gid") byte gid, @PathParam("exaNuid") String exaNuid) {
        
    	long nuid;
    	try {
    		nuid = Long.decode(exaNuid);
    	} catch (NumberFormatException ex) {
    		throw new WebApplicationException(Response.Status.BAD_REQUEST);
    	}
    	
        NodePersistentInfo info = resEjb.getPersistentInfo(gid,nuid);
        
        if (info == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        return info;
    }
    
    // curl -X POST http://localhost:8080/easyhome/rest/persistentinfo/2/0 -H "Content-Type: application/x-www-form-urlencoded" --data-binary "name=Gateway&location=Salotto" 
    @POST
    @Path("{gid}/{exaNuid}")
    public Response insertOrUpdatePersistentInfo(@PathParam("gid") byte gid, 
    											@PathParam("exaNuid") String exaNuid, 
    								  			@FormParam("name") String name, 
    								  			@FormParam("location") String location) {
    	
    	long nuid;
    	try {
    		nuid = Long.decode(exaNuid);
    	} catch (NumberFormatException ex) {
    		throw new WebApplicationException(Response.Status.BAD_REQUEST);
    	}
    	
    	synchronized(infoLock) {
    		
    		NodePersistentInfo info = resEjb.getPersistentInfo(gid,nuid);
    		if (info == null) {
    			resEjb.insertPersistentInfo(new NodePersistentInfo(++nodePersistentInfoId,gid,nuid,name,location));
    			return Response.created(
                        	uriInfo.getAbsolutePathBuilder()
                        	.path(Byte.toString(gid))
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
    @Path("{gid}/{nuid}")
    public Response deleteInfo(@PathParam("gid") byte gid, @PathParam("exaNuid") String exaNuid) {

    	long nuid;
    	try {
    		nuid = Long.decode(exaNuid);
    	} catch (NumberFormatException ex) {
    		throw new WebApplicationException(Response.Status.BAD_REQUEST);
    	}
    	
        boolean found = resEjb.removeInfo(gid, nuid);
        
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
