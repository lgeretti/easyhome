package it.uniud.easyhome.rest;

import static org.junit.Assert.*;

import java.util.List;

import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.gateway.Gateway;
import it.uniud.easyhome.gateway.ProtocolType;

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

@Ignore
public class SIPROGatewayTest {
	
	private static final String TARGET = "http://localhost:5000/";
	private static final String TARGET_GET = "http://localhost:5000/?method=getData&params=actuators";
	
	private static Client client;
	
	@BeforeClass
    public static void setup() {
        client = Client.create();
    }

	@Test
	public void initialSetup() throws JSONException {
		
        MultivaluedMap<String,String> queryParams = new MultivaluedMapImpl();
        queryParams.add("method","setValueParam");
        queryParams.add("params","output1;changeColor;52;47;42;IR;IG;IB;IW;AA");
		
		ClientResponse response = client.resource(TARGET).queryParams(queryParams).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		
		assertEquals(ClientResponse.Status.OK,response.getClientResponseStatus());
		
        queryParams = new MultivaluedMapImpl();
        queryParams.add("method","getData");
        queryParams.add("params","actuators");
        
		response = client.resource(TARGET).queryParams(queryParams).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		
		String responseString = response.getEntity(String.class);
		
		assertEquals(ClientResponse.Status.OK,response.getClientResponseStatus());        
	}
	
}
