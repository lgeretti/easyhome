package it.uniud.easyhome.rest;


import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.devices.HomeAutomationDevice;
import it.uniud.easyhome.network.NetworkEJB;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.NodeLogicalType;

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
