package it.uniud.easyhome.rest;

import it.uniud.easyhome.gateway.Gateway;
import it.uniud.easyhome.gateway.ProtocolType;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.*;
import javax.ws.rs.*;

@Path("/gateways")
public class GatewayResource {
    
    @Context
    private UriInfo uriInfo;
    
    private static final List<Gateway> gateways = new ArrayList<Gateway>();
    private static int gidCount = 0;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Gateway> getGateways() {

        return gateways;
    }
    
    // curl -X POST http://localhost:8080/easyhome/rest/gateways -H "Content-Type: application/x-www-form-urlencoded" --data-binary "port=3000&protocol=EHS" 
    @POST
    public Response registerGateway(@FormParam("protocol") ProtocolType protocol,
            @FormParam("port") int port) {
        
        boolean found = false;
        for (Gateway gw : gateways)
            if (gw.getPort() == port)
                found = true;
        
        if (!found) {
            int gid = ++gidCount;
            
            gateways.add(new Gateway(gid,protocol,port));

            return Response.created(
                uriInfo.getAbsolutePathBuilder().path(String.valueOf(gid)).build())                
                .build();
        } else
            return Response.ok().build();
    }

    @DELETE
    @Path("{gid}")
    public Response unregisterGateway(@PathParam("gid") int gid) {
        
        for (int i=0; i<gateways.size(); i++) {
            if (gateways.get(i).getId() == gid) {
                gateways.remove(i);
                return Response.ok().build();
            }
        }
        
        return Response.status(404).build();
    }
    
    @DELETE
    public Response clearAll() {
        
        gateways.clear();
        
        return Response.ok().build();
    }
    
}
