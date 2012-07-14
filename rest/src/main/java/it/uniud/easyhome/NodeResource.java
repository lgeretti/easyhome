package it.uniud.easyhome;

import java.net.URI;

import javax.ws.rs.core.*;
import javax.ws.rs.*;

@Path("/nodes")
public class NodeResource {
    
    @Context
    private UriInfo uriInfo;
    
    @GET
    @Path("{nodeid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Node getNode(@PathParam("nodeid") int nodeId) {
        
        return new Node(nodeId,"test");
    }
    
    @PUT
    @Consumes("application/json")
    public Response changeNode(Node node) {
    	
	    return Response.created(
	                         uriInfo.getAbsolutePathBuilder()
	                                .path(String.valueOf(node.getId()))
	                                .build())
	                    .build();
    }
      
}
