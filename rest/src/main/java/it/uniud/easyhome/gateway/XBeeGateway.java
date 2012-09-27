package it.uniud.easyhome.gateway;

import it.uniud.easyhome.network.EHPacket;
import it.uniud.easyhome.network.ModuleCoordinates;
import it.uniud.easyhome.network.Operation;
import it.uniud.easyhome.network.exceptions.IllegalBroadcastPortException;
import it.uniud.easyhome.network.exceptions.RoutingEntryMissingException;

import java.net.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.io.*;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;

public class XBeeGateway implements Gateway {
    
    public static final byte START_DELIMITER = 0x7E;
    public static final byte EXPLICIT_RX_INDICATOR_FRAME_TYPE = (byte)0x91;
    
    private ServerSocket server = null;
    
    private final Map<ModuleCoordinates,Integer> routingTable = new HashMap<ModuleCoordinates,Integer>();
    
    private int port;
    
    private int mappedPortCounter = 0;
    
    private int id;
    
    public int getId() {
        return id;
    }
    
    public ProtocolType getProtocolType() {
        return ProtocolType.XBEE;
    }
    
    public int getTCPPort() {
        return port;
    }
    
    public Map<ModuleCoordinates,Integer> getRoutingTable() {
        return routingTable;
    }
    
    public XBeeGateway(int id, int port) {
        
        this.id = id;
        this.port = port;
    }
    
    public int addRoutingEntry(ModuleCoordinates coords) {
        
        routingTable.put(coords, ++mappedPortCounter);
        
        return mappedPortCounter;
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
    
    public Integer getPortFor(ModuleCoordinates coords) {
        return routingTable.get(coords);
    }
    
    private ModuleCoordinates getCoordinatesFor(int port) {
        ModuleCoordinates coords = null;

        for (Entry<ModuleCoordinates,Integer> pair : routingTable.entrySet()) 
            if (pair.getValue() == port) {
                coords = pair.getKey();
                break;
            }
        
        return coords;
    }  
    
    
    /**
     * Converts an XBee API received packet, starting from the 16 bit source network address forth,
     * checksum excluded.
     */
    private EHPacket convertFromPayload(ByteArrayInputStream bais) throws RoutingEntryMissingException {
        
        int srcAddress = (bais.read() << 8) + bais.read();
        int srcPort = bais.read();
        
        ModuleCoordinates srcCoords = new ModuleCoordinates(id,srcAddress,srcPort);
        
        int dstPort = bais.read();
        
        System.out.println("Source address and port: " + srcAddress + ", " + srcPort 
        		+ " Destination port: " + dstPort);
                
        int opContext = (bais.read() << 8) + bais.read();
        int opDomain = (bais.read() << 8) + bais.read();
        
        int receiveOptions = bais.read();
        
        ModuleCoordinates dstCoords = null;
        
        // If a broadcast, we use the broadcast format for the destination coordinates, but only
        // if the destination port is actually the administration port
        if (receiveOptions == 0x02) {
        	if (dstPort == 0x00) {        		
	        	dstCoords = new ModuleCoordinates(0,0,0);
	        	System.out.println("Setting destination as broadcast");
        	} else {
        		throw new IllegalBroadcastPortException();
        	}
        } else {
	        
	        dstCoords = getCoordinatesFor(dstPort);
	        
	        if (dstCoords == null)
	            throw new RoutingEntryMissingException();
	        
	        System.out.println("Retrieved coordinates for mapped port " + dstPort);
	    }
        
        int opFlags = bais.read();
        
        bais.read(); // Read out the transaction sequence number
        
        int opCommand = bais.read();
        
        int readByte;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((readByte = bais.read()) != -1) {
            baos.write(readByte);
        }
        
        Operation op = new Operation(opFlags,opDomain,opContext,opCommand,baos.toByteArray());
        
        return new EHPacket(srcCoords,dstCoords,op);
    }
    
    /**
     * Dispatches the packet to the processes and the gateways
     */
    private void dispatchPacket(EHPacket pkt) {
        
    	try {
    		Context jndiContext = new InitialContext();
            ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup("jms/easyhome/ConnectionFactory");
            Connection connection = connectionFactory.createConnection();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    		
            try {
                Topic inboundTopic = (Topic) jndiContext.lookup("jms/easyhome/InboundPacketsTopic");
                MessageProducer inboundProducer = session.createProducer(inboundTopic);
                ObjectMessage inboundMessage = session.createObjectMessage(pkt);
                inboundProducer.send(inboundMessage);
                System.out.println("Message dispatched to inbound packets topic");
            } catch (Exception e) {
            	System.out.println("Message not dispatched to inbound packets topic");
            }

            try {
	            Topic outboundTopic = (Topic) jndiContext.lookup("jms/easyhome/OutboundPacketsTopic");
	            MessageProducer outboundProducer = session.createProducer(outboundTopic);
	            ObjectMessage outboundMessage = session.createObjectMessage(pkt);
	            outboundProducer.send(outboundMessage);
	            System.out.println("Message dispatched to outbound packets topic");            	
            } catch (Exception e) {
            	System.out.println("Message could not be dispatched to outbound packets topic");
            }
            
            connection.close();
            
    	} catch (Exception e) {
    		System.out.println("Session could not be created");
    	}
    }
    
    private class GatewayRunnable implements Runnable {
    
        private void handleInboundPacketFrom(InputStream in) throws IOException {
            
            System.out.println("Recognized XBee packet");
            
            int highLength = in.read();
            // The frame type and source 64 bit address (hence 9 octets) are not stored
            int length = highLength*256 + in.read() - 9;
            
            byte[] packetPayload = new byte[length];
            
            int sum = EXPLICIT_RX_INDICATOR_FRAME_TYPE;
            byte frameType = (byte)in.read();
            if (frameType == EXPLICIT_RX_INDICATOR_FRAME_TYPE) {
                
                // Read out the source 64 bit address
                for (int i=0; i<8; i++) {
                    byte readValue = (byte)in.read();
                    sum += readValue;
                }
                    
                for (int i=0; i<length; i++) {
                    byte readValue = (byte)in.read();
                    packetPayload[i] = readValue;
                    sum += readValue;
                }
                sum += (byte)in.read();
                 
                if (0xFF == (sum & 0xFF)) {
                    
                    System.out.println("Checksum success, converting and dispatching");
                    
                    try {
                    
                    	EHPacket pkt = convertFromPayload(new ByteArrayInputStream(packetPayload));
                    
                    	dispatchPacket(pkt);
                    	
                    } catch (RoutingEntryMissingException ex) {
                    	
                    	System.out.println("No routing entry exists for the given destination port, unable to dispatch");
                    }
                } else {
                    System.out.println("Checksum failure");
                }
            } else {
                System.out.println("Incorrect packet type: discarding");
            }
        }
        
        public void run() {
            
            try {
              server = new ServerSocket(port, 1);
              System.out.println("Gateway opened on port " + server.getLocalPort());
    
              while (true) {
                  
                Socket connection = server.accept();
                try {
                    System.out.println("Connection established with " + connection);
                    
                    InputStream in = connection.getInputStream();
                    
                    int octet;
                    while ((octet = in.read()) != -1) {
                        if (octet == START_DELIMITER) {
                            try {
                                handleInboundPacketFrom(in);
                            } catch (Exception e) {
                                // We want to gracefully handle incorrect packets
                            }
                        }
                    }
                
                } catch (IOException ex) {
                  System.out.println(ex);
                } finally {
                  try {
                    if (connection != null) connection.close();
                  } catch (IOException ex) {
                      // Whatever the case, the connection is not available anymore
                  }
                  System.out.println("Connection with " + connection + " closed");
                }
              }
            } catch (IOException ex) {
                if (!(ex instanceof SocketException))
                    ex.printStackTrace();
                System.out.println("Gateway closed");
            }
        }
    
    }

    public void open() {

        Thread thr = new Thread(new GatewayRunnable());
        thr.start();
    }
    
    public void close() {
        try {
            server.close();
        } catch (IOException ex) {
            // We swallow any IO error
        }
    }
}