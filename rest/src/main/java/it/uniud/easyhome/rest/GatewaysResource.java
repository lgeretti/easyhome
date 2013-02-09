package it.uniud.easyhome.rest;

import it.uniud.easyhome.gateway.Gateway;
import it.uniud.easyhome.gateway.HubContext;
import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.network.ModuleCoordinates;

import java.util.List;

import javax.ws.rs.core.*;
import javax.ws.rs.*;

@Path(RestPaths.GATEWAYS)
public class GatewaysResource {
    
    @Context
    private UriInfo uriInfo;
    
    private static HubContext networkContext = HubContext.getInstance();
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Gateway> getGateways() { 
    	return networkContext.getGateways();
    }
    
    @GET
    @Path("howmany")
    @Produces(MediaType.TEXT_PLAIN)
    public String getHowManyGateways() {
    	return String.valueOf(networkContext.getGateways().size());
    }
    
    // curl -X POST http://localhost:8080/easyhome/rest/hub/gateways -H "Content-Type: application/x-www-form-urlencoded" --data-binary "port=5100&protocol=XBEE" 
    @POST
    public Response registerGateway(@FormParam("id") byte id, @FormParam("protocol") ProtocolType protocol,
            @FormParam("port") int port) {

        try {
            networkContext.addGateway(id, protocol, port);

            return Response.created(
                uriInfo.getAbsolutePathBuilder().path(String.valueOf(id)).build())                
                .build();
        } catch (Exception ex) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @DELETE
    @Path("{gid}")
    public Response unregisterGateway(@PathParam("gid") byte gid) {
        
        if (networkContext.hasGateway(gid)) {
            networkContext.removeGateway(gid);
            return Response.ok().build();
        } else
            return Response.status(Response.Status.NOT_FOUND).build();
    }
    
    @POST
    @Path("{gid}/open")
    public Response openGateway(@PathParam("gid") byte gid) {
        
        if (networkContext.hasGateway(gid)) {
            networkContext.openGateway(gid);
            return Response.ok().build();
        } else
            return Response.status(Response.Status.NOT_FOUND).build();
    }
    
    @POST
    @Path("{gid}/close")
    public Response closeGateway(@PathParam("gid") byte gid) {
        
        if (networkContext.hasGateway(gid)) {
            networkContext.closeGateway(gid);
            return Response.ok().build();
        } else
            return Response.status(Response.Status.NOT_FOUND).build();
    }
    
    @DELETE
    public Response clearAll() {
        
        networkContext.removeAllGateways();
        
        return Response.ok().build();
    }
    
    @GET
    @Path("{gid}/routing/count")
    public String getRoutingTableCount(@PathParam("gid") byte gid) {
        Gateway gw = networkContext.getGatewayForId(gid);
        return String.valueOf(gw.getRoutingTable().size());
    }
    
    @GET
    @Path("{srcgid}/routing/{dstgid}/{dstnuid}/{address}/{port}")
    public String getGatewayPort(@PathParam("srcgid") byte srcGid,
                                 @PathParam("dstgid") byte dstGid,
                                 @PathParam("dstnuid") long dstNuid,
                                 @PathParam("address") short address,
                                 @PathParam("port") byte port) {
        
        ModuleCoordinates coords = new ModuleCoordinates(dstGid,dstNuid,address,port);

        Gateway gw = networkContext.getGatewayForId(srcGid);
        
        if (gw == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        Integer retrievedPort = gw.getEndpointFor(coords);
                
        if (retrievedPort == null) 
             throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        return String.valueOf(retrievedPort);
    }
    
    
    /**
     * Associates a new gateway port to a gid/address/port. 
     * If an entry already exists, it is updated with a new port value.
     */
    @POST
    @Path("{srcgid}/routing")
    public Response putRoutingEntry(@PathParam("srcgid") byte srcGid,
            @FormParam("gid") byte entryGid,
            @FormParam("nuid") long entryNuid,
            @FormParam("address") short entryAddress,
            @FormParam("port") byte entryPort) {
        
        ModuleCoordinates coords = new ModuleCoordinates(entryGid,entryNuid,entryAddress,entryPort);
        
        Gateway gw = networkContext.getGatewayForId(srcGid);
        
        if (gw == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);

        gw.addRoutingEntry(coords);
        
        return Response.created(uriInfo.getAbsolutePathBuilder()
                                .path(String.valueOf(entryGid))
                                .path(String.valueOf(entryNuid))
                                .path(String.valueOf(entryAddress))
                                .path(String.valueOf(entryPort))
                                .build())                
                .build();
    }
    
}