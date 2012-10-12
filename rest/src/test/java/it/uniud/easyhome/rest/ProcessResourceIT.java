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
        
        ClientResponse insertionResponse = insertProcess(ProcessKind.NodeRegistration);
    	
        assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());
        
		int numProcesses = Integer.valueOf(client.resource(TARGET).path("size").accept(MediaType.TEXT_PLAIN).get(String.class));
		assertEquals(1,numProcesses);
    }

    @Test
    public void testDelete() {
        
    	insertProcess(ProcessKind.NodeRegistration);
    	
    	ClientResponse response =  client.resource(TARGET).delete(ClientResponse.class);
    	
    	assertEquals(ClientResponse.Status.OK,response.getClientResponseStatus());
    	
    	noProcesses();
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