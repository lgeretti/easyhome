package it.uniud.easyhome.rest;

import static org.junit.Assert.*;

import java.util.List;

import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.network.Node;

import javax.ws.rs.core.MediaType;

import org.codehaus.jettison.json.JSONException;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

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
		
		List<Node> nodeList = JsonUtils.getListFrom(getResponse,Node.class);
		
		assertEquals(0,nodeList.size());
	}
	
	
	@Test
	public void testInsert() throws JSONException {
		
        Node.Builder nb = new Node.Builder(10L);
        
        nb.setName("test");
        nb.setGatewayId((byte)2);
        nb.setAddress((short)15);
        
        Node node = nb.build();
       
        ClientResponse insertionResponse = postNode(node.getId(),node.getName(),node.getGatewayId(),node.getAddress());
        assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());

        Node recoveredNode = client.resource(TARGET).path("10").accept(MediaType.APPLICATION_JSON).get(Node.class);
        
        assertEquals(node,recoveredNode);
     
        ClientResponse getResponse = client.resource(TARGET).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        List<Node> nodeList = JsonUtils.getListFrom(getResponse,Node.class);
        assertEquals(1,nodeList.size());
        assertEquals(node,nodeList.get(0));
        
    }
	
	@Test
	public void testUpdate() throws JSONException {
		
        Node.Builder nb = new Node.Builder(10L);
        
        nb.setName("test");
        nb.setGatewayId((byte)2);
        nb.setAddress((short)15);
        
        Node node = nb.build();
       
        postNode(node.getId(),node.getName(),node.getGatewayId(),node.getAddress());
		
		ClientResponse updateResponse = postNode(node.getId(),node.getName(),node.getGatewayId(),(short)(node.getAddress()+1));
        assertEquals(ClientResponse.Status.OK,updateResponse.getClientResponseStatus());
        
        ClientResponse getUpdatedNodeResponse = client.resource(TARGET).path(String.valueOf(node.getId()))
        											  .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        
        Node updatedNode = JsonUtils.getFrom(getUpdatedNodeResponse, Node.class);
        
        assertFalse(node.equals(updatedNode));
        
		ClientResponse getResponse = client.resource(TARGET).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		List<Node> nodeList = JsonUtils.getListFrom(getResponse,Node.class);
		
		assertEquals(1,nodeList.size());
	}
	
	@Test
	public void multipleNodes() throws JSONException {
		
		postNode(1L,"test",(byte)2,(short)3333);
		postNode(2L,"test",(byte)2,(short)3333);
		
		ClientResponse response = client.resource(TARGET).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        List<Node> nodeList = JsonUtils.getListFrom(response,Node.class);
		
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
		
}
