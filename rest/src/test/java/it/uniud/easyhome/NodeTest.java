package it.uniud.easyhome;

import static org.junit.Assert.*;

import javax.ws.rs.core.MediaType;

import org.codehaus.jettison.json.*;
import org.junit.*;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;

public class NodeTest extends JerseyTest {

    public static final String PACKAGE_NAME = "it.uniud.easyhome";
 
    public NodeTest() {
          super(new WebAppDescriptor.Builder(PACKAGE_NAME).contextPath("easyhome").build());
    }
    
    @Test 
    public void getPlainText() {
        WebResource webResource = resource();        
        String str = webResource.path("rest/nodes/testme").accept(MediaType.TEXT_PLAIN).get(String.class);
        assertEquals(str,"test");
    }
    
    @Test 
    public void getJSON() throws JSONException {
        WebResource webResource = resource();
        JSONObject node = webResource.path("rest/nodes/1").accept(MediaType.APPLICATION_JSON).get(JSONObject.class);        
        assertEquals(node.getString("name"),"test");
    }
    
}
