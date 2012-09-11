package it.uniud.easyhome.rest;

import it.uniud.easyhome.gateway.GatewayInfo;
import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.network.ModuleCoordinates;
import it.uniud.easyhome.network.NetworkContext;
import it.uniud.easyhome.network.exceptions.PortAlreadyBoundException;

import java.util.List;

import javax.ws.rs.core.*;
import javax.ws.rs.*;

@Path("/hub")
public class HubResource {
    
    @Context
    private UriInfo uriInfo;
    
    private static NetworkContext networkContext = NetworkContext.getInstance();
    
    @GET
    @Path("routing/count")
    public String getRoutingTableCount() {
        return String.valueOf(networkContext.getRoutingTable().size());
    }
    
    @GET
    @Path("routing/{gid}/{address}/{port}")
    public String getGatewayPort(@PathParam("gid") int gid,
                                  @PathParam("address") int address,
                                  @PathParam("port") int port) {
        
        ModuleCoordinates coords = new ModuleCoordinates(gid,address,port);

        Integer retrievedPort = networkContext.getPortFor(coords);
                
        if (retrievedPort == null) 
             throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        return String.valueOf(retrievedPort);
    }
    
    
    /**
     * Associates a new gateway port to a gid/address/port. 
     * If an entry already exists, it is updated with a new port value.
     */
    @POST
    @Path("routing")
    public Response putGatewayPort(@FormParam("gid") int gid,
            @FormParam("address") int address,
            @FormParam("port") int port) {
        
        ModuleCoordinates coords = new ModuleCoordinates(gid,address,port);
        
        networkContext.addRoutingEntry(coords);
        
        return Response.created(uriInfo.getAbsolutePathBuilder()
                                .path(String.valueOf(gid)).path(String.valueOf(address)).path(String.valueOf(port))
                                .build())                
                .build();
    }

    @GET
    @Path("gateways")
    @Produces(MediaType.APPLICATION_JSON)
    public List<GatewayInfo> getGateways() {

        return GatewayInfo.createFromAll(networkContext.getGateways());
    }
    
    // curl -X POST http://localhost:8080/easyhome/rest/hub/gateways -H "Content-Type: application/x-www-form-urlencoded" --data-binary "port=5000&protocol=XBEE" 
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
            return Response.status(Response.Status.PRECONDITION_FAILED).build();
        }
    }

    @DELETE
    @Path("gateways/{gid}")
    public Response unregisterGateway(@PathParam("gid") int gid) {
        
        if (networkContext.hasGateway(gid)) {
            networkContext.removeGateway(gid);
            return Response.ok().build();
        } else
            return Response.status(Response.Status.NOT_FOUND).build();
    }
    
    @DELETE
    @Path("gateways")
    public Response clearAll() {
        
        networkContext.removeAllGateways();
        
        return Response.ok().build();
    }
    
}
