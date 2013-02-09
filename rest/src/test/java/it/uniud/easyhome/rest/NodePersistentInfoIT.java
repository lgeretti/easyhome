package it.uniud.easyhome.rest;

import static org.junit.Assert.*;

import java.util.List;

import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.network.*;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.codehaus.jettison.json.JSONException;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class NodePersistentInfoIT {
	
	private static final String TARGET = "http://localhost:8080/easyhome/rest/" + RestPaths.PERSISTENTINFO;
	
	private static Client client;
	
	@BeforeClass
    public static void setup() {
        client = Client.create();
    }
	
	@After
	public void removeAll() {
		client.resource(TARGET).delete();
	}
    
	@Test
	public void testNoPersistentInfos() throws JSONException {
		
		ClientResponse getResponse = client.resource(TARGET).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		
		List<NodePersistentInfo> infoList = JsonUtils.getListFrom(getResponse,NodePersistentInfo.class);
		
		assertEquals(0,infoList.size());
	}

	@Test
	public void testInsertNode() throws JSONException {
		
    	byte gatewayId = 2;
    	long nuid = 10L;
    	String name = "Oven";
    	Location location = new Location("Kitchen",LocationType.KITCHEN);
       
    	ClientResponse insertionResponse = postPersistentInfo(gatewayId,nuid,name,location);
        assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());

        NodePersistentInfo recoveredInfo = client.resource(TARGET).path(Byte.toString(gatewayId)).path(Long.toString(nuid))
        															.accept(MediaType.APPLICATION_JSON).get(NodePersistentInfo.class);
        
        assertEquals(name,recoveredInfo.getName());
        assertTrue(location.equals(recoveredInfo.getLocation()));
    }

	@Test
	public void testDeletePersistentInfo() throws JSONException {
		
    	byte gatewayId = 2;
    	long nuid = 10L;
    	postPersistentInfo(gatewayId,nuid,null,null);

        ClientResponse deletionResponse = client.resource(TARGET).path(Byte.toString(gatewayId)).path(Long.toString(nuid)).delete(ClientResponse.class);
        
        assertEquals(ClientResponse.Status.OK,deletionResponse.getClientResponseStatus());
    }
	
	@Test
	public void testUpdateNode() throws JSONException {
		
    	byte gatewayId = 2;
    	long nuid = 10L;
    	String name = "Oven";
    	Location location = new Location("Kitchen",LocationType.KITCHEN);
       
    	ClientResponse insertionResponse = postPersistentInfo(gatewayId,nuid,name,location);
        assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());
        
        name = "Fridge";
    	ClientResponse updateResponse = postPersistentInfo(gatewayId,nuid,name,location);
        assertEquals(ClientResponse.Status.OK,updateResponse.getClientResponseStatus());        
	}
	
	private ClientResponse postPersistentInfo(byte gatewayId, long nuid, String name, Location location) {
		
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("name",name);
        if (location != null) {
        	formData.add("locationName",location.getName());
        	formData.add("locationType",location.getType().toString());
        }
        
        return client.resource(TARGET).path(Byte.toString(gatewayId)).path(Long.toString(nuid))
        							.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	}
	
}
