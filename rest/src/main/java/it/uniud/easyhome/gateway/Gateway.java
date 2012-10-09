package it.uniud.easyhome.gateway;

import it.uniud.easyhome.network.ModuleCoordinates;
import it.uniud.easyhome.network.NativePacket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Gateway implements Runnable {
    
	@XmlElement(name="id")
    protected byte id;
	
	@XmlElement(name="protocolType")
    private ProtocolType protocolType;
	
	@XmlElement(name="port")
    protected int port;
	
	// Default behavior requires only one connection; the native gateway is an exception, 
	// reachable by any node in the native subnetwork
	protected int MAX_CONNECTIONS = 1;
    
    private final Map<ModuleCoordinates,Integer> routingTable = new HashMap<ModuleCoordinates,Integer>();
    
    private int mappedEndpointCounter = 0;
    
    protected ServerSocket server = null;
    
	protected volatile boolean disconnected = false;
	
    @SuppressWarnings("unused")
    private Gateway() { }
    
    protected Gateway(byte id, ProtocolType protocolType, int port) {
    	this.id = id;
    	this.protocolType = protocolType;
    	this.port = port;
    }
    
    public byte getId() {
        return id;
    }
    
    public ProtocolType getProtocolType() {
        return protocolType;
    }
    
    public int getPort() {
        return port;
    }
    
    public Map<ModuleCoordinates,Integer> getRoutingTable() {
        return routingTable;
    }
    
    public int addRoutingEntry(ModuleCoordinates coords) {
        
        println("Putting routing entry (endpoint " + (mappedEndpointCounter+1) + ") for " + coords);
    	
        routingTable.put(coords, ++mappedEndpointCounter);
        
        return mappedEndpointCounter;
    }
    
    public void removeRoutingEntry(ModuleCoordinates coords) {
        routingTable.remove(coords);
    }
    
    public void removeRoutingEntriesForGateway(int gid) {
        
        Iterator<Map.Entry<ModuleCoordinates,Integer>> it = routingTable.entrySet().iterator();
        while (it.hasNext())
            if (it.next().getKey().getGatewayId() == gid)
                it.remove();
    }
    
    public Integer getEndpointFor(ModuleCoordinates coords) {
        return routingTable.get(coords);
    }
    
    protected ModuleCoordinates getCoordinatesFor(int endpoint) {
        ModuleCoordinates coords = null;

        for (Entry<ModuleCoordinates,Integer> pair : routingTable.entrySet()) 
            if (pair.getValue() == endpoint) {
                coords = pair.getKey();
                break;
            }
        
        return coords;
    }
    
    public void open() { 
    	// To be overridden
    }
    
    public void close() {
        try {
        	disconnect();
            server.close();
        } catch (IOException ex) {
            // We swallow any IO error
        }
    }
    
    /** Drop any existing connection */
    public void disconnect() {
    	disconnected = true;
    }
    
    protected void println(String msg) {
    	System.out.println("Gw #" + id + ": " + msg);
    }

    @Override
    public void run() {
        
        try {
          server = new ServerSocket(port, MAX_CONNECTIONS);
          println("Gateway opened on port " + server.getLocalPort());

          while (true) {
            
            Socket skt = server.accept();
            Connection jmsConnection = null;
            try {
                println("Connection established with " + skt);
                
                disconnected = false;
                
                InputStream istream = new BufferedInputStream(skt.getInputStream());
                BufferedOutputStream ostream = new BufferedOutputStream(skt.getOutputStream());
                
    	   		Context jndiContext = new InitialContext();
    	        ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup("jms/easyhome/ConnectionFactory");
    	        
                Topic outboundTopic = (Topic) jndiContext.lookup("jms/easyhome/OutboundPacketsTopic");
                Topic inboundTopic = (Topic) jndiContext.lookup("jms/easyhome/InboundPacketsTopic");
                
    	        jmsConnection = connectionFactory.createConnection();
    	        Session jmsSession = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                
                MessageConsumer outboundConsumer = jmsSession.createConsumer(outboundTopic);
                MessageProducer inboundProducer = jmsSession.createProducer(inboundTopic);
                MessageProducer outboundProducer = jmsSession.createProducer(outboundTopic);
                
                jmsConnection.start();

                while (!disconnected) {
                    
                	if (istream.available() > 0) {
	                    handleInboundPacketFrom(istream,jmsSession,inboundProducer,outboundProducer);
                	}
                    
                    handleOutboundPacketsTo(ostream,outboundConsumer);	                    
                }
            
            } catch (Exception ex) {
              System.out.println(ex);
            } finally {
              try {
            	  if (skt != null) skt.close();
              } catch (IOException ex) {
          		// Whatever the case, the connection is not available anymore
              } finally {
            	  println("Connection with " + skt + " closed");  
              }
              
        	  try {
        		  jmsConnection.close();
        	  } catch (JMSException jmsEx) {
        		// Whatever the case, the connection is not available anymore  
        	  } finally {
        		  println("JMS connection closed");
        	  }
            }
          }
        } catch (Exception ex) {
            if (ex instanceof SocketException)
            	println("Gateway cannot accept connections anymore");
            else
            	println("Gateway could not be opened");
        }
    }
    
    /**
     * Dispatches the packet to the processes and the gateways
     */
    private void dispatchPacket(NativePacket pkt, Session jmsSession, MessageProducer inboundProducer, MessageProducer outboundProducer) {
  
        try {
            ObjectMessage inboundMessage = jmsSession.createObjectMessage(pkt);
            inboundProducer.send(inboundMessage);
        } catch (Exception e) {
        	println("Message could not be dispatched to inbound packets topic");
        }

        try {
            ObjectMessage outboundMessage = jmsSession.createObjectMessage(pkt);
            outboundProducer.send(outboundMessage);            	
        } catch (Exception e) {
        	println("Message could not be dispatched to outbound packets topic");
        }
    }
    
    private void handleInboundPacketFrom(InputStream is, Session jmsSession,
    		MessageProducer inboundProducer, MessageProducer outboundProducer) throws IOException {
        
        try {
        	
        	NativePacket nativePkt = readFrom(is);
        	dispatchPacket(nativePkt,jmsSession,inboundProducer,outboundProducer);
        	
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    protected NativePacket readFrom(InputStream is) throws IOException {
    	// To be overridden
    	return null;
    }
    
    protected void write(NativePacket pkt, OutputStream os) throws IOException {
    	// To be overridden
    }
    
    private void handleOutboundPacketsTo(OutputStream os, MessageConsumer consumer) {
    	
        try {
            while (true) {
            	ObjectMessage msg = (ObjectMessage) consumer.receiveNoWait();
                if (msg == null) {
                	break;
                }
            	NativePacket nativePkt = (NativePacket) msg.getObject();
            	byte srcGid = nativePkt.getSrcCoords().getGatewayId();
            	byte dstGid = nativePkt.getDstCoords().getGatewayId();
            	if (srcGid != id) {
	            	if (dstGid == id || dstGid == 0) {
	            		println("Packet received from " + nativePkt.getSrcCoords());
	            		write(nativePkt,os);
	            	}
            	}
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    	
    }
}
