package it.uniud.easyhome.gateway;

import it.uniud.easyhome.network.EHPacket;
import it.uniud.easyhome.network.ModuleCoordinates;
import it.uniud.easyhome.network.NetworkContext;
import it.uniud.easyhome.network.Operation;
import it.uniud.easyhome.network.exceptions.RoutingEntryMissingException;

import java.net.*;
import java.io.*;

public class XBeeGateway implements Gateway {
    
    public static final byte START_DELIMITER = 0x7E;
    public static final byte EXPLICIT_RX_INDICATOR_FRAME_TYPE = (byte)0x91;
    
    private ServerSocket server = null;
    
    private NetworkContext networkContext;
    
    private int port;
    
    private int id;
    
    public int getId() {
        return id;
    }
    
    public ProtocolType getProtocolType() {
        return ProtocolType.XBEE;
    }
    
    public int getPort() {
        return port;
    }
    
    public XBeeGateway(int id, int port, NetworkContext context) {
        
        this.id = id;
        this.port = port;
        this.networkContext = context;
    }
    
    /**
     * Converts an XBee API received packet, starting from the 16 bit source network address forth,
     * checksum excluded
     */
    private EHPacket convertFrom(ByteArrayInputStream bais) {
        
        int srcAddress = (bais.read() << 8) + bais.read();
        int srcPort = bais.read();
        
        ModuleCoordinates srcCoords = new ModuleCoordinates(id,srcAddress,srcPort);
        
        int dstPort = bais.read();
        
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
    
    private class GatewayRunnable implements Runnable {
    
        public void run() {
            
            try {
              server = new ServerSocket(port, 1);
              System.out.println("Gateway opened on port " + server.getLocalPort());
    
              while (true) {
                  
                Socket connection = server.accept();
                try {
                    System.out.println("Connection established with " + connection);
                    
                    InputStream in = connection.getInputStream();
                    
                    while (true) {
                        int octet = in.read();
                        if (octet == START_DELIMITER) {
                            
                            int highLength = in.read();
                            // The frame type and source 64 bit address (hence 5 octets) are not stored
                            int length = highLength*256 + in.read() - 5;
                            
                            byte[] packetPayload = new byte[length];
                            
                            int sum = EXPLICIT_RX_INDICATOR_FRAME_TYPE;
                            byte frameType = (byte)in.read();
                            if (frameType == EXPLICIT_RX_INDICATOR_FRAME_TYPE) {
                            
                                // Read out the source 64 bit address
                                for (int i=0; i<4; i++) {
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
                                    
                                    EHPacket pkt = convertFrom(new ByteArrayInputStream(packetPayload));
                                    
                                    System.out.println(pkt.printBytes());
                                }
                            }
                        }
                    }
                
                } catch (IOException ex) {
                  System.err.println(ex);
                } finally {
                  try {
                    if (connection != null) connection.close();
                    System.out.println("Connection with " + connection + " closed");
                  } catch (IOException ex) {
                  }
                }
              }
            } catch (IOException ex) {
                if (!(ex instanceof SocketException))
                    ex.printStackTrace();
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