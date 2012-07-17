package it.uniud.easyhome.gateway;

import java.net.*;
import java.io.*;

import javax.servlet.ServletConfig;

public class EchoServer implements Runnable {
    
    private ServerSocket server;
    
    int port;
    
    public EchoServer(ServletConfig config) {
        
        this.port = Integer.parseInt(config.getInitParameter("it.uniud.easyhome.gateway.port"));
    }
    
    public void run() {
        
        try {
          server = new ServerSocket(port, 1);
          System.out.println("Listening for a connection on port " 
           + server.getLocalPort());

          while (true) {
              
            Socket connection = server.accept();
            try {
                System.out.println("Connection established with " + connection);
              
                InputStream in = connection.getInputStream();
                OutputStream out = connection.getOutputStream();
                
                while (true) {
                    int i = in.read();
                    if (i == -1) 
                        break;
                    System.out.write(i);
                    out.write(i);
                    out.flush();
                }
                
                try {
                  in.close();
                } catch (IOException ex) {
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
            // We swallow any error
        } finally {
            System.out.println("Stopped server on port " 
                    + server.getLocalPort());
        }
    }
    
}