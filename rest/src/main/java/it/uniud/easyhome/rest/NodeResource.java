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
@Path(RestPaths.NODES)
public final class NodeResource {
	
    private NodeEJB resEjb;
    
    private static long nodeId = 0;
    private static Object nodeLock = new Object();

    public NodeResource() throws NamingException {
    	resEjb = (NodeEJB) new InitialContext().lookup("java:global/easyhome/" + NodeEJB.class.getSimpleName());
    }
    
    @Context
    private UriInfo uriInfo;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Node> getNodes(@QueryParam("gatewayId") byte gatewayId, 
 		   					   @QueryParam("nuid") long nuid) {        
        return resEjb.getNodes(gatewayId,nuid);
    }
    
    @GET
    @Path("infrastructural")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Node> getInfrastructuralNodes() {        
        return resEjb.getInfrastructuralNodes();
    }
    
    @GET
    @Path("{gatewayId}/{address}")
    @Produces(MediaType.APPLICATION_JSON)
    public Node getNode(@PathParam("gatewayId") byte gatewayId, @PathParam("address") short address) {
        
        Node node = resEjb.findNode(gatewayId,address);
        
        if (node == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        return node;
    }
    
    /**
     * Inserts of updates a node.
     * Persistent info on the node supersedes the provided values.
     */
    @POST
    @Path("insert")
    public Response insertOrUpdateNode(@FormParam("gatewayId") byte gatewayId, 
    						   @FormParam("nuid") long nuid, 
    						   @FormParam("address") short address,
    						   @FormParam("logicalType") NodeLogicalType logicalType,
    						   @FormParam("manufacturer") Manufacturer manufacturer,
    						   @FormParam("locationName") String locationName,
    						   @FormParam("name") String name) {
    	
    	boolean existed = false;
    	
    	synchronized(nodeLock) {
    		
    		long newNodeId = nodeId + 1;
    		
    		Node.Builder nodeBuilder = new Node.Builder(newNodeId,gatewayId,nuid,address);
					   
    		if (locationName != null) {
    			Location loc = resEjb.getLocation(locationName);
    		
	    		if (loc == null)
	    			throw new WebApplicationException(Response.Status.BAD_REQUEST);
	    		else 
	    			nodeBuilder.setLocation(loc);
    		}
    		
    		if (logicalType != null)
    			nodeBuilder.setLogicalType(logicalType);
    		if (manufacturer != null)
    			nodeBuilder.setManufacturer(manufacturer);
    		if (name != null)
    			nodeBuilder.setName(name);

    		existed = resEjb.insertOrUpdateNode(nodeBuilder.build());
    		
    		// This is in order to avoid increasing the node id when not necessary
    		if (!existed)
    			nodeId++;
    	}
        
        if (existed)
        	return Response.ok().build();
        
        return Response.created(
                             uriInfo.getAbsolutePathBuilder()
                                    .path(Byte.toString(gatewayId))
                                    .path(Short.toString(address))
                                    .build())
                           .build();
    }
    
    @POST
    @Path("cleanup")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Node> cleanupNodes() {
    	
    	List<Node> cleanedNodes;
    	
    	synchronized(nodeLock) {
    		
    		cleanedNodes = resEjb.cleanupNodes();
    	}
    	
    	return cleanedNodes;
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
    @Path("{gatewayId}/{address}")
    public Response deleteNode(@PathParam("gatewayId") byte gatewayId, @PathParam("address") short address) {
        
    	boolean existed;
    	synchronized(nodeLock) {
    		existed = resEjb.removeNode(gatewayId,address);
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
       
}
