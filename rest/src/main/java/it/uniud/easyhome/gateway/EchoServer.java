package it.uniud.easyhome.gateway;

import java.net.*;
import java.io.*;

public class EchoServer {
    
    private static class EchoThread extends Thread {
        
        InputStream in;
        OutputStream out;
        
         public EchoThread(InputStream in, OutputStream out) {
           this.in = in;
           this.out = out;
         }

         public void run()  {
         
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
             // output thread closed the socket
               System.out.println("wut?");
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

      }

  public static void main(String[] args) {

    int port;
    
    try {
      port = Integer.parseInt(args[0]);
    }  
    catch (Exception ex) {
      port = 0;
    }
    
    try {
      ServerSocket server = new ServerSocket(port, 1);
      System.out.println("Listening for connections on port " 
       + server.getLocalPort());

      while (true) {
        Socket connection = server.accept();
        try {
          System.out.println("Connection established with " 
           + connection);
          Thread input = new EchoThread(connection.getInputStream(),connection.getOutputStream());
          input.start();
          // wait for input to finish 
          try {
            input.join();
          }
          catch (InterruptedException ex) {
          }
        }
        catch (IOException ex) {
          System.err.println(ex); 
        }
        finally {
          try {
            if (connection != null) connection.close();
          }
          catch (IOException ex) {}
        }
      }
    }
    catch (IOException ex) {
      ex.printStackTrace();
    }
  
  }

}