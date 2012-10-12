package it.uniud.easyhome.common;

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
		
		List<T> objList;
		
		if (responseString.equals("null"))
			objList = new ArrayList<T>();
		else {
			Gson gson = new Gson();
			
			JSONObject jsonResponse = new JSONObject(responseString);
			
			try {
				
				JSONArray jsonArray = jsonResponse.getJSONArray(cls.getSimpleName().toLowerCase());
				Type listType = new TypeToken<List<T>>() { }.getType();
				objList = gson.fromJson(jsonArray.toString(), listType);
			} catch (JSONException ex) {
				JSONObject jsonObj = jsonResponse.getJSONObject(cls.getSimpleName().toLowerCase());
				T node = gson.fromJson(jsonObj.toString(), cls);
				objList = new ArrayList<T>();
				objList.add(node);
			}
		}
		
		return objList;
	}
	
	public static <T> T getFrom(ClientResponse response, Class<T> cls) throws JSONException {

		T result = new Gson().fromJson(response.getEntity(String.class), cls);
		
		return result;
	}
}
