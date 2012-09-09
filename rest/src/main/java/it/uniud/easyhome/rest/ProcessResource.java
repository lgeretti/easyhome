package it.uniud.easyhome.rest;

import it.uniud.easyhome.processing.NodeRegistrationProcess;
import it.uniud.easyhome.processing.Process;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.*;

@Path("/processes")
public class ProcessResource {
    
    private static final List<Process> processes = new ArrayList<Process>();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Process> getProcesses() {

        return processes;
    }           
    
    @POST
    @Path("produceMessage")
    public void produceMessage() throws NamingException, JMSException {
        
        // Gets the JNDI context
        Context jndiContext = new InitialContext();

        // Looks up the administered objects
        ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup("jms/javaee6/ConnectionFactory");
        Queue queue = (Queue) jndiContext.lookup("jms/javaee6/Queue");

        // Creates the needed artifacts to connect to the queue
        Connection connection = connectionFactory.createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer = session.createProducer(queue);

        // Sends a text message to the queue
        TextMessage message = session.createTextMessage();
        message.setText("This is a text message sent at " + new Date());
        producer.send(message);
        System.out.println("\nMessage sent !");

        connection.close();
    }
    
    @POST
    @Path("listen")
    public void startListening() {
        
        Thread thr = new Thread(new Runnable() { public void run() {
            
                try {
                    Context jndiContext = new InitialContext();
                
                    // Looks up the administered objects
                    ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup("jms/javaee6/ConnectionFactory");
                    Queue queue = (Queue) jndiContext.lookup("jms/javaee6/Queue");
        
                    // Creates the needed artifacts to connect to the queue
                    Connection connection = connectionFactory.createConnection();
                    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    MessageConsumer consumer = session.createConsumer(queue);
                    connection.start();
        
                    // Loops to receive the messages
                    System.out.println("\nInfinite loop. Waiting for a message...");            
                
                    while (true) {
                        TextMessage message = (TextMessage) consumer.receive();
                        System.out.println("Message received: " + message.getText());
                    }
                
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
        
        thr.start();
    }
    
    @GET
    @Path("/howmany")
    @Produces(MediaType.TEXT_PLAIN)
    public String getNum() {
        
        StringBuilder strb = new StringBuilder();
        strb.append("There are ").append(processes.size()).append(" processes around, with ids");
        for (Process p : processes)
            strb.append(p.getPid()).append(",");
        strb.deleteCharAt(strb.length()-1);
        
        return strb.toString();
    }
    
    @POST
    @Path("{pid}")
    public Response postProcess(@PathParam("pid") int pid) {
        
        for (Process p : processes) {
            if (p.getPid() == pid)
                return Response.notModified().build();
        }
        
        processes.add(new NodeRegistrationProcess(pid,Process.Session.STATELESS,Process.Interaction.SYNC));
        
        return Response.ok().build();
    }

    @DELETE
    @Path("{pid}")
    public Response deleteProcess(@PathParam("pid") int pid) {
        
        for (int i=0; i<processes.size(); i++) {
            if (processes.get(i).getPid() == pid) {
                processes.remove(i);
                return Response.ok().build();
            }
        }
        
        return Response.status(Status.NOT_FOUND).build();
    }
    
    @DELETE
    public Response clearAll() {
        
        processes.clear();
        
        return Response.ok().build();
    }
    
}
