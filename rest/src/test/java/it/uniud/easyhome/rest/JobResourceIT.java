package it.uniud.easyhome.rest;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.devices.HomeAutomationDevice;
import it.uniud.easyhome.network.Link;
import it.uniud.easyhome.network.LocalCoordinates;
import it.uniud.easyhome.network.Location;
import it.uniud.easyhome.network.LocationType;
import it.uniud.easyhome.network.Manufacturer;
import it.uniud.easyhome.network.NetworkJob;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.GlobalCoordinates;
import it.uniud.easyhome.network.NodeLogicalType;

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

public class JobResourceIT {
	
	private static final String TARGET = "http://localhost:8080/easyhome/rest/" + RestPaths.JOBS;
	
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
	public void testInsertJob() throws JSONException {
		
        ClientResponse insertionResponse = postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,(short)11,(byte)9,(byte)1);
        
        assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());

        String locationPath = insertionResponse.getLocation().getPath();
        String[] segments = locationPath.split("/");
        String jobIdString = segments[segments.length-1];
        
        NetworkJob recoveredJob = client.resource(TARGET).path(jobIdString).accept(MediaType.APPLICATION_JSON).get(NetworkJob.class);
        
        assertEquals(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,recoveredJob.getType());        
    }
	
	@Test
	public void testDeleteJobById() throws JSONException {
		
        ClientResponse insertionResponse = postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,(short)11,(byte)9,(byte)3);

        String locationPath = insertionResponse.getLocation().getPath();
        String[] segments = locationPath.split("/");
        String jobIdString = segments[segments.length-1];
		
        ClientResponse deletionResponse = client.resource(TARGET).path(jobIdString).delete(ClientResponse.class);
        
        assertEquals(ClientResponse.Status.OK,deletionResponse.getClientResponseStatus());
	}
	
	
	@Test
	public void testDeleteJobByCoords() throws JSONException {
		
        postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,(short)11,(byte)9,(byte)1);
		
		MultivaluedMap<String,String> queryData = new MultivaluedMapImpl();
		queryData.add("type",NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST.toString());
		queryData.add("gatewayId",String.valueOf((byte)3));
		queryData.add("address",String.valueOf((short)11));
        
        ClientResponse deletionResponse = client.resource(TARGET).queryParams(queryData).delete(ClientResponse.class);
        
        assertEquals(ClientResponse.Status.OK,deletionResponse.getClientResponseStatus());
	}		
	
	@Test
	public void testGetLatestJobs() throws JSONException {
		
		postBunchOfJobs();
		
		MultivaluedMap<String,String> queryData = new MultivaluedMapImpl();
		queryData.add("type",NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST.toString());
        
        ClientResponse getResponse = client.resource(TARGET).queryParams(queryData).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        List<NetworkJob> jobList = JsonUtils.getListFrom(getResponse,NetworkJob.class);
        
        assertEquals(7,jobList.size());
	}
	
	@Test
	public void testTypeAndTransactionSpecificJob() throws JSONException {
		
		postBunchOfJobs();
		
		MultivaluedMap<String,String> queryData = new MultivaluedMapImpl();
		queryData.add("type",NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST.toString());
		queryData.add("tsn",Byte.toString((byte)6));
        
        ClientResponse getResponse = client.resource(TARGET).queryParams(queryData).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        List<NetworkJob> jobList = JsonUtils.getListFrom(getResponse,NetworkJob.class);
        
        assertEquals(1,jobList.size());
	}
	
	@Test
	public void testTypeNodeSpecificJob() throws JSONException {
		
		postBunchOfJobs();
		
		MultivaluedMap<String,String> queryData = new MultivaluedMapImpl();
		queryData.add("type",NetworkJobType.NODE_DESCR_REQUEST.toString());
		queryData.add("gatewayId",String.valueOf((byte)3));
		queryData.add("address",String.valueOf((short)12));
		queryData.add("endpoint",String.valueOf((byte)7));
        
        ClientResponse getResponse = client.resource(TARGET).queryParams(queryData).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        List<NetworkJob> jobList = JsonUtils.getListFrom(getResponse,NetworkJob.class);
        
        assertEquals(1,jobList.size());
        assertEquals((byte)6,jobList.get(0).getTsn());
	}
	
	@Test
	public void testNodeSpecificJob() throws JSONException {
		
		postBunchOfJobs();
		
		MultivaluedMap<String,String> queryData = new MultivaluedMapImpl();
		queryData.add("gatewayId",String.valueOf((byte)3));
		queryData.add("address",String.valueOf((short)12));
		queryData.add("endpoint",String.valueOf((byte)7));
        
        ClientResponse getResponse = client.resource(TARGET).queryParams(queryData).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        List<NetworkJob> jobList = JsonUtils.getListFrom(getResponse,NetworkJob.class);
        
        assertEquals(1,jobList.size());
        assertEquals((byte)7,jobList.get(0).getTsn());
	}
	
	private void postBunchOfJobs() {
		
		postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,(short)9,(byte)9,(byte)1);
		postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,(short)9,(byte)9,(byte)2);
		postJob(NetworkJobType.NODE_DESCR_REQUEST,           (byte)3,(short)9,(byte)9,(byte)2);
		postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,(short)10,(byte)9,(byte)3);
		postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,(short)11,(byte)9,(byte)4);
		postJob(NetworkJobType.NODE_DESCR_REQUEST,           (byte)3,(short)11,(byte)9,(byte)4);
		postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,(short)11,(byte)10,(byte)5);
		postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,(short)12,(byte)7,(byte)6);
		postJob(NetworkJobType.NODE_DESCR_REQUEST,           (byte)3,(short)12,(byte)7,(byte)5);
		postJob(NetworkJobType.NODE_DESCR_REQUEST,           (byte)3,(short)12,(byte)7,(byte)6);
		postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,(short)12,(byte)7,(byte)7);
		postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,(short)12,(byte)9,(byte)8);
		postJob(NetworkJobType.NODE_DESCR_REQUEST,           (byte)3,(short)12,(byte)9,(byte)8);
		postJob(NetworkJobType.NODE_ACTIVE_ENDPOINTS_REQUEST,(byte)3,(short)12,(byte)15,(byte)9);
	}
		
	private ClientResponse postJob(NetworkJobType type, byte gatewayId, short address, byte endpoint, byte tsn) {
		
		MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("type",type.toString());
        formData.add("gatewayId",Byte.toString(gatewayId));
        formData.add("address",Short.toString(address));
        formData.add("endpoint",Byte.toString(endpoint));
        formData.add("tsn",Byte.toString(tsn));
        
        return client.resource(TARGET).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	}
	
}
