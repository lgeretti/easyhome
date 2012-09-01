package it.uniud.easyhome.gateway;

import it.uniud.easyhome.network.EHPacket;
import it.uniud.easyhome.network.NetworkContext;

import java.net.*;
import java.io.*;

public class XBeeGateway implements Gateway {
    
    static final byte DELIMITER = 0x7E;
    
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
    
    private EHPacket convertFrom(ByteArrayInputStream bais) {
        
        return new EHPacket(new byte[2]);
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
                        if (octet == DELIMITER) {
                            
                            int highLength = in.read();
                            int length = highLength*256 + in.read();
                            
                            byte[] packetPayload = new byte[length];
                            
                            int sum = 0;
                            for (int i=0; i<length; i++) {
                                byte readValue = (byte)in.read();
                                packetPayload[i] = readValue;
                                sum += readValue;
                            }
                            sum += (byte)in.read();
                             
                            if (0xFF == (sum & 0xFF)) {
                                
                                EHPacket pkt = convertFrom(new ByteArrayInputStream(packetPayload));
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