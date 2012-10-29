package it.uniud.easyhome.gateway;

import static org.junit.Assert.*;
import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.xbee.XBeeConstants;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;


import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.codehaus.jackson.util.ByteArrayBuilder;
import org.junit.*;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class XBeeGatewayIT {
   
	private static final String TARGET = "http://localhost:8080/easyhome/rest/hub/gateways";
	
	private final static int SRC_GATEWAY_PORT = 5000;
	private final static ProtocolType SRC_GATEWAY_PROTOCOL = ProtocolType.XBEE;
	private final static int DST_GATEWAY_PORT = 6000;
	private final static ProtocolType DST_GATEWAY_PROTOCOL = ProtocolType.XBEE;
	
	private static Client client;
	
	@BeforeClass
    public static void setup() {
        client = Client.create();
    }
	
    @Test
    public void testXBeePacket() throws IOException {
        
        long dstUuid = 0x45DCBA98;
        int dstAddress = 20;
        int dstPort = 2;
        int srcEndpoint = 15;
        
        ClientResponse srcGwInsertionResponse = insertGateway(SRC_GATEWAY_PORT,SRC_GATEWAY_PROTOCOL);
        String locationPath = srcGwInsertionResponse.getLocation().getPath();
        String[] segments = locationPath.split("/");
        int srcGid = Integer.parseInt(segments[segments.length-1]);

        ClientResponse dstGwInsertionResponse = insertGateway(DST_GATEWAY_PORT,DST_GATEWAY_PROTOCOL);
        locationPath = dstGwInsertionResponse.getLocation().getPath();
        segments = locationPath.split("/");
        int dstGid = Integer.parseInt(segments[segments.length-1]);
        
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        
        formData.add("gid",String.valueOf(dstGid));
        formData.add("nuid",String.valueOf(dstUuid));
        formData.add("address",String.valueOf(dstAddress));
        formData.add("port",String.valueOf(dstPort));
        
        ClientResponse routingInsertionResponse = client.resource(TARGET)
                                            .path(String.valueOf(srcGid))
                                            .path("routing").post(ClientResponse.class,formData); 
        
        assertEquals(ClientResponse.Status.CREATED,routingInsertionResponse.getClientResponseStatus());
        
        String routingInsertionPath = routingInsertionResponse.getLocation().toString();
        
        
        int mappedDstEndpoint = Integer.parseInt(client.resource(routingInsertionPath).get(String.class));
        
        Socket xbeeSkt1 = new Socket("localhost",SRC_GATEWAY_PORT);
        
        Socket xbeeSkt2 = new Socket("localhost",DST_GATEWAY_PORT);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        int sum = 0;
        // Delimiter
        baos.write(XBeeConstants.START_DELIMITER);
        // Length (21)
        baos.write(0x00);
        baos.write(0x15);
        // Frame type
        baos.write(XBeeConstants.EXPLICIT_RX_INDICATOR_FRAME_TYPE);
        sum += XBeeConstants.EXPLICIT_RX_INDICATOR_FRAME_TYPE;
        // Source 64 bit address (arbitrary)
        baos.write(new byte[7]);
        baos.write(0x77);
        sum += 0x77;
        // Source 16 bit address (arbitrary)
        baos.write(new byte[]{(byte)0x7D,(byte)0xB3});
        sum += 0x7D;
        sum += 0xB3;
        // Source endpoint
        baos.write(srcEndpoint);
        sum += srcEndpoint;
        // Destination endpoint (mapped by the hub)
        baos.write(mappedDstEndpoint);
        sum += mappedDstEndpoint;
        // Cluster Id (On/Off)
        baos.write(new byte[]{0x00,0x06});
        sum += 0x06;
        // Profile Id (Home Automation)
        baos.write(new byte[]{0x01,0x04});
        sum += 0x01;
        sum += 0x04;
        // Receive options (0x02: packet was a broadcast; 0x00 otherwise)
        baos.write(0x00);
        sum += 0x00;
        // Frame control (Cluster specific)
        baos.write(0x01);
        sum += 0x01;
        // Transaction sequence number (arbitrary)
        baos.write(0x71);
        sum += 0x71;
        // Command (toggle)
        baos.write(0x02);
        sum += 0x02;
        // (empty data)
        // Checksum
        baos.write(0xFF - (sum & 0xFF));
        
        byte[] bytesToSend = baos.toByteArray();
        
        BufferedOutputStream os = new BufferedOutputStream(xbeeSkt1.getOutputStream());
        BufferedInputStream is = new BufferedInputStream(xbeeSkt2.getInputStream());
        
        os.write(bytesToSend);
        os.flush();
        os.close();
        
        ByteArrayBuilder ba = new ByteArrayBuilder();
        ba.append(is.read());
        int highLength = is.read();
        ba.append(highLength);
        int lowLength = is.read();
        ba.append(lowLength);
        int length = (highLength << 8) + lowLength;
        int receivedSum = 0;
        for (int i=0; i<length+1; i++) {
        	int byteRead = is.read();
        	ba.append(byteRead);
        	receivedSum += byteRead;
        }

        ba.close();
        
        xbeeSkt1.close();
        xbeeSkt2.close();
        
        assertEquals(0xFF, receivedSum & 0xFF);
    }
    
    private ClientResponse insertGateway(int port, ProtocolType protocol) {
        
    	MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
    	formData.add("port", String.valueOf(port));
    	formData.add("protocol", protocol.toString());
    	ClientResponse response = client.resource(TARGET)
    							  .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
    	
    	return response;
    }
    
    @After
    public void clearGateways() {
    	client.resource(TARGET).delete();
    }
    
}