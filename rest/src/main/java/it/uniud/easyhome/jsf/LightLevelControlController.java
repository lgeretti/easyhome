package it.uniud.easyhome.jsf;

import java.util.List;

import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.NodeEJB;
import it.uniud.easyhome.network.Node;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.icefaces.application.PortableRenderer;
import org.icefaces.application.PushRenderer;

@ManagedBean
@SessionScoped 
public class LightLevelControlController implements Runnable {
    
    private static final String PUSH_GROUP = "lightLevel";
    
    private static final long EVENT_WAIT_TIME_MILLIS = 500;
    
    private volatile boolean stopped;
    
    private PortableRenderer pRenderer;
    
    private int change;
    
    private int counter;
	
    @PostConstruct
    public void init() {
        PushRenderer.addCurrentView(PUSH_GROUP);
        Thread thr = new Thread(this);
        thr.start();
        pRenderer = PushRenderer.getPortableRenderer();
    }
    
    public int getChange() {
    	return change;
    }
    
    public int getCounter() {
    	return counter;
    }

	@Override
	public void run() {
		
		Connection jmsConnection = null;
		Session jmsSession = null;
		
		try {
			
	   		Context jndiContext = new InitialContext();
	        ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup(JMSConstants.CONNECTION_FACTORY);
	        
	        jmsConnection = connectionFactory.createConnection();
	        jmsSession = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	        
	        jmsConnection.start();
	        
	        Topic networkEventsTopic = (Topic) jndiContext.lookup(JMSConstants.NETWORK_EVENTS_TOPIC);
	    	MessageConsumer networkEventsConsumer = jmsSession.createConsumer(networkEventsTopic);
			
			while(!stopped) {

				ObjectMessage msg = (ObjectMessage) networkEventsConsumer.receive(EVENT_WAIT_TIME_MILLIS);
		    	if (msg != null) {
		    		
	        		NetworkEvent event = (NetworkEvent) msg.getObject();
	        		if (event != null && event.getKind() == NetworkEvent.EventKind.LEVEL_CONTROL_VARIATION) {
	        			change = event.getPayload()[0];
	        			counter++;
	        			pRenderer.render(PUSH_GROUP);	
	        		} else if (event == null){
	        			stopped = true;
	        		}
		       	} 
			}	
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (jmsConnection != null)
					jmsConnection.stop();
			} catch (JMSException e) {
			}
		}
		
	}
	
	public void doStop() {
		stopped = true;
	}

}   