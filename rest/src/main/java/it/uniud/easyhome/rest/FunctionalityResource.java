package it.uniud.easyhome.rest;

import it.uniud.easyhome.network.*;

import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.*;
import javax.ws.rs.*;

@Path(RestPaths.FUNCTIONALITIES)
public final class FunctionalityResource {
	
    private FunctionalityEJB resEjb;
    
    private static Object funcLock = new Object();
    
    private static int functionalityId = 0;

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
    
    // curl -X POST http://localhost:8080/easyhome/rest/functionalities -H "Content-Type: application/x-www-form-urlencoded" --data-binary "name=Link&deviceId=2&imgPath='img/link.svg'&help='Seleziona il dispositivo da linkare alla lampada'" 
    @POST
    public Response insertFunctionality(@FormParam("name") String name, 
    									@FormParam("deviceId") long deviceId, 
    								  	@FormParam("imgPath") String imgPath,
    								  	@FormParam("help") String help) {
    	
    	NodePersistentInfo info = resEjb.findPersistentInfoById(deviceId);
    		
	    if (info == null)
	    	throw new WebApplicationException(Response.Status.BAD_REQUEST);
    			
	    synchronized(funcLock) {
	    	
    		resEjb.insertFunctionality(new Functionality(++functionalityId,name,info,imgPath,help));
    		
    		return Response.created(
                	uriInfo.getAbsolutePathBuilder()
                	.path(Long.toString(functionalityId))
                	.build())
                .build();
    	}
    }
    
    @DELETE
    @Path("{id}")
    public Response deleteFunctionality(@PathParam("id") long id) {
    	
        Functionality func = resEjb.findFunctionalityById(id);
        
        if (func == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        synchronized(funcLock) {
        	resEjb.removeUnmanaged(func);
        }
        
        return Response.ok().build();
    }
       
    @DELETE
    public Response deleteAll() {
    	
    	synchronized(funcLock) {
    		resEjb.removeAllFunctionalities();
    	}
    	
    	return Response.ok().build();
    }    
    
}
