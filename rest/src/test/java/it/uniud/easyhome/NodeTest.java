package it.uniud.easyhome;

import static org.junit.Assert.*;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Configuration;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
    
    public org.glassfish.jersey.client.JerseyClient cl;
    
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
    
    @Before
    public void cleanTable() {
        
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("EasyHome");
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        
        tx.begin();
        
        Query deletion = em.createQuery("DELETE FROM Node");
        deletion.executeUpdate();
        
        tx.commit();    
       
        em.close();
        emf.close();
    }
    
    @Test
    public void noNodes() {
        GenericType<List<Node>> nodesType = new GenericType<List<Node>>() {};
        List<Node> nodes = target().path("nodes").request(MediaType.APPLICATION_JSON).get(nodesType);
        assertEquals(nodes.size(),0);
    }
    
    @Test
    public void insertOneNode() throws JSONException {
        
        Node node = new Node(1,"first");
        target().path("nodes").request().put(Entity.json(node));
        
        GenericType<List<Node>> nodesType = new GenericType<List<Node>>() {};
        List<Node> nodes = target().path("nodes").request(MediaType.APPLICATION_JSON).get(nodesType);
        assertEquals(nodes.size(),1);
        
        Node recoveredNode = target().path("nodes/1").request(MediaType.APPLICATION_JSON).get(Node.class);
        assertNotNull(recoveredNode);
        assertTrue(node.equals(recoveredNode));

        JSONObject jsonNode = target().path("nodes/1").request(MediaType.APPLICATION_JSON).get(JSONObject.class);
        assertNotNull(jsonNode);
        assertEquals(jsonNode.getInt("id"),1);
        assertEquals(jsonNode.getString("name"),"first");
    }
    
    
    
}
