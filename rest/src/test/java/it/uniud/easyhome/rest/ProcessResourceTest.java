package it.uniud.easyhome.rest;

import static org.junit.Assert.*;

import it.uniud.easyhome.processing.Process;
import it.uniud.easyhome.testutils.JsonJaxbContextResolver;

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

public class ProcessResourceTest extends JerseyTest {

    private static final String[] PACKAGE_NAMES = {
        "it.uniud.easyhome.processing",
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
    public void testNoProcesses() {
        
        GenericType<List<Process>> processesType = new GenericType<List<Process>>() {};
        List<Process> processes = target().path("processes").request(MediaType.APPLICATION_JSON).get(processesType);
        assertEquals(0,processes.size());
    }

    @Test
    public void testInsertion() throws JSONException {
        
        MultivaluedMap<String,String> formData = new MultivaluedHashMap<String,String>();
        
        formData.add("kind","nodeRegistration");
        
        Response insertionResponse = target().path("processes")
                                             .request().post(Entity.form(formData)); 
    	
        assertEquals(Status.CREATED,insertionResponse.getStatusInfo());
        
        GenericType<List<Process>> processesType = new GenericType<List<Process>>() {};
        List<Process> processes = target().path("processes").request(MediaType.APPLICATION_JSON).get(processesType);
        assertEquals(1,processes.size());
    }

    @Test
    public void testDelete() {
        
        MultivaluedMap<String,String> formData = new MultivaluedHashMap<String,String>();
        
        formData.add("kind","nodeRegistration");
        
        Response insertionResponse = target().path("processes")
                                             .request().post(Entity.form(formData)); 
        assertEquals(Status.CREATED,insertionResponse.getStatusInfo());

        String locationPath = insertionResponse.getLocation().getPath();
        String[] segments = locationPath.split("/");
        String pidString = segments[segments.length-1];
        
        Response deleteResponse1 = target().path("processes").path(pidString).request().delete();
        assertEquals(Status.OK,deleteResponse1.getStatusInfo());
        Response deleteResponse2 = target().path("processes").path(pidString).request().delete();
        assertEquals(Status.NOT_FOUND,deleteResponse2.getStatusInfo());
    }
    
    @After
    public void clear() {
        target().path("processes").request().delete();
    }
}
