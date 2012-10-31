package it.uniud.easyhome.processing;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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

public abstract class Process implements Runnable {
	
	public final static int MESSAGE_WAIT_TIME_MS = 500;
	
    private int pid;
    private final ProcessKind kind;
    
    private volatile boolean stopped = false;
    
    private Client restClient;
    
    protected WebResource restResource;
    
    private List<MessageConsumer> consumers;
    private List<MessageProducer> producers;
    
	private Connection jmsConnection = null;
	protected Context jndiContext = null;
	protected Session jmsSession = null;
    
    protected Process(int pid, URI restTarget, ProcessKind kind) throws NamingException, JMSException {
        this.pid = pid;
        this.kind = kind;
        this.restClient = Client.create(new DefaultClientConfig());
        this.restResource = restClient.resource(restTarget);
        
    	consumers = new ArrayList<MessageConsumer>();
    	producers = new ArrayList<MessageProducer>();
    	
   		jndiContext = new InitialContext();
        ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup("jms/easyhome/ConnectionFactory");
        
        Topic inboundPacketsTopic = (Topic) jndiContext.lookup("jms/easyhome/InboundPacketsTopic");
        Topic outboundPacketsTopic = (Topic) jndiContext.lookup("jms/easyhome/OutboundPacketsTopic");
        
        jmsConnection = connectionFactory.createConnection();
        jmsSession = jmsConnection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

        consumers.add(jmsSession.createConsumer(inboundPacketsTopic));  
        producers.add(jmsSession.createProducer(outboundPacketsTopic));
        
        jmsConnection.start();
    }
    
    protected abstract void process() throws JMSException, NamingException;
    
    protected MessageConsumer getInboundPacketsConsumer() {
    	return consumers.get(0);
    }
    
    protected MessageProducer getOutboundPacketsProducer() {
    	return producers.get(0);
    }
    
    protected void registerConsumer(MessageConsumer consumer) {
    	consumers.add(consumer);
    }

    protected void registerProducer(MessageProducer producer) {
    	producers.add(producer);
    }
    
    public final int getPid() {
        return pid;
    }
	
    public ProcessKind getKind() {
    	return kind;
    }
    
	protected boolean isStopped() {
		return stopped;
	}

    public void start() {
    	if (!stopped) {
	        Thread thr = new Thread(this);
	        thr.start();
    	}
    }
	
	public void stop() {
		stopped = true;
	}
	
	protected void println(String msg) {
    	System.out.println("Pr #" + pid + ": " + msg);
    }

	@Override
    public final void run() {
    	
    	try {
	        
	        println(getKind().toString() + " processing started");
	        
	    	while (!isStopped()) 
	    		process();
	    	
    	} catch (NamingException ex) {
    		ex.printStackTrace();
    	} catch (JMSException ex) {
    		// Here we also consider the case of termination via consumer closing
    	} finally {
    		try {
    		
	    		if (jmsConnection != null) {
	    			jmsConnection.close();
	    		}
    		} catch (JMSException ex) {
    			// Can't do better than this, hence we ignore the error
    		}
    		
    		restClient.destroy();
    	}
    	
    	println("Processing stopped");
    }
}
