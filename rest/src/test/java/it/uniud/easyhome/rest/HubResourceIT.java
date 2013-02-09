package it.uniud.easyhome.rest;

import static org.junit.Assert.*;

import java.util.List;

import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.gateway.Gateway;
import it.uniud.easyhome.gateway.ProtocolType;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.codehaus.jettison.json.JSONException;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class HubResourceIT {
	
	private static final String TARGET = "http://localhost:8080/easyhome/rest/" + RestPaths.GATEWAYS;
	
	private final static int GATEWAY_PORT = 5050;
	private final static ProtocolType GATEWAY_PROTOCOL = ProtocolType.XBEE;
	
	private static Client client;
	
	@BeforeClass
    public static void setup() {
        client = Client.create();
    }

	@Test
	public void noGateways() throws JSONException {
		
		ClientResponse response = client.resource(TARGET).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		List<Gateway> gateways = JsonUtils.getListFrom(response, Gateway.class);
		assertEquals(0,gateways.size());
	}
	
	@Test
	public void createGateway() throws JSONException {
		
		ClientResponse creationResponse = insertGateway((byte)2,GATEWAY_PORT,GATEWAY_PROTOCOL);
		
		assertEquals(ClientResponse.Status.CREATED,creationResponse.getClientResponseStatus());
		
		ClientResponse response = client.resource(TARGET).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		List<Gateway> gateways = JsonUtils.getListFrom(response, Gateway.class);
		assertEquals(1,gateways.size());
	}

	@Test
	public void createTwoGateways() throws JSONException {
		
		insertGateway((byte)2,GATEWAY_PORT,GATEWAY_PROTOCOL);
		
		ClientResponse creationResponse2 = insertGateway((byte)3,GATEWAY_PORT+1,GATEWAY_PROTOCOL);
		assertEquals(ClientResponse.Status.CREATED,creationResponse2.getClientResponseStatus());
		
		ClientResponse response = client.resource(TARGET).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		List<Gateway> gateways = JsonUtils.getListFrom(response, Gateway.class);
		assertEquals(2,gateways.size());
	}	
	
	@Test
	public void deleteGateway() throws JSONException {
		
		insertGateway((byte)2,GATEWAY_PORT,GATEWAY_PROTOCOL);
		
    	ClientResponse deletionResponse =  client.resource(TARGET).delete(ClientResponse.class);
    	
    	assertEquals(ClientResponse.Status.OK,deletionResponse.getClientResponseStatus());
	}
	
    @Test
    public void putRoutingEntry() {
        
        int srcGatewayPort = 5000;
        byte dstGid = 2;
        long dstNuid = 0x55AAAAAA;
        int dstAddress = 20;
        int dstPort = 4;
        
        ProtocolType protocol = ProtocolType.XBEE;
        
        ClientResponse gwInsertionResponse = insertGateway(dstGid,srcGatewayPort,protocol);
        assertEquals(ClientResponse.Status.CREATED,gwInsertionResponse.getClientResponseStatus());
        String locationPath = gwInsertionResponse.getLocation().getPath();
        String[] segments = locationPath.split("/");
        int gid = Integer.parseInt(segments[segments.length-1]);
        
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        
        formData.add("gid",String.valueOf(dstGid));
        formData.add("nuid",String.valueOf(dstNuid));
        formData.add("address",String.valueOf(dstAddress));
        formData.add("port",String.valueOf(dstPort));
        
        ClientResponse routingInsertionResponse = client.resource(TARGET)
                                            		.path(String.valueOf(gid))
                                            		.path("routing")
                                            		.post(ClientResponse.class,formData); 
        
        assertEquals(ClientResponse.Status.CREATED,routingInsertionResponse.getClientResponseStatus());
        
        String count = client.resource(TARGET)
                       .path(String.valueOf(gid))
                       .path("routing/count")
                       .get(String.class);
        
        assertEquals(1,Integer.parseInt(count));
        
        String routedPort = client.resource(routingInsertionResponse.getLocation().toString()).get(String.class);

        assertTrue(Integer.parseInt(routedPort)>0);
    }
	
    @After
    public void clearGateways() {
    	client.resource(TARGET).delete();
    }
    
    private ClientResponse insertGateway(byte gid, int port, ProtocolType protocol) {
        
    	MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
    	formData.add("id", Byte.toString(gid));
    	formData.add("port", String.valueOf(port));
    	formData.add("protocol", protocol.toString());
    	ClientResponse response = client.resource(TARGET)
    							  .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
    	
    	return response;
    }
}
