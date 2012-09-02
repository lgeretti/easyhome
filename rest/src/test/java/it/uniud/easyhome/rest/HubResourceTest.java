package it.uniud.easyhome.rest;

import static org.junit.Assert.*;

import it.uniud.easyhome.gateway.GatewayInfo;
import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.testutils.JsonJaxbContextResolver;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
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
        
        GenericType<List<GatewayInfo>> gatewaysType = new GenericType<List<GatewayInfo>>() {};
        List<GatewayInfo> gateways = target().path("hub/gateways").request(MediaType.APPLICATION_JSON).get(gatewaysType);
        assertEquals(0,gateways.size());
    }

    @Test
    public void testCreateGateway() throws JSONException, IOException {
        
        int port = 5000;
        ProtocolType protocol = ProtocolType.XBEE;
        
        Response insertionResponse = insertGateway(port,protocol);
        assertEquals(Status.CREATED,insertionResponse.getStatusInfo());
        
        GenericType<List<GatewayInfo>> gatewaysType = new GenericType<List<GatewayInfo>>() {};
        List<GatewayInfo> gateways = target().path("hub/gateways").request(MediaType.APPLICATION_JSON).get(gatewaysType);
        assertEquals(1,gateways.size());
        GatewayInfo gw = gateways.get(0);
        assertTrue(gw.getId()>0);
        assertEquals(port,gw.getPort());
        assertEquals(protocol,gw.getProtocolType());
    }

    @Test
    public void testDeleteGateway() {
        
        Response insertionResponse = insertGateway(5000,ProtocolType.XBEE);
        URI location = insertionResponse.getLocation();
        
        Response deleteResponse1 = target().path(location.getPath()).request().delete();
        assertEquals(Status.OK,deleteResponse1.getStatusInfo());
        Response deleteResponse2 = target().path(location.getPath()).request().delete();
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
