package it.uniud.easyhome.rest.it;

import it.uniud.easyhome.rest.it.JsonJaxbContextResolver;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientFactory;
import javax.ws.rs.core.MediaType;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.media.json.JsonJaxbFeature;

public class HubResourceIT {
	
	private static final String target = "http://localhost:8080/easyhome/rest/hub/gateways";
	
    public static void main(String[] args) throws JSONException {
        
    	Client client = ClientFactory.newClient();
    	client.configuration().register(new JsonJaxbFeature()).register(JsonJaxbContextResolver.class);
    	
        JSONObject jsonObj = client.target(target).request(MediaType.APPLICATION_JSON).get(JSONObject.class);
        
        JSONArray jsonArray = jsonObj.getJSONArray("gatewayInfo");
        System.out.println("How many gateways? " + jsonArray.length());
    }
}
