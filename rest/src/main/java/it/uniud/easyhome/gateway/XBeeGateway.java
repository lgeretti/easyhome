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

import javax.annotation.Resource;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;

public class XBeeGateway implements Gateway {
    
    public static final byte START_DELIMITER = 0x7E;
    public static final byte EXPLICIT_RX_INDICATOR_FRAME_TYPE = (byte)0x91;
    public static final byte EXPLICIT_ADDRESSING_COMMAND_FRAME_TYPE = 0x11;
    
    private int transactionSequenceNumberCounter = 0;

    private int mappedPortCounter = 0;
    
    private ServerSocket server = null;
    
    private final Map<ModuleCoordinates,Integer> routingTable = new HashMap<ModuleCoordinates,Integer>();
    
    private int port;
    
    private int id;
    
	private volatile boolean disconnected = false;
    
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
        
        println("Putting routing entry (port " + (mappedPortCounter+1) + ") for " + coords);
    	
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
        
        println("Source address and port: " + srcAddress + ", " + srcPort 
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
	        	println("Setting destination as broadcast");
        	} else {
        		throw new IllegalBroadcastPortException();
        	}
        } else {
	        
	        dstCoords = getCoordinatesFor(dstPort);
	        
	        if (dstCoords == null)
	            throw new RoutingEntryMissingException();
	        
	        println("Retrieved coordinates for mapped port " + dstPort);
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
    
    /**
     * Injects the packet into the subnetwork
     */
    private void injectPacket(EHPacket pkt, OutputStream os) {
    	
    	try {
    		os.write(START_DELIMITER);
    		
    		byte[] opData = pkt.getOperation().getData();
    		int length = 23 + opData.length;
    		
    		// High and low lengths
    		os.write((length >>> 8) & 0xFF);
    		os.write(length & 0xFF);
    		
    		int sum = 0;
    		
    		// Frame type
    		byte frameType = EXPLICIT_ADDRESSING_COMMAND_FRAME_TYPE; 
    		os.write(frameType);
    		sum += frameType;
    		// Frame ID (0 for no response)
    		byte frameId = 0x00;
    		os.write(frameId);
    		sum += frameId;
    		// 64 bit destination address (broadcast in order to left it unspecified)
    		byte[] ieeeDestAddr = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xFF, (byte)0xFF}; 
    		for (byte b: ieeeDestAddr) {
    			os.write(b);
    			sum += b;
    		}
    		// 16 bit destination address
    		int nwkDestAddr = pkt.getDstCoords().getAddress();
    		byte highNwkDestAddr = (byte)((nwkDestAddr >>> 8) & 0xFF);
    		byte lowNwkDestAddr = (byte)(nwkDestAddr & 0xFF);
    		os.write(highNwkDestAddr);
    		sum += highNwkDestAddr;
    		os.write(lowNwkDestAddr);
    		sum += lowNwkDestAddr;
    		// Source endpoint
    		int srcEndpoint = pkt.getSrcCoords().getPort();
    		os.write(srcEndpoint);
    		sum += srcEndpoint;
    		// Destination endpoint
    		int dstEndpoint = pkt.getDstCoords().getPort();
    		os.write(dstEndpoint);
    		sum += dstEndpoint;
    		// Cluster ID
    		int clusterId = pkt.getOperation().getContext();
    		byte highClusterId = (byte)((clusterId >>> 8) & 0xFF);
    		byte lowClusterId = (byte)(clusterId & 0xFF);
    		os.write(highClusterId);
    		sum += highClusterId;
    		os.write(lowClusterId);
    		sum += lowClusterId;
    		// Profile ID
    		int profileId = pkt.getOperation().getDomain();
    		byte highProfileId = (byte)((profileId >>> 8) & 0xFF);
    		byte lowProfileId = (byte)(profileId & 0xFF);
    		os.write(highProfileId);
    		sum += highProfileId;
    		os.write(lowProfileId);
    		sum += lowProfileId;
    		// Broadcast radius (unlimited)
    		int broadcastRadius = 0x00;
    		os.write(broadcastRadius);
    		sum += broadcastRadius;
    		// Transmit options (none)
    		int transmitOptions = 0x00;
    		os.write(transmitOptions);
    		sum += transmitOptions;
    		// Frame control
    		int frameControl = (pkt.getOperation().isContextSpecific() ? 0x01 : 0x00);
    		os.write(frameControl);
    		sum += frameControl;
    		// Transaction sequence number
    		int tsn = ++transactionSequenceNumberCounter;
    		os.write(tsn);
    		sum += tsn;
    		// Command
    		int command = pkt.getOperation().getCommand();
    		os.write(command);
    		sum += command;
    		// Command payload
    		for (byte b: opData) {
    			os.write(b);
    			sum += b;
    		}
    		// Checksum
    		os.write(0xFF - (sum & 0xFF));
    		os.flush();
    		
    		println("XBee packet written");
    		
    	} catch (IOException ex) {
    		println(ex.getMessage());
    	}
    }
    
    private class GatewayRunnable implements Runnable {
    
        private void handleInboundPacketFrom(InputStream in, Session jmsSession,
        		MessageProducer inboundProducer, MessageProducer outboundProducer) throws IOException {
            
            println("Recognized XBee packet");
            
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
                    
                    println("Checksum success, converting and dispatching");
                    
                    try {
                    
                    	EHPacket pkt = convertFromPayload(new ByteArrayInputStream(packetPayload));
                    
                    	dispatchPacket(pkt,jmsSession,inboundProducer,outboundProducer);
                    	
                    } catch (RoutingEntryMissingException ex) {
                    	
                    	println("No routing entry exists for the given destination port, unable to dispatch");
                    }
                } else {
                    println("Checksum failure");
                }
            } else {
                println("Incorrect packet type: discarding");
            }
        }
        
        private void handleOutboundPacketsTo(OutputStream os, MessageConsumer consumer) {
        	
            try {
                while (true) {
                	ObjectMessage msg = (ObjectMessage) consumer.receiveNoWait();
                    if (msg == null) {
                    	break;
                    }
                	EHPacket pkt = (EHPacket) msg.getObject();
                	if (pkt.getDstCoords().getGatewayId() == id) {
                		println("Packet received from " + pkt.getSrcCoords() + ", injecting");
                		injectPacket(pkt,os);
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
	                    
                    	int octet = -1;
                    	if (istream.available() > 0) {
	                    	while ((octet = istream.read()) != -1) {
			                    if (octet == START_DELIMITER) {
		                            try {
		                                handleInboundPacketFrom(istream,jmsSession,inboundProducer,outboundProducer);
		                                break; // We avoid to process a continuous flow of inbound packets without handling outbound packets too
		                            } catch (Exception e) {
		                                // We want to gracefully handle incorrect packets
		                            }
			                    }
	                    	}
                    	}
	                    
	                    handleOutboundPacketsTo(ostream,outboundConsumer);	                    
                    }
                
                } catch (Exception ex) {
                  System.out.println(ex);
                } finally {
                  try {
                    if (skt != null) skt.close();
                    jmsConnection.close();
                  } catch (Exception ex) {
                      // Whatever the case, the connection is not available anymore
                  }
                  
                  println("Connection with " + skt + " closed");
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