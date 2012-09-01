package it.uniud.easyhome.rest;

import static org.junit.Assert.*;

import it.uniud.easyhome.gateway.Gateway;
import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.testutils.JsonJaxbContextResolver;

import java.util.List;

import javax.ws.rs.client.Configuration;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.*;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.media.json.JsonJaxbFeature;
import org.glassfish.jersey.media.json.JsonJaxbModule;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

public class GatewayResourceTest extends JerseyTest {

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
        List<Gateway> gateways = target().path("gateways").request(MediaType.APPLICATION_JSON).get(gatewaysType);
        assertEquals(0,gateways.size());
    }

    @Test
    public void testInsertion() throws JSONException {
        
        MultivaluedMap<String,String> formData = new MultivaluedHashMap<String,String>();
        formData.add("port","5000");
        formData.add("protocol",ProtocolType.ZIGBEE.toString());
        
        Response insertionResponse = target().path("gateways").request().post(Entity.form(formData));
        assertEquals(Status.CREATED,insertionResponse.getStatusInfo());
        
        GenericType<List<Gateway>> gatewaysType = new GenericType<List<Gateway>>() {};
        List<Gateway> gateways = target().path("gateways").request(MediaType.APPLICATION_JSON).get(gatewaysType);
        assertEquals(1,gateways.size());
        Gateway gw = gateways.get(0);
        assertTrue(gw.getId()>0);
        assertEquals(5000,gw.getPort());
        assertEquals(ProtocolType.ZIGBEE,gw.getProtocolType());
    }

    @Ignore
    @Test
    public void testDelete() {
        
        MultivaluedMap<String,String> formData = new MultivaluedHashMap<String,String>();
        formData.add("port","2000");
        formData.add("protocol",ProtocolType.EHS.toString());
        target().path("gateways").request().post(Entity.form(formData));
        
        Response deleteResponse1 = target().path("gateways/1").request().delete();
        assertEquals(Status.OK,deleteResponse1.getStatusInfo());
        Response deleteResponse2 = target().path("gateways/1").request().delete();
        assertEquals(Status.NOT_FOUND,deleteResponse2.getStatusInfo());
    }
    
    @After
    public void clear() {
        target().path("gateways").request().delete();
    }
}
