package it.uniud.easyhome.rest;

import it.uniud.easyhome.exceptions.MultipleLinkException;
import it.uniud.easyhome.network.*;

import java.util.List;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.*;
import javax.ws.rs.*;

@Path(RestPaths.FUNCTIONALITIES)
public final class FunctionalityResource {
	
    private FunctionalityEJB resEjb;

    public FunctionalityResource() throws NamingException {
    	resEjb = (FunctionalityEJB) new InitialContext().lookup("java:global/easyhome/" + FunctionalityEJB.class.getSimpleName());
    }
    
    @Context
    private UriInfo uriInfo;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Functionality> getFunctionalitiesByDeviceId(@QueryParam("deviceId") long deviceId) {
    	
        return resEjb.getFunctionalitiesByDeviceId(deviceId);
    }
    
    
}
