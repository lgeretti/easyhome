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
        
        ClientResponse insertionResponse = insertNewNode(new GlobalCoordinates(gid,nuid,address));
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
		
        Node node1 = new Node.Builder(1,gid,nuid,address).build();
       
        ClientResponse insertionResponse = insertNewNode(new GlobalCoordinates(gid,nuid,address));
        assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());

        Node recoveredNode = client.resource(TARGET).path(Byte.toString(gid)).path(Short.toString(address)).accept(MediaType.APPLICATION_JSON).get(Node.class);
        
        assertEquals(node1,recoveredNode);
     
        ClientResponse getResponse = client.resource(TARGET).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        List<Node> nodeList = JsonUtils.getListFrom(getResponse,Node.class);
        assertEquals(1,nodeList.size());
        assertEquals(node1,nodeList.get(0));
    }
	
	@Test
	public void testReInsertNode() throws JSONException {

    	byte gid = 2;
    	long nuid = 10L;
    	short address = 0x00CD;
    	NodeLogicalType logicalType = NodeLogicalType.ROUTER;
    	Manufacturer manufacturer = Manufacturer.DIGI;
		
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("gid",Byte.toString(gid));
        formData.add("nuid",Long.toString(nuid));
        formData.add("address",Short.toString(address));
        formData.add("logicalType",logicalType.toString());
        formData.add("manufacturer",manufacturer.toString());
		
        ClientResponse insertionResponse = client.resource(TARGET).path("insert").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
        assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());
        
        Node recoveredNode = client.resource(TARGET).path(Byte.toString(gid)).path(Short.toString(address)).accept(MediaType.APPLICATION_JSON).get(Node.class);
        
        assertEquals(logicalType,recoveredNode.getLogicalType());
        assertEquals(manufacturer,recoveredNode.getManufacturer());
        
        formData = new MultivaluedMapImpl();
        formData.add("gid",Byte.toString(gid));
        formData.add("nuid",Long.toString(nuid));
        formData.add("address",Short.toString(address));
        
        ClientResponse reInsertionResponse = client.resource(TARGET).path("insert").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
        assertEquals(ClientResponse.Status.OK,reInsertionResponse.getClientResponseStatus());
        
        recoveredNode = client.resource(TARGET).path(Byte.toString(gid)).path(Short.toString(address)).accept(MediaType.APPLICATION_JSON).get(Node.class);
        
        assertEquals(logicalType,recoveredNode.getLogicalType());
        assertEquals(manufacturer,recoveredNode.getManufacturer());
	}

	@Test
	public void testDeleteNode() throws JSONException {
		
    	byte gid = 2;
    	long nuid = 10L;
    	short address = 0x00CD;
       
        insertNewNode(new GlobalCoordinates(gid,nuid,address));

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

        ClientResponse firstInsertionResponse = insertNewNode(new GlobalCoordinates(gid,nuid1,address1));
        assertEquals(ClientResponse.Status.CREATED,firstInsertionResponse.getClientResponseStatus());
        ClientResponse secondInsertionResponse = insertNewNode(new GlobalCoordinates(gid,nuid2,address2));
        assertEquals(ClientResponse.Status.CREATED,secondInsertionResponse.getClientResponseStatus());
        
        Node recoveredNode1 = client.resource(TARGET).path(Byte.toString(gid)).path(Short.toString(address1)).accept(MediaType.APPLICATION_JSON).get(Node.class);
        Node recoveredNode2 = client.resource(TARGET).path(Byte.toString(gid)).path(Short.toString(address2)).accept(MediaType.APPLICATION_JSON).get(Node.class);
        
        recoveredNode1.addNeighbor(recoveredNode2);
        
        ClientResponse updateResponse = client.resource(TARGET).path("update").type(MediaType.APPLICATION_JSON).post(ClientResponse.class,recoveredNode1);
        assertEquals(ClientResponse.Status.OK,updateResponse.getClientResponseStatus());
        
        recoveredNode1 = client.resource(TARGET).path(Byte.toString(gid)).path(Short.toString(address1)).accept(MediaType.APPLICATION_JSON).get(Node.class);
        assertEquals(1,recoveredNode1.getNeighbors().size());
        
		ClientResponse getResponse = client.resource(TARGET).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		List<Node> nodeList = JsonUtils.getListFrom(getResponse,Node.class);
		
		assertEquals(2,nodeList.size());
    }
	
	@Test
	public void testComplexNetwork() throws JSONException {
		
    	byte gid = 2;
    	
    	int numNodes = 8;
    	
    	Map<Integer,Node> nodes = new HashMap<Integer,Node>();
    	
    	for (int i=1; i<=numNodes; i++) {
    		ClientResponse insertionResponse = insertNewNode(new GlobalCoordinates(gid,i,(short)i));
            assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());    		
            nodes.put(i,client.resource(TARGET).path(Byte.toString(gid)).path(Short.toString((short)i)).accept(MediaType.APPLICATION_JSON).get(Node.class));
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
		
    	byte gid = 2;
    	long nuid = 10L;
    	short address = 0x00CD;
		
        Node node = new Node.Builder(1,gid,nuid,address)
        				.setName("test")
        				.build();
       
        insertNewNode(node.getCoordinates());
        
        ClientResponse insertionResponse = client.resource(TARGET).path(Byte.toString(gid)).path(Short.toString((address)))
        											  .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        
        Node recoveredNode = JsonUtils.getFrom(insertionResponse, Node.class);
        
        recoveredNode.setLocation(new Location("Camera",LocationType.BEDROOM));
        
        ClientResponse locationUpdateResponse = client.resource(TARGET).path("update").type(MediaType.APPLICATION_JSON).post(ClientResponse.class,recoveredNode);
        assertEquals(ClientResponse.Status.OK,locationUpdateResponse.getClientResponseStatus());
        
        ClientResponse locationUpdateNodeResponse = client.resource(TARGET).path(Byte.toString(gid)).path(Short.toString(address))
				  .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        Node locationUpdatedNode = JsonUtils.getFrom(locationUpdateNodeResponse, Node.class);
        assertTrue(recoveredNode.getLocation().equals(locationUpdatedNode.getLocation()));
        
        locationUpdatedNode.setLocation(new Location("Cucina",LocationType.KITCHEN));
        
        ClientResponse locationUpdateResponse2 = client.resource(TARGET).path("update").type(MediaType.APPLICATION_JSON).post(ClientResponse.class,locationUpdatedNode);
        assertEquals(ClientResponse.Status.OK,locationUpdateResponse2.getClientResponseStatus());
        
        ClientResponse restUpdateNodeResponse = client.resource(TARGET).path(Byte.toString(gid)).path(Short.toString(address))
				  .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        Node restUpdateNode = JsonUtils.getFrom(restUpdateNodeResponse, Node.class);
        assertTrue(new Location("Cucina",LocationType.KITCHEN).equals(restUpdateNode.getLocation()));
	}
	
	@After
	public void removeNodes() {
		client.resource(TARGET).delete();
		client.resource(TARGET).path("jobs").delete();
		client.resource(TARGET).path("links").delete();
	}
	
	private ClientResponse insertNewNode(GlobalCoordinates coords) {
		
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("gid",Byte.toString(coords.getGatewayId()));
        formData.add("nuid",Long.toString(coords.getNuid()));
        formData.add("address",Short.toString(coords.getAddress()));
		
        return client.resource(TARGET).path("insert").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	}
	
	@Test
	public void testInsertAndUpdateLink() throws JSONException {
		
		byte gatewayId = 2;
		LocalCoordinates source = new LocalCoordinates(11L,(short)1);
		LocalCoordinates destination = new LocalCoordinates(12L,(short)2);
		
		ClientResponse insertionResponse = insertLink(gatewayId,source,destination);
		assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());
		
        String locationPath = insertionResponse.getLocation().getPath();
        String[] segments = locationPath.split("/");
        long id = Long.parseLong(segments[segments.length-1]);
		
        ClientResponse getResponse = client.resource(TARGET).path("links").path(Long.toString(id)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK,getResponse.getClientResponseStatus());
        
        Link link = JsonUtils.getFrom(getResponse, Link.class);
        
        assertEquals(gatewayId,link.getGatewayId());
        assertTrue(source.equals(link.getSource()));
        assertTrue(destination.equals(link.getDestination()));
        
		ClientResponse updateResponse = insertLink(gatewayId,source,destination);
		assertEquals(ClientResponse.Status.OK,updateResponse.getClientResponseStatus());
		
		Link updatedLink = client.resource(TARGET).path("links").path(Long.toString(id)).accept(MediaType.APPLICATION_JSON).get(Link.class);
	    assertTrue(updatedLink.getDate().after(link.getDate()));
	}
	
	
	private ClientResponse insertLink(byte gatewayId, LocalCoordinates source, LocalCoordinates destination) throws JSONException {
		
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("gatewayId",Byte.toString(gatewayId));
        formData.add("sourceNuid",Long.toString(source.getNuid()));
        formData.add("sourceAddress",Short.toString(source.getAddress()));
        formData.add("destinationNuid",Long.toString(destination.getNuid()));
        formData.add("destinationAddress",Short.toString(destination.getAddress()));
        
        return client.resource(TARGET).path("links").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);		
	}
	
	@Test
	public void testInsertJob() throws JSONException {
		
        ClientResponse insertionResponse = postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,(short)11,(byte)9,(byte)1);
        
        assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());

        String locationPath = insertionResponse.getLocation().getPath();
        String[] segments = locationPath.split("/");
        String jobIdString = segments[segments.length-1];
        
        NetworkJob recoveredJob = client.resource(TARGET).path("jobs").path(jobIdString).accept(MediaType.APPLICATION_JSON).get(NetworkJob.class);
        
        assertEquals(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,recoveredJob.getType());        
    }
	
	@Test
	public void testDeleteJobById() throws JSONException {
		
        ClientResponse insertionResponse = postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,(short)11,(byte)9,(byte)3);

        String locationPath = insertionResponse.getLocation().getPath();
        String[] segments = locationPath.split("/");
        String jobIdString = segments[segments.length-1];
		
        ClientResponse deletionResponse = client.resource(TARGET).path("jobs").path(jobIdString).delete(ClientResponse.class);
        
        assertEquals(ClientResponse.Status.OK,deletionResponse.getClientResponseStatus());
	}
	
	
	@Test
	public void testDeleteJobByCoords() throws JSONException {
		
        postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,(short)11,(byte)9,(byte)1);
		
		MultivaluedMap<String,String> queryData = new MultivaluedMapImpl();
		queryData.add("type",NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST.toString());
		queryData.add("gid",String.valueOf((byte)3));
		queryData.add("address",String.valueOf((short)11));
        
        ClientResponse deletionResponse = client.resource(TARGET).path("jobs").queryParams(queryData).delete(ClientResponse.class);
        
        assertEquals(ClientResponse.Status.OK,deletionResponse.getClientResponseStatus());
	}		
	
	@Test
	public void testGetLatestJobs() throws JSONException {
		
		postBunchOfJobs();
		
		MultivaluedMap<String,String> queryData = new MultivaluedMapImpl();
		queryData.add("type",NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST.toString());
        
        ClientResponse getResponse = client.resource(TARGET).path("jobs").queryParams(queryData).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        List<NetworkJob> jobList = JsonUtils.getListFrom(getResponse,NetworkJob.class);
        
        assertEquals(7,jobList.size());
	}
	
	@Test
	public void testTypeAndTransactionSpecificJob() throws JSONException {
		
		postBunchOfJobs();
		
		MultivaluedMap<String,String> queryData = new MultivaluedMapImpl();
		queryData.add("type",NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST.toString());
		queryData.add("tsn",Byte.toString((byte)6));
        
        ClientResponse getResponse = client.resource(TARGET).path("jobs").queryParams(queryData).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        List<NetworkJob> jobList = JsonUtils.getListFrom(getResponse,NetworkJob.class);
        
        assertEquals(1,jobList.size());
	}
	
	@Test
	public void testTypeNodeSpecificJob() throws JSONException {
		
		postBunchOfJobs();
		
		MultivaluedMap<String,String> queryData = new MultivaluedMapImpl();
		queryData.add("type",NetworkJobType.NODE_DESCR_REQUEST.toString());
		queryData.add("gid",String.valueOf((byte)3));
		queryData.add("address",String.valueOf((short)12));
		queryData.add("endpoint",String.valueOf((byte)7));
        
        ClientResponse getResponse = client.resource(TARGET).path("jobs").queryParams(queryData).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        List<NetworkJob> jobList = JsonUtils.getListFrom(getResponse,NetworkJob.class);
        
        assertEquals(1,jobList.size());
        assertEquals((byte)6,jobList.get(0).getTsn());
	}
	
	@Test
	public void testNodeSpecificJob() throws JSONException {
		
		postBunchOfJobs();
		
		MultivaluedMap<String,String> queryData = new MultivaluedMapImpl();
		queryData.add("gid",String.valueOf((byte)3));
		queryData.add("address",String.valueOf((short)12));
		queryData.add("endpoint",String.valueOf((byte)7));
        
        ClientResponse getResponse = client.resource(TARGET).path("jobs").queryParams(queryData).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        List<NetworkJob> jobList = JsonUtils.getListFrom(getResponse,NetworkJob.class);
        
        assertEquals(1,jobList.size());
        assertEquals((byte)7,jobList.get(0).getTsn());
	}
	
	private void postBunchOfJobs() {
		
		postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,(short)9,(byte)9,(byte)1);
		postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,(short)9,(byte)9,(byte)2);
		postJob(NetworkJobType.NODE_DESCR_REQUEST,           (byte)3,(short)9,(byte)9,(byte)2);
		postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,(short)10,(byte)9,(byte)3);
		postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,(short)11,(byte)9,(byte)4);
		postJob(NetworkJobType.NODE_DESCR_REQUEST,           (byte)3,(short)11,(byte)9,(byte)4);
		postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,(short)11,(byte)10,(byte)5);
		postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,(short)12,(byte)7,(byte)6);
		postJob(NetworkJobType.NODE_DESCR_REQUEST,           (byte)3,(short)12,(byte)7,(byte)5);
		postJob(NetworkJobType.NODE_DESCR_REQUEST,           (byte)3,(short)12,(byte)7,(byte)6);
		postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,(short)12,(byte)7,(byte)7);
		postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,(short)12,(byte)9,(byte)8);
		postJob(NetworkJobType.NODE_DESCR_REQUEST,           (byte)3,(short)12,(byte)9,(byte)8);
		postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,(short)12,(byte)15,(byte)9);
	}
		
	private ClientResponse postJob(NetworkJobType type, byte gatewayId, short address, byte endpoint, byte tsn) {
		
		MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("type",type.toString());
        formData.add("gid",Byte.toString(gatewayId));
        formData.add("address",Short.toString(address));
        formData.add("endpoint",Byte.toString(endpoint));
        formData.add("tsn",Byte.toString(tsn));
        
        return client.resource(TARGET).path("jobs").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	}
	
}
