package it.uniud.easyhome.processing;

import java.net.URI;

import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.NativePacket;
import it.uniud.easyhome.packets.Operation;
import it.uniud.easyhome.packets.specific.NodeAnnouncePacket;
import it.uniud.easyhome.rest.JsonJaxbContextResolver;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jettison.json.JSONObject;

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
    	
    	URI target = UriBuilder.fromUri(uriInfo.getBaseUri()).path("network").build(new Object[0]);
    	
    	ObjectMessage msg = (ObjectMessage) consumer.receive(RECEPTION_WAIT_TIME_MS);
    	if (msg != null) {
        	NativePacket pkt = (NativePacket) msg.getObject();
        	println("Packet received from " + pkt.getSrcCoords());
        	
        	try {
        		NodeAnnouncePacket announce = new NodeAnnouncePacket(pkt);
        		
        		long nuid = announce.getAnnouncedNuid();
        		short address = announce.getAnnouncedAddress();
        		byte gatewayId = announce.getSrcCoords().getGatewayId();
        		
                Node.Builder nodeBuilder = new Node.Builder(nuid);
                Node node = nodeBuilder.setName(Long.toHexString(nuid))
                					   .setAddress(address)
                					   .setGatewayId(gatewayId).build();
                
        	} catch (InvalidPacketTypeException ex) {
        		return;
        	}
        	
        	
    	}
    }
    
}