package it.uniud.easyhome.rest;

import javax.ws.rs.core.MediaType;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.Client;

public class HubResourceIT {
	
	private static final String target = "http://localhost:8080/easyhome/rest/hub/gateways";
	
	private static Client client;
	
	@BeforeClass
    public static void setup() {

        client = Client.create();
    }
	
	@Test
	public void getNumGateways() throws JSONException {
		
        JSONObject jsonObj = client.resource(target).accept(MediaType.APPLICATION_JSON).get(JSONObject.class);
        JSONArray jsonArray = jsonObj.getJSONArray("gateway");
        System.out.println("How many gateways? " + jsonArray.length());
	}
}
