package it.uniud.easyhome.rest;


import it.uniud.easyhome.network.Node;

import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.*;
import javax.ws.rs.*;

/** Handles the access to the network of nodes */
@Path("/network")
public final class NetworkResource {

    private NetworkResourceEJB resEjb;

    public NetworkResource() throws NamingException {
    	resEjb = (NetworkResourceEJB) new
                InitialContext().lookup("java:global/easyhome/NetworkResourceEJB");
    }
    
    @Context
    private UriInfo uriInfo;
    
    @GET
    @Path("checkInjection")
    @Produces(MediaType.TEXT_PLAIN)
    public String number() {
    	if (resEjb == null)
    		return "EJB not injected";
    	else return resEjb.getStatusMessage();
    }    
    
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
        
        if (existed) {
        	throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        return Response.ok().build();
    }    
    
    /** Removes all the nodes.
     * 
     */
    @DELETE
    public Response clear() {
        
    	resEjb.removeAllNodes();
        
        return Response.ok().build();
    }
       
}
