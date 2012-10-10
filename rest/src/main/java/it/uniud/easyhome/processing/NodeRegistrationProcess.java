package it.uniud.easyhome.processing;

import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.packets.NativePacket;
import it.uniud.easyhome.packets.Operation;
import it.uniud.easyhome.packets.specific.NodeAnnouncePacket;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.ws.rs.core.UriInfo;

public class NodeRegistrationProcess extends Process {
	
	private static long RECEPTION_WAIT_TIME_MS = 5000;	
	
	// Necessary for calls to REST resources
	private UriInfo uriInfo;
	
    public NodeRegistrationProcess(int pid, UriInfo uriInfo) {
        super(pid, Session.STATEFUL, Interaction.ASYNC);
        this.uriInfo = uriInfo;
    }
    
    @Override
    public void start() {
        Thread thr = new Thread(this);
        thr.start();
    }
    
    @Override
    final protected void process(MessageConsumer consumer, MessageProducer producer) throws JMSException {
    	
    	ObjectMessage msg = (ObjectMessage) consumer.receive(RECEPTION_WAIT_TIME_MS);
    	if (msg != null) {
        	NativePacket pkt = (NativePacket) msg.getObject();
        	println("Packet received from " + pkt.getSrcCoords());
        	
        	try {
        		NodeAnnouncePacket announce = new NodeAnnouncePacket(pkt);
        		
        		
        	} catch (InvalidPacketTypeException ex) {
        		return;
        	}
        	
        	
    	}
    }
    
}