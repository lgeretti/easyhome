package it.uniud.easyhome.common;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import com.google.gson.Gson;
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

		String responseString = response.getEntity(String.class);
		
		T result = new Gson().fromJson(responseString, cls);
		
		return result;
	}
}
