package it.uniud.easyhome.rest;

import static org.junit.Assert.*;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.mock.MockXBeeNetwork;
import it.uniud.easyhome.packets.RawPacket;
import it.uniud.easyhome.processing.ProcessKind;
import it.uniud.easyhome.xbee.XBeeConstants;
import it.uniud.easyhome.xbee.XBeeInboundPacket;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class DeviceAnnounceIT {
    
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
    
    private ClientResponse insertNodeRegistrationProcess() {
        
    	MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
    	formData.add("kind", ProcessKind.NodeRegistration.toString());
    	ClientResponse response = client.resource(TARGET).path("processes")
    							  .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
    	
    	return response;
    }    

	@Test
    public void testDeviceRegistration() throws Exception {
		
		ClientResponse gatewayInsertion = insertGateway(XBEE_GATEWAY_PORT, ProtocolType.XBEE);
		assertEquals(ClientResponse.Status.CREATED,gatewayInsertion.getClientResponseStatus());
		
		ClientResponse processInsertion = insertNodeRegistrationProcess();
		assertEquals(ClientResponse.Status.CREATED,processInsertion.getClientResponseStatus());
        
        RawPacket.Builder rpBuilder = new RawPacket.Builder();
        
        int sum = 0;
        // Delimiter
        rpBuilder.append(XBeeConstants.START_DELIMITER);
        // Length (31)
        rpBuilder.append(0x00).append(0x1F);
        // Frame type
        rpBuilder.append(XBeeConstants.EXPLICIT_RX_INDICATOR_FRAME_TYPE);
        sum += XBeeConstants.EXPLICIT_RX_INDICATOR_FRAME_TYPE;
        // Source 64 bit address (arbitrary)
        rpBuilder.append(new byte[7]).append(0x77);
        sum += 0x77;
        // Source 16 bit address (arbitrary)
        rpBuilder.append(new byte[]{(byte)0x7D,(byte)0xB3});
        sum += 0x7D;
        sum += 0xB3;
        // Source endpoint
        rpBuilder.append(0x01);
        sum += 0x01;
        // Destination endpoint
        rpBuilder.append(0x01);
        sum += 0x01;
        // Cluster Id (DeviceAnnce)
        rpBuilder.append(new byte[]{0x00,0x13});
        sum += 0x13;
        // Profile Id (EasyHome ZDP)
        rpBuilder.append(new byte[]{(byte)0xEA,0x50});
        sum += 0xEA;
        sum += 0x50;
        // Receive options (0x02: packet was a broadcast; 0x00 otherwise)
        rpBuilder.append(0x02);
        sum += 0x02;
        // Frame control
        rpBuilder.append(0x00);
        sum += 0x00;
        // Transaction sequence number (arbitrary)
        rpBuilder.append(0x71);
        sum += 0x71;
        // Device announce data
        // NWK addr
        rpBuilder.append(0x23);
        sum += 0x23;
        rpBuilder.append(0x34);
        sum += 0x34;
        // IEEE addr
        for (int i=0;i<7;i++) {
        	rpBuilder.append(0x00);
	        sum += 0x00;        
        }
        rpBuilder.append(0x55);
        sum += 0x55;
        // Capability (random)
        rpBuilder.append(0x7A);
        sum += 0x7A;
        // Checksum
        rpBuilder.append(0xFF - (sum & 0xFF));
        
        XBeeInboundPacket xbeePkt = new XBeeInboundPacket();
        ByteArrayInputStream bais = new ByteArrayInputStream(rpBuilder.build().getBytes());
        xbeePkt.read(bais);
        
        mn.turnOn();
        mn.broadcast(xbeePkt);
        
        // Robustly check that we persist the node within a reasonably high time, since 
        // the process persists it asynchronously
        int counter = 0;
        long sleepTime = 500;
        long maximumSleepTime = 5000;
        while (sleepTime*counter < maximumSleepTime) {
        	Thread.sleep(sleepTime);
        	counter++;
	    	ClientResponse getNodesResponse = client.resource(TARGET).path("network")
						.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	    	List<Node> nodes = JsonUtils.getListFrom(getNodesResponse, Node.class);
	    	if (nodes.size() > 0)
	    		break;
        }
    	assertTrue(sleepTime*counter < maximumSleepTime);
    	
    	mn.turnOff();
	}
    
}
