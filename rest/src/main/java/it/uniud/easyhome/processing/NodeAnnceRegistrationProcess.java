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

public class NodeAnnceRegistrationProcess extends Process {
	
	private static long RECEPTION_WAIT_TIME_MS = 1000;	
	
    public NodeAnnceRegistrationProcess(int pid, UriInfo uriInfo) {
        super(pid, UriBuilder.fromUri(uriInfo.getBaseUri()).build(new Object[0]));
    }
    
    @Override
    public ProcessKind getKind() {
    	return ProcessKind.NODE_ANNCE_REGISTRATION;
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
        MessageProducer networkEventsProducer = session.createProducer(networkEventsTopic);

    	ObjectMessage msg = (ObjectMessage) inboundPacketsConsumer.receive(RECEPTION_WAIT_TIME_MS);
    	if (msg != null) {
        	NativePacket pkt = (NativePacket) msg.getObject();
        	println("Packet received from " + pkt.getSrcCoords());
        	
        	try {
        		NodeAnnouncePacket announce = new NodeAnnouncePacket(pkt);
        		
        		long nuid = announce.getAnnouncedNuid();
        		byte gatewayId = announce.getSrcCoords().getGatewayId();
        		short address = announce.getAnnouncedAddress();
        		byte capability = announce.getAnnouncedCapability();
        		
                Node.Builder nodeBuilder = new Node.Builder(nuid);
                Node node = nodeBuilder.setGatewayId(gatewayId)
                					   .setAddress(address)
                					   .setCapability(capability)
                					   .build();
                
                ClientResponse response = restResource.path("network")
                		.type(MediaType.APPLICATION_JSON).post(ClientResponse.class,node);
                
                if (response.getClientResponseStatus() == Status.CREATED) {
                	
                	NetworkEvent event = new NetworkEvent(NetworkEvent.EventKind.NODE_ADDED, gatewayId, nuid);
                    try {
                        ObjectMessage eventMessage = session.createObjectMessage(event);
                        networkEventsProducer.send(eventMessage);
                        println("Node announcement registered and event dispatched");
                    } catch (Exception e) {
                    	println("Message could not be dispatched to inbound packets topic");
                    }
                	
                } else if (response.getClientResponseStatus() != Status.OK)
                	println("Node announcement registration failed");
                
        	} catch (InvalidPacketTypeException ex) {
        		ex.printStackTrace();
        		return;
        	}
    	}
    	
    	networkEventsProducer.close();
    }
    
}