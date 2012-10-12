package it.uniud.easyhome.rest;

import static org.junit.Assert.*;
import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.xbee.XBeeConstants;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.util.ByteArrayBuilder;
import org.junit.After;
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
	
	@BeforeClass
    public static void setup() {

        client = Client.create();
    }
		
    @After
    public void clearGateways() {
    	client.resource(TARGET).path("hub").path("gateways").delete();
    }
    
    private ClientResponse insertGateway(int port, ProtocolType protocol) {
        
    	MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
    	formData.add("port", String.valueOf(port));
    	formData.add("protocol", protocol.toString());
    	ClientResponse response = client.resource(TARGET).path("hub").path("gateways")
    							  .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
    	
    	return response;
    }
	
    @Ignore
	@Test
    public void testDeviceRegistration() throws NumberFormatException, UnknownHostException, IOException {
		
		insertGateway(XBEE_GATEWAY_PORT, ProtocolType.XBEE);    	
		
        Socket xbeeSkt = new Socket("localhost",XBEE_GATEWAY_PORT);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        int sum = 0;
        // Delimiter
        baos.write(XBeeConstants.START_DELIMITER);
        // Length (32)
        baos.write(0x00);
        baos.write(0x20);
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
        baos.write(0x00);
        sum += 0x00;
        // Destination endpoint
        baos.write(0x00);
        sum += 0x00;
        // Cluster Id (DeviceAnnce)
        baos.write(new byte[]{0x00,0x13});
        sum += 0x13;
        // Profile Id (ZDP)
        baos.write(new byte[]{0x00,0x00});
        sum += 0x00;
        sum += 0x00;
        // Receive options (0x02: packet was a broadcast; 0x00 otherwise)
        baos.write(0x02);
        sum += 0x02;
        // Frame control
        baos.write(0x00);
        sum += 0x00;
        // Transaction sequence number (arbitrary)
        baos.write(0x71);
        sum += 0x71;
        // Command (toggle)
        baos.write(0x02);
        sum += 0x02;
        // Device announce data
        // NWK addr
        baos.write(0x5F);
        sum += 0x5F;
        baos.write(0x34);
        sum += 0x34;
        // IEEE addr
        for (int i=0;i<8;i++) {
	        baos.write(0x55);
	        sum += 0x55;        
        }
        // Capability (random)
        baos.write(0x7A);
        sum += 0x7A;
        // Checksum
        baos.write(0xFF - (sum & 0xFF));
        
        byte[] bytesToSend = baos.toByteArray();
        
        BufferedOutputStream os = new BufferedOutputStream(xbeeSkt.getOutputStream());
        
        os.write(bytesToSend);
        os.flush();
        os.close();
        
        xbeeSkt.close();
    }
    
}
