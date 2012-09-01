package it.uniud.easyhome.rest;

import it.uniud.easyhome.gateway.Gateway;
import it.uniud.easyhome.gateway.GatewayInfo;
import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.network.NetworkContext;
import it.uniud.easyhome.network.exceptions.PortAlreadyBoundException;

import java.util.List;

import javax.ws.rs.core.*;
import javax.ws.rs.*;

@Path("/hub")
public class HubResource {
    
    @Context
    private UriInfo uriInfo;
    
    private static NetworkContext networkContext = new NetworkContext();

    @GET
    @Path("gateways")
    @Produces(MediaType.APPLICATION_JSON)
    public List<GatewayInfo> getGateways() {

        return GatewayInfo.createFromAll(networkContext.getGateways());
    }
    
    // curl -X POST http://localhost:8080/easyhome/rest/hub/gateways -H "Content-Type: application/x-www-form-urlencoded" --data-binary "port=3000&protocol=EHS" 
    @POST
    @Path("gateways")
    public Response registerGateway(@FormParam("protocol") ProtocolType protocol,
            @FormParam("port") int port) {

        try {
            int gid = networkContext.addGateway(protocol, port);

            return Response.created(
                uriInfo.getAbsolutePathBuilder().path(String.valueOf(gid)).build())                
                .build();
        } catch (PortAlreadyBoundException ex) {
            // Precondition failure
            return Response.status(412).build();
        }
    }

    @DELETE
    @Path("gateways/{gid}")
    public Response unregisterGateway(@PathParam("gid") int gid) {
        
        if (networkContext.hasGateway(gid)) {
            networkContext.removeGateway(gid);
            return Response.ok().build();
        } else
            return Response.status(404).build();
    }
    
    @DELETE
    @Path("gateways")
    public Response clearAll() {
        
        networkContext.removeAllGateways();
        
        return Response.ok().build();
    }
    
}
