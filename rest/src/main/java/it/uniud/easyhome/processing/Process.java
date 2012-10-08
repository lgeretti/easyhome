package it.uniud.easyhome.processing;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Process implements Runnable {
    
    public enum Session { STATEFUL, STATELESS };
    
    public enum Interaction { ASYNC, SYNC };
    
    @XmlElement(name="pid")
    private int pid;
    
    @XmlElement(name="session")
    private Session session;
    
    @XmlElement(name="interaction")
    private Interaction interaction;
    
    private volatile boolean stopped = false;
    
    @SuppressWarnings("unused")
    private Process() {}
    
    protected Process(int pid, Session session, Interaction interaction) {
        this.pid = pid;
        this.session = session;
        this.interaction = interaction;
    }
    
    public final int getPid() {
        return pid;
    }
    
    public final Session getSession() {
        return session;
    }
    
    public final Interaction getInteraction() {
        return interaction;
    }
	
	protected boolean isStopped() {
		return stopped;
	}
	
	public void stop() {
		stopped = true;
	}
	
	public void start() {
		// Empty implementation to be overridden
	}
	
	protected void process(MessageConsumer consumer, MessageProducer producer) throws JMSException {
		// Empty implementation to be overridden
	}
	
	protected void println(String msg) {
    	System.out.println("Pr #" + pid + ": " + msg);
    }

	@Override
    public void run() {
    	
    	Connection jmsConnection = null;
    	
    	try {
	   		Context jndiContext = new InitialContext();
	        ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup("jms/easyhome/ConnectionFactory");
	        
	        Topic inboundTopic = (Topic) jndiContext.lookup("jms/easyhome/InboundPacketsTopic");
	        Topic outboundTopic = (Topic) jndiContext.lookup("jms/easyhome/OutboundPacketsTopic");
	        
	        jmsConnection = connectionFactory.createConnection();
	        javax.jms.Session jmsSession = jmsConnection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
	        
	        MessageConsumer consumer = jmsSession.createConsumer(inboundTopic);  
	        MessageProducer producer = jmsSession.createProducer(outboundTopic);
	    	
	        jmsConnection.start();
	        
	        println("processing started");
	        
	    	while (!isStopped()) 
	    		process(consumer,producer);
	    	
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
