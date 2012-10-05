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

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;

public class XBeeGateway implements Gateway {
    
    public static final byte START_DELIMITER = 0x7E;
    public static final byte EXPLICIT_RX_INDICATOR_FRAME_TYPE = (byte)0x91;
    public static final byte EXPLICIT_ADDRESSING_COMMAND_FRAME_TYPE = 0x11;
    
    private int transactionSequenceNumberCounter = 0;

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
    private EHPacket convertFromPayload(ByteArrayInputStream bais) throws RoutingEntryMissingException {
        
    	long srcUuid = (((long)bais.read()) << 56) + 
    			       (((long)bais.read()) << 48) + 
    			       (((long)bais.read()) << 40) + 
    			       (((long)bais.read()) << 32) +
    			       (((long)bais.read()) << 24) + 
    			       (((long)bais.read()) << 16) + 
    			       (((long)bais.read()) << 8) + 
    			       (long)bais.read();
        short srcAddress = (short)((bais.read() << 8) + bais.read());
        byte srcEndpoint = (byte)bais.read();
        
        ModuleCoordinates srcCoords = new ModuleCoordinates(id,srcUuid,srcAddress,srcEndpoint);
        
        byte dstEndpoint = (byte)bais.read();
        
        println("Source address and port: " + srcAddress + ", " + srcEndpoint 
        		+ " Destination port: " + dstEndpoint);
                
        short opContext = (short)((bais.read() << 8) + bais.read());
        short opDomain = (short)((bais.read() << 8) + bais.read());
        
        byte receiveOptions = (byte)bais.read();
        
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
        
        byte opFlags = (byte)bais.read();
        
        bais.read(); // Read out the transaction sequence number
        
        byte opCommand = (byte)bais.read();
        
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
    		// 64 bit destination address
    		byte[] ieeeDestAddr = new byte[8];
    		long uuid = pkt.getDstCoords().getUnitUid();
    		ieeeDestAddr[0] = (byte)((uuid >>> 56) & 0xFF);
    		ieeeDestAddr[1] = (byte)((uuid >>> 48) & 0xFF);
    		ieeeDestAddr[2] = (byte)((uuid >>> 40) & 0xFF);
    		ieeeDestAddr[3] = (byte)((uuid >>> 32) & 0xFF);
    		ieeeDestAddr[4] = (byte)((uuid >>> 24) & 0xFF);
    		ieeeDestAddr[5] = (byte)((uuid >>> 16) & 0xFF);
    		ieeeDestAddr[6] = (byte)((uuid >>> 8) & 0xFF);
    		ieeeDestAddr[7] = (byte)(uuid & 0xFF); 
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
    		int srcEndpoint = pkt.getSrcCoords().getEndpoint();
    		os.write(srcEndpoint);
    		sum += srcEndpoint;
    		// Destination endpoint
    		int dstEndpoint = pkt.getDstCoords().getEndpoint();
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
            // (The frame type is not stored)
            int length = highLength*256 + in.read() - 1;
            
            byte[] packetPayload = new byte[length];
            
            int sum = EXPLICIT_RX_INDICATOR_FRAME_TYPE;
            byte frameType = (byte)in.read();
            if (frameType == EXPLICIT_RX_INDICATOR_FRAME_TYPE) {
                    
                for (int i=0; i<length; i++) {
                    int readValue = in.read();
                    packetPayload[i] = (byte)readValue;
                    sum += readValue;
                }
                sum += in.read();
                 
                if (0xFF == (sum & 0xFF)) {
                    
                    try {
                    
                    	EHPacket pkt = convertFromPayload(new ByteArrayInputStream(packetPayload));
                    	dispatchPacket(pkt,jmsSession,inboundProducer,outboundProducer);
                    	
                    } catch (RoutingEntryMissingException ex) {
                    	println("No routing entry exists for the given destination endpoint, unable to dispatch");
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
	                    
                    	if (istream.available() > 0) {
                    		int octet;
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