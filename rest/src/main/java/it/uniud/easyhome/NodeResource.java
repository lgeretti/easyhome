package it.uniud.easyhome;

import javax.ws.rs.core.*;
import javax.ws.rs.*;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

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
    
    /*
    @GET
    @Path("{nodeid}")
    @Produces(MediaType.APPLICATION_JSON)
    public JSONObject getNode(@PathParam("nodeid") int nodeId) throws JSONException {
        
        JSONObject json = new JSONObject();
        json.append("id", String.valueOf(nodeId))
            .append("name", "test");
        
        return json;
    }
    */

    @GET
    @Path("testme")
    @Produces(MediaType.TEXT_PLAIN)
    public String getTestMe() {
        
        return "test";
    }

    
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeNode(Node node) {
    	
	    return Response.created(
	                         uriInfo.getAbsolutePathBuilder()
	                                .path(String.valueOf(node.getId()))
	                                .build())
	                    .build();
    }
      
}
