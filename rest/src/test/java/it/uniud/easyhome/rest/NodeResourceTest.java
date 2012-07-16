package it.uniud.easyhome.rest;

import static org.junit.Assert.*;

import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.rest.NodeResource;
import it.uniud.easyhome.testutils.JsonJaxbContextResolver;

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

public class NodeResourceTest extends JerseyTest {

    private static final String[] PACKAGE_NAMES = { 
        "it.uniud.easyhome.network",
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
    
    @After
    public void deleteAll() {
        NodeResource.clear();
    }
    
    @Test
    public void testNoNodes() {
        
        GenericType<List<Node>> nodesType = new GenericType<List<Node>>() {};
        List<Node> nodes = target().path("nodes").request(MediaType.APPLICATION_JSON).get(nodesType);
        assertEquals(0,nodes.size());
    }

    @Test
    public void testInsertion() throws JSONException {
        
        Node.Builder nodeBuilder = new Node.Builder(1);
        Node node = nodeBuilder.setName("first").build();
        Response insertionResponse = target().path("nodes").request().post(Entity.json(node));
        assertEquals(Status.CREATED,insertionResponse.getStatusInfo());
        
        GenericType<List<Node>> nodesType = new GenericType<List<Node>>() {};
        List<Node> nodes = target().path("nodes").request(MediaType.APPLICATION_JSON).get(nodesType);
        assertEquals(1,nodes.size());
        
        Node recoveredNode = target().path("nodes/1").request(MediaType.APPLICATION_JSON).get(Node.class);
        assertTrue(node.equals(recoveredNode));

        JSONObject jsonNode = target().path("nodes/1").request(MediaType.APPLICATION_JSON).get(JSONObject.class);
        assertEquals(1,jsonNode.getInt("id"));
        assertEquals("first",jsonNode.getString("name"));
    }

    @Test
    public void testUpdate() {
        
        createOneNode();
        
        Node.Builder nodeBuilder = new Node.Builder(1);
        Node modifiedNode = nodeBuilder.setName("modifiedFirst").build();
        Response updateResponse = target().path("nodes").request().post(Entity.json(modifiedNode));
        assertEquals(Status.OK,updateResponse.getStatusInfo());
        
        Node recoveredNode = target().path("nodes/1").request(MediaType.APPLICATION_JSON).get(Node.class);
        assertEquals("modifiedFirst",recoveredNode.getName());
    }

    @Test
    public void testDelete() {
        
        createOneNode();
        
        Response deleteResponse = target().path("nodes/1").request().delete();
        assertEquals(Status.OK,deleteResponse.getStatusInfo());
        
        Response getResponse = target().path("nodes/1").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Status.NOT_FOUND,getResponse.getStatusInfo());
    }
    
    private void createOneNode() {

        Node.Builder nodeBuilder = new Node.Builder(1);
        Node node = nodeBuilder.setName("first").build();
        target().path("nodes").request().post(Entity.json(node));
    }
}
