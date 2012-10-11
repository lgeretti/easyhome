package it.uniud.easyhome.rest;

import it.uniud.easyhome.network.Node;

import javax.ws.rs.core.MediaType;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class NetworkResourceIT {
	
	private static final String target = "http://localhost:8080/easyhome/rest/network";
	
	private static Client client;
	
	@BeforeClass
    public static void setup() {
    	
    	ClientConfig config = new DefaultClientConfig();
        client = Client.create(config);
    }
	
	@Test
	public void putNode() throws JSONException {
		
        Node.Builder nb = new Node.Builder(10);
        
        nb.setName("test");
        nb.setGatewayId((byte)2);
        nb.setAddress((short)15);
        
        Node node = nb.build();
        
        ClientResponse response = client.resource(target).type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node);
        System.out.println("Response status code: " + response.getStatus());
        
        JSONObject jsonObj = client.resource(target).path("10").accept(MediaType.APPLICATION_JSON).get(JSONObject.class);
        System.out.println("Recovered node address: " + jsonObj.getInt("address"));	
    }
}
