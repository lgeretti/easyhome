package it.uniud.easyhome.gateway;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.LogLevel;
import it.uniud.easyhome.common.RunnableState;
import it.uniud.easyhome.exceptions.ChecksumException;
import it.uniud.easyhome.exceptions.IllegalGatewayStateException;
import it.uniud.easyhome.exceptions.IncompletePacketException;
import it.uniud.easyhome.exceptions.InvalidDelimiterException;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.exceptions.NoBytesAvailableException;
import it.uniud.easyhome.network.ModuleCoordinates;
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
    
    // id = 0 for broadcast
    // id = 1 for the native network
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
	
    protected volatile LogLevel logLevel;
    
	protected volatile RunnableState state = RunnableState.STOPPED;
	
    @SuppressWarnings("unused")
    private Gateway() { }
    
    protected Gateway(byte id, ProtocolType protocolType, int port) {
    	this(id,protocolType,port,LogLevel.NONE);
    }
    
    protected Gateway(byte id, ProtocolType protocolType, int port, LogLevel logLevel) {
    	if (id == 0 || id == 1)
    		throw new RuntimeException("The gateway id must be different from 0 or 1");
    	this.id = id;
    	this.protocolType = protocolType;
    	this.port = port;
    	this.logLevel = logLevel;
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
    
    protected int getMessageWaitTime() {
    	return MESSAGE_WAIT_TIME_MS;
    }
    
    public final int addRoutingEntry(ModuleCoordinates coords) {
        
    	int mappedEndpoint = mappedEndpointCounter++;
    	
        log(LogLevel.INFO, "Putting routing entry (endpoint " + (mappedEndpoint) + ") for " + coords);
    	
        routingTable.put(coords, mappedEndpoint);
        
        return mappedEndpoint;
    }
    
    public final void removeRoutingEntry(ModuleCoordinates coords) {
        routingTable.remove(coords);
    }
    
    public final void removeRoutingEntriesForGateway(int gatewayId) {
        
        Iterator<Map.Entry<ModuleCoordinates,Integer>> it = routingTable.entrySet().iterator();
        while (it.hasNext())
            if (it.next().getKey().getGatewayId() == gatewayId)
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
    	if (state != RunnableState.STOPPED)
    		throw new IllegalGatewayStateException();
    	state = RunnableState.STARTING;
    	
    	start();
    }
    
    // To be overridden
    protected void start() {
    	
    }

    /** Close the server */
    public final void close() {
    	state = RunnableState.STOPPING;
    	try {
    		server.close();	
    	} catch (Exception ex) { }
    }
    
    public LogLevel getLogLevel() {
    	return this.logLevel;
    }
    
	protected final void log(LogLevel logLevel, String msg) {
		if (this.logLevel.acceptsLogOf(logLevel))
			System.out.println("Gw #" + id + ": " + msg);
    }
	
	public void setLogLevel(LogLevel logLevel) {
		this.logLevel = logLevel;
	}
    
    @Override
    public void run() {
    	
    	state = RunnableState.STARTED;

        while (state != RunnableState.STOPPING) {
        	
        	Socket skt = null;
        	Connection jmsConnection = null;
        	
        	try {
        		
            	log(LogLevel.INFO, "Trying to open the socket for the gateway...");
        	
	        	server = new ServerSocket(port, MAX_CONNECTIONS);
	        	log(LogLevel.INFO, "Gateway opened on port " + server.getLocalPort());
	            
	            skt = server.accept();
            
                log(LogLevel.INFO, "Connection established with " + skt);
                
                InputStream istream = new BufferedInputStream(skt.getInputStream());
                OutputStream ostream = new BufferedOutputStream(skt.getOutputStream());
                
    	   		Context jndiContext = new InitialContext();
    	        ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup(JMSConstants.CONNECTION_FACTORY);
    	        
                Topic outboundTopic = (Topic) jndiContext.lookup(JMSConstants.OUTBOUND_PACKETS_TOPIC);
                Topic inboundTopic = (Topic) jndiContext.lookup(JMSConstants.INBOUND_PACKETS_TOPIC);
                
    	        jmsConnection = connectionFactory.createConnection();
    	        Session jmsSession = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                
                MessageConsumer outboundConsumer = jmsSession.createConsumer(outboundTopic);
                MessageProducer inboundProducer = jmsSession.createProducer(inboundTopic);
                MessageProducer outboundProducer = jmsSession.createProducer(outboundTopic);
                
                jmsConnection.start();
                
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                
                while (state != RunnableState.STOPPING) {
                	
		            handleInboundPacketFrom(istream,buffer,jmsSession,inboundProducer,outboundProducer);
                    handleOutboundPacketsTo(ostream,outboundConsumer,jmsSession,inboundProducer,MESSAGE_WAIT_TIME_MS);
                }
                
            } catch (SocketException ex) {
            	// We do not want errors to show when close() is called during operations
            } catch (Exception ex) {
            	ex.printStackTrace();
            } finally {
	              try {
	            	  if (skt != null) skt.close();
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
    
    /**
     * Dispatches the packet to the processes and the gateways
     */
    private final void dispatchPacket(NativePacket pkt, Session jmsSession, MessageProducer inboundProducer, MessageProducer outboundProducer) {
  
        try {
            ObjectMessage inboundMessage = jmsSession.createObjectMessage(pkt);
            inboundProducer.send(inboundMessage);
        } catch (Exception e) {
        	log(LogLevel.INFO, "Message could not be dispatched to inbound packets topic");
        }

        try {
            ObjectMessage outboundMessage = jmsSession.createObjectMessage(pkt);
            outboundProducer.send(outboundMessage);            	
        } catch (Exception e) {
        	log(LogLevel.INFO, "Message could not be dispatched to outbound packets topic");
        }
    }
    
    private final void handleInboundPacketFrom(InputStream is, ByteArrayOutputStream buffer, Session jmsSession,
    		MessageProducer inboundProducer, MessageProducer outboundProducer) throws IOException {
    	
        try {
        	
        	NativePacket nativePkt = readFrom(is,buffer);
        	log(LogLevel.DEBUG, "Inbound packet received from " + nativePkt.getSrcCoords());
        	dispatchPacket(nativePkt,jmsSession,inboundProducer,outboundProducer);
        } catch (NoBytesAvailableException | IncompletePacketException ex) {
        	// Just move on
        } catch (ChecksumException | InvalidPacketTypeException ex) {
        	// Handled by discarding within the specific gateway
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
    
    protected void write(NativePacket pkt, OutputStream os, Session jmsSession, MessageProducer producer) throws IOException {
    	// To be overridden
    }
    
    protected void handleOutboundPacketsTo(OutputStream os, MessageConsumer consumer, Session jmsSession, MessageProducer producer, int waitTime) throws IOException {
    	
        try {
            while (true) {
            	ObjectMessage msg = (ObjectMessage) consumer.receive(waitTime);
                if (msg == null) {
                	break;
                }
            	NativePacket nativePkt = (NativePacket) msg.getObject();
            	byte srcGatewayId = nativePkt.getSrcCoords().getGatewayId();
            	byte dstGatewayId = nativePkt.getDstCoords().getGatewayId();
            	if (srcGatewayId != id) {
	            	if (dstGatewayId == id || dstGatewayId == 0) {
	            		log(LogLevel.DEBUG, "Outbound packet received from " + nativePkt.getSrcCoords());
	            		write(nativePkt,os,jmsSession,producer);
	            	}
            	}
            }
        } catch (IOException e) {
        	e.printStackTrace();
        	throw e;
        } catch (JMSException e) {
            e.printStackTrace();
        }
    	
    }
}
