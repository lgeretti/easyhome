package it.uniud.easyhome.rest;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.devices.HomeAutomationDevice;
import it.uniud.easyhome.network.Link;
import it.uniud.easyhome.network.LocalCoordinates;
import it.uniud.easyhome.network.Location;
import it.uniud.easyhome.network.LocationType;
import it.uniud.easyhome.network.Manufacturer;
import it.uniud.easyhome.network.NetworkJob;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.GlobalCoordinates;
import it.uniud.easyhome.network.NodeLogicalType;

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

public class NodeResourceIT {
	
	private static final String TARGET = "http://localhost:8080/easyhome/rest/" + RestPaths.NODES;
	
	private static Client client;
	
	@BeforeClass
    public static void setup() {
        client = Client.create();
    }
	
	@After
	public void removeAll() {
		client.resource(TARGET).delete();
	}
	
    @Test
    public void putDevicesForNode() throws JSONException {
    	
    	byte gatewayId = 2;
    	long nuid = 10L;
    	short address = 15;
        
        ClientResponse insertionResponse = insertNewNode(new GlobalCoordinates(gatewayId,nuid,address));
        assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());

        ClientResponse retrievalResponse = client.resource(TARGET).path(Byte.toString(gatewayId)).path(Short.toString(address))
        														  .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
    	Node retrievedNode = JsonUtils.getFrom(retrievalResponse,Node.class);
    	
    	retrievedNode.setEndpoints(Arrays.asList((short)2,(short)7,(short)3,(short)5));
    	retrievedNode.addDevice((short)5, HomeAutomationDevice.ONOFF_SWITCH);
    	retrievedNode.addDevice((short)3, HomeAutomationDevice.DIMMABLE_LIGHT);    	
    	
    	ClientResponse updateResponse = client.resource(TARGET).path("update").type(MediaType.APPLICATION_JSON).post(ClientResponse.class,retrievedNode);
        assertEquals(ClientResponse.Status.OK,updateResponse.getClientResponseStatus());
    	
    	retrievalResponse = client.resource(TARGET).path(Byte.toString(gatewayId)).path(Short.toString(address))
				  .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
    	retrievedNode = JsonUtils.getFrom(retrievalResponse,Node.class);
    	
    	Map<Short,HomeAutomationDevice> retrievedDevices = retrievedNode.getMappedDevices();
    	assertEquals(4,retrievedDevices.size());
    	assertEquals(HomeAutomationDevice.ONOFF_SWITCH,retrievedDevices.get((short)5));
    	assertEquals(HomeAutomationDevice.DIMMABLE_LIGHT,retrievedDevices.get((short)3));
    	assertEquals(HomeAutomationDevice.UNKNOWN,retrievedDevices.get((short)2));
    	assertEquals(HomeAutomationDevice.UNKNOWN,retrievedDevices.get((short)7));
    	
    	retrievedNode.addDevice((short)7, HomeAutomationDevice.LEVEL_CONTROL_SWITCH);
    	updateResponse = client.resource(TARGET).path("update").type(MediaType.APPLICATION_JSON).post(ClientResponse.class,retrievedNode);
        assertEquals(ClientResponse.Status.OK,updateResponse.getClientResponseStatus());
        
        retrievalResponse = client.resource(TARGET).path(Byte.toString(gatewayId)).path(Short.toString(address)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
    	retrievedNode = JsonUtils.getFrom(retrievalResponse,Node.class);

    	retrievedDevices = retrievedNode.getMappedDevices();
    	
    	assertEquals(4,retrievedDevices.size());
    	assertEquals(HomeAutomationDevice.ONOFF_SWITCH,retrievedDevices.get((short)5));
    	assertEquals(HomeAutomationDevice.DIMMABLE_LIGHT,retrievedDevices.get((short)3));
    	assertEquals(HomeAutomationDevice.UNKNOWN,retrievedDevices.get((short)2));
    	assertEquals(HomeAutomationDevice.LEVEL_CONTROL_SWITCH,retrievedDevices.get((short)7));           
    }
    
	@Test
	public void testNoNodes() throws JSONException {
		
		ClientResponse getResponse = client.resource(TARGET).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		
		List<Node> nodeList = JsonUtils.getListFrom(getResponse,Node.class);
		
		assertEquals(0,nodeList.size());
	}
	
	@Test
	public void testInsertNode() throws JSONException {
		
    	byte gatewayId = 2;
    	long nuid = 10L;
    	short address = 0x00CD;
		
        Node node1 = new Node.Builder(1,gatewayId,nuid,address).build();
       
        ClientResponse insertionResponse = insertNewNode(new GlobalCoordinates(gatewayId,nuid,address));
        assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());

        Node recoveredNode = client.resource(TARGET).path(Byte.toString(gatewayId)).path(Short.toString(address)).accept(MediaType.APPLICATION_JSON).get(Node.class);
        
        assertEquals(node1,recoveredNode);
     
        ClientResponse getResponse = client.resource(TARGET).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        List<Node> nodeList = JsonUtils.getListFrom(getResponse,Node.class);
        assertEquals(1,nodeList.size());
        assertEquals(node1,nodeList.get(0));
    }
	
	@Test
	public void testReInsertNode() throws JSONException {

    	byte gatewayId = 2;
    	long nuid = 10L;
    	short address = 0x00CD;
    	NodeLogicalType logicalType = NodeLogicalType.ROUTER;
    	Manufacturer manufacturer = Manufacturer.DIGI;
		
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("gatewayId",Byte.toString(gatewayId));
        formData.add("nuid",Long.toString(nuid));
        formData.add("address",Short.toString(address));
        formData.add("logicalType",logicalType.toString());
        formData.add("manufacturer",manufacturer.toString());
		
        ClientResponse insertionResponse = client.resource(TARGET).path("insert").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
        assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());
        
        Node recoveredNode = client.resource(TARGET).path(Byte.toString(gatewayId)).path(Short.toString(address)).accept(MediaType.APPLICATION_JSON).get(Node.class);
        
        assertEquals(logicalType,recoveredNode.getLogicalType());
        assertEquals(manufacturer,recoveredNode.getManufacturer());
        
        formData = new MultivaluedMapImpl();
        formData.add("gatewayId",Byte.toString(gatewayId));
        formData.add("nuid",Long.toString(nuid));
        formData.add("address",Short.toString(address));
        
        ClientResponse reInsertionResponse = client.resource(TARGET).path("insert").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
        assertEquals(ClientResponse.Status.OK,reInsertionResponse.getClientResponseStatus());
        
        recoveredNode = client.resource(TARGET).path(Byte.toString(gatewayId)).path(Short.toString(address)).accept(MediaType.APPLICATION_JSON).get(Node.class);
        
        assertEquals(logicalType,recoveredNode.getLogicalType());
        assertEquals(manufacturer,recoveredNode.getManufacturer());
	}

	@Test
	public void testDeleteNode() throws JSONException {
		
    	byte gatewayId = 2;
    	long nuid = 10L;
    	short address = 0x00CD;
       
        insertNewNode(new GlobalCoordinates(gatewayId,nuid,address));

        ClientResponse deletionResponse = client.resource(TARGET).path(Byte.toString(gatewayId)).path(Short.toString(address)).delete(ClientResponse.class);
        
        assertEquals(ClientResponse.Status.OK,deletionResponse.getClientResponseStatus());
    }
	
	
	@Test
	public void testInsertTwoNodesAndAddNeighbors() throws JSONException {
		
    	byte gatewayId = 2;
    	long nuid1 = 10L;
    	long nuid2 = 11L;
    	short address1 = 0x00CD;
    	short address2 = (short)0xAFCD;

        ClientResponse firstInsertionResponse = insertNewNode(new GlobalCoordinates(gatewayId,nuid1,address1));
        assertEquals(ClientResponse.Status.CREATED,firstInsertionResponse.getClientResponseStatus());
        ClientResponse secondInsertionResponse = insertNewNode(new GlobalCoordinates(gatewayId,nuid2,address2));
        assertEquals(ClientResponse.Status.CREATED,secondInsertionResponse.getClientResponseStatus());
        
        Node recoveredNode1 = client.resource(TARGET).path(Byte.toString(gatewayId)).path(Short.toString(address1)).accept(MediaType.APPLICATION_JSON).get(Node.class);
        Node recoveredNode2 = client.resource(TARGET).path(Byte.toString(gatewayId)).path(Short.toString(address2)).accept(MediaType.APPLICATION_JSON).get(Node.class);
        
        recoveredNode1.addNeighbor(recoveredNode2);
        
        ClientResponse updateResponse = client.resource(TARGET).path("update").type(MediaType.APPLICATION_JSON).post(ClientResponse.class,recoveredNode1);
        assertEquals(ClientResponse.Status.OK,updateResponse.getClientResponseStatus());
        
        recoveredNode1 = client.resource(TARGET).path(Byte.toString(gatewayId)).path(Short.toString(address1)).accept(MediaType.APPLICATION_JSON).get(Node.class);
        assertEquals(1,recoveredNode1.getNeighbors().size());
        
		ClientResponse getResponse = client.resource(TARGET).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		List<Node> nodeList = JsonUtils.getListFrom(getResponse,Node.class);
		
		assertEquals(2,nodeList.size());
    }
	
	@Test
	public void testComplexNetwork() throws JSONException {
		
    	byte gatewayId = 2;
    	
    	int numNodes = 8;
    	
    	Map<Integer,Node> nodes = new HashMap<Integer,Node>();
    	
    	for (int i=1; i<=numNodes; i++) {
    		ClientResponse insertionResponse = insertNewNode(new GlobalCoordinates(gatewayId,i,(short)i));
            assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());    		
            nodes.put(i,client.resource(TARGET).path(Byte.toString(gatewayId)).path(Short.toString((short)i)).accept(MediaType.APPLICATION_JSON).get(Node.class));
    	}
        
    	nodes.get(1).setLogicalType(NodeLogicalType.COORDINATOR);
    	nodes.get(2).setLogicalType(NodeLogicalType.ROUTER);
    	nodes.get(3).setLogicalType(NodeLogicalType.ROUTER);
    	nodes.get(4).setLogicalType(NodeLogicalType.END_DEVICE);
    	nodes.get(5).setLogicalType(NodeLogicalType.ROUTER);
    	nodes.get(6).setLogicalType(NodeLogicalType.END_DEVICE);
    	nodes.get(7).setLogicalType(NodeLogicalType.END_DEVICE);
    	nodes.get(8).setLogicalType(NodeLogicalType.END_DEVICE);
    	
        nodes.get(1).addNeighbor(nodes.get(2));
        nodes.get(1).addNeighbor(nodes.get(3));
        nodes.get(1).addNeighbor(nodes.get(8));
        nodes.get(2).addNeighbor(nodes.get(3));
        nodes.get(2).addNeighbor(nodes.get(4));
        nodes.get(3).addNeighbor(nodes.get(5));
        nodes.get(5).addNeighbor(nodes.get(6));

    	for (int i=1; i<=numNodes; i++) {
            ClientResponse updateResponse = client.resource(TARGET).path("update").type(MediaType.APPLICATION_JSON).post(ClientResponse.class,nodes.get(i));
            assertEquals(ClientResponse.Status.OK,updateResponse.getClientResponseStatus());
    	}
        
		ClientResponse getResponse = client.resource(TARGET).path("infrastructural").accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		List<Node> infrastructuralNodesList = JsonUtils.getListFrom(getResponse,Node.class);
		
		assertEquals(4,infrastructuralNodesList.size());
    }
	
	@Test
	public void testUpdateNode() throws JSONException {
		
    	byte gatewayId = 2;
    	long nuid = 10L;
    	short address = 0x00CD;
		
        Node node = new Node.Builder(1,gatewayId,nuid,address)
        				.setName("test")
        				.build();
       
        insertNewNode(node.getCoordinates());
        
        ClientResponse insertionResponse = client.resource(TARGET).path(Byte.toString(gatewayId)).path(Short.toString((address)))
        											  .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        
        Node recoveredNode = JsonUtils.getFrom(insertionResponse, Node.class);
        
        recoveredNode.setLocation(new Location("Camera",LocationType.BEDROOM));
        
        ClientResponse locationUpdateResponse = client.resource(TARGET).path("update").type(MediaType.APPLICATION_JSON).post(ClientResponse.class,recoveredNode);
        assertEquals(ClientResponse.Status.OK,locationUpdateResponse.getClientResponseStatus());
        
        ClientResponse locationUpdateNodeResponse = client.resource(TARGET).path(Byte.toString(gatewayId)).path(Short.toString(address))
				  .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        Node locationUpdatedNode = JsonUtils.getFrom(locationUpdateNodeResponse, Node.class);
        assertTrue(recoveredNode.getLocation().equals(locationUpdatedNode.getLocation()));
        
        locationUpdatedNode.setLocation(new Location("Cucina",LocationType.KITCHEN));
        
        ClientResponse locationUpdateResponse2 = client.resource(TARGET).path("update").type(MediaType.APPLICATION_JSON).post(ClientResponse.class,locationUpdatedNode);
        assertEquals(ClientResponse.Status.OK,locationUpdateResponse2.getClientResponseStatus());
        
        ClientResponse restUpdateNodeResponse = client.resource(TARGET).path(Byte.toString(gatewayId)).path(Short.toString(address))
				  .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        Node restUpdateNode = JsonUtils.getFrom(restUpdateNodeResponse, Node.class);
        assertTrue(new Location("Cucina",LocationType.KITCHEN).equals(restUpdateNode.getLocation()));
	}
	
	private ClientResponse insertNewNode(GlobalCoordinates coords) {
		
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("gatewayId",Byte.toString(coords.getGatewayId()));
        formData.add("nuid",Long.toString(coords.getNuid()));
        formData.add("address",Short.toString(coords.getAddress()));
		
        return client.resource(TARGET).path("insert").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	}
	
}
