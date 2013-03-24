package it.uniud.easyhome.rest;

import it.uniud.easyhome.devices.Functionality;
import it.uniud.easyhome.devices.FunctionalityType;
import it.uniud.easyhome.devices.PersistentInfo;
import it.uniud.easyhome.ejb.FunctionalityEJB;
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
    public List<Functionality> getFunctionalities(@QueryParam("infoId") long infoId,
    											  @QueryParam("functionalityType") FunctionalityType funcType) {
    	
    	if (infoId != 0)
    		return resEjb.getFunctionalitiesByInfoId(infoId);
    	
    	if (funcType != null)
    		return resEjb.getFunctionalitiesByFunctionalityType(funcType);
    	else
    		return resEjb.getFunctionalities();
    }
    
    // curl -X POST http://localhost:8080/easyhome/rest/functionalities -H "Content-Type: application/x-www-form-urlencoded" --data-binary "name=Link&deviceId=2&imgPath='img/link.svg'&help='Seleziona il dispositivo da linkare alla lampada'" 
    @POST
    public Response insertFunctionality(@FormParam("name") String name, 
    									@FormParam("type") FunctionalityType type,
    									@FormParam("deviceId") long deviceId, 
    								  	@FormParam("imgPath") String imgPath,
    								  	@FormParam("help") String help) {
    	
    	PersistentInfo device = resEjb.findPersistentInfoById(deviceId);
    		
	    if (device == null)
	    	throw new WebApplicationException(Response.Status.BAD_REQUEST);
    			
	    synchronized(funcLock) {
	    	
    		resEjb.insertFunctionality(new Functionality(++functionalityId,name,type,device,imgPath,help));
    		
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
