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
		
		List<T> nodeList;
		
		if (responseString.equals("null"))
			nodeList = new ArrayList<T>();
		else {
			Gson gson = new Gson();
			
			JSONObject jsonObj = new JSONObject(responseString);
			
			try {
				
				JSONArray jsonArray = jsonObj.getJSONArray(cls.getSimpleName().toLowerCase());
				Type listType = new TypeToken<List<T>>() { }.getType();
				nodeList = gson.fromJson(jsonArray.toString(), listType);
			} catch (JSONException ex) {
				JSONObject jsonNode = jsonObj.getJSONObject(cls.getSimpleName().toLowerCase());
				T node = gson.fromJson(jsonNode.toString(), cls);
				nodeList = new ArrayList<T>();
				nodeList.add(node);
			}
		}
		
		return nodeList;
	}
}
