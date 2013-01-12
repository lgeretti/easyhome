package it.uniud.easyhome.rest;

import static org.junit.Assert.*;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.devices.HomeAutomationDevice;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.NodeLogicalType;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.codehaus.jettison.json.JSONException;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

@Ignore
public class NetworkResourceIT {
	
	private static final String TARGET = "http://localhost:8080/easyhome/rest/network";
	
	private static Client client;
	
	@BeforeClass
    public static void setup() {
        client = Client.create();
    }
	
    @Test
    public void putDevicesForNode() throws JSONException {
    	
        Node.Builder nb1 = new Node.Builder(10L);
        nb1.setName("test");
        nb1.setGatewayId((byte)2);
        nb1.setAddress((short)15);
        nb1.setCapability((byte)14);        
        Node node1 = nb1.build();
        
        node1.setEndpoints(Arrays.asList((short)2,(short)7,(short)3,(short)5));
        node1.addDevice((short)5, HomeAutomationDevice.ONOFF_SWITCH);
        node1.addDevice((short)3, HomeAutomationDevice.DIMMABLE_LIGHT);
        
        Map<Short,HomeAutomationDevice> originalDevices = node1.getMappedDevices();
    	assertEquals(4,originalDevices.size());
    	assertEquals(HomeAutomationDevice.ONOFF_SWITCH,originalDevices.get((short)5));
    	assertEquals(HomeAutomationDevice.DIMMABLE_LIGHT,originalDevices.get((short)3));
    	assertEquals(HomeAutomationDevice.UNKNOWN,originalDevices.get((short)2));
    	assertEquals(HomeAutomationDevice.UNKNOWN,originalDevices.get((short)7));
        
        ClientResponse insertionResponse = client.resource(TARGET).type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node1);
        assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());

        ClientResponse retrievalResponse = client.resource(TARGET).path("10").accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
    	Node retrievedNode = JsonUtils.getFrom(retrievalResponse,Node.class);
    	Map<Short,HomeAutomationDevice> retrievedDevices = retrievedNode.getMappedDevices();
    	assertEquals(4,retrievedDevices.size());
    	assertEquals(HomeAutomationDevice.ONOFF_SWITCH,retrievedDevices.get((short)5));
    	assertEquals(HomeAutomationDevice.DIMMABLE_LIGHT,retrievedDevices.get((short)3));
    	assertEquals(HomeAutomationDevice.UNKNOWN,retrievedDevices.get((short)2));
    	assertEquals(HomeAutomationDevice.UNKNOWN,retrievedDevices.get((short)7));    
    	
    	node1.addDevice((short)7, HomeAutomationDevice.LEVEL_CONTROL_SWITCH);
    	ClientResponse updateResponse = client.resource(TARGET).type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node1);
        assertEquals(ClientResponse.Status.OK,updateResponse.getClientResponseStatus());
        
        ClientResponse retrievalResponse2 = client.resource(TARGET).path("10").accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
    	Node retrievedNode2 = JsonUtils.getFrom(retrievalResponse2,Node.class);

    	Map<Short,HomeAutomationDevice> retrievedDevices2 = retrievedNode2.getMappedDevices();
    	
    	assertEquals(4,retrievedDevices2.size());
    	assertEquals(HomeAutomationDevice.ONOFF_SWITCH,retrievedDevices2.get((short)5));
    	assertEquals(HomeAutomationDevice.DIMMABLE_LIGHT,retrievedDevices2.get((short)3));
    	assertEquals(HomeAutomationDevice.UNKNOWN,retrievedDevices2.get((short)2));
    	assertEquals(HomeAutomationDevice.LEVEL_CONTROL_SWITCH,retrievedDevices2.get((short)7));            
    }

	@Test
	public void testNoNodes() throws JSONException {
		
		ClientResponse getResponse = client.resource(TARGET).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		
		List<Node> nodeList = JsonUtils.getListFrom(getResponse,Node.class);
		
		assertEquals(0,nodeList.size());
	}
	
	@Test
	public void testInsert() throws JSONException {
		
        Node.Builder nb1 = new Node.Builder(10L);
        
        nb1.setName("test");
        nb1.setGatewayId((byte)2);
        nb1.setAddress((short)15);
        nb1.setCapability((byte)14);

        Node node1 = nb1.build();
       
        ClientResponse insertionResponse = client.resource(TARGET).type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node1);
        assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());

        Node recoveredNode = client.resource(TARGET).path("10").accept(MediaType.APPLICATION_JSON).get(Node.class);
        
        assertEquals(node1,recoveredNode);
     
        ClientResponse getResponse = client.resource(TARGET).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        List<Node> nodeList = JsonUtils.getListFrom(getResponse,Node.class);
        assertEquals(1,nodeList.size());
        assertEquals(node1,nodeList.get(0));
    }

	@Test
	public void testDelete() throws JSONException {
		
        Node.Builder nb1 = new Node.Builder(10L);
        
        nb1.setName("test");
        nb1.setGatewayId((byte)2);
        nb1.setAddress((short)15);
        nb1.setCapability((byte)14);

        Node node1 = nb1.build();
       
        ClientResponse insertionResponse = client.resource(TARGET).type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node1);
        assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());

        ClientResponse deletionResponse = client.resource(TARGET).path("10").delete(ClientResponse.class);
        
        assertEquals(ClientResponse.Status.OK,deletionResponse.getClientResponseStatus());
    }
	
	
	@Test
	public void testInsertTwoNodes() throws JSONException {
		
        Node.Builder nb1 = new Node.Builder(10L);
        
        nb1.setName("test");
        nb1.setGatewayId((byte)2);
        nb1.setAddress((short)15);
        nb1.setCapability((byte)14);
        
        Node node1 = nb1.build();
        
        Node.Builder nb2 = new Node.Builder(11L);
        
        nb2.setName("test2");
        nb2.setGatewayId((byte)2);
        nb2.setAddress((short)24);
        nb2.setCapability((byte)14);
        
        Node node2 = nb2.build();
        node1.addNeighbor(node2);
       
        ClientResponse secondInsertionResponse = client.resource(TARGET).type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node2);
        assertEquals(ClientResponse.Status.CREATED,secondInsertionResponse.getClientResponseStatus());
        ClientResponse firstInsertionResponse = client.resource(TARGET).type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node1);
        assertEquals(ClientResponse.Status.CREATED,firstInsertionResponse.getClientResponseStatus());

        Node recoveredNode = client.resource(TARGET).path(Long.toString(node1.getId())).accept(MediaType.APPLICATION_JSON).get(Node.class);
        assertEquals(1,recoveredNode.getNeighborIds().size());
        
		ClientResponse getResponse = client.resource(TARGET).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		List<Node> nodeList = JsonUtils.getListFrom(getResponse,Node.class);
		
		assertEquals(2,nodeList.size());
    }
	
	@Test
	public void testInsertMultipleNodes() throws JSONException {
		
        Node node1 = new Node.Builder(0xA1L)
		 .setAddress((short)0x543F)
		 .setGatewayId((byte)1)
		 .setCapability((byte)0x7A)
		 .setLogicalType(NodeLogicalType.ROUTER).build();
        Node node2 = new Node.Builder(0xA2L)
		 .setAddress((short)0x543F)
		 .setGatewayId((byte)1)
		 .setCapability((byte)0x7A)
		 .setLogicalType(NodeLogicalType.ROUTER).build();        
        Node node3 = new Node.Builder(0xA3L)
		 .setAddress((short)0x543F)
		 .setGatewayId((byte)1)
		 .setCapability((byte)0x7A)
		 .setLogicalType(NodeLogicalType.ROUTER).build();
        
        node1.addNeighbor(node2);
        node1.addNeighbor(node3);
        node2.addNeighbor(node3);
        
        ClientResponse insertionResponse;
        insertionResponse = client.resource(TARGET).type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node1);
        assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());
        insertionResponse = client.resource(TARGET).type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node2);
        assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());
        insertionResponse = client.resource(TARGET).type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node3);
        assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());
        
    	Node recoveredNode;      
    	
        recoveredNode = client.resource(TARGET).path(Long.toString(node1.getId())).accept(MediaType.APPLICATION_JSON).get(Node.class);
        assertEquals(2,recoveredNode.getNeighborIds().size());
        recoveredNode = client.resource(TARGET).path(Long.toString(node2.getId())).accept(MediaType.APPLICATION_JSON).get(Node.class);
        assertEquals(1,recoveredNode.getNeighborIds().size());
        recoveredNode = client.resource(TARGET).path(Long.toString(node3.getId())).accept(MediaType.APPLICATION_JSON).get(Node.class);
        assertEquals(0,recoveredNode.getNeighborIds().size());
        
		ClientResponse getResponse = client.resource(TARGET).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		List<Node> nodeList = JsonUtils.getListFrom(getResponse,Node.class);
		
		assertEquals(3,nodeList.size());
	}
	
	@Test
	public void testUpdate() throws JSONException {
		
        Node.Builder nb = new Node.Builder(10L);
        
        nb.setName("test");
        nb.setGatewayId((byte)2);
        nb.setAddress((short)15);
        nb.setCapability((byte)14);
        
        Node node = nb.build();
       
        postNode(node.getId(),node.getName(),node.getGatewayId(),node.getAddress(),node.getCapability());
		
		ClientResponse genericUpdateResponse = postNode(node.getId(),node.getName(),node.getGatewayId(),(short)(node.getAddress()+1),node.getCapability());
        assertEquals(ClientResponse.Status.OK,genericUpdateResponse.getClientResponseStatus());
        
        ClientResponse genericUpdateNodeResponse = client.resource(TARGET).path(String.valueOf(node.getId()))
        											  .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        
        Node updatedNode = JsonUtils.getFrom(genericUpdateNodeResponse, Node.class);
        
        assertFalse(node.equals(updatedNode));
        
        updatedNode.setLocation("Bedroom");
        
        ClientResponse locationUpdateResponse = client.resource(TARGET).type(MediaType.APPLICATION_JSON).post(ClientResponse.class,updatedNode);
        assertEquals(ClientResponse.Status.OK,locationUpdateResponse.getClientResponseStatus());
        
        ClientResponse locationUpdateNodeResponse = client.resource(TARGET).path(String.valueOf(node.getId()))
				  .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        Node locationUpdatedNode = JsonUtils.getFrom(locationUpdateNodeResponse, Node.class);
        assertEquals(updatedNode.getLocation(),locationUpdatedNode.getLocation());
        
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("location","Kitchen");
        
        ClientResponse restUpdateResponse = client.resource(TARGET)
                                            		.path(String.valueOf(node.getId()))
                                            		.post(ClientResponse.class,formData);
        assertEquals(ClientResponse.Status.OK,restUpdateResponse.getClientResponseStatus());
        
        ClientResponse restUpdateNodeResponse = client.resource(TARGET).path(String.valueOf(node.getId()))
				  .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        Node restUpdateNode = JsonUtils.getFrom(restUpdateNodeResponse, Node.class);
        assertEquals("Kitchen",restUpdateNode.getLocation());
	}
	
	@After
	public void removeNodes() {
		client.resource(TARGET).delete();
	}
	
	private ClientResponse postNode(long id, String name, byte gatewayId, short address, byte capability) {
		
		Node.Builder nb = new Node.Builder(id);
        Node node = nb.setName(name).setGatewayId(gatewayId).setAddress(address).setCapability(capability).build();
        
        return client.resource(TARGET).type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node);
	}
		
}
