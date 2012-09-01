package it.uniud.easyhome.rest;

import it.uniud.easyhome.gateway.Gateway;
import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.network.NetworkContext;
import it.uniud.easyhome.network.PortAlreadyBoundException;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.*;
import javax.ws.rs.*;

@Path("/gateways")
public class GatewayResource {
    
    @Context
    private UriInfo uriInfo;
    
    private static NetworkContext networkContext = new NetworkContext();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Gateway> getGateways() {

        return networkContext.getGateways();
    }
    
    // curl -X POST http://localhost:8080/easyhome/rest/gateways -H "Content-Type: application/x-www-form-urlencoded" --data-binary "port=3000&protocol=EHS" 
    @POST
    public Response registerGateway(@FormParam("protocol") ProtocolType protocol,
            @FormParam("port") int port) {

        try {
            int gid = networkContext.addGateway(protocol, port);

            return Response.created(
                uriInfo.getAbsolutePathBuilder().path(String.valueOf(gid)).build())                
                .build();
        } catch (PortAlreadyBoundException ex) {
            // We accept trying to bind on the same port multiple times
            return Response.ok().build();
        }
    }

    @DELETE
    @Path("{gid}")
    public Response unregisterGateway(@PathParam("gid") int gid) {
        
        if (networkContext.hasGateway(gid)) {
            networkContext.removeGateway(gid);
            return Response.ok().build();
        } else
            return Response.status(404).build();
    }
    
    @DELETE
    public Response clearAll() {
        
        networkContext.removeAllGateways();
        
        return Response.ok().build();
    }
    
}
