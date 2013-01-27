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
@Path("/network")
public final class NetworkResource {
	
    private NetworkEJB resEjb;
    
    private static int nodeId = 0;
    private static long linkId = 0;
    private static int jobId = 0;
    private static Object nodeLock = new Object();
    private static Object linkLock = new Object();
    private static Object jobLock = new Object();

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
    @Path("infrastructural")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Node> getInfrastructuralNodes() {        
        return resEjb.getInfrastructuralNodes();
    }
    
    @GET
    @Path("{gid}/{address}")
    @Produces(MediaType.APPLICATION_JSON)
    public Node getNode(@PathParam("gid") byte gid, @PathParam("address") short address) {
        
        Node node = resEjb.findNode(gid,address);
        
        if (node == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        return node;
    }
    
    @POST
    @Path("{gid}/{address}")
    public Response setNodeName(@PathParam("gid") byte gid, @PathParam("address") short address, @FormParam("location") String location) {
    	
    	synchronized(nodeLock) {
	        Node node = resEjb.findNode(gid,address);
	        
	        if (node == null) 
	            throw new WebApplicationException(Response.Status.NOT_FOUND);
	        
	        node.setLocation(location);
	        
	        resEjb.updateManaged(node);
    	}
        
    	return Response.ok().build();
    }
    
    @POST
    @Path("insert")
    public Response insertNode(@FormParam("gid") byte gid, 
    						   @FormParam("nuid") long nuid, 
    						   @FormParam("address") short address,
    						   @FormParam("logicalType") NodeLogicalType logicalType,
    						   @FormParam("manufacturer") Manufacturer manufacturer) {
    	
    	boolean existed = false;
    	
    	synchronized(nodeLock) {
    		Node.Builder nodeBuilder = new Node.Builder(++nodeId,gid,nuid,address);
					   
    		if (logicalType != null)
    			nodeBuilder.setLogicalType(logicalType);
    		if (manufacturer != null)
    			nodeBuilder.setManufacturer(manufacturer);

    		existed = resEjb.insertOrUpdateNode(nodeBuilder.build());
    	}
        
        
        if (existed)
        	return Response.ok().build();
        
        return Response.created(
                             uriInfo.getAbsolutePathBuilder()
                                    .path(Byte.toString(gid))
                                    .path(Short.toString(address))
                                    .build())
                           .build();
    }
    
    @POST
    @Path("links")
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
    @Produces(MediaType.APPLICATION_JSON)
    public List<Node> cleanupLinksAndNodes() {
    	
    	List<Node> cleanedNodes;
    	
    	synchronized(linkLock) {
    		resEjb.cleanupLinks();
    	}
    	synchronized(nodeLock) {
    		cleanedNodes = resEjb.cleanupNodesAndJobs();
    	}
    	
    	return cleanedNodes;
    }

	@GET
    @Path("links/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Link getLink(@PathParam("id") long id) {
        
        Link link = resEjb.findLinkById(id);
        
        if (link == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        return link;
    }
    
    @DELETE
    @Path("links/{id}")
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
    @Path("links")
    public Response deleteLinks() {
        
    	synchronized(linkLock) {
    		resEjb.removeAllLinks();
    	}
        
        return Response.ok().build();
    }
    
    @POST
    @Path("/update")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateNode(Node node) {
    	
        if (!resEjb.exists(node)) {
        	throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        synchronized(nodeLock) {
        	resEjb.updateManaged(node);	
        }	
        
        return Response.ok().build();
    }
      
    @DELETE
    @Path("{gid}/{address}")
    public Response deleteNode(@PathParam("gid") byte gid, @PathParam("address") short address) {
        
    	boolean existed;
    	synchronized(nodeLock) {
    		existed = resEjb.removeNode(gid,address);
    	}
    	
        if (!existed) {
        	throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        return Response.ok().build();
    }    

    @DELETE
    public Response deleteNodes() {
        
    	synchronized(nodeLock) {
    		resEjb.removeAllNodes();
    	}
        
        return Response.ok().build();
    }
    
    // curl -X POST http://localhost:8080/easyhome/rest/network/jobs -H "Content-Type: application/x-www-form-urlencoded" --data-binary "type=NODE_DESCR_REQUEST&gid=1&nuid=392342391&address=24&endpoint=7"
    @POST
    @Path("/jobs")
    public Response addJob(@FormParam("type") NetworkJobType type,
    					   @FormParam("gid") byte gatewayId,
    					   @FormParam("address") short address,
    					   @DefaultValue("127") @FormParam("endpoint") byte endpoint,
    					   @DefaultValue("0") @FormParam("tsn") byte tsn) {
    	
    	int newJobId;
    	
    	synchronized(jobLock) {
    		newJobId = ++jobId;
	    	resEjb.insertJob(newJobId, type, gatewayId, address, endpoint, tsn);
    	}
    	
    	return Response.created(
    					uriInfo.getAbsolutePathBuilder()
    						   .path(String.valueOf(newJobId))
    						   .build())
    					.build();
    }
    
    @GET
    @Path("/jobs")
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
    @Path("/jobs/{jobid}")
    @Produces(MediaType.APPLICATION_JSON)
    public NetworkJob getJobById(@PathParam("jobid") int jobId) {
        
        NetworkJob job = resEjb.findJobById(jobId);
        
        if (job == null)
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        return job;
    }    
    
    @DELETE
    @Path("/jobs/{jobid}")
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
    @Path("/jobs")
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
