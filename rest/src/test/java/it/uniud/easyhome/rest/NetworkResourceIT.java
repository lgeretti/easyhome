package it.uniud.easyhome.rest;

import static org.junit.Assert.*;
import it.uniud.easyhome.network.Node;

import javax.ws.rs.core.MediaType;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

public class NetworkResourceIT {
	
	private static final String TARGET = "http://localhost:8080/easyhome/rest/network";
	
	private static Client client;
	
	@BeforeClass
    public static void setup() {
    	
        client = Client.create();
    }
	
	@Test
	public void putNode() throws JSONException {
		
        Node.Builder nb = new Node.Builder(10L);
        
        nb.setName("test");
        nb.setGatewayId((byte)2);
        nb.setAddress((short)15);
        
        Node node = nb.build();
        
        ClientResponse response = client.resource(TARGET).type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node);
        assertEquals(ClientResponse.Status.CREATED,response.getClientResponseStatus());
        
        ClientResponse response2 = client.resource(TARGET).type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node);
        assertEquals(ClientResponse.Status.OK,response2.getClientResponseStatus());        
        
        //JSONObject jsonObj = client.resource(TARGET).path("10").accept(MediaType.APPLICATION_JSON).get(JSONObject.class);
        //assertEquals(15,jsonObj.getInt("address"));
        
        ClientResponse resp = client.resource(TARGET).path("10").accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        Node recoveredNode = new Gson().fromJson(resp.getEntity(String.class), Node.class);
        
        assertEquals(2,recoveredNode.getGatewayId());
        
    }
	
	@After
	public void removeNode() {
		client.resource(TARGET).path("10").delete();
	}
}
