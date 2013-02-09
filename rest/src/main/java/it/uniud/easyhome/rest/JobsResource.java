package it.uniud.easyhome.rest;

import it.uniud.easyhome.network.*;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.*;
import javax.ws.rs.*;

@Path(RestPaths.JOBS)
public final class JobsResource {
	
    private JobsEJB resEjb;

    private static int jobId = 0;
    private static Object jobLock = new Object();

    public JobsResource() throws NamingException {
    	resEjb = (JobsEJB) new  InitialContext().lookup("java:global/easyhome/JobsEJB");
    }
    
    @Context
    private UriInfo uriInfo;
    
    // curl -X POST http://localhost:8080/easyhome/rest/network/jobs -H "Content-Type: application/x-www-form-urlencoded" --data-binary "type=NODE_DESCR_REQUEST&gid=1&nuid=392342391&address=24&endpoint=7"
    @POST
    public Response addJob(@FormParam("type") NetworkJobType type,
    					   @FormParam("gid") byte gatewayId,
    					   @FormParam("address") short address,
    					   @DefaultValue("127") @FormParam("endpoint") byte endpoint,
    					   @DefaultValue("0") @FormParam("tsn") byte tsn,
    					   @DefaultValue("0") @FormParam("payload") byte payload) {
    	
    	int newJobId;
    	
    	synchronized(jobLock) {
    		newJobId = ++jobId;
	    	resEjb.insertJob(newJobId, type, gatewayId, address, endpoint, tsn, payload);
    	}
    	
    	return Response.created(
    					uriInfo.getAbsolutePathBuilder()
    						   .path(String.valueOf(newJobId))
    						   .build())
    					.build();
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<NetworkJob> getLatestJobs(
    		   @QueryParam("type") NetworkJobType type, 
    		   @DefaultValue("0") @QueryParam("gid") byte gatewayId, 
    		   @DefaultValue("0") @QueryParam("address") short address, 
			   @DefaultValue("127") @QueryParam("endpoint") byte endpoint,
			   @DefaultValue("0") @QueryParam("tsn") byte tsn) {
    	
    	return resEjb.getLatestJobs(type,gatewayId,address,endpoint,tsn);
    }
    
    @GET
    @Path("{jobid}")
    @Produces(MediaType.APPLICATION_JSON)
    public NetworkJob getJobById(@PathParam("jobid") int jobId) {
        
        NetworkJob job = resEjb.findJobById(jobId);
        
        if (job == null)
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        return job;
    }    
    
    @DELETE
    @Path("{jobid}")
    public Response deleteJob(@PathParam("jobid") int jobId) {
        
    	boolean existed;
    	synchronized(jobLock) {
    		existed = resEjb.removeJobById(jobId);	
    	}
        
        if (!existed) {
        	throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        return Response.ok().build();
    } 
    
    @DELETE
    public Response deleteJobs(@QueryParam("type") NetworkJobType type, 
    						   @QueryParam("gid") byte gatewayId, 
    						   @QueryParam("address") short address, 
    						   @DefaultValue("127") @QueryParam("endpoint") byte endpoint) {
    	
    	synchronized(jobLock) {
	    	if (type == null) 
	    		resEjb.removeAllJobs();
		    else {
		    	int numRemoved;
		    	
		    	if (endpoint == 127)
		    		numRemoved = resEjb.removeJobs(type, gatewayId, address);
		    	else
		    		numRemoved = resEjb.removeJobs(type, gatewayId, address, endpoint);
		    	
		    	if (numRemoved == 0)
		    		throw new WebApplicationException(Response.Status.NOT_FOUND);
	    	}
    	}
    	
    	return Response.ok().build();
    }
       
}
