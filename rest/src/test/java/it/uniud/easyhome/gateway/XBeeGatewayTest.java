package it.uniud.easyhome.gateway;

import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.testutils.JsonJaxbContextResolver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.ws.rs.client.Configuration;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.*;

import org.glassfish.jersey.media.json.JsonJaxbFeature;
import org.glassfish.jersey.media.json.JsonJaxbModule;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

public class XBeeGatewayTest extends JerseyTest {
    
    private static final String[] PACKAGE_NAMES = {
        "it.uniud.easyhome.gateway",
        "it.uniud.easyhome.rest"};
    
    private static ResourceConfig createApp() {
        final ResourceConfig rc = new ResourceConfig()
                .addModules(new JsonJaxbModule())
                .packages(PACKAGE_NAMES);

        return rc;
    }
    
    @Override
    protected ResourceConfig configure() {
        //enable(org.glassfish.jersey.test.TestProperties.LOG_TRAFFIC);
        return createApp();
    }
    
    @Override
    protected void configureClient(Configuration config) {
        config.register(new JsonJaxbFeature()).register(JsonJaxbContextResolver.class);
    }
    
    @Test
    public void testXBeePacket() throws IOException {
        
        int srcGwPort = 5000;
        ProtocolType srcGwProtocol = ProtocolType.XBEE;
        int dstGwPort = 4000;
        ProtocolType dstGwProtocol = ProtocolType.XBEE;
        
        int dstAddress = 20;
        int dstPort = 2;
        int srcEndpoint = 15;
        
        Response srcGwInsertionResponse = insertGateway(srcGwPort,srcGwProtocol);
        String locationPath = srcGwInsertionResponse.getLocation().getPath();
        String[] segments = locationPath.split("/");
        int srcGid = Integer.parseInt(segments[segments.length-1]);

        Response dstGwInsertionResponse = insertGateway(dstGwPort,dstGwProtocol);
        locationPath = dstGwInsertionResponse.getLocation().getPath();
        segments = locationPath.split("/");
        int dstGid = Integer.parseInt(segments[segments.length-1]);
        
        MultivaluedMap<String,String> formData = new MultivaluedHashMap<String,String>();
        
        formData.add("gid",String.valueOf(dstGid));
        formData.add("address",String.valueOf(dstAddress));
        formData.add("port",String.valueOf(dstPort));
        
        Response routingInsertionResponse = target()
                                            .path("hub/gateways")
                                            .path(String.valueOf(srcGid))
                                            .path("routing").request().post(Entity.form(formData)); 
        
        String routingInsertionPath = routingInsertionResponse.getLocation().getPath();
        int mappedDstEndpoint = Integer.parseInt(target().path(routingInsertionPath).request().get(String.class));
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        int sum = 0;
        
        // Delimiter
        baos.write(0x7E);
        // Length (20)
        baos.write(0x01);
        baos.write(0x04);
        // Frame type
        baos.write(0x91);
        sum += 0x91;
        // Source 64 bit address (arbitrary)
        baos.write(new byte[8]);
        // Source 16 bit address (arbitrary)
        baos.write(new byte[]{(byte)0xA2,(byte)0xB3});
        sum += 0xA2;
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
        // Receive options (packet was a broadcast)
        baos.write(0x02);
        sum += 0x02;
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
        
        byte[] packetBytes = baos.toByteArray();
        StringBuilder strb = new StringBuilder();
        for (byte b: packetBytes) {
            if ((0xFF & b) < 0x10)
                strb.append("0");
            strb.append(Integer.toHexString(0xFF & b).toUpperCase()).append(" ");
        }
        
        System.out.println("Sent bytes: " + strb.toString());
    }
    
    private Response insertGateway(int port, ProtocolType protocol) {
        
        MultivaluedMap<String,String> formData = new MultivaluedHashMap<String,String>();
        formData.add("port",String.valueOf(port));
        formData.add("protocol",protocol.toString());
        
        return target().path("hub/gateways").request().post(Entity.form(formData));        
    }
    
    @After
    public void clearGateways() {
        target().path("hub/gateways").request().delete();
    }
}