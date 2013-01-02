package it.uniud.easyhome.rest;

import static org.junit.Assert.*;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.devices.HomeAutomationDevice;
import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.network.Manufacturer;
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
    	client.resource(TARGET).path("hub").path("gateways").delete();
    	client.resource(TARGET).path("processes").delete();
    	client.resource(TARGET).path("network").delete();
    }
    
    private ClientResponse insertGateway(int port, ProtocolType protocol) {
        
    	MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
    	formData.add("port", String.valueOf(port));
    	formData.add("protocol", protocol.toString());
    	ClientResponse response = client.resource(TARGET).path("hub").path("gateways")
    							  .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
    	
    	return response;
    }
    
    private ClientResponse insertProcess(ProcessKind kind) {
        
    	MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
    	formData.add("kind", kind.toString());
    	ClientResponse response = client.resource(TARGET).path("processes")
    							  .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
    	
    	return response;
    }    
	
	@Test
    public void testRegistration() throws Exception {
		
		ClientResponse gatewayInsertion = insertGateway(XBEE_GATEWAY_PORT, ProtocolType.XBEE);
		assertEquals(ClientResponse.Status.CREATED,gatewayInsertion.getClientResponseStatus());
        String locationPath = gatewayInsertion.getLocation().getPath();
        String[] segments = locationPath.split("/");
        int gid = Integer.parseInt(segments[segments.length-1]);
		
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
		
        Node node1 = new Node.Builder(0xA1L)
        							 .setAddress((short)0x543F)
        							 .setGatewayId((byte)gid)
        							 .setCapability((byte)0x7A)
        							 .setLogicalType(NodeLogicalType.ROUTER)
        							 .setManufacturer(Manufacturer.DIGI).build();
        
        Node node2 = new Node.Builder(0xA2L)
		 .setAddress((short)0x544F)
		 .setGatewayId((byte)gid)
		 .setCapability((byte)0x7A)
		 .setLogicalType(NodeLogicalType.ROUTER)
		 .setManufacturer(Manufacturer.DIGI).build();
        
        node1.addNeighbor(node2);
        node1.setEndpoints(Arrays.asList((short)18,(short)3));
        node1.addDevice((short)18, HomeAutomationDevice.DIMMABLE_LIGHT);
        node1.addDevice((short)3, HomeAutomationDevice.SIMPLE_SENSOR);
        
        mn.register(node1);
        mn.register(node2);
        mn.turnOn();
        
        // Robustly check that we persist within a reasonably high time, since 
        // the process persists it asynchronously
        int counter = 0;
        long sleepTime = 500;
        long maximumSleepTime = 8000;
        while (sleepTime*counter < maximumSleepTime) {
        	counter++;
	    	ClientResponse getNodesResponse = client.resource(TARGET).path("network")
						.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	    	List<Node> nodes = JsonUtils.getListFrom(getNodesResponse, Node.class);
	    	
	    	if (nodes.size() == 2) {
	    		Node recoveredNode1 = client.resource(TARGET).path("network").path(Long.toString(node1.getId())).accept(MediaType.APPLICATION_JSON).get(Node.class);
	    		Map<Short,HomeAutomationDevice> devices = recoveredNode1.getMappedDevices();
	    		System.out.println("Devices endpoints: " + Arrays.toString(devices.keySet().toArray()));
	    		System.out.println("Devices types: " + Arrays.toString(devices.values().toArray()));
	    		if (recoveredNode1.getNeighborIds().size() == 1 &&
	    			devices.size() == 2 &&
	    			devices.get(18) == HomeAutomationDevice.DIMMABLE_LIGHT &&
	    			devices.get(3) == HomeAutomationDevice.SIMPLE_SENSOR
	    			)
		    		break;
	    	}
	    	
	    	Thread.sleep(sleepTime);
        }
        
    	assertTrue(sleepTime*counter < maximumSleepTime);
    	
    	mn.turnOff();
    	mn.unregisterAll();
	}
    
}
