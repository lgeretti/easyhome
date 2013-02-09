package it.uniud.easyhome.rest;

import it.uniud.easyhome.exceptions.MultipleLinkException;
import it.uniud.easyhome.network.*;
import it.uniud.easyhome.processing.NetworkUpdateProcess;
import it.uniud.easyhome.processing.NodeDiscoveryRequestProcess;

import java.util.List;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.*;
import javax.ws.rs.*;

/** Handles the access to the network of nodes */
@Path(RestPaths.LINKS)
public final class LinksResource {
	
    private LinksEJB resEjb;
    
    private static long linkId = 0;
    private static Object linkLock = new Object();

    public LinksResource() throws NamingException {
    	resEjb = (LinksEJB) new InitialContext().lookup("java:global/easyhome/LinksEJB");
    }
    
    @Context
    private UriInfo uriInfo;
    
    @POST
    public Response insertOrUpdateLink(@FormParam("gatewayId") byte gatewayId, 
    						   @FormParam("sourceNuid") long sourceNuid, 
    						   @FormParam("sourceAddress") short sourceAddress,
    						   @FormParam("destinationNuid") long destinationNuid,
    						   @FormParam("destinationAddress") short destinationAddress) throws MultipleLinkException {
    	
    	long thisLinkId;
    	
    	LocalCoordinates source = new LocalCoordinates(sourceNuid,sourceAddress);
    	LocalCoordinates destination = new LocalCoordinates(destinationNuid,destinationAddress);
    	
    	synchronized(linkLock) {
    		
    		Link link = resEjb.findLink(gatewayId,source,destination); 
    			
    		if (link == null) {
	    		thisLinkId = ++linkId;
	    		
	    		resEjb.insertLink(thisLinkId, gatewayId, source, destination);
	    		
	            return Response.created(
                        uriInfo.getAbsolutePathBuilder()
                               .path(Long.toString(thisLinkId))
                               .build())
                      .build();
    		} else {
    			resEjb.updateLink(link);
    			
    			return Response.ok().build();
    		}
    	}
    }
    
	@POST
	@Path("cleanup")
	public Response cleanupLinks() {
    	synchronized(linkLock) {
    		resEjb.cleanupLinks(NetworkUpdateProcess.KEEP_LINK_ALIVE_MS);
    	}
    	return Response.ok().build();
	}

	@GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Link getLink(@PathParam("id") long id) {
        
        Link link = resEjb.findLinkById(id);
        
        if (link == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        return link;
    }
    
    @DELETE
    @Path("{id}")
    public Response deleteLink(@PathParam("id") long id) {
    	
    	boolean existed;
    	
    	synchronized(linkLock) {
        	existed = resEjb.removeLink(id);
    	}
    	
        if (!existed) {
        	throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        return Response.ok().build();
    }
    
    @DELETE
    public Response deleteLinks() {
        
    	synchronized(linkLock) {
    		resEjb.removeAllLinks();
    	}
        
        return Response.ok().build();
    }
}
