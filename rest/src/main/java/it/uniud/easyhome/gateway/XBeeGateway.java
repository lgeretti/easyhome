package it.uniud.easyhome.gateway;

import it.uniud.easyhome.network.EHPacket;
import it.uniud.easyhome.network.ModuleCoordinates;
import it.uniud.easyhome.network.NetworkContext;
import it.uniud.easyhome.network.Operation;
import it.uniud.easyhome.network.exceptions.RoutingEntryMissingException;

import java.net.*;
import java.util.Date;
import java.io.*;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class XBeeGateway implements Gateway {
    
    public static final byte START_DELIMITER = 0x7E;
    public static final byte EXPLICIT_RX_INDICATOR_FRAME_TYPE = (byte)0x91;
    
    private ServerSocket server = null;
    
    private NetworkContext networkContext;
    
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
    
    public int getNewMappedPort() {
        return ++mappedPortCounter;
    }
    
    public XBeeGateway(int id, int port, NetworkContext context) {
        
        this.id = id;
        this.port = port;
        this.networkContext = context;
    }
    
    /**
     * Converts an XBee API received packet, starting from the 16 bit source network address forth,
     * checksum excluded.
     */
    private EHPacket convertFromPayload(ByteArrayInputStream bais) {
        
        int srcAddress = (bais.read() << 8) + bais.read();
        int srcPort = bais.read();
        
        System.out.println("Source address and port: " + srcAddress + ", " + srcPort);
        
        ModuleCoordinates srcCoords = new ModuleCoordinates(id,srcAddress,srcPort);
        
        int dstPort = bais.read();
        
        System.out.println("Looking for gateway id " + id + " and destination port " + dstPort);
        
        ModuleCoordinates dstCoords = networkContext.getCoordinatesFor(id, dstPort);
        
        if (dstCoords == null)
            throw new RoutingEntryMissingException();
        
        int opContext = (bais.read() << 8) + bais.read();
        int opDomain = (bais.read() << 8) + bais.read();
        
        bais.read(); // Read out the Receive Options
        
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

            // Looks up the administered objects
            ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup("jms/easyhome/ConnectionFactory");
            Topic topic = (Topic) jndiContext.lookup("jms/easyhome/OutboundPacketsTopic");
    
            // Creates the needed artifacts to connect to the queue
            Connection connection = connectionFactory.createConnection();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(topic);
    
            // Sends a text message to the queue
            ObjectMessage message = session.createObjectMessage(pkt);
            producer.send(message);
            System.out.println("Message sent!");
    
            connection.close();
            
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        // If the source and destination subnetwork are the same, the packet is not dispatched further   
        
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
                    
                    EHPacket pkt = convertFromPayload(new ByteArrayInputStream(packetPayload));
                    
                    dispatchPacket(pkt);
                } else {
                    System.out.println("Checksum failure");
                }
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
                                System.out.println("Incorrect packet discarded");
                            }
                        }
                    }
                
                } catch (IOException ex) {
                  System.out.println(ex);
                } finally {
                  try {
                    if (connection != null) connection.close();
                  } catch (IOException ex) {
                  }
                  System.out.println("Connection with " + connection + " closed");
                }
              }
           /* } catch (IOException ex) {
                if (!(ex instanceof SocketException))
                    ex.printStackTrace();*/
            }
            catch (Exception ex) {
                System.out.println("Server closed");
                  System.out.println(ex);
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
        } finally {
            System.out.println("Gateway closed");
        }
    }
}