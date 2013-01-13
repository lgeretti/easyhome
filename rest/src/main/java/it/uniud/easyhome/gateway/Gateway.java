package it.uniud.easyhome.gateway;

import it.uniud.easyhome.exceptions.IncompletePacketException;
import it.uniud.easyhome.exceptions.InvalidDelimiterException;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.exceptions.NoBytesAvailableException;
import it.uniud.easyhome.packets.ModuleCoordinates;
import it.uniud.easyhome.packets.natives.NativePacket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
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

/**
 * A gateway to access a subnetwork.
 * 
 * Regarding endpoints and addresses:
 * 
 * Since from the global address includes the subnetwork address, and given that a node in a subnetwork knows nothing about both its
 * subnetwork address and the other subnetworks, we need to translate coordinates.
 * Source coordinates are always unicast, meaning that source information is always related to the sender of the message. Nevertheless,
 * in multi-hop networks the source of the message may not be the actual final sender that is able to reach the destination. For this
 * reason, it is not possible in general to reply to a message unless the source is already known or it is included in the payload.
 * Native packets explicitly includes source information and therefore do not need translation.
 * XBee packets, instead, for example, will lose the source information as soon as the packet is sent. The only information retained in
 * such cases is the endpoint: this means that we need to remap endpoints of nodes outside a subnetwork to appear as extra endpoints of
 * the gateway node. More on that in the following.
 * 
 * There exist three classes of messages that can be sent by a subnetwork node: Non-management unicast messages, Management unicast messages and Management 
 * broadcast messages. Non-management broadcast messages are not allowed, since they must refer to a specific endpoint, which does not in general map 
 * to the same device of different nodes. Management unicast messages address one node in the subnetwork, where in particular the gateway node passes the messages
 * through to the coordinator; consequently, the gateway node is transparent to Management response messages from the subnetwork; it still can receive requests,
 * but those should be issued by the coordinator only. 
 * Management unicast messages to other nodes outside the subnetwork are not possible, since the destination coordinates
 * minus the endpoint are insufficient to identify an external node. Management broadcast messages are allowed since they can be intercepted by any node.  
 * 
 * Native packets can in fact support Management unicast to the whole network, but only when the packet originates from a node in the native network, 
 * and therefore the support is removed because they do not allow a response in general.
 * 
 * While endpoint 0 is standard for the native network, the actual endpoint used by a subnetwork can vary. In particular for the XBee case, it is the 
 * case that some packets must be supported on an endpoint different from 0. Hence in general the translation of the destination endpoint for a packet
 * that is injected in a subnetwork may depend on the actual node and the specific Management context, and not simply on the subnetwork type. 
 * Such translation is performed by the coordinator and the resulting packet is issued to the gateway device.
 * 
 * To expose external Non-management endpoints to the interior of a subnetwork, a dynamical map must be created. Gateway devices do not know of 
 * such a map, which is handled by the coordinator that receives the untampered packet from the subnetwork. 
 * 
 * @author Luca Geretti
 */

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
	
	public final static int MESSAGE_WAIT_TIME_MS = 250;
    
    private final Map<ModuleCoordinates,Integer> routingTable = new HashMap<ModuleCoordinates,Integer>();
    
    // We start from 2 in order to leave the two management ports free
    private int mappedEndpointCounter = 2;
    
    protected ServerSocket server = null;
    
	protected volatile boolean disconnected = false;
	
    @SuppressWarnings("unused")
    private Gateway() { }
    
    protected Gateway(byte id, ProtocolType protocolType, int port) {
    	this.id = id;
    	this.protocolType = protocolType;
    	this.port = port;
    }
    
    public final byte getId() {
        return id;
    }
    
    public final ProtocolType getProtocolType() {
        return protocolType;
    }
    
    public final int getPort() {
        return port;
    }
    
    public final Map<ModuleCoordinates,Integer> getRoutingTable() {
        return routingTable;
    }
    
    public final int addRoutingEntry(ModuleCoordinates coords) {
        
    	int mappedEndpoint = mappedEndpointCounter++;
    	
        println("Putting routing entry (endpoint " + (mappedEndpoint) + ") for " + coords);
    	
        routingTable.put(coords, mappedEndpoint);
        
        return mappedEndpoint;
    }
    
    public final void removeRoutingEntry(ModuleCoordinates coords) {
        routingTable.remove(coords);
    }
    
    public final void removeRoutingEntriesForGateway(int gid) {
        
        Iterator<Map.Entry<ModuleCoordinates,Integer>> it = routingTable.entrySet().iterator();
        while (it.hasNext())
            if (it.next().getKey().getGatewayId() == gid)
                it.remove();
    }
    
    public final Integer getEndpointFor(ModuleCoordinates coords) {
        return routingTable.get(coords);
    }
    
    protected final ModuleCoordinates getCoordinatesFor(int endpoint) {
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
    
    public final void close() {
        try {
        	disconnect();
            server.close();
        } catch (IOException ex) {
            // We swallow any IO error
        }
    }
    
    /** Drop any existing connection */
    public final void disconnect() {
    	disconnected = true;
    }
    
    protected final void println(String msg) {
    	System.out.println("Gw #" + id + ": " + msg);
    }

    @Override
    public final void run() {
    	
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
                OutputStream ostream = new BufferedOutputStream(skt.getOutputStream());
                
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
                
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                
                while (!disconnected) {
                	
		            handleInboundPacketFrom(istream,buffer,jmsSession,inboundProducer,outboundProducer);
                    handleOutboundPacketsTo(ostream,outboundConsumer);	                    
                }
            
            } catch (Exception ex) {
              println("Exception: " + ex.getCause().toString() + ", will try to reconnect");
            } finally {
              try {
            	  if (skt != null) skt.close();
              } catch (IOException ex) {
          		// Whatever the case, the connection is not available anymore
              } finally {
            	  println("Connection with " + skt + " closed");  
              }
              
        	  try {
        		  if (jmsConnection != null)
        			  jmsConnection.close();
        	  } catch (JMSException ex) {
        		// Whatever the case, the connection is not available anymore  
        	  }
            }
          }
        } catch (Exception ex) {
            if (ex instanceof SocketException)
            	println("Gateway is closed");
            else
            	println("Gateway could not be opened");
        }
    }
    
    /**
     * Dispatches the packet to the processes and the gateways
     */
    private final void dispatchPacket(NativePacket pkt, Session jmsSession, MessageProducer inboundProducer, MessageProducer outboundProducer) {
  
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
    
    private final void handleInboundPacketFrom(InputStream is, ByteArrayOutputStream buffer, Session jmsSession,
    		MessageProducer inboundProducer, MessageProducer outboundProducer) throws IOException {
    	
        try {
        	
        	NativePacket nativePkt = readFrom(is,buffer);
        	dispatchPacket(nativePkt,jmsSession,inboundProducer,outboundProducer);
        } catch (NoBytesAvailableException ex) {
        	// Just move on
        } catch (IncompletePacketException ex) {
        	// Just move on
        } catch (InvalidDelimiterException ex) {
        	// Prune out the first byte
        	byte[] bufferBytes = buffer.toByteArray();
        	buffer.reset();
        	buffer.write(bufferBytes, 1, bufferBytes.length-1);
        } catch (Exception ex) {
        	ex.printStackTrace();
        }
    }
    
    protected NativePacket readFrom(InputStream is, ByteArrayOutputStream buffer) throws IOException {
    	// To be overridden
    	return null;
    }
    
    protected void write(NativePacket pkt, OutputStream os) throws IOException {
    	// To be overridden
    }
    
    private final void handleOutboundPacketsTo(OutputStream os, MessageConsumer consumer) {
    	
        try {
            while (true) {
            	ObjectMessage msg = (ObjectMessage) consumer.receive(MESSAGE_WAIT_TIME_MS);
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
