package it.uniud.easyhome.rest;

import it.uniud.easyhome.exceptions.PortAlreadyBoundException;
import it.uniud.easyhome.gateway.Gateway;
import it.uniud.easyhome.gateway.HubContext;
import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.packets.ModuleCoordinates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.*;
import javax.ws.rs.*;

@Path("/hub")
public class HubResource {
    
    @Context
    private UriInfo uriInfo;
    
    private static HubContext networkContext = HubContext.getInstance();
    
    @GET
    @Path("gateways")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Gateway> getGateways() { 
    	return networkContext.getGateways();
    }
    
    @GET
    @Path("gateways/howmany")
    @Produces(MediaType.TEXT_PLAIN)
    public String getHowManyGateways() {
    	return String.valueOf(networkContext.getGateways().size());
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
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @DELETE
    @Path("gateways/{gid}")
    public Response unregisterGateway(@PathParam("gid") byte gid) {
        
        if (networkContext.hasGateway(gid)) {
            networkContext.removeGateway(gid);
            return Response.ok().build();
        } else
            return Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @Path("gateways/{gid}/disconnect")
    public Response disconnectGateway(@PathParam("gid") byte gid) {
        
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
    public String getRoutingTableCount(@PathParam("gid") byte gid) {
        Gateway gw = networkContext.getGatewayForId(gid);
        return String.valueOf(gw.getRoutingTable().size());
    }
    
    @GET
    @Path("gateways/{srcgid}/routing/{dstgid}/{dstnuid}/{address}/{port}")
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
    @Path("gateways/{srcgid}/routing")
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
    
    @POST
    @Path("populate")
    public Response populate() {
    	
    	// We can have more than 1 network only if at least one gateway has been registered
    	if (networkContext.getGidCount() > 1)
    		return Response.notModified().build();
    	
    	registerGateway(ProtocolType.XBEE,5050);
    	registerGateway(ProtocolType.XBEE,6060);
        
    	putRoutingEntry((byte)2,(byte)3,2309737967L,(short)15,(byte)7);
    	
    	return Response.ok().build();
    }
    
}
