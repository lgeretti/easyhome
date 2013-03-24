package it.uniud.easyhome.gateway;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.LogLevel;
import it.uniud.easyhome.common.RunnableState;
import it.uniud.easyhome.devices.states.ColoredAlarm;
import it.uniud.easyhome.devices.states.FridgeCode;
import it.uniud.easyhome.exceptions.IncompletePacketException;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.packets.natives.LampStateSetPacket;
import it.uniud.easyhome.packets.natives.NativePacket;
import it.uniud.easyhome.rest.RestPaths;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class SIPROGateway extends Gateway {
	
	private static final String SIPRO_TARGET = "http://localhost:5000/";
	private static final String INTERNAL_TARGET = "http://localhost:8080/easyhome/rest/";
	
	private static Client client = Client.create();
	
	private Set<String> identifiersRegistered = new HashSet<String>();
			
    public SIPROGateway(byte id, int port, LogLevel logLevel) {
    	super(id,ProtocolType.NATIVE,port,logLevel);
    }
    
    @Override
    public final void open() {

        Thread thr = new Thread(this);
        thr.start();
    }
    
    @Override
    final protected NativePacket readFrom(InputStream is, ByteArrayOutputStream buffer) throws IOException {
    	
    	NativePacket result = null;
    	return result;
    }
    
    @Override
    public void run() {
    	
    	state = RunnableState.STARTED;

        while (state != RunnableState.STOPPING) {
        	
        	Connection jmsConnection = null;
        	
        	try {
                
    	   		Context jndiContext = new InitialContext();
    	        ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup(JMSConstants.CONNECTION_FACTORY);
    	        
                Topic outboundTopic = (Topic) jndiContext.lookup(JMSConstants.OUTBOUND_PACKETS_TOPIC);
                Topic inboundTopic = (Topic) jndiContext.lookup(JMSConstants.INBOUND_PACKETS_TOPIC);
                
    	        jmsConnection = connectionFactory.createConnection();
    	        Session jmsSession = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                
                MessageConsumer outboundConsumer = jmsSession.createConsumer(outboundTopic);
                MessageProducer inboundProducer = jmsSession.createProducer(inboundTopic);
                
                jmsConnection.start();
                
                while (state != RunnableState.STOPPING) {
                	
                	registerDevices();
                    handleOutboundPacketsTo(null,outboundConsumer,inboundProducer);
                }
                
            } catch (SocketException ex) {
            	// We do not want errors to show when close() is called during operations
            } catch (Exception ex) {
            	ex.printStackTrace();
            } finally {
	              try {
	            	  server.close();
	              } catch (IOException ex) {
	          		  // Whatever the case, the connection is not available anymore
	              } finally {
		        	  try {
		        		  if (jmsConnection != null)
		        			  jmsConnection.close();
		        	  } catch (JMSException ex) {
		        		// Whatever the case, the connection is not available anymore  
		        	  }
	              }
            }
        }
        state = RunnableState.STOPPED;
        log(LogLevel.INFO, "Gateway is closed");
    }
    
    @Override
    final protected void write(NativePacket pkt, OutputStream os, MessageProducer producer) throws IOException {
    	
		if (LampStateSetPacket.validates(pkt)) {
			
			LampStateSetPacket statePacket = (LampStateSetPacket) pkt;
			
			String paramsString = statePacket.getIdentifier()+";changeColor"+statePacket.getSeparatedParameters();
			
			log(LogLevel.DEBUG,"Request: ?method=setValueParam&params="+paramsString);
			
	        MultivaluedMap<String,String> queryParams = new MultivaluedMapImpl();
	        queryParams.add("method","setValueParam");
	        queryParams.add("params",paramsString);
			
			client.resource(SIPRO_TARGET).queryParams(queryParams).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		}
    }
    
    private void registerDevices() {
    	
	    try {
	    	 
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
	     
	    	handleSensors(dataCategories.item(0));
	    	handleActuators(dataCategories.item(1));
	    	
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }	
    }
    
    private void handleSensors(Node node) {
    	NodeList children = node.getChildNodes();
    	for (int i=0;i<children.getLength();i++) {
    		Node child = children.item(i);
    		if (child.getNodeName() != "#text") {
    			String identifier = child.getNodeName();
    			if (!identifiersRegistered.contains(identifier)) 
    				registerLamp(identifier,child.getChildNodes());
    		}
    	}
    }

    private void handleActuators(Node node) {
    	NodeList children = node.getChildNodes();
    	for (int i=0;i<children.getLength();i++) {
    		Node child = children.item(i);
    		if (child.getNodeName() != "#text") {
    			String identifier = child.getNodeName();
    			if (!identifiersRegistered.contains(identifier)) { 
	    			NodeList parameters = child.getChildNodes();
	    			if (parameters.getLength() == 15)
	    				registerFridge(identifier,parameters);
	    			else
	    				registerPIR(identifier,parameters);
    			}
    		}
    	}
    }    
    
    private byte fromHexStringToByte(String value) {
    	return (byte)(Integer.parseInt(value,16) & 0xFF);
    }

    private void registerLamp(String identifier, NodeList parameters) {
    	byte gatewayId = 3;
    	boolean online = (parameters.item(1).getTextContent() == "ON");
		long nuid = Long.parseLong(parameters.item(3).getTextContent() + parameters.item(5).getTextContent() + parameters.item(7).getTextContent(),16);
		byte red = fromHexStringToByte(parameters.item(9).getTextContent());
		byte green = fromHexStringToByte(parameters.item(11).getTextContent());
		byte blue = fromHexStringToByte(parameters.item(13).getTextContent());
		byte white = fromHexStringToByte(parameters.item(15).getTextContent());
		ColoredAlarm alarm = ColoredAlarm.fromCode((byte)(Integer.parseInt(parameters.item(17).getTextContent(),16) & 0xFF));
		log(LogLevel.DEBUG,"To transmit: online=" + online + ", nuid=" + nuid + 
						   ", red=" + red + ", green=" + green + ", blue=" + blue + ", white=" + white + ", alarm=" + alarm);
		
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("gatewayId",Byte.toString(gatewayId));
        formData.add("nuid",Long.toString(nuid));  
        formData.add("online",Boolean.toString(online));
        formData.add("identifier",identifier);
        formData.add("red",Byte.toString(red));
        formData.add("green",Byte.toString(green));
        formData.add("blue",Byte.toString(blue));
        formData.add("white",Byte.toString(white));
        formData.add("alarm",alarm.toString());
        client.resource(INTERNAL_TARGET).path(RestPaths.STATES).path("lamps").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
        
        identifiersRegistered.add(identifier);
    }
    
    private void registerFridge(String identifier, NodeList parameters) {
    	byte gatewayId = 3;
		long nuid = Long.parseLong(parameters.item(1).getTextContent() + parameters.item(3).getTextContent() + parameters.item(5).getTextContent(),16);
		String codeString = parameters.item(7).getTextContent() + parameters.item(9).getTextContent() + parameters.item(11).getTextContent();
		FridgeCode lastCode = FridgeCode.fromCode(Short.parseShort(codeString));
		log(LogLevel.DEBUG,"To transmit: nuid=" + nuid + ", code=" + lastCode);
		
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("gatewayId",Byte.toString(gatewayId));
        formData.add("nuid",Long.toString(nuid));  
        formData.add("identifier",identifier);
        formData.add("lastCode",lastCode.toString());
        client.resource(INTERNAL_TARGET).path(RestPaths.STATES).path("lamps").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
        
        identifiersRegistered.add(identifier);
    }
    
    private void registerPIR(String identifier, NodeList parameters) {
    	byte gatewayId = 3;
		long nuid = Long.parseLong(parameters.item(1).getTextContent() + parameters.item(3).getTextContent() + parameters.item(5).getTextContent(),16);
		String occupation = parameters.item(7).getTextContent() + parameters.item(9).getTextContent();
		boolean occupied = (occupation == "5031");
		log(LogLevel.DEBUG,"To transmit: nuid=" + nuid + ", occupied=" + occupied);
		
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("gatewayId",Byte.toString(gatewayId));
        formData.add("nuid",Long.toString(nuid));  
        formData.add("identifier",identifier);
        formData.add("occupied",Boolean.toString(occupied));
        client.resource(INTERNAL_TARGET).path(RestPaths.STATES).path("lamps").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
        
        identifiersRegistered.add(identifier);
    }
    
}