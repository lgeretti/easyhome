package it.uniud.easyhome.processing;

import java.net.URI;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;

@XmlRootElement
public class Process implements Runnable {
    
    @XmlElement(name="pid")
    private int pid;
    
    private volatile boolean stopped = false;
    
    // REST client
    private Client restClient;
    
    protected WebResource restResource;
    
    @SuppressWarnings("unused")
    private Process() {}
    
    protected Process(int pid, URI restTarget) {
        this.pid = pid;
        this.restClient = Client.create(new DefaultClientConfig());
        this.restResource = restClient.resource(restTarget);
    }
    
    public final int getPid() {
        return pid;
    }
	
    public ProcessKind getKind() {
    	return null;
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
	
	protected void process(MessageConsumer inboundPacketsConsumer, MessageProducer outboundPacketsProducer,
						   Context context, javax.jms.Session session) throws JMSException, NamingException {
		// Empty implementation to be overridden
	}
	
	protected void println(String msg) {
    	System.out.println("Pr #" + pid + ": " + msg);
    }

	@Override
    public void run() {
    	
    	Connection jmsConnection = null;
    	Session jmsSession = null;
    	MessageConsumer inboundPacketsConsumer = null;
    	MessageProducer outboundPacketsProducer = null;
    	
    	try {
	   		Context jndiContext = new InitialContext();
	        ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup("jms/easyhome/ConnectionFactory");
	        
	        Topic inboundPacketsTopic = (Topic) jndiContext.lookup("jms/easyhome/InboundPacketsTopic");
	        Topic outboundPacketsTopic = (Topic) jndiContext.lookup("jms/easyhome/OutboundPacketsTopic");
	        
	        jmsConnection = connectionFactory.createConnection();
	        jmsSession = jmsConnection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

	        inboundPacketsConsumer = jmsSession.createConsumer(inboundPacketsTopic);  
	        outboundPacketsProducer = jmsSession.createProducer(outboundPacketsTopic);
	    	
	        jmsConnection.start();
	        
	        println(getKind().toString() + " processing started");
	        
	    	while (!isStopped()) 
	    		process(inboundPacketsConsumer,outboundPacketsProducer,jndiContext,jmsSession);
	    	
    	} catch (NamingException ex) {
    		ex.printStackTrace();
    	} catch (JMSException ex) {
    		ex.printStackTrace();
    	} finally {
    		try {
    		
	    		if (jmsConnection != null) {
	    			inboundPacketsConsumer.close();
	    			outboundPacketsProducer.close();
	    			jmsSession.close();
	    			jmsConnection.close();
	    		}
    		} catch (JMSException ex) {
    			// Can't do better than this, hence we ignore the error
    		} finally {
    			println("JMS connection stopped");
    		}
    		
    		restClient.destroy();
    	}
    	
    	println("Processing stopped");
    }
}
