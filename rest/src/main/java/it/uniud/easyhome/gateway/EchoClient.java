package it.uniud.easyhome.gateway;

import java.net.*;
import java.io.*;

public class EchoClient {

  public static void main(String[] args) {

    String hostname = "localhost";
    int port = 0;

    if (args.length > 1) {
      hostname = args[0];
      port = Integer.parseInt(args[1]);
    } else {
        throw new IllegalArgumentException();
    }

    PrintWriter out = null;
    BufferedReader networkIn = null;
    try {
      Socket theSocket = new Socket(hostname, port);
      networkIn = new BufferedReader(
       new InputStreamReader(theSocket.getInputStream()));
      BufferedReader userIn = new BufferedReader(
       new InputStreamReader(System.in));
      out = new PrintWriter(theSocket.getOutputStream());
      System.out.println("Connected to echo server");

      while (true) {
        String theLine = userIn.readLine();
        if (theLine.equals(".")) break;
        out.println(theLine);
        out.flush();
        System.out.println(networkIn.readLine());
      }
      
    }  // end try
    catch (IOException ex) {
      System.err.println(ex);
    }
    finally {
      try {
        if (networkIn != null) networkIn.close(); 
        if (out != null) out.close(); 
      }
      catch (IOException ex) {}
    }

  }  // end main
}
