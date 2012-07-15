package it.uniud.easyhome;

import static org.junit.Assert.*;

import java.util.List;

import javax.ws.rs.client.Configuration;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.junit.*;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.media.json.JsonJaxbFeature;
import org.glassfish.jersey.media.json.JsonJaxbModule;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

public class NodeTest extends JerseyTest {

    public static final String PACKAGE_NAME = "it.uniud.easyhome";
    
    public static ResourceConfig createApp() {
        final ResourceConfig rc = new ResourceConfig()
                .addModules(new JsonJaxbModule())
                .packages(PACKAGE_NAME);

        return rc;
    }
    
    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);
        return createApp();
    }
    
    @Override
    protected void configureClient(Configuration config) {
        config.register(new JsonJaxbFeature()).register(JsonJaxbContextResolver.class);
    }
    
    @Ignore
    @Test 
    public void getNodeJSON() throws JSONException {   
        JSONObject node = target().path("nodes/1").request(MediaType.APPLICATION_JSON).get(JSONObject.class);        
        assertEquals(node.getString("name"),"test");
    }

    @Ignore
    @Test 
    public void getNodePOJO() {
        Node node = target().path("nodes/1").request(MediaType.APPLICATION_JSON).get(Node.class);        
        assertEquals(node.getName(),"test");
    }
    
    @Ignore
    @Test
    public void getNodes() {
        GenericType<List<Node>> nodesType = new GenericType<List<Node>>() {};
        List<Node> nodes = target().path("nodes").request(MediaType.APPLICATION_JSON).get(nodesType);
        assertEquals(nodes.size(),0);
    }
    
}
