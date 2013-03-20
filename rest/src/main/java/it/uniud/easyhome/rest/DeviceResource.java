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
@Path(RestPaths.DEVICES)
public final class DeviceResource {
	
    private PersistentInfoEJB resEjb;

    public DeviceResource() throws NamingException {
    	resEjb = (PersistentInfoEJB) new InitialContext().lookup("java:global/easyhome/" + PersistentInfoEJB.class.getSimpleName());
    }
    
    @Context
    private UriInfo uriInfo;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<PersistentInfo> getDevicesByLocation(@QueryParam("locationId") int locationId) {
    	
        return resEjb.getPersistentInfosByLocationId(locationId);
    }
}
