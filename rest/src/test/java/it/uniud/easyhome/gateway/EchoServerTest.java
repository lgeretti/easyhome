package it.uniud.easyhome.gateway;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import org.junit.*;

public class EchoServerTest {
    
    private EchoServer server;
    
    private final static int PORT = 8000;
    
    @Before
    public void init() {
        
        server = new EchoServer(PORT);
        Thread serverThread = new Thread(server);
        serverThread.start();
    }
    
    @Test
    public void testEcho() throws IOException {
        
        BufferedWriter networkOut = null;
        BufferedReader networkIn = null;
        final String outputLine = "This is a line";

        Socket theSocket = new Socket("localhost",PORT);
        networkIn = new BufferedReader(
                new InputStreamReader(theSocket.getInputStream()));
        networkOut = new BufferedWriter(
                new OutputStreamWriter(theSocket.getOutputStream()));

        networkOut.write(outputLine+"\n"); // Need to terminate the line since we readline() below
        networkOut.flush();
        String inputLine = networkIn.readLine();

        networkIn.close(); 
        networkOut.close(); 
        
        assertEquals(outputLine,inputLine);
    }
    
    @After
    public void close() {
        
        server.close();
    }
    
}
