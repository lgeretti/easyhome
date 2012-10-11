package it.uniud.easyhome.processing;

import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.NativePacket;
import it.uniud.easyhome.packets.specific.NodeAnnouncePacket;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;

public class NodeRegistrationProcess extends Process {
	
	private static long RECEPTION_WAIT_TIME_MS = 1000;	
	
    public NodeRegistrationProcess(int pid, UriInfo uriInfo) {
        super(pid, Session.STATEFUL, Interaction.ASYNC,
        		UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]));
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
        		
        		long nuid = announce.getAnnouncedNuid();
        		short address = announce.getAnnouncedAddress();
        		byte gatewayId = announce.getSrcCoords().getGatewayId();
        		
                Node.Builder nodeBuilder = new Node.Builder(nuid);
                Node node = nodeBuilder.setName(Long.toHexString(nuid))
                					   .setAddress(address)
                					   .setGatewayId(gatewayId).build();
                
                ClientResponse response = restResource.path("network")
                		.type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node);
                
                if (response.getClientResponseStatus() == Status.CREATED)
                	println("Node announcement registered");
                else if (response.getClientResponseStatus() == Status.OK)
                	println("Node announcement re-registered");
                else
                	println("Node announcement registration failed");
                
        	} catch (InvalidPacketTypeException ex) {
        		return;
        	}
    	}
    }
    
}