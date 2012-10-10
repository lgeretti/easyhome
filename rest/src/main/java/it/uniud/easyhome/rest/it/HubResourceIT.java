package it.uniud.easyhome.rest.it;

import javax.ws.rs.core.MediaType;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class HubResourceIT {
	
	private static final String target = "http://localhost:8080/easyhome/rest/hub/gateways/1";
	
    public static void main(String[] args) throws JSONException {
    	
    	ClientConfig config = new DefaultClientConfig();
        Client client = Client.create(config);
        
        JSONObject jsonObj = client.resource(target).accept(MediaType.APPLICATION_JSON).get(JSONObject.class);
        JSONArray jsonArray = jsonObj.getJSONArray("gateway");
        System.out.println("How many gateways? " + jsonArray.length());
    }
}
