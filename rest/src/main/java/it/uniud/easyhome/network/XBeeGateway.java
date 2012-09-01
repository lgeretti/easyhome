package it.uniud.easyhome.network;

import java.net.*;
import java.io.*;

public class XBeeGateway implements Runnable {
    
    private ServerSocket server = null;
    
    private int port;
    
    public XBeeGateway(int port, NetworkContext context) {
        
        this.port = port;
    }
    
    private EHPacket convertFrom(ByteArrayInputStream bais) {
        
        
        return new EHPacket(new byte[2]);
    }
    
    public void run() {
        
        try {
          server = new ServerSocket(port, 1);
          System.out.println("Listening for a connection on port " + server.getLocalPort());

          while (true) {
              
            Socket connection = server.accept();
            try {
                System.out.println("Connection established with " + connection);
                
                InputStream in = connection.getInputStream();
                
                while (true) {
                    int delimiter = in.read();
                    if (delimiter == 0x7E) {
                        
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

    public void close() {
        try {
            server.close();
        } catch (IOException ex) {
            // We swallow any IO error
        } finally {
            System.out.println("Stopped server on port " + server.getLocalPort());
        }
    }
    
}