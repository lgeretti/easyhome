package it.uniud.easyhome.rest;

import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.processing.*;
import javax.ws.rs.core.*;
import javax.ws.rs.*;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

@Path("/admin")
public class AdminResource {
    
	private static final String TARGET = "http://localhost:8080/easyhome/rest/";
	private static Client client = Client.create();
	private static final int XBEE_GATEWAY_PORT = 5100;
    
    private ClientResponse insertGateway(int port, ProtocolType protocol) {
        
    	MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
    	formData.add("port", String.valueOf(port));
    	formData.add("protocol", protocol.toString());
    	ClientResponse response = client.resource(TARGET).path("hub").path("gateways")
    							  .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
    	
    	return response;
    }
    
    private ClientResponse insertProcess(ProcessKind kind) {
        
    	MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
    	formData.add("kind", kind.toString());
    	ClientResponse response = client.resource(TARGET).path("processes")
    							  .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
    	
    	return response;
    } 
    
    @Path("/up")
    @POST
    public Response up() {
    	
		insertGateway(XBEE_GATEWAY_PORT, ProtocolType.XBEE);
		insertProcess(ProcessKind.NODE_ANNCE_REGISTRATION);
		insertProcess(ProcessKind.NODE_DESCR_REQUEST);
		insertProcess(ProcessKind.NODE_DESCR_REGISTRATION);	
    	
        return Response.ok().build();
    }

    @Path("/down")
    @POST
    public Response down() {
    	
    	client.resource(TARGET).path("hub").path("gateways").delete();
    	client.resource(TARGET).path("processes").delete();
    	client.resource(TARGET).path("network").delete();
    	
        return Response.ok().build();
    }
    
}