package it.uniud.easyhome.processing;

import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.natives.NativePacket;
import it.uniud.easyhome.packets.natives.NodeAnnouncePacket;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;

public class NodeDescrAcquirementProcess extends Process {
	
	private static long RECEPTION_WAIT_TIME_MS = 1000;	
	
    public NodeDescrAcquirementProcess(int pid, UriInfo uriInfo) {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]));
    }
    
    @Override
    public ProcessKind getKind() {
    	return ProcessKind.NODE_DESCR_ACQUIREMENT;
    }
    
    @Override
    public void start() {
        Thread thr = new Thread(this);
        thr.start();
    }
    
    @Override
	protected void process(MessageConsumer inboundPacketsConsumer, MessageProducer outboundPacketsProducer,
			   			   Context context, Session session) throws JMSException, NamingException {
    	
        Topic networkEventsTopic = (Topic) context.lookup("jms/easyhome/NetworkEventsTopic");
        MessageConsumer networkEventsConsumer = session.createConsumer(networkEventsTopic);

    	ObjectMessage msg = (ObjectMessage) networkEventsConsumer.receive(RECEPTION_WAIT_TIME_MS);
    	if (msg != null) {
    		NetworkEvent event = (NetworkEvent) msg.getObject();
    		if (event.getKind() == NetworkEvent.EventKind.NODE_ADDED)
    			println("New node event received for gid " + event.getGid() + " and node id " + event.getNuid());
       	}
    	
    	networkEventsConsumer.close();
    }
    
}