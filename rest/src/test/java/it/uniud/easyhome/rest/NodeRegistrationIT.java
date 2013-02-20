package it.uniud.easyhome.rest;

import static org.junit.Assert.*;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.devices.HomeAutomationDevice;
import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.network.Manufacturer;
import it.uniud.easyhome.network.NetworkJob;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.network.mock.MockXBeeNetwork;
import it.uniud.easyhome.network.mock.MockXBeeNode;
import it.uniud.easyhome.processing.Process;
import it.uniud.easyhome.processing.ProcessKind;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.codehaus.jettison.json.JSONException;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

@Ignore
public class NodeRegistrationIT {
    
	private final static int XBEE_GATEWAY_PORT = 5050;
	
	private static final String TARGET = "http://localhost:8080/easyhome/rest/";
	
	private static Client client;
	
	private static MockXBeeNetwork mn;
	
	@BeforeClass
    public static void setup() {

        client = Client.create();
        mn = new MockXBeeNetwork("localhost",XBEE_GATEWAY_PORT);
    }
	
    @After
    public void clear() throws InterruptedException {
    	client.resource(TARGET).path(RestPaths.GATEWAYS).delete();
    	client.resource(TARGET).path(RestPaths.PROCESSES).delete();
    	client.resource(TARGET).path(RestPaths.NODES).delete();
    	client.resource(TARGET).path(RestPaths.NODES).path(RestPaths.JOBS).delete();
    }
    
    private ClientResponse insertGateway(int port, ProtocolType protocol) {
        
    	MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
    	formData.add("port", String.valueOf(port));
    	formData.add("protocol", protocol.toString());
    	ClientResponse response = client.resource(TARGET).path(RestPaths.GATEWAYS)
    							  .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
    	
    	return response;
    }
    
    private ClientResponse insertProcess(ProcessKind kind) {
        
    	MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
    	formData.add("kind", kind.toString());
    	ClientResponse response = client.resource(TARGET).path(RestPaths.PROCESSES)
    							  .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
    	
    	return response;
    }    
	
	@Test
    public void testRegistration() throws Exception {
		
		ClientResponse gatewayInsertion = insertGateway(XBEE_GATEWAY_PORT, ProtocolType.XBEE);
		assertEquals(ClientResponse.Status.CREATED,gatewayInsertion.getClientResponseStatus());
        String locationPath = gatewayInsertion.getLocation().getPath();
        String[] segments = locationPath.split("/");
        int gatewayId = Integer.parseInt(segments[segments.length-1]);
		
		ClientResponse nodeAnnceRegProcessInsertion = insertProcess(ProcessKind.NODE_ANNCE_REGISTRATION);
		assertEquals(ClientResponse.Status.CREATED,nodeAnnceRegProcessInsertion.getClientResponseStatus());
	
		ClientResponse nodeDescrAcqProcessInsertion = insertProcess(ProcessKind.NODE_DESCR_REQUEST);
		assertEquals(ClientResponse.Status.CREATED,nodeDescrAcqProcessInsertion.getClientResponseStatus());
		ClientResponse nodeDescrRegProcessInsertion = insertProcess(ProcessKind.NODE_DESCR_REGISTRATION);
		assertEquals(ClientResponse.Status.CREATED,nodeDescrRegProcessInsertion.getClientResponseStatus());	
		
		ClientResponse activeEpAcqProcessInsertion = insertProcess(ProcessKind.ACTIVE_ENDPOINTS_REQUEST);
		assertEquals(ClientResponse.Status.CREATED,activeEpAcqProcessInsertion.getClientResponseStatus());		
		ClientResponse activeEpRegProcessInsertion = insertProcess(ProcessKind.ACTIVE_ENDPOINTS_REGISTRATION);
		assertEquals(ClientResponse.Status.CREATED,activeEpRegProcessInsertion.getClientResponseStatus());			

		ClientResponse nodeNeighAcqProcessInsertion = insertProcess(ProcessKind.NODE_NEIGH_REQUEST);
		assertEquals(ClientResponse.Status.CREATED,nodeNeighAcqProcessInsertion.getClientResponseStatus());
		ClientResponse nodeNeighRegProcessInsertion = insertProcess(ProcessKind.NODE_NEIGH_REGISTRATION);
		assertEquals(ClientResponse.Status.CREATED,nodeNeighRegProcessInsertion.getClientResponseStatus());
		
		ClientResponse nodeSimpleDescrAcqProcessInsertion = insertProcess(ProcessKind.SIMPLE_DESCR_REQUEST);
		assertEquals(ClientResponse.Status.CREATED,nodeSimpleDescrAcqProcessInsertion.getClientResponseStatus());
		ClientResponse nodeSimpleDescrRegProcessInsertion = insertProcess(ProcessKind.SIMPLE_DESCR_REGISTRATION);
		assertEquals(ClientResponse.Status.CREATED,nodeSimpleDescrRegProcessInsertion.getClientResponseStatus());
		
        Node node1 = new Node.Builder(1,(byte)gatewayId,0x1,(short)0x1)
        							.setLogicalType(NodeLogicalType.COORDINATOR)
        							.setManufacturer(Manufacturer.DIGI).build();
        
        Node node2 = new Node.Builder(2,(byte)gatewayId,0x2,(short)0x2)
		 							.setLogicalType(NodeLogicalType.ROUTER)
		 							.setManufacturer(Manufacturer.DIGI).build();
        
        Node node3 = new Node.Builder(3,(byte)gatewayId,0x3,(short)0x3)
		 							.setLogicalType(NodeLogicalType.END_DEVICE)
		 							.setManufacturer(Manufacturer.DIGI).build();
        
        node1.addNeighbor(node2);
        
        node2.addNeighbor(node1);
        
        node3.setEndpoints(Arrays.asList((byte)18,(byte)3));
        node3.addDevice((byte)18, HomeAutomationDevice.DIMMABLE_LIGHT);
        node3.addDevice((byte)3, HomeAutomationDevice.SIMPLE_SENSOR);
        
        node2.addNeighbor(node3);
        
        mn.register(node1);
        mn.register(node2);
        mn.register(node3);
        mn.turnOn();
        
        // Robustly check that we persist within a reasonably high time, since 
        // the process persists it asynchronously
        int counter = 0;
        long sleepTime = 500;
        long maximumSleepTime = 25000;
        int testsToPass = 6;
        int passedTests = 0;
        while (sleepTime*counter < maximumSleepTime) {
        	counter++;
        	System.out.format("Run #%d:", counter);
        			
        	passedTests = 0;
        	
	    	ClientResponse getNodesResponse = client.resource(TARGET).path(RestPaths.NODES)
						.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	    	List<Node> nodes = JsonUtils.getListFrom(getNodesResponse, Node.class);
	    	
	    	if (nodes.size() == mn.getNodes().size()) {
	    		System.out.print("a");
	    		passedTests++;
	    		
	    		Node recoveredNode1 = client.resource(TARGET).path(RestPaths.NODES)
	    									.path(Byte.toString(node1.getCoordinates().getGatewayId()))
	    									.path(Short.toString(node1.getCoordinates().getAddress()))
	    									.accept(MediaType.APPLICATION_JSON).get(Node.class);
	    		Node recoveredNode2 = client.resource(TARGET).path(RestPaths.NODES)
						.path(Byte.toString(node2.getCoordinates().getGatewayId()))
						.path(Short.toString(node2.getCoordinates().getAddress()))
						.accept(MediaType.APPLICATION_JSON).get(Node.class);
	    		Node recoveredNode3 = client.resource(TARGET).path(RestPaths.NODES)
						.path(Byte.toString(node3.getCoordinates().getGatewayId()))
						.path(Short.toString(node3.getCoordinates().getAddress()))
						.accept(MediaType.APPLICATION_JSON).get(Node.class);
	    		
	    		if (recoveredNode1.getLogicalType() == NodeLogicalType.COORDINATOR  
	    			&& recoveredNode2.getLogicalType() == NodeLogicalType.ROUTER
	    			&& recoveredNode3.getLogicalType() == NodeLogicalType.END_DEVICE) {
	    			System.out.print("b");
	    			passedTests++;
	    		}
	    		
	    		if (recoveredNode1.getNeighbors().size() == 1 && recoveredNode2.getNeighbors().size() == 2) {
	    			System.out.print("c");
	    			passedTests++;
	    		}

	    		if (recoveredNode3.getEndpoints().size() == 2) {
	    			System.out.print("d");
	    			passedTests++;
	    		}
	    		
		    	Map<Byte,HomeAutomationDevice> devices = recoveredNode3.getMappedDevices();
		    	
		    	if (devices.size() == 2 &&
		    		devices.get((byte)18) == HomeAutomationDevice.DIMMABLE_LIGHT &&
		    		devices.get((byte)3) == HomeAutomationDevice.SIMPLE_SENSOR
		    		) {
		    		System.out.print("e");
			    	passedTests++;
		    	}
			    	
	        	ClientResponse getJobsResponse = client.resource(TARGET).path(RestPaths.NODES).path(RestPaths.JOBS)
	    				.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	        	int numJobs = JsonUtils.getListFrom(getJobsResponse, NetworkJob.class).size();
	        	
	        	if (numJobs == 0) {
	        		System.out.print("f");
	        		passedTests++;
	        	}
	        		
	        	
	    	}
	    	System.out.println();
	    	
	    	if (passedTests == testsToPass)
	    		break;
	    	
	    	Thread.sleep(sleepTime);
        }
    	
    	assertEquals(testsToPass,passedTests);
    	
    	mn.turnOff();
    	mn.unregisterAll();
	}
    
}
