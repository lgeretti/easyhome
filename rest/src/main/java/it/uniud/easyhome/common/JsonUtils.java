package it.uniud.easyhome.common;

import it.uniud.easyhome.network.Node;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.ClientResponse;

public class JsonUtils {

	public static <T> List<T> getListFrom(ClientResponse response, Class<T> cls) throws JSONException {
		
		String responseString = response.getEntity(String.class);
		
		List<T> objList = new ArrayList<T>();
			
		Gson gson = new Gson();
				
		JSONArray jsonArray = new JSONArray(responseString);
				
		for (int i=0; i<jsonArray.length(); i++)
			objList.add(gson.fromJson(jsonArray.getString(i), cls));
		
		return objList;
	}
	
	public static <T> T getFrom(ClientResponse response, Class<T> cls) throws JSONException {

		T result = new Gson().fromJson(response.getEntity(String.class), cls);
		
		return result;
	}
}
