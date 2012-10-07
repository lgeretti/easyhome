package it.uniud.easyhome.gateway;

import it.uniud.easyhome.network.EHPacket;
import it.uniud.easyhome.network.ModuleCoordinates;
import it.uniud.easyhome.network.Operation;
import it.uniud.easyhome.network.xbee.XBeeReceivedPacket;
import it.uniud.easyhome.network.xbee.XBeeTransmittedPacket;
import it.uniud.easyhome.network.exceptions.IllegalBroadcastPortException;
import it.uniud.easyhome.network.exceptions.RoutingEntryMissingException;

import java.net.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.io.*;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;

public class XBeeGateway implements Gateway {

    private int mappedEndpointCounter = 0;
    
    private ServerSocket server = null;
    
    private final Map<ModuleCoordinates,Integer> routingTable = new HashMap<ModuleCoordinates,Integer>();
    
    private int port;
    
    private byte id;
    
	private volatile boolean disconnected = false;
    
    public byte getId() {
        return id;
    }
    
    public ProtocolType getProtocolType() {
        return ProtocolType.XBEE;
    }
    
    public int getPort() {
        return port;
    }
    
    public Map<ModuleCoordinates,Integer> getRoutingTable() {
        return routingTable;
    }
    
    public XBeeGateway(byte id, int port) {
        
        this.id = id;
        this.port = port;
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
    
    private ModuleCoordinates getCoordinatesFor(int endpoint) {
        ModuleCoordinates coords = null;

        for (Entry<ModuleCoordinates,Integer> pair : routingTable.entrySet()) 
            if (pair.getValue() == endpoint) {
                coords = pair.getKey();
                break;
            }
        
        return coords;
    }
    
    
    /**
     * Converts an XBee API received packet, starting from the 64 bit source network address forth,
     * checksum excluded.
     */
    private EHPacket convertFrom(XBeeReceivedPacket xpkt) throws RoutingEntryMissingException {
        
        ModuleCoordinates srcCoords = new ModuleCoordinates(
        		id,xpkt.get64BitSrcAddr(),xpkt.get16BitSrcAddr(),xpkt.getSrcEndpoint());
        
        byte receiveOptions = xpkt.getReceiveOptions();
        byte dstEndpoint = xpkt.getDstEndpoint();
        
        ModuleCoordinates dstCoords = null;
        
        // If a broadcast, we use the broadcast format for the destination coordinates, but only
        // if the destination port is actually the administration port
        if (receiveOptions == 0x02) {
        	if (dstEndpoint == 0x00) {        		
	        	dstCoords = new ModuleCoordinates((byte)0,(short)0xFFFF,(short)0xFFFE,(byte)0);
	        	println("Setting destination as broadcast");
        	} else {
        		throw new IllegalBroadcastPortException();
        	}
        } else {
	        
	        dstCoords = getCoordinatesFor(dstEndpoint);
	        
	        if (dstCoords == null)
	            throw new RoutingEntryMissingException();
	        
	        println("Retrieved coordinates for mapped endpoint " + dstEndpoint);
	    }
        
        Operation op = new Operation(xpkt.getTransactionSeqNumber(),xpkt.getProfileId(),
        		xpkt.getClusterId(),xpkt.getFrameControl(),xpkt.getCommand(),xpkt.getApsPayload());
        
        return new EHPacket(srcCoords,dstCoords,op);
    }
    
    /**
     * Dispatches the packet to the processes and the gateways
     */
    private void dispatchPacket(EHPacket pkt, Session jmsSession, MessageProducer inboundProducer, MessageProducer outboundProducer) {
  
        try {
            ObjectMessage inboundMessage = jmsSession.createObjectMessage(pkt);
            inboundProducer.send(inboundMessage);
            println("Message dispatched to inbound packets topic");
        } catch (Exception e) {
        	println("Message not dispatched to inbound packets topic");
        }

        try {
            ObjectMessage outboundMessage = jmsSession.createObjectMessage(pkt);
            outboundProducer.send(outboundMessage);
            println("Message dispatched to outbound packets topic");            	
        } catch (Exception e) {
        	println("Message could not be dispatched to outbound packets topic");
        }
    }
    
    private class GatewayRunnable implements Runnable {
    
        private void handleInboundPacketFrom(InputStream in, Session jmsSession,
        		MessageProducer inboundProducer, MessageProducer outboundProducer) throws IOException {
            
            println("Recognized XBee packet");
            
            try {
            	
            	XBeeReceivedPacket xbeePkt = new XBeeReceivedPacket();
            	xbeePkt.read(in);
            	EHPacket ehPkt = convertFrom(xbeePkt);
            	dispatchPacket(ehPkt,jmsSession,inboundProducer,outboundProducer);
            	
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        private void handleOutboundPacketsTo(OutputStream os, MessageConsumer consumer) {
        	
            try {
                while (true) {
                	ObjectMessage msg = (ObjectMessage) consumer.receiveNoWait();
                    if (msg == null) {
                    	break;
                    }
                	EHPacket ehPkt = (EHPacket) msg.getObject();
                	if (ehPkt.getDstCoords().getGatewayId() == id) {
                		println("Packet received from " + ehPkt.getSrcCoords() + ", injecting");
                		XBeeTransmittedPacket xbeePkt = new XBeeTransmittedPacket(ehPkt);
                		xbeePkt.write(os);
                	} else {
                		println("Packet received from self, discarding");
                	}
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        	
        }
        
        public void run() {
            
            try {
              server = new ServerSocket(port, 1);
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
    
    }

    public void open() {

        Thread thr = new Thread(new GatewayRunnable());
        thr.start();
    }
    
    public void disconnect() {
    	disconnected = true;
    }
    
    public void close() {
        try {
        	disconnect();
            server.close();
        } catch (IOException ex) {
            // We swallow any IO error
        }
    }
    
    private void println(String msg) {
    	System.out.println("Gw #" + id + ": " + msg);
    }
}