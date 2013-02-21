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

public class LocationAndInfoIT {
	
	private static final String LOC_TARGET = "http://localhost:8080/easyhome/rest/" + RestPaths.LOCATIONS;
	private static final String INFO_TARGET = "http://localhost:8080/easyhome/rest/" + RestPaths.PERSISTENTINFO;
	
	private static Client client;
	
	@BeforeClass
    public static void setup() {
        client = Client.create();
    }
	
	@After
	public void removeAll() {
		client.resource(INFO_TARGET).delete();
		client.resource(LOC_TARGET).delete();
	}
    
	@Test
	public void testNoPersistentInfos() throws JSONException {
		
		ClientResponse getResponse = client.resource(INFO_TARGET).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		
		List<NodePersistentInfo> infoList = JsonUtils.getListFrom(getResponse,NodePersistentInfo.class);
		
		assertEquals(0,infoList.size());
	}
	
	@Test
	public void testInsertLocation() throws JSONException {
		
		String name = "Cucina";
		LocationType type = LocationType.KITCHEN;
		
		ClientResponse insertionResponse = postLocation(name,type);
		assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());
		
        String locationPath = insertionResponse.getLocation().getPath();
        String[] segments = locationPath.split("/");
        int id = Integer.parseInt(segments[segments.length-1]);
		
        ClientResponse getResponse = client.resource(LOC_TARGET).path(Long.toString(id)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK,getResponse.getClientResponseStatus());
        
        Location loc = JsonUtils.getFrom(getResponse, Location.class);
        
        assertTrue(loc.getName().equals(name));
        assertEquals(type,loc.getType());
	}

	@Test
	public void testInsertInfo() throws JSONException {
		
    	byte gatewayId = 2;
    	long nuid = 10L;
    	String name = "Oven";
    	Location location = new Location(1,"Kitchen",LocationType.KITCHEN);

    	postLocation(location.getName(),location.getType());    	
       
    	ClientResponse infoInsertionResponse = postPersistentInfo(gatewayId,nuid,name,location);
        assertEquals(ClientResponse.Status.CREATED,infoInsertionResponse.getClientResponseStatus());

        NodePersistentInfo recoveredInfo = client.resource(INFO_TARGET).path(Byte.toString(gatewayId)).path(Long.toString(nuid))
        															.accept(MediaType.APPLICATION_JSON).get(NodePersistentInfo.class);
        
        assertEquals(name,recoveredInfo.getName());
        assertTrue(location.equals(recoveredInfo.getLocation()));
    }

	@Test
	public void testDeletePersistentInfo() throws JSONException {
		
    	byte gatewayId = 2;
    	long nuid = 10L;
    	postPersistentInfo(gatewayId,nuid,null,null);

        ClientResponse deletionResponse = client.resource(INFO_TARGET).path(Byte.toString(gatewayId)).path(Long.toString(nuid)).delete(ClientResponse.class);
        
        assertEquals(ClientResponse.Status.OK,deletionResponse.getClientResponseStatus());
    }
	
	@Test
	public void testUpdateInfo() throws JSONException {
		
    	byte gatewayId = 2;
    	long nuid = 10L;
    	String name = "Oven";
    	Location kitchen = new Location(1,"Kitchen",LocationType.KITCHEN);
    	Location bathroom = new Location(2,"Bathroom",LocationType.BATHROOM);

    	postLocation(kitchen.getName(),kitchen.getType());    	
    	postLocation(bathroom.getName(),bathroom.getType());
       
    	ClientResponse insertionResponse = postPersistentInfo(gatewayId,nuid,name,kitchen);
        assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());
        
        name = "Fridge";
        
    	ClientResponse updateResponse = postPersistentInfo(gatewayId,nuid,name,bathroom);
        assertEquals(ClientResponse.Status.OK,updateResponse.getClientResponseStatus());   
        
        NodePersistentInfo recoveredInfo = client.resource(INFO_TARGET).path(Byte.toString(gatewayId)).path(Long.toString(nuid))
				.accept(MediaType.APPLICATION_JSON).get(NodePersistentInfo.class);
        
        assertEquals(name,recoveredInfo.getName());
        assertTrue(bathroom.equals(recoveredInfo.getLocation()));
	}

	private ClientResponse postLocation(String name, LocationType type) throws JSONException {
		
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("name",name);
        formData.add("type",type.toString());
        
        return client.resource(LOC_TARGET).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	}
	
	private ClientResponse postPersistentInfo(byte gatewayId, long nuid, String name, Location location) throws JSONException {
		
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("name",name);
        if (location != null) {
        	formData.add("locationName",location.getName());
        }
        
        return client.resource(INFO_TARGET).path(Byte.toString(gatewayId)).path(Long.toString(nuid))
        							.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	}
	
}
