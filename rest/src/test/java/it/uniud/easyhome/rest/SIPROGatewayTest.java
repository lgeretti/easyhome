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
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

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
	    	
	    	doc.getDocumentElement().normalize();
	    	
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
    		if (child.getNodeType() == Node.ELEMENT_NODE) {
    			handleLamp(child.getNodeName(),(Element)child);
    		}
    	}
    }

    private void handleSensors(Node node) {
    	NodeList children = node.getChildNodes();
    	for (int i=0;i<children.getLength();i++) {
    		Node child = children.item(i);
    		if (child.getNodeType() == Node.ELEMENT_NODE) {
    			String identifier = child.getNodeName();
    			String descrName = getDescriptorName((Element)child);
    			if (descrName.equals("Fridge Alarm Light"))
    				handleFridge(identifier,(Element)child);
    			else if (descrName.equals("PIR"))
    				handlePIR(identifier,(Element)child);
    		}
    	}
    }    
    
    private byte fromHexStringToByte(String value) {
    	
    	return (byte)(Integer.parseInt(value,16) & 0xFF);
    }

    private void handleLamp(String identifier, Element input) {
    	byte gatewayId = 3;
    	boolean online = (getTxtFor(input,"state").equals("ON"));
		long nuid = Long.parseLong(getTxtFor(input,"value-3") + getTxtFor(input,"value-2") + getTxtFor(input,"value-1"),16);
		byte red = fromHexStringToByte(getTxtFor(input,"value0"));
		byte green = fromHexStringToByte(getTxtFor(input,"value1"));
		byte blue = fromHexStringToByte(getTxtFor(input,"value2"));
		byte white = fromHexStringToByte(getTxtFor(input,"value3"));
		ColoredAlarm alarm = ColoredAlarm.fromCode((byte)(Integer.parseInt(getTxtFor(input,"value4"),16) & 0xFF));
		
		System.out.println("Lamp: identifier=" + identifier + ", online=" + online + ", nuid=0x" + Long.toHexString(nuid) + 
				   ", red=" + red + ", green=" + green + ", blue=" + blue + ", white=" + white + ", alarm=" + alarm);
    }
    
    private void handleFridge(String identifier, Element input) {
    	byte gatewayId = 3;
		long nuid = Long.parseLong(getTxtFor(input,"value-3") + getTxtFor(input,"value-2") + getTxtFor(input,"value-1"),16);
		String codeString = getTxtFor(input,"value0") + getTxtFor(input,"value1") + getTxtFor(input,"value2");
		FridgeCode lastCode = FridgeCode.fromCode(Short.parseShort(codeString));	
		
		System.out.println("Fridge: identifier=" + identifier + ", nuid=0x" + Long.toHexString(nuid) + ", code=" + lastCode);
    }
    
    private void handlePIR(String identifier, Element input) {
    	byte gatewayId = 3;    	
    	
		long nuid = Long.parseLong(getTxtFor(input,"value-3") + getTxtFor(input,"value-2") + getTxtFor(input,"value-1"),16);
		String occupation = getTxtFor(input,"value0") + getTxtFor(input,"value1");
		boolean occupied = (occupation.equals("5031"));
		
		System.out.println("PIR: identifier=" + identifier + ", nuid=0x" + Long.toHexString(nuid) + ", occupied=" + occupied);
    }
    
    private String getTxtFor(Element input, String tagName) {
    	
        NodeList tagList = input.getElementsByTagName(tagName);
        Element tagElement = (Element)tagList.item(0);

        NodeList textFNList = tagElement.getChildNodes();
        return ((Node)textFNList.item(0)).getNodeValue().trim();
    }
    
    private String getDescriptorName(Element input) {
    	
        NodeList inputElementList = input.getElementsByTagName("descriptor");
        Element descrElement = (Element)inputElementList.item(0);
        NodeList descriptorElements = descrElement.getElementsByTagName("name");
        Element descrNameElement = (Element)descriptorElements.item(0);
        NodeList textFNList = descrNameElement.getChildNodes();
        return ((Node)textFNList.item(0)).getNodeValue().trim();
    }
}
