package it.uniud.easyhome.gateway;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.LogLevel;
import it.uniud.easyhome.common.RunnableState;
import it.uniud.easyhome.exceptions.IncompletePacketException;
import it.uniud.easyhome.packets.natives.LampStateSetPacket;
import it.uniud.easyhome.packets.natives.NativePacket;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

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
                
                registerDevices();
                
                while (state != RunnableState.STOPPING) {
                	
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
	    	 
	    	File fXmlFile = new File("/home/geretti/Public/sources/uniud/easyhome/rest/src/test/resources/datamodel.xml");
	    	String xmlContent = fXmlFile.toString();
	    	InputSource is = new InputSource(new StringReader(xmlContent));
	    	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	    	Document doc = dBuilder.parse(is);

	    	//doc.getDocumentElement().normalize();
	    
	    	NodeList dataCategories = doc.getElementsByTagName("data");
	     
	    	handleSensors(dataCategories.item(0).getFirstChild());
	    	handleActuators(dataCategories.item(1).getFirstChild());
	    	
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }	
    	
    	
    }
    
    private void handleSensors(Node node) {
    	while (node != null) {
    		
    		System.out.println("Found one sensor");
    		
    		handleSensors(node.getNextSibling());
    	}
    }

    private void handleActuators(Node node) {
    	while (node != null) {
    		
    		System.out.println("Found one actuator");
    		
    		handleSensors(node.getNextSibling());
    	}
    }
    
}