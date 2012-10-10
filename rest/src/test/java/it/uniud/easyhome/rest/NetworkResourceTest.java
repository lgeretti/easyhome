package it.uniud.easyhome.rest;

import static org.junit.Assert.*;

import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.rest.NetworkResource;
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

public class NetworkResourceTest extends JerseyTest {

    private static final String[] PACKAGE_NAMES = {
        "it.uniud.easyhome.packets",
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
        NetworkResource.clear();
    }
    
    @Test
    public void testNoNodes() {
        
        GenericType<List<Node>> nodesType = new GenericType<List<Node>>() {};
        List<Node> nodes = target().path("network").request(MediaType.APPLICATION_JSON).get(nodesType);
        assertEquals(0,nodes.size());
    }

    @Test
    public void testInsertion() throws JSONException {
        
        Node.Builder nodeBuilder = new Node.Builder(1L);
        Node node = nodeBuilder.setName("first")
        					   .setGatewayId((byte)3)
        					   .setAddress((short)5000)
        					   .build();
        Response insertionResponse = target().path("network").request().post(Entity.json(node));
        assertEquals(Status.CREATED,insertionResponse.getStatusInfo());
        
        GenericType<List<Node>> nodesType = new GenericType<List<Node>>() {};
        List<Node> nodes = target().path("network").request(MediaType.APPLICATION_JSON).get(nodesType);
        assertEquals(1,nodes.size());
        
        Node recoveredNode = target().path("network/1").request(MediaType.APPLICATION_JSON).get(Node.class);
        assertTrue(node.equals(recoveredNode));

        JSONObject jsonNode = target().path("network/1").request(MediaType.APPLICATION_JSON).get(JSONObject.class);
        assertEquals(1,jsonNode.getInt("id"));
        assertEquals("first",jsonNode.getString("name"));
        assertEquals(3,jsonNode.getInt("gatewayId"));
        assertEquals(5000,jsonNode.getInt("address"));
    }

    @Test
    public void testUpdate() {
        
        createOneNode();
        
        Node.Builder nodeBuilder = new Node.Builder(1L);
        Node modifiedNode = nodeBuilder.setName("modifiedFirst")
        							   .setAddress((short)5000).setGatewayId((byte)3).build();
        Response updateResponse = target().path("network").request().post(Entity.json(modifiedNode));
        assertEquals(Status.OK,updateResponse.getStatusInfo());
        
        Node recoveredNode = target().path("network/1").request(MediaType.APPLICATION_JSON).get(Node.class);
        assertEquals("modifiedFirst",recoveredNode.getName());
    }

    @Test
    public void testDelete() {
        
        createOneNode();
        
        Response deleteResponse = target().path("network/1").request().delete();
        assertEquals(Status.OK,deleteResponse.getStatusInfo());
        
        Response getResponse = target().path("network/1").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Status.NOT_FOUND,getResponse.getStatusInfo());
    }
    
    private void createOneNode() {

        Node.Builder nodeBuilder = new Node.Builder(1L);
        Node node = nodeBuilder.setName("first").setAddress((short)5000).setGatewayId((byte)3).build();
        
        target().path("network").request().post(Entity.json(node));
    }
}
