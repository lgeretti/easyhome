package it.uniud.easyhome.rest;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.devices.HomeAutomationDevice;
import it.uniud.easyhome.devices.Location;
import it.uniud.easyhome.devices.LocationType;
import it.uniud.easyhome.devices.Manufacturer;
import it.uniud.easyhome.network.Link;
import it.uniud.easyhome.network.LocalCoordinates;
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

public class LinkResourceIT {
	
	private static final String TARGET = "http://localhost:8080/easyhome/rest/" + RestPaths.LINKS;
	
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
	public void testInsertAndUpdateLink() throws JSONException {
		
		byte gatewayId = 2;
		LocalCoordinates source = new LocalCoordinates(11L,(short)1);
		LocalCoordinates destination = new LocalCoordinates(12L,(short)2);
		
		ClientResponse insertionResponse = insertLink(gatewayId,source,destination);
		assertEquals(ClientResponse.Status.CREATED,insertionResponse.getClientResponseStatus());
		
        String locationPath = insertionResponse.getLocation().getPath();
        String[] segments = locationPath.split("/");
        long id = Long.parseLong(segments[segments.length-1]);
		
        ClientResponse getResponse = client.resource(TARGET).path(Long.toString(id)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK,getResponse.getClientResponseStatus());
        
        Link link = JsonUtils.getFrom(getResponse, Link.class);
        
        assertEquals(gatewayId,link.getGatewayId());
        assertTrue(source.equals(link.getSource()));
        assertTrue(destination.equals(link.getDestination()));
        
		ClientResponse updateResponse = insertLink(gatewayId,source,destination);
		assertEquals(ClientResponse.Status.OK,updateResponse.getClientResponseStatus());
		
		Link updatedLink = client.resource(TARGET).path(Long.toString(id)).accept(MediaType.APPLICATION_JSON).get(Link.class);
	    assertTrue(updatedLink.getDate().after(link.getDate()));
	}
	
	
	private ClientResponse insertLink(byte gatewayId, LocalCoordinates source, LocalCoordinates destination) throws JSONException {
		
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("gatewayId",Byte.toString(gatewayId));
        formData.add("sourceNuid",Long.toString(source.getNuid()));
        formData.add("sourceAddress",Short.toString(source.getAddress()));
        formData.add("destinationNuid",Long.toString(destination.getNuid()));
        formData.add("destinationAddress",Short.toString(destination.getAddress()));
        
        return client.resource(TARGET).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);		
	}
	
}
