package it.uniud.easyhome.rest;

import it.uniud.easyhome.common.LogLevel;
import it.uniud.easyhome.gateway.Gateway;
import it.uniud.easyhome.gateway.HubContext;
import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.network.ModuleCoordinates;

import java.util.List;

import javax.ws.rs.core.*;
import javax.ws.rs.*;

@Path(RestPaths.GATEWAYS)
public class GatewayResource {
    
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
    
    // curl -X POST http://localhost:8080/easyhome/rest/gateways -H "Content-Type: application/x-www-form-urlencoded" --data-binary "port=5100&protocol=XBEE" 
    @POST
    public Response registerGateway(@FormParam("id") byte id, @FormParam("protocol") ProtocolType protocol,
            @FormParam("port") int port, @FormParam("logLevel") LogLevel logLevel) {
    	
        try {
            networkContext.addGateway(id, protocol, port, logLevel);

            return Response.created(
                uriInfo.getAbsolutePathBuilder().path(String.valueOf(id)).build())                
                .build();
        } catch (Exception ex) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @DELETE
    @Path("{gatewayId}")
    public Response unregisterGateway(@PathParam("gatewayId") byte gatewayId) {
        
        if (networkContext.hasGateway(gatewayId)) {
            networkContext.removeGateway(gatewayId);
            return Response.ok().build();
        } else
            return Response.status(Response.Status.NOT_FOUND).build();
    }
    
    @POST
    @Path("{gatewayId}/open")
    public Response openGateway(@PathParam("gatewayId") byte gatewayId) {
        
        if (networkContext.hasGateway(gatewayId)) {
            networkContext.openGateway(gatewayId);
            return Response.ok().build();
        } else
            return Response.status(Response.Status.NOT_FOUND).build();
    }
    
    @POST
    @Path("{gatewayId}/close")
    public Response closeGateway(@PathParam("gatewayId") byte gatewayId) {
        
        if (networkContext.hasGateway(gatewayId)) {
            networkContext.closeGateway(gatewayId);
            return Response.ok().build();
        } else
            return Response.status(Response.Status.NOT_FOUND).build();
    }
    
    // curl -X POST http://localhost:8080/easyhome/rest/gateways/2/logLevel -H "Content-Type: application/x-www-form-urlencoded" --data-binary "logLevel=DEBUG" 
    @POST
    @Path("{gatewayId}/logLevel")
    public Response changeLogLevel(@PathParam("gatewayId") byte gatewayId,
    							   @FormParam("logLevel") LogLevel logLevel) {
        
        if (networkContext.hasGateway(gatewayId)) {
            if (logLevel == null)
            	return Response.status(Response.Status.BAD_REQUEST).build();
            networkContext.setLogLevel(gatewayId,logLevel);
            	
            return Response.ok().build();
        } else
            return Response.status(Response.Status.NOT_FOUND).build();
    }    
    
    // curl -X POST http://localhost:8080/easyhome/rest/gateways/logLevel -H "Content-Type: application/x-www-form-urlencoded" --data-binary "logLevel=DEBUG" 
    @POST
    @Path("logLevel")
    public Response changeLogLevel(@FormParam("logLevel") LogLevel logLevel) {
        
    	networkContext.setLogLevel(logLevel);
    	
        return Response.ok().build();
    }  
    
    @DELETE
    public Response clearAll() {
        
        networkContext.removeAllGateways();
        
        return Response.ok().build();
    }
    
    @GET
    @Path("{gatewayId}/routing/count")
    public String getRoutingTableCount(@PathParam("gatewayId") byte gatewayId) {
        Gateway gw = networkContext.getGatewayForId(gatewayId);
        return String.valueOf(gw.getRoutingTable().size());
    }
    
    @GET
    @Path("{srcGatewayId}/routing/{dstGatewayId}/{dstnuid}/{address}/{port}")
    public String getGatewayPort(@PathParam("srcGatewayId") byte srcGatewayId,
                                 @PathParam("dstGatewayId") byte dstGatewayId,
                                 @PathParam("dstnuid") long dstNuid,
                                 @PathParam("address") short address,
                                 @PathParam("port") byte port) {
        
        ModuleCoordinates coords = new ModuleCoordinates(dstGatewayId,dstNuid,address,port);

        Gateway gw = networkContext.getGatewayForId(srcGatewayId);
        
        if (gw == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        Integer retrievedPort = gw.getEndpointFor(coords);
                
        if (retrievedPort == null) 
             throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        return String.valueOf(retrievedPort);
    }
    
    
    /**
     * Associates a new gateway port to a gatewayId/address/port. 
     * If an entry already exists, it is updated with a new port value.
     */
    @POST
    @Path("{srcGatewayId}/routing")
    public Response putRoutingEntry(@PathParam("srcGatewayId") byte srcGatewayId,
            @FormParam("gatewayId") byte entryGatewayId,
            @FormParam("nuid") long entryNuid,
            @FormParam("address") short entryAddress,
            @FormParam("port") byte entryPort) {
        
        ModuleCoordinates coords = new ModuleCoordinates(entryGatewayId,entryNuid,entryAddress,entryPort);
        
        Gateway gw = networkContext.getGatewayForId(srcGatewayId);
        
        if (gw == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);

        gw.addRoutingEntry(coords);
        
        return Response.created(uriInfo.getAbsolutePathBuilder()
                                .path(String.valueOf(entryGatewayId))
                                .path(String.valueOf(entryNuid))
                                .path(String.valueOf(entryAddress))
                                .path(String.valueOf(entryPort))
                                .build())                
                .build();
    }
    
}
