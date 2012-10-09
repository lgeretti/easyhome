package it.uniud.easyhome.rest;

import static org.junit.Assert.*;

import it.uniud.easyhome.gateway.Gateway;
import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.testutils.JsonJaxbContextResolver;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.client.Configuration;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.*;

import org.codehaus.jettison.json.JSONException;
import org.glassfish.jersey.media.json.JsonJaxbFeature;
import org.glassfish.jersey.media.json.JsonJaxbModule;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

public class HubResourceTest extends JerseyTest {

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
    public void testNoGateways() {
        
        GenericType<List<Gateway>> gatewaysType = new GenericType<List<Gateway>>() {};
        List<Gateway> gateways = target().path("hub/gateways").request(MediaType.APPLICATION_JSON).get(gatewaysType);
        assertEquals(0,gateways.size());
    }

    @Test
    public void testCreateGateway() throws JSONException, IOException {
        
        int port = 5000;
        ProtocolType protocol = ProtocolType.XBEE;
        
        Response insertionResponse = insertGateway(port,protocol);
        assertEquals(Status.CREATED,insertionResponse.getStatusInfo());
        
        GenericType<List<Gateway>> gatewaysType = new GenericType<List<Gateway>>() {};
        List<Gateway> gateways = target().path("hub/gateways").request(MediaType.APPLICATION_JSON).get(gatewaysType);
        assertEquals(1,gateways.size());
        Gateway gw = gateways.get(0);
        assertTrue(gw.getId()>0);
        assertEquals(port,gw.getPort());
        assertEquals(protocol,gw.getProtocolType());
    }
    
    @Test
    public void testRoutingEntry() {
        
        int srcGatewayPort = 5000;
        int dstGid = 2;
        long dstUuid = 0xFAAAAAAA;
        int dstAddress = 20;
        int dstPort = 4;
        
        ProtocolType protocol = ProtocolType.XBEE;
        
        Response gwInsertionResponse = insertGateway(srcGatewayPort,protocol);
        assertEquals(Status.CREATED,gwInsertionResponse.getStatusInfo());
        String locationPath = gwInsertionResponse.getLocation().getPath();
        String[] segments = locationPath.split("/");
        int gid = Integer.parseInt(segments[segments.length-1]);
        
        MultivaluedMap<String,String> formData = new MultivaluedHashMap<String,String>();
        
        formData.add("gid",String.valueOf(dstGid));
        formData.add("uuid",String.valueOf(dstUuid));
        formData.add("address",String.valueOf(dstAddress));
        formData.add("port",String.valueOf(dstPort));
        
        Response routingInsertionResponse = target()
                                            .path("hub/gateways")
                                            .path(String.valueOf(gid))
                                            .path("routing")
                                            .request().post(Entity.form(formData)); 
        
        //assertEquals(Status.CREATED,routingInsertionResponse.getStatusInfo());
        
        String count = target()
                       .path("hub/gateways")
                       .path(String.valueOf(gid))
                       .path("routing/count")
                       .request().get(String.class);
        
        assertEquals(Integer.parseInt(count),1);
        
        String routedPort = target().path(routingInsertionResponse.getLocation().getPath()).request().get(String.class);

        assertNotNull(routedPort);
    }

    @Test
    public void testDeleteGateway() {
        
        int port = 5000;
        ProtocolType protocol = ProtocolType.XBEE;
        
        Response insertionResponse = insertGateway(port,protocol);
        assertEquals(Status.CREATED,insertionResponse.getStatusInfo());
        String insertionPath = insertionResponse.getLocation().getPath();
        
        Response deleteResponse1 = target().path(insertionPath).request().delete();
        assertEquals(Status.OK,deleteResponse1.getStatusInfo());
        Response deleteResponse2 = target().path(insertionPath).request().delete();
        assertEquals(Status.NOT_FOUND,deleteResponse2.getStatusInfo());
    }
    
    @After
    public void clearGateways() {
        target().path("hub/gateways").request().delete();
    }
    
    private Response insertGateway(int port, ProtocolType protocol) {
        
        MultivaluedMap<String,String> formData = new MultivaluedHashMap<String,String>();
        formData.add("port",String.valueOf(port));
        formData.add("protocol",protocol.toString());
        
        return target().path("hub/gateways").request().post(Entity.form(formData));        
    }
    
    
}