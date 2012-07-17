package it.uniud.easyhome.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class EchoServlet extends GenericServlet {

    /**
     * 
     */
    private static final long serialVersionUID = -8819699947334069861L;

    @Override
    public void init() {
        
        int port = 6969;
        
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
    
    @Override
    public void service(ServletRequest req, ServletResponse res)
            throws ServletException, IOException {
        // TODO Auto-generated method stub
        
    }
    
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


}
