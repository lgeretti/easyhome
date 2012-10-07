package it.uniud.easyhome.processing;

import it.uniud.easyhome.network.EHPacket;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class NodeRegistrationProcess extends Process implements Runnable {
	
    public NodeRegistrationProcess(int pid) {
        super(pid, Session.STATEFUL, Interaction.ASYNC);
    }
    
    @Override
    public void start() {
        Thread thr = new Thread(this);
        
        thr.start();    	
    }

    @Override
    public void run() {
    	
    	Connection jmsConnection = null;
    	
    	try {
	   		Context jndiContext = new InitialContext();
	        ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup("jms/easyhome/ConnectionFactory");
	        
	        Topic inboundTopic = (Topic) jndiContext.lookup("jms/easyhome/InboundPacketsTopic");
	        
	        jmsConnection = connectionFactory.createConnection();
	        javax.jms.Session jmsSession = jmsConnection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
	        
	        MessageConsumer consumer = jmsSession.createConsumer(inboundTopic);    	
	    	
	        jmsConnection.start();
	        
	        println("processing started");
	        
	    	while (!isStopped()) {
	    		
            	ObjectMessage msg = (ObjectMessage) consumer.receive(5000);
            	if (msg != null) {
	            	EHPacket pkt = (EHPacket) msg.getObject();
	            	println("Packet received from " + pkt.getSrcCoords());
            	}
	    	}
	    	
    	} catch (NamingException ex) {
    		ex.printStackTrace();
    	} catch (JMSException ex) {
    		ex.printStackTrace();
    	} finally {
    		try {
	    		if (jmsConnection != null)
	    			jmsConnection.close();
    		} catch (JMSException ex) {
    			// Can't do better than this, hence we ignore the error
    		} finally {
    			println("JMS connection stopped");
    		}
    	}
    	
    	println("processing stopped");
    }
}
