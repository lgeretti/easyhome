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

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;

public class SIPROGatewayTest {
	
	private static final String TARGET = "http://localhost:5000/";
	
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
	
	@Test
	public void readXml() {
		
	    try {
	    	 
	    	File fXmlFile = new File("/home/geretti/Public/sources/uniud/easyhome/rest/src/test/resources/actuators.xml");
	    	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	    	Document doc = dBuilder.parse(fXmlFile);

	    	//doc.getDocumentElement().normalize();
	     
	    	System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
	     
	    	NodeList nList = doc.getElementsByTagName("output1");
	     
	    	System.out.println("----------------------------");
	     
	    	for (int temp = 0; temp < nList.getLength(); temp++) {
	     
	    		Node nNode = nList.item(temp);
	     
	    		System.out.println("\nCurrent Element :" + nNode.getNodeName());
	     
	    		if (nNode.getNodeType() == Node.ELEMENT_NODE) {
	     
	    			Element eElement = (Element) nNode;
	     
	    			System.out.println("State : " + eElement.getElementsByTagName("state").item(0).getTextContent());
	     
	    		}
	    	}
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }		
		
	}
	
}
