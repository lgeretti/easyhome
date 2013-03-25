package it.uniud.easyhome.rest;

import static org.junit.Assert.*;

import java.util.List;

import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.common.LogLevel;
import it.uniud.easyhome.devices.states.ColoredAlarm;
import it.uniud.easyhome.devices.states.FridgeCode;
import it.uniud.easyhome.gateway.Gateway;
import it.uniud.easyhome.gateway.ProtocolType;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.FileUtils;
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
import org.xml.sax.InputSource;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

public class SIPROGatewayTest {
	
	private static final String TARGET = "http://localhost:5000/";
	
	private static Client client;
	
	@BeforeClass
    public static void setup() {
        client = Client.create();
    }

	@Ignore
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
    public void registerDevices() {
    	
	    try {
	    	 
	    	/*
	        MultivaluedMap<String,String> queryParams = new MultivaluedMapImpl();
	        queryParams.add("method","getDataModel");
	        queryParams.add("params","");
    		ClientResponse dataModelResponse = client.resource(TARGET).queryParams(queryParams).accept(MediaType.TEXT_XML).get(ClientResponse.class);
	    	String xmlContent = dataModelResponse.getEntity(String.class);
	    	*/
	    	
	    	File xmlFile = new File("/home/geretti/Public/sources/uniud/easyhome/rest/src/test/resources/datamodel.xml");
	    	String xmlContent = "";
	    	
	        try{
	            xmlContent = FileUtils.readFileToString(xmlFile);
	        }catch(IOException e){
	            e.printStackTrace();
	        }
	        
	        
	    	InputSource is = new InputSource(new StringReader(xmlContent));
	    	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	    	Document doc = dBuilder.parse(is);
	    	
	    	NodeList dataCategories = doc.getElementsByTagName("data");
	     
	    	handleActuators(dataCategories.item(0));
	    	handleSensors(dataCategories.item(1));
	    	
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }	
    	
    	
    }
    
    private void handleActuators(Node node) {
    	NodeList children = node.getChildNodes();
    	for (int i=0;i<children.getLength();i++) {
    		Node child = children.item(i);
    		if (child.getNodeName() != "#text") {
    			handleLamp(child.getNodeName(),child.getChildNodes());
    		}
    	}
    }

    private void handleSensors(Node node) {
    	NodeList children = node.getChildNodes();
    	for (int i=0;i<children.getLength();i++) {
    		Node child = children.item(i);
    		if (child.getNodeName() != "#text") {
    			String identifier = child.getNodeName();
    			NodeList parameters = child.getChildNodes();
    			if (parameters.getLength() == 15)
    				handleFridge(identifier,parameters);
    			else
    				handlePIR(identifier,parameters);
    		}
    	}
    }    
    
    private byte fromHexStringToByte(String value) {
    	
    	return (byte)(Integer.parseInt(value,16) & 0xFF);
    }

    private void handleLamp(String identifier, NodeList parameters) {
    	byte gatewayId = 3;
    	boolean online = (parameters.item(1).getTextContent().equals("ON"));
		long nuid = Long.parseLong(parameters.item(3).getTextContent() + parameters.item(5).getTextContent() + parameters.item(7).getTextContent(),16);
		byte red = fromHexStringToByte(parameters.item(9).getTextContent());
		byte green = fromHexStringToByte(parameters.item(11).getTextContent());
		byte blue = fromHexStringToByte(parameters.item(13).getTextContent());
		byte white = fromHexStringToByte(parameters.item(15).getTextContent());
		ColoredAlarm alarm = ColoredAlarm.fromCode((byte)(Integer.parseInt(parameters.item(17).getTextContent(),16) & 0xFF));
    }
    
    private void handleFridge(String identifier, NodeList parameters) {
    	byte gatewayId = 3;
		long nuid = Long.parseLong(parameters.item(1).getTextContent() + parameters.item(3).getTextContent() + parameters.item(5).getTextContent(),16);
		String codeString = parameters.item(7).getTextContent() + parameters.item(9).getTextContent() + parameters.item(11).getTextContent();
		FridgeCode lastCode = FridgeCode.fromCode(Short.parseShort(codeString));	
    }
    
    private void handlePIR(String identifier, NodeList parameters) {
    	byte gatewayId = 3;
		long nuid = Long.parseLong(parameters.item(1).getTextContent() + parameters.item(3).getTextContent() + parameters.item(5).getTextContent(),16);
		String occupation = parameters.item(7).getTextContent() + parameters.item(9).getTextContent();
		boolean occupied = (occupation == "5031");
    }
}
