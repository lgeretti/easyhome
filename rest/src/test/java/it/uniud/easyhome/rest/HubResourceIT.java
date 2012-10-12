package it.uniud.easyhome.rest;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.List;

import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.network.Node;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class HubResourceIT {
	
	private static final String TARGET = "http://localhost:8080/easyhome/rest/hub/gateways";
	
	private final static int GATEWAY_PORT = 5050;
	private final static ProtocolType GATEWAY_PROTOCOL = ProtocolType.XBEE;
	
	private static Client client;
	
	@BeforeClass
    public static void setup() {
        client = Client.create();
    }
	
	@Ignore
	@Test
	public void noGateways() {
		
		JSONObject jsonObj = client.resource(TARGET).accept(MediaType.APPLICATION_JSON).get(JSONObject.class);
		assertTrue(jsonObj == null);
	}
	
	@Test
	public void createGateway() throws JSONException {
		
		ClientResponse creationResponse = insertGateway(GATEWAY_PORT,GATEWAY_PROTOCOL);
		
		assertEquals(ClientResponse.Status.CREATED,creationResponse.getClientResponseStatus());
		
        JSONObject jsonObj = client.resource(TARGET).accept(MediaType.APPLICATION_JSON).get(JSONObject.class);
        JSONObject innerJsonObj = jsonObj.getJSONObject("gateway");
        assertEquals(GATEWAY_PORT,innerJsonObj.getInt("port"));
	}
	
	@Test
	public void createTwoGateways() throws JSONException {
		
		ClientResponse creationResponse = insertGateway(GATEWAY_PORT,GATEWAY_PROTOCOL);
		assertEquals(ClientResponse.Status.CREATED,creationResponse.getClientResponseStatus());

		ClientResponse creationResponse2 = insertGateway(GATEWAY_PORT+1,GATEWAY_PROTOCOL);
		assertEquals(ClientResponse.Status.CREATED,creationResponse2.getClientResponseStatus());
		
        JSONObject jsonObj = client.resource(TARGET).accept(MediaType.APPLICATION_JSON).get(JSONObject.class);
        JSONArray innerJsonArray = jsonObj.getJSONArray("gateway");
        assertEquals(2,innerJsonArray.length());
	}	
	
    @After
    public void clearGateways() {
    	client.resource(TARGET).delete();
    }
    
    private ClientResponse insertGateway(int port, ProtocolType protocol) {
        
    	MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
    	formData.add("port", String.valueOf(port));
    	formData.add("protocol", protocol.toString());
    	ClientResponse response = client.resource(TARGET)
    							  .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
    	
    	return response;
    }
}
