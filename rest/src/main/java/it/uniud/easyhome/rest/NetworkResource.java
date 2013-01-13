package it.uniud.easyhome.rest;


import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.devices.HomeAutomationDevice;
import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.network.NetworkEJB;
import it.uniud.easyhome.network.NetworkJob;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.NodeLogicalType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.*;
import javax.ws.rs.*;

import org.codehaus.jettison.json.JSONException;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

/** Handles the access to the network of nodes */
@Path("/network")
public final class NetworkResource {
	
    private NetworkEJB resEjb;
    
    private static int jobId = 0;

    public NetworkResource() throws NamingException {
    	resEjb = (NetworkEJB) new
                InitialContext().lookup("java:global/easyhome/NetworkEJB");
    }
    
    @Context
    private UriInfo uriInfo;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Node> getNodes() {
        
        return resEjb.getNodes();
        
    }
    
    @GET
    @Path("{nodeid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Node getNode(@PathParam("nodeid") long nodeId) {
        
        Node node = resEjb.findNodeById(nodeId);
        
        if (node == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        return node;
    }
    
    @POST
    @Path("{nodeid}")
    public Response setNodeName(@PathParam("nodeid") long nodeId, @FormParam("location") String location) {
    	
        Node node = resEjb.findNodeById(nodeId);
        
        if (node == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        node.setLocation(location);
        
        resEjb.updateManaged(node);
    	
    	return Response.ok().build();
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateOrInsertNode(Node node) {
    	
        boolean existed = resEjb.insertOrUpdateNode(node);
        
        if (!existed)
            return Response.created(
                             uriInfo.getAbsolutePathBuilder()
                                    .path(String.valueOf(node.getId()))
                                    .build())
                           .build();
        else
            return Response.ok().build();
    }
      
    @DELETE
    @Path("{nodeid}")
    public Response deleteNode(@PathParam("nodeid") long nodeId) {
        
        boolean existed = resEjb.removeNodeById(nodeId);
        
        if (!existed) {
        	throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        return Response.ok().build();
    }    
    
    

    @DELETE
    public Response deleteNodes() {
        
    	resEjb.removeAllNodes();
        
        return Response.ok().build();
    }
    
    // curl -X POST http://localhost:8080/easyhome/rest/network/jobs -H "Content-Type: application/x-www-form-urlencoded" --data-binary "type=NODE_DESCR_REQUEST&gid=1&nuid=392342391&address=24&endpoint=7"
    @POST
    @Path("/jobs")
    public Response addJob(@FormParam("type") NetworkJobType type,
    					   @FormParam("gid") byte gatewayId,
    					   @FormParam("nuid") long nuid,
    					   @FormParam("address") short address,
    					   @DefaultValue("127") @FormParam("endpoint") byte endpoint) {
    	
    	int newJobId;
    	
    	
    	synchronized(this) {
    		newJobId = ++jobId;
	    	resEjb.insertJob(newJobId, type, gatewayId, nuid, address, endpoint);
    	}
    	
    	return Response.created(
    					uriInfo.getAbsolutePathBuilder()
    						   .path(String.valueOf(newJobId))
    						   .build())
    					.build();
    }
    
    @POST
    @Path("/jobs/{jobId}/reset")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response resetJobDate(@PathParam("jobId") int jobId) {
    	
    	System.out.println("JobId: " + jobId);
    	
        boolean existed = resEjb.resetJobDate(jobId);
        
        if (!existed) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        return Response.ok().build();
    }
    
    @GET
    @Path("/jobs")
    @Produces(MediaType.APPLICATION_JSON)
    public List<NetworkJob> getNumJobs(@QueryParam("type") NetworkJobType type) {
    	
    	if (type != null)
    		return resEjb.getJobsByType(type);
    	
    	return resEjb.getJobs();
    }
    
    @GET
    @Path("/jobs/{jobid}")
    @Produces(MediaType.APPLICATION_JSON)
    public NetworkJob getJob(@PathParam("jobid") int jobId) {
        
        NetworkJob job = resEjb.findJobById(jobId);
        
        if (job == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        return job;
    }
    
    @DELETE
    @Path("/jobs/{jobid}")
    public Response deleteJob(@PathParam("jobid") int jobId) {
        
        boolean existed = resEjb.removeJobById(jobId);
        
        if (!existed) {
        	throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        return Response.ok().build();
    } 
    
    @POST
    @Path("/jobs/delete")
    public Response deleteJobs(@FormParam("type") NetworkJobType type, 
    						   @FormParam("gid") byte gatewayId, 
    						   @FormParam("address") short address, 
    						   @DefaultValue("127") @FormParam("endpoint") byte endpoint) {
    	
    	int numRemoved;
    	
    	if (endpoint == 127)
    		numRemoved = resEjb.removeJobs(type, gatewayId, address);
    	else
    		numRemoved = resEjb.removeJobs(type, gatewayId, address,endpoint);
    	
    	if (numRemoved == 0)
    		throw new WebApplicationException(Response.Status.NOT_FOUND);
    	
    	return Response.ok().build();
    }
    
    @DELETE
    @Path("/jobs")
    public Response deleteJobs() {
        
    	resEjb.removeAllJobs();
        
        return Response.ok().build();
    }

       
}
