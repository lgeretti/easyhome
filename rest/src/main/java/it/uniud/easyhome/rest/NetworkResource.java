package it.uniud.easyhome.rest;


import it.uniud.easyhome.network.Node;

import java.util.List;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.ws.rs.core.*;
import javax.ws.rs.*;

/** Handles the access to the network of nodes */
@RequestScoped
@Path("/network")
public final class NetworkResource {
    
    @Inject
    private static NetworkResourceEJB resEjb;
    
    @Context
    private UriInfo uriInfo;
    
    @GET
    @Path("check")
    @Produces(MediaType.TEXT_PLAIN)
    public String check() {
    	if (resEjb == null)
    		return "Failure";
    	return "Success";
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
