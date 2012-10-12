package it.uniud.easyhome.rest;

import static org.junit.Assert.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import it.uniud.easyhome.network.Node;

import javax.ws.rs.core.MediaType;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
	public void testNoNodes() throws JSONException {
		
		ClientResponse getResponse = client.resource(TARGET).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		
		List<Node> nodeList = getNodeListFrom(getResponse);
		
		assertEquals(0,nodeList.size());
	}
	
	
	@Test
	public void testPostNode() throws JSONException {
		
        Node.Builder nb = new Node.Builder(10L);
        
        nb.setName("test");
        nb.setGatewayId((byte)2);
        nb.setAddress((short)15);
        
        Node node = nb.build();
       
        ClientResponse postResponse = postNode(node.getId(),node.getName(),node.getGatewayId(),node.getAddress());
        assertEquals(ClientResponse.Status.CREATED,postResponse.getClientResponseStatus());

        Node recoveredNode = client.resource(TARGET).path("10").accept(MediaType.APPLICATION_JSON).get(Node.class);
        
        assertEquals(node,recoveredNode);
     
        ClientResponse getResponse = client.resource(TARGET).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        List<Node> nodeList = getNodeListFrom(getResponse);
        assertEquals(1,nodeList.size());
        assertEquals(node,nodeList.get(0));
        
        ClientResponse postResponse2 = postNode(node.getId(),node.getName(),node.getGatewayId(),node.getAddress());
        assertEquals(ClientResponse.Status.OK,postResponse2.getClientResponseStatus());        
    }
	
	@Test
	public void multipleNodes() throws JSONException {
		
		postNode(1L,"test",(byte)2,(short)3333);
		postNode(2L,"test",(byte)2,(short)3333);
		
		ClientResponse response = client.resource(TARGET).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        List<Node> nodeList = getNodeListFrom(response);
		
		assertEquals(2,nodeList.size());
	}
	
	@After
	public void removeNodes() {
		client.resource(TARGET).delete();
	}
	
	private ClientResponse postNode(long id, String name, byte gatewayId, short address) {
		
		Node.Builder nb = new Node.Builder(id);
        Node node = nb.setName(name).setGatewayId(gatewayId).setAddress(address).build();
        
        return client.resource(TARGET).type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node);
	}
	
	private List<Node> getNodeListFrom(ClientResponse response) throws JSONException {
		
		String responseString = response.getEntity(String.class);
		
		List<Node> nodeList;
		
		if (responseString.equals("null"))
			nodeList = new ArrayList<Node>();
		else {
			Gson gson = new Gson();
			
			JSONObject jsonObj = new JSONObject(responseString);
			
			try {
				
				JSONArray jsonArray = jsonObj.getJSONArray("node");
				Type listType = new TypeToken<List<Node>>() { }.getType();
				nodeList = gson.fromJson(jsonArray.toString(), listType);
			} catch (JSONException ex) {
				JSONObject jsonNode = jsonObj.getJSONObject("node");
				Node node = gson.fromJson(jsonNode.toString(), Node.class);
				nodeList = new ArrayList<Node>();
				nodeList.add(node);
			}
		}
		
		return nodeList;
	}
		
}
