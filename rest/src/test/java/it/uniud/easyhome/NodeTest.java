package it.uniud.easyhome;

import static org.junit.Assert.*;

import java.util.List;

import javax.ws.rs.client.Configuration;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.*;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.media.json.JsonJaxbFeature;
import org.glassfish.jersey.media.json.JsonJaxbModule;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

public class NodeTest extends JerseyTest {

    private static final String PACKAGE_NAME = "it.uniud.easyhome";
    
    private static ResourceConfig createApp() {
        final ResourceConfig rc = new ResourceConfig()
                .addModules(new JsonJaxbModule())
                .packages(PACKAGE_NAME);

        return rc;
    }
    
    @Override
    protected ResourceConfig configure() {
        enable(org.glassfish.jersey.test.TestProperties.LOG_TRAFFIC);
        return createApp();
    }
    
    @Override
    protected void configureClient(Configuration config) {
        config.register(new JsonJaxbFeature()).register(JsonJaxbContextResolver.class);
    }
    
    @After
    public void deleteAll() {
        NodeResource.clear();
    }
    
    @Test
    public void testNoNodes() {
        
        GenericType<List<Node>> nodesType = new GenericType<List<Node>>() {};
        List<Node> nodes = target().path("nodes").request(MediaType.APPLICATION_JSON).get(nodesType);
        assertEquals(nodes.size(),0);
    }
    
    @Test
    public void testInsertion() throws JSONException {
        
        Node node = new Node(1,"first");
        Response insertionResponse = target().path("nodes").request().post(Entity.json(node));
        assertEquals(insertionResponse.getStatusInfo(),Status.CREATED);
        
        GenericType<List<Node>> nodesType = new GenericType<List<Node>>() {};
        List<Node> nodes = target().path("nodes").request(MediaType.APPLICATION_JSON).get(nodesType);
        assertEquals(nodes.size(),1);
        
        Node recoveredNode = target().path("nodes/1").request(MediaType.APPLICATION_JSON).get(Node.class);
        assertTrue(node.equals(recoveredNode));

        JSONObject jsonNode = target().path("nodes/1").request(MediaType.APPLICATION_JSON).get(JSONObject.class);
        assertEquals(jsonNode.getInt("id"),1);
        assertEquals(jsonNode.getString("name"),"first");
    }
    
    @Test
    public void testUpdate() {
        
        createOneNode();
        
        Node modifiedNode = new Node(1,"modifiedFirst");
        Response updateResponse = target().path("nodes").request().post(Entity.json(modifiedNode));
        assertEquals(updateResponse.getStatusInfo(),Status.OK);
        
        Node recoveredNode = target().path("nodes/1").request(MediaType.APPLICATION_JSON).get(Node.class);
        assertEquals(recoveredNode.getName(),"modifiedFirst");
    }
    
    @Test
    public void testDelete() {
        
        createOneNode();
        
        Response deleteResponse = target().path("nodes/1").request().delete();
        assertEquals(deleteResponse.getStatusInfo(),Status.OK);
        
        Response getResponse = target().path("nodes/1").request(MediaType.APPLICATION_JSON).get();
        assertEquals(getResponse.getStatusInfo(),Status.NOT_FOUND);
    }
    
    private void createOneNode() {

        Node node = new Node(1,"first");
        target().path("nodes").request().post(Entity.json(node));
    }
}
