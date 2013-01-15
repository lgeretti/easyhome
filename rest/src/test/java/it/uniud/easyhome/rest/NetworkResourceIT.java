package it.uniud.easyhome.rest;

import static org.junit.Assert.*;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.devices.HomeAutomationDevice;
import it.uniud.easyhome.network.NetworkJob;
import it.uniud.easyhome.network.NetworkJobType;
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
    	
    	byte gid = 2;
    	long nuid = 10L;
    	short address = 15;
    	byte capability = 14;
        
        ClientResponse insertionResponse = insertNewNode(gid,nuid,address,capability);
        assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());

        ClientResponse retrievalResponse = client.resource(TARGET).path(Byte.toString(gid)).path(Short.toString(address))
        														  .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
    	Node retrievedNode = JsonUtils.getFrom(retrievalResponse,Node.class);
    	
    	retrievedNode.setEndpoints(Arrays.asList((short)2,(short)7,(short)3,(short)5));
    	retrievedNode.addDevice((short)5, HomeAutomationDevice.ONOFF_SWITCH);
    	retrievedNode.addDevice((short)3, HomeAutomationDevice.DIMMABLE_LIGHT);    	
    	
    	ClientResponse updateResponse = client.resource(TARGET).path("update").type(MediaType.APPLICATION_JSON).post(ClientResponse.class,retrievedNode);
        assertEquals(ClientResponse.Status.OK,updateResponse.getClientResponseStatus());
    	
    	retrievalResponse = client.resource(TARGET).path(Byte.toString(gid)).path(Short.toString(address))
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
        
        retrievalResponse = client.resource(TARGET).path(Byte.toString(gid)).path(Short.toString(address)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
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
		
    	byte gid = 2;
    	long nuid = 10L;
    	short address = 0x00CD;
    	byte capability = 14;
		
        Node node1 = new Node.Builder(1,nuid)
        						.setGatewayId(gid)
        						.setAddress(address)
        						.setCapability(capability)
        						.build();
       
        ClientResponse insertionResponse = insertNewNode(gid,nuid,address,capability);
        assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());

        Node recoveredNode = client.resource(TARGET).path(Byte.toString(gid)).path(Short.toString(address)).accept(MediaType.APPLICATION_JSON).get(Node.class);
        
        assertEquals(node1,recoveredNode);
     
        ClientResponse getResponse = client.resource(TARGET).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        List<Node> nodeList = JsonUtils.getListFrom(getResponse,Node.class);
        assertEquals(1,nodeList.size());
        assertEquals(node1,nodeList.get(0));
    }

	@Test
	public void testDeleteNode() throws JSONException {
		
    	byte gid = 2;
    	long nuid = 10L;
    	short address = 0x00CD;
    	byte capability = 14;
       
        insertNewNode(gid,nuid,address,capability);

        ClientResponse deletionResponse = client.resource(TARGET).path(Byte.toString(gid)).path(Short.toString(address)).delete(ClientResponse.class);
        
        assertEquals(ClientResponse.Status.OK,deletionResponse.getClientResponseStatus());
    }
	
	
	@Test
	public void testInsertTwoNodesAndAddNeighbors() throws JSONException {
		
    	byte gid = 2;
    	long nuid1 = 10L;
    	long nuid2 = 11L;
    	short address1 = 0x00CD;
    	short address2 = (short)0xAFCD;
    	byte capability = 14;

        ClientResponse firstInsertionResponse = insertNewNode(gid,nuid1,address1,capability);
        assertEquals(ClientResponse.Status.CREATED,firstInsertionResponse.getClientResponseStatus());
        ClientResponse secondInsertionResponse = insertNewNode(gid,nuid2,address2,capability);
        assertEquals(ClientResponse.Status.CREATED,secondInsertionResponse.getClientResponseStatus());
        
        Node recoveredNode1 = client.resource(TARGET).path(Byte.toString(gid)).path(Short.toString(address1)).accept(MediaType.APPLICATION_JSON).get(Node.class);
        Node recoveredNode2 = client.resource(TARGET).path(Byte.toString(gid)).path(Short.toString(address2)).accept(MediaType.APPLICATION_JSON).get(Node.class);
        
        recoveredNode1.addNeighbor(recoveredNode2);
        
        ClientResponse updateResponse = client.resource(TARGET).path("update").type(MediaType.APPLICATION_JSON).post(ClientResponse.class,recoveredNode1);
        assertEquals(ClientResponse.Status.OK,updateResponse.getClientResponseStatus());
        
        recoveredNode1 = client.resource(TARGET).path(Byte.toString(gid)).path(Short.toString(address1)).accept(MediaType.APPLICATION_JSON).get(Node.class);
        assertEquals(1,recoveredNode1.getNeighborIds().size());
        
		ClientResponse getResponse = client.resource(TARGET).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		List<Node> nodeList = JsonUtils.getListFrom(getResponse,Node.class);
		
		assertEquals(2,nodeList.size());
    }
	
	@Test
	public void testUpdateNode() throws JSONException {
		
    	byte gid = 2;
    	long nuid = 10L;
    	short address = 0x00CD;
    	byte capability = 14;
		
        Node node = new Node.Builder(1,nuid)
        				.setName("test")
        				.setGatewayId(gid)
        				.setAddress(address)
        				.setCapability(capability)
        				.build();
       
        insertNewNode(node.getGatewayId(),node.getNuid(),node.getAddress(),node.getCapability());
        
        ClientResponse insertionResponse = client.resource(TARGET).path(Byte.toString(gid)).path(Short.toString((address)))
        											  .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        
        Node recoveredNode = JsonUtils.getFrom(insertionResponse, Node.class);
        
        recoveredNode.setLocation("Bedroom");
        
        ClientResponse locationUpdateResponse = client.resource(TARGET).path("update").type(MediaType.APPLICATION_JSON).post(ClientResponse.class,recoveredNode);
        assertEquals(ClientResponse.Status.OK,locationUpdateResponse.getClientResponseStatus());
        
        ClientResponse locationUpdateNodeResponse = client.resource(TARGET).path(Byte.toString(gid)).path(Short.toString(address))
				  .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        Node locationUpdatedNode = JsonUtils.getFrom(locationUpdateNodeResponse, Node.class);
        assertEquals(recoveredNode.getLocation(),locationUpdatedNode.getLocation());
        
        locationUpdatedNode.setLocation("Kitchen");
        
        ClientResponse locationUpdateResponse2 = client.resource(TARGET).path("update").type(MediaType.APPLICATION_JSON).post(ClientResponse.class,locationUpdatedNode);
        assertEquals(ClientResponse.Status.OK,locationUpdateResponse2.getClientResponseStatus());
        
        ClientResponse restUpdateNodeResponse = client.resource(TARGET).path(Byte.toString(gid)).path(Short.toString(address))
				  .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        Node restUpdateNode = JsonUtils.getFrom(restUpdateNodeResponse, Node.class);
        assertEquals("Kitchen",restUpdateNode.getLocation());
	}
	
	@After
	public void removeNodes() {
		client.resource(TARGET).delete();
		client.resource(TARGET).path("jobs").delete();
	}
	
	private ClientResponse insertNewNode(byte gatewayId, long nuid, short address, byte capability) {
		
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("gid",Byte.toString(gatewayId));
        formData.add("nuid",Long.toString(nuid));
        formData.add("address",Short.toString(address));
        formData.add("capability",Byte.toString(capability));
		
        return client.resource(TARGET).path("insert").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	}
	
	@Test
	public void testInsertJob() throws JSONException {
		
        ClientResponse insertionResponse = postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,-12983219012L,(short)11,(byte)9);
        
        assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());

        String locationPath = insertionResponse.getLocation().getPath();
        String[] segments = locationPath.split("/");
        String jobIdString = segments[segments.length-1];
        
        NetworkJob recoveredJob = client.resource(TARGET).path("jobs").path(jobIdString).accept(MediaType.APPLICATION_JSON).get(NetworkJob.class);
        
        assertEquals(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,recoveredJob.getType());        
    }
	
	@Test
	public void testDeleteJobById() throws JSONException {
		
        ClientResponse insertionResponse = postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,-12983219012L,(short)11,(byte)9);

        String locationPath = insertionResponse.getLocation().getPath();
        String[] segments = locationPath.split("/");
        String jobIdString = segments[segments.length-1];
		
        ClientResponse deletionResponse = client.resource(TARGET).path("jobs").path(jobIdString).delete(ClientResponse.class);
        
        assertEquals(ClientResponse.Status.OK,deletionResponse.getClientResponseStatus());
	}
	
	
	@Test
	public void testDeleteJobByCoords() throws JSONException {
		
        postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,-12983219012L,(short)11,(byte)9);
		
		MultivaluedMap<String,String> queryData = new MultivaluedMapImpl();
		queryData.add("type",NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST.toString());
		queryData.add("gid",String.valueOf((byte)3));
		queryData.add("address",String.valueOf((short)11));
        
        ClientResponse deletionResponse = client.resource(TARGET).path("jobs").queryParams(queryData).delete(ClientResponse.class);
        
        assertEquals(ClientResponse.Status.OK,deletionResponse.getClientResponseStatus());
	}		
	
	@Test
	public void testResetJobById() throws JSONException {
		
        ClientResponse insertionResponse = postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,-12983219012L,(short)11,(byte)9);

        String locationPath = insertionResponse.getLocation().getPath();
        String[] segments = locationPath.split("/");
        String jobIdString = segments[segments.length-1];
        
        NetworkJob recoveredJob = client.resource(TARGET).path("jobs").path(jobIdString).accept(MediaType.APPLICATION_JSON).get(NetworkJob.class);
        assertTrue(recoveredJob.isFresh());
        Date oldDate = recoveredJob.getDate();
        
        ClientResponse resetResponse = client.resource(TARGET).path("jobs").path(jobIdString).path("reset").post(ClientResponse.class);
        
        assertEquals(ClientResponse.Status.OK,resetResponse.getClientResponseStatus());
        
        NetworkJob recoveredJob2 = client.resource(TARGET).path("jobs").path(jobIdString).accept(MediaType.APPLICATION_JSON).get(NetworkJob.class);
        Date newDate = recoveredJob2.getDate();
        
        assertTrue(newDate.after(oldDate));
        assertFalse(recoveredJob2.isFresh());
    }
	
	@Test
	public void testResetJobByTypeAndCoordinates() throws JSONException {
		
		NetworkJobType type = NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST;
		byte gatewayId = (byte)3;
		long nuid = -219329112312L;
		short address = 11;
		byte endpoint = (byte)9;
		
		ClientResponse insertionResponse = postJob(type,gatewayId,nuid,address,endpoint);
        String locationPath = insertionResponse.getLocation().getPath();
        String[] segments = locationPath.split("/");
        String jobIdString = segments[segments.length-1];
        
        NetworkJob recoveredJob = client.resource(TARGET).path("jobs").path(jobIdString).accept(MediaType.APPLICATION_JSON).get(NetworkJob.class);
        assertTrue(recoveredJob.isFresh());
        Date oldDate = recoveredJob.getDate();

		MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("type",type.toString());
        formData.add("gid",String.valueOf(gatewayId));
        formData.add("nuid",String.valueOf(nuid));
        formData.add("address",String.valueOf(address));
        formData.add("endpoint",String.valueOf(endpoint));
        
        ClientResponse resetResponse = client.resource(TARGET).path("jobs").path("reset").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
        assertEquals(ClientResponse.Status.OK,resetResponse.getClientResponseStatus());
        
        NetworkJob recoveredJob2 = client.resource(TARGET).path("jobs").path(jobIdString).accept(MediaType.APPLICATION_JSON).get(NetworkJob.class);
        Date newDate = recoveredJob2.getDate();
        
        assertTrue(newDate.after(oldDate));
        assertFalse(recoveredJob2.isFresh());
    }
	
		
	private ClientResponse postJob(NetworkJobType type, byte gatewayId, long nuid, short address, byte endpoint) {
		
		MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("type",type.toString());
        formData.add("gid",String.valueOf(gatewayId));
        formData.add("nuid",String.valueOf(nuid));
        formData.add("address",String.valueOf(address));
        formData.add("endpoint",String.valueOf(endpoint));
        
        return client.resource(TARGET).path("jobs").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
        /*
        NetworkJob job = new NetworkJob(7,type,gatewayId,nuid,address,endpoint);
        
        return client.resource(TARGET).path("jobs").type(MediaType.APPLICATION_JSON).post(ClientResponse.class,job);
        */
	}
	
}
