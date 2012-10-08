package it.uniud.easyhome.processing;

import it.uniud.easyhome.network.EHPacket;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;

public class NodeRegistrationProcess extends Process implements Runnable {
	
	private static long RECEPTION_WAIT_TIME_MS = 5000;
	
    public NodeRegistrationProcess(int pid) {
        super(pid, Session.STATEFUL, Interaction.ASYNC);
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
        	EHPacket pkt = (EHPacket) msg.getObject();
        	println("Packet received from " + pkt.getSrcCoords());
    	}    	
    }
}
