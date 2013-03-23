package it.uniud.easyhome.rest;

import it.uniud.easyhome.ejb.PairingEJB;
import it.uniud.easyhome.network.*;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.*;
import javax.ws.rs.*;

@Path(RestPaths.PAIRINGS)
public final class PairingResource {
	
    private PairingEJB resEjb;
    
    private static long pairingId = 0;
    private static Object pairingLock = new Object();

    public PairingResource() throws NamingException {
    	resEjb = (PairingEJB) new InitialContext().lookup("java:global/easyhome/" + PairingEJB.class.getSimpleName());
    }
    
    @Context
    private UriInfo uriInfo;
    
    @POST
    public Response insertPairing(@FormParam("sourceId") long sourceId,
    						      @FormParam("destinationId") long destinationId) {
    	
    	long thisPairingId;
    	
    	PersistentInfo source = resEjb.findPersistentInfoById(sourceId);
    	PersistentInfo destination = resEjb.findPersistentInfoById(destinationId);
    	
    	if (source == null || destination == null)
	    	throw new WebApplicationException(Response.Status.BAD_REQUEST);    		
    	
    	synchronized(pairingLock) { 
    		thisPairingId = ++pairingId;
    		
    		resEjb.insertPairing(thisPairingId, source, destination);
    		
            return Response.created(
                    uriInfo.getAbsolutePathBuilder()
                           .path(Long.toString(thisPairingId))
                           .build())
                  .build();
    	}
    }

	@GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Pairing getPairing(@PathParam("id") long id) {
        
		Pairing pairing = resEjb.findPairingById(id);
        
        if (pairing == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        return pairing;
    }
    
    @DELETE
    @Path("{id}")
    public Response deletePairing(@PathParam("id") long id) {
    	
    	boolean existed;
    	
    	synchronized(pairingLock) {
        	existed = resEjb.removePairing(id);
    	}
    	
        if (!existed) {
        	throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        return Response.ok().build();
    }
    
    @DELETE
    public Response deletePairings() {
        
    	synchronized(pairingLock) {
    		resEjb.removeAllPairings();
    	}
        
        return Response.ok().build();
    }
}
