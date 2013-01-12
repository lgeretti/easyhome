package it.uniud.easyhome.processing;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.RunnableState;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
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
	
	// The time between jobs checking, when a particular job type is not resolved
	protected static long JOB_POLLING_TIME_MILLIS = 1000;
	// The time after the job should be reissued
	protected static long JOB_TIMEOUT_MILLIS = 5000;
	
    private int pid;
    private final ProcessKind kind;
    
    private volatile RunnableState runState = RunnableState.STOPPED;
    
    private Client restClient;
    
    protected WebResource restResource;
    
    private Map<Topic,MessageConsumer> consumers;
    private Map<Topic,MessageProducer> producers;
    
    private Topic inboundPacketsTopic;
    private Topic outboundPacketsTopic;
    
	private Connection jmsConnection = null;
	protected Context jndiContext = null;
	protected Session jmsSession = null;
    
    protected Process(int pid, URI restTarget, ProcessKind kind) throws NamingException, JMSException {
        this.pid = pid;
        this.kind = kind;
        this.restClient = Client.create(new DefaultClientConfig());
        this.restResource = restClient.resource(restTarget);
        
    	consumers = new HashMap<Topic,MessageConsumer>();
    	producers = new HashMap<Topic,MessageProducer>();
    	
   		jndiContext = new InitialContext();
        ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup(JMSConstants.CONNECTION_FACTORY);
        
        inboundPacketsTopic = (Topic) jndiContext.lookup(JMSConstants.INBOUND_PACKETS_TOPIC);
        outboundPacketsTopic = (Topic) jndiContext.lookup(JMSConstants.OUTBOUND_PACKETS_TOPIC);
        
        jmsConnection = connectionFactory.createConnection();
        jmsSession = jmsConnection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

        registerConsumerFor(inboundPacketsTopic);  
        registerProducerFor(outboundPacketsTopic);
        
        jmsConnection.start();
    }
    
    protected abstract void process() throws JMSException, NamingException;
    
    protected MessageConsumer getInboundPacketsConsumer() {
    	return consumers.get(inboundPacketsTopic);
    }
    
    protected MessageProducer getOutboundPacketsProducer() {
    	return producers.get(outboundPacketsTopic);
    }
    
    protected MessageConsumer registerConsumerFor(Topic topic) throws JMSException {
    	MessageConsumer consumer = jmsSession.createConsumer(topic);
    	consumers.put(topic,consumer);
    	return consumer;
    }

    protected MessageProducer registerProducerFor(Topic topic) throws JMSException {
    	MessageProducer producer = jmsSession.createProducer(topic);
    	producers.put(topic,producer);
    	return producer;
    }
    
    public final int getPid() {
        return pid;
    }
	
    public ProcessKind getKind() {
    	return kind;
    }
    
	protected boolean isStopped() {
		return runState == RunnableState.STOPPED;
	}

    public void start() {
    	if (runState == RunnableState.STOPPED) {
    		runState = RunnableState.STARTING;
	        Thread thr = new Thread(this);
	        thr.start();
    	}
    }
	
	public void stop() {
		
		runState = RunnableState.STOPPING;
		sendNullMessageToConsumers();
	}
	
	protected void println(String msg) {
    	System.out.println("Pr #" + pid + ": " + msg);
    }
	
	private void sendNullMessageToConsumers() {
		try {
			// Under the assumption that the "regular" session is blocked, we need a new session 
			Session newSession = jmsConnection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
			ObjectMessage nullMessage = newSession.createObjectMessage(null);
			for (Entry<Topic,MessageConsumer> consumer : consumers.entrySet()) {
				MessageProducer prod = newSession.createProducer(consumer.getKey());
				prod.send(nullMessage);
			}
	        
		} catch (JMSException ex) {
			// Can't do anything anyway, so we swallow the error
		} /*catch (NamingException e) {
		}*/
	}

	@Override
    public final void run() {
    	
    	try {
    		
    		runState = RunnableState.STARTED;
	        println(getKind().toString() + " started");
	        
	    	while (runState != RunnableState.STOPPING) 
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
    			// Can't do better than this, hence we ignore the error. 
    			// An exception may happen if we manage to close before issuing the null messages    		
    		}
    		
    		restClient.destroy();
    	}
    	
    	runState = RunnableState.STOPPED;
    	println(getKind().toString() + " stopped");
    }
}
