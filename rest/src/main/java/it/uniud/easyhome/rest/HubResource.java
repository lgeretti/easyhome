package it.uniud.easyhome.rest;

import it.uniud.easyhome.gateway.Gateway;
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
    
    @POST
    @Path("gateways/{gid}/disconnect")
    public Response disconnectGateway(@PathParam("gid") int gid) {
        
        if (networkContext.hasGateway(gid)) {
            networkContext.disconnectGateway(gid);
            return Response.ok().build();
        } else
            return Response.status(Response.Status.NOT_FOUND).build();
    }
    
    @POST
    @Path("gateways/disconnect")
    public Response disconnectAllGateways() {
        
        networkContext.disconnectAllGateways();
        return Response.ok().build();
    }
    
    @DELETE
    @Path("gateways")
    public Response clearAll() {
        
        networkContext.removeAllGateways();
        
        return Response.ok().build();
    }
    
    @GET
    @Path("gateways/{gid}/routing/count")
    public String getRoutingTableCount(@PathParam("gid") int gid) {
        Gateway gw = networkContext.getGatewayForId(gid);
        return String.valueOf(gw.getRoutingTable().size());
    }
    
    @GET
    @Path("gateways/{srcgid}/routing/{dstgid}/{address}/{port}")
    public String getGatewayPort(@PathParam("srcgid") int srcGid,
                                 @PathParam("dstgid") int dstGid,
                                 @PathParam("address") int address,
                                 @PathParam("port") int port) {
        
        ModuleCoordinates coords = new ModuleCoordinates(dstGid,address,port);

        Gateway gw = networkContext.getGatewayForId(srcGid);
        
        if (gw == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        Integer retrievedPort = gw.getPortFor(coords);
                
        if (retrievedPort == null) 
             throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        return String.valueOf(retrievedPort);
    }
    
    
    /**
     * Associates a new gateway port to a gid/address/port. 
     * If an entry already exists, it is updated with a new port value.
     */
    @POST
    @Path("gateways/{srcgid}/routing")
    public Response putRoutingEntry(@PathParam("srcgid") int srcGid,
            @FormParam("gid") int entryGid,
            @FormParam("address") int entryAddress,
            @FormParam("port") int entryPort) {
        
        ModuleCoordinates coords = new ModuleCoordinates(entryGid,entryAddress,entryPort);
        
        Gateway gw = networkContext.getGatewayForId(srcGid);
        
        if (gw == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);

        gw.addRoutingEntry(coords);
        
        return Response.created(uriInfo.getAbsolutePathBuilder()
                                .path(String.valueOf(entryGid))
                                .path(String.valueOf(entryAddress))
                                .path(String.valueOf(entryPort))
                                .build())                
                .build();
    }
    
    @POST
    @Path("populate")
    public Response populate() {
    	
    	// We can have more than 1 network only if at least one gateway has been registered
    	if (networkContext.getGidCount() > 1)
    		return Response.notModified().build();
    	
    	registerGateway(ProtocolType.XBEE,5050);
    	registerGateway(ProtocolType.XBEE,6060);
        
    	putRoutingEntry(2,3,15,7);
    	
    	return Response.ok().build();
    }
    
}
