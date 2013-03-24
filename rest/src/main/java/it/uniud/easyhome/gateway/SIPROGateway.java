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

import org.codehaus.jettison.json.JSONException;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class SIPROGateway extends Gateway {
	
	private static final String TARGET = "http://localhost:5000/";
	
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
			
			log(LogLevel.DEBUG,"params="+paramsString);
			
	        MultivaluedMap<String,String> queryParams = new MultivaluedMapImpl();
	        queryParams.add("method","setValueParam");
	        queryParams.add("params",paramsString);
			
			ClientResponse response = client.resource(TARGET).queryParams(queryParams).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	
			
		}
    }
  
}