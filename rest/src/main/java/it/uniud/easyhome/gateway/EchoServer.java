package it.uniud.easyhome.gateway;

import java.net.*;
import java.io.*;

import javax.servlet.ServletConfig;

public class EchoServer implements Runnable {
    
    private ServletConfig config;
    private ServerSocket server;
    
    public EchoServer(ServletConfig config) {
        this.config = config;
    }
    
    public void run() {
        
        int port = Integer.parseInt(config.getInitParameter("it.uniud.easyhome.gateway.port"));
        
        try {
          server = new ServerSocket(port, 1);
          System.out.println("Listening for connections on port " 
           + server.getLocalPort());

          while (true) {
              
            Socket connection = server.accept();
            try {
                System.out.println("Connection established with " + connection);
              
                InputStream in = connection.getInputStream();
                OutputStream out = connection.getOutputStream();
                
                try {     
                  while (true) {
                    int i = in.read();
                    if (i == -1) 
                        break;
                    System.out.write(i);
                    out.write(i);
                    out.flush();
                  }
                }
                catch (SocketException ex) {
                }
                catch (IOException ex) {
                  System.err.println(ex);
                }
                try {
                  in.close();
                }
                catch (IOException ex) { 
                } 
              
            }
            catch (IOException ex) {
              System.err.println(ex); 
            }
            finally {
              try {
                if (connection != null) connection.close();
                System.out.println("Connection with " + connection + " closed");
              }
              catch (IOException ex) {}
            }
          }
        }
        catch (IOException ex) {
            if (!(ex instanceof SocketException))
                ex.printStackTrace();
        }
    }

    public void closeConnection() {
        try {
            server.close();
        } catch (IOException ex) {
            // We swallow any error
        } finally {
            System.out.println("Stopped listening for connections on port " 
                    + server.getLocalPort());
        }
    }
    
}