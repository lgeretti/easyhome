package it.uniud.easyhome.rest;

import static org.junit.Assert.*;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.network.mock.MockXBeeNetwork;
import it.uniud.easyhome.processing.ProcessKind;

import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

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
    public void clearGateways() {
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
    public void testDeviceRegistration() throws Exception {
		
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
		
        Node.Builder nodeBuilder = new Node.Builder(0xA1L)
        							 .setAddress((short)0x543F)
        							 .setGatewayId((byte)gid)
        							 .setCapability((byte)0x7A)
        							 .setLogicalType(NodeLogicalType.ROUTER);
        Node node = nodeBuilder.build();
        
        mn.register(node);
        mn.turnOn();
        
        // Robustly check that we persist within a reasonably high time, since 
        // the process persists it asynchronously
        int counter = 0;
        long sleepTime = 500;
        long maximumSleepTime = 5000;
        while (sleepTime*counter < maximumSleepTime) {
        	counter++;
	    	ClientResponse getNodesResponse = client.resource(TARGET).path("network")
						.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	    	List<Node> nodes = JsonUtils.getListFrom(getNodesResponse, Node.class);
	    	if (nodes.size() > 0) {
	    		if (nodes.get(0).getLogicalType() == NodeLogicalType.ROUTER)
	    			break;
	    	}
	    		
	    	Thread.sleep(sleepTime);
        }
        
    	assertTrue(sleepTime*counter < maximumSleepTime);
    	
    	mn.turnOff();
	}
    
}
