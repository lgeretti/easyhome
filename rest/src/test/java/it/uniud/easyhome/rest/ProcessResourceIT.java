package it.uniud.easyhome.rest;

import static org.junit.Assert.*;

import it.uniud.easyhome.processing.ProcessKind;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.*;

import org.codehaus.jettison.json.JSONException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class ProcessResourceIT {
    
	private static final String TARGET = "http://localhost:8080/easyhome/rest/processes";
	
	private static Client client;
	
	@BeforeClass
    public static void setup() {
        client = Client.create();
    }
	
    @Test
    public void noProcesses() {
        
		int numProcesses = Integer.valueOf(client.resource(TARGET).path("size").accept(MediaType.TEXT_PLAIN).get(String.class));
		assertEquals(0,numProcesses);
    }

    @Test
    public void testInsertion() throws JSONException {
        
        ClientResponse insertionResponse = insertProcess(ProcessKind.NODE_ANNCE_REGISTRATION);
    	
        assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());
        
		int numProcesses = Integer.valueOf(client.resource(TARGET).path("size").accept(MediaType.TEXT_PLAIN).get(String.class));
		assertEquals(1,numProcesses);
    }

    @Test
    public void testDelete() {
        
    	insertProcess(ProcessKind.NODE_ANNCE_REGISTRATION);
    	
    	ClientResponse firstDeletion =  client.resource(TARGET).delete(ClientResponse.class);
    	
    	assertEquals(ClientResponse.Status.OK,firstDeletion.getClientResponseStatus());
    	
    	noProcesses();
    	
    	ClientResponse secondInsertion = insertProcess(ProcessKind.NODE_ANNCE_REGISTRATION);
        String locationPath = secondInsertion.getLocation().getPath();
        String[] segments = locationPath.split("/");
        String pid = segments[segments.length-1];
    	
        ClientResponse secondDeletion = client.resource(TARGET).path(pid).delete(ClientResponse.class);
        
        assertEquals(ClientResponse.Status.OK,secondDeletion.getClientResponseStatus());
        
        ClientResponse thirdDeletion = client.resource(TARGET).path(pid).delete(ClientResponse.class);
        
        assertEquals(ClientResponse.Status.NOT_FOUND,thirdDeletion.getClientResponseStatus());
    }

    @After
    public void removeProcesses() {
        client.resource(TARGET).delete();
    }
   
    private ClientResponse insertProcess(ProcessKind kind) {
        
    	MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
    	formData.add("kind", kind.toString());
    	ClientResponse response = client.resource(TARGET)
    							  .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
    	
    	return response;
    }
}