package it.uniud.easyhome.gateway;

import it.uniud.easyhome.network.NativePacket;
import it.uniud.easyhome.network.ModuleCoordinates;
import it.uniud.easyhome.network.Operation;
import it.uniud.easyhome.network.xbee.XBeeReceivedPacket;
import it.uniud.easyhome.network.xbee.XBeeTransmittedPacket;
import it.uniud.easyhome.network.exceptions.IllegalBroadcastPortException;
import it.uniud.easyhome.network.exceptions.RoutingEntryMissingException;

import java.net.*;
import java.io.*;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;

public class XBeeGateway extends Gateway {
    
    public XBeeGateway(byte id, int port) {
    	super(id,ProtocolType.XBEE,port);
    }
    
    /**
     * Converts an XBee API received packet, starting from the 64 bit source network address forth,
     * checksum excluded.
     */
    private NativePacket convertFrom(XBeeReceivedPacket xpkt) throws RoutingEntryMissingException {
        
        ModuleCoordinates srcCoords = new ModuleCoordinates(
        		id,xpkt.get64BitSrcAddr(),xpkt.get16BitSrcAddr(),xpkt.getSrcEndpoint());
        
        byte receiveOptions = xpkt.getReceiveOptions();
        byte dstEndpoint = xpkt.getDstEndpoint();
        
        ModuleCoordinates dstCoords = null;
        
        // If a broadcast, we use the broadcast format for the destination coordinates, but only
        // if the destination port is actually the administration port
        if (receiveOptions == 0x02) {
        	if (dstEndpoint == 0x00) {        		
	        	dstCoords = new ModuleCoordinates((byte)0,(short)0xFFFF,(short)0xFFFE,(byte)0);
	        	println("Setting destination as broadcast");
        	} else {
        		throw new IllegalBroadcastPortException();
        	}
        } else {
	        
	        dstCoords = getCoordinatesFor(dstEndpoint);
	        
	        if (dstCoords == null)
	            throw new RoutingEntryMissingException();
	        
	        println("Retrieved coordinates for mapped endpoint " + dstEndpoint);
	    }
        
        Operation op = new Operation(xpkt.getTransactionSeqNumber(),xpkt.getProfileId(),
        		xpkt.getClusterId(),xpkt.getFrameControl(),xpkt.getCommand(),xpkt.getApsPayload());
        
        return new NativePacket(srcCoords,dstCoords,op);
    }
    
    /**
     * Dispatches the packet to the processes and the gateways
     */
    private void dispatchPacket(NativePacket pkt, Session jmsSession, MessageProducer inboundProducer, MessageProducer outboundProducer) {
  
        try {
            ObjectMessage inboundMessage = jmsSession.createObjectMessage(pkt);
            inboundProducer.send(inboundMessage);
            println("Message dispatched to inbound packets topic");
        } catch (Exception e) {
        	println("Message not dispatched to inbound packets topic");
        }

        try {
            ObjectMessage outboundMessage = jmsSession.createObjectMessage(pkt);
            outboundProducer.send(outboundMessage);
            println("Message dispatched to outbound packets topic");            	
        } catch (Exception e) {
        	println("Message could not be dispatched to outbound packets topic");
        }
    }
    
    private void handleInboundPacketFrom(InputStream in, Session jmsSession,
    		MessageProducer inboundProducer, MessageProducer outboundProducer) throws IOException {
        
        println("Recognized XBee packet");
        
        try {
        	
        	XBeeReceivedPacket xbeePkt = new XBeeReceivedPacket();
        	xbeePkt.read(in);
        	NativePacket ehPkt = convertFrom(xbeePkt);
        	dispatchPacket(ehPkt,jmsSession,inboundProducer,outboundProducer);
        	
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private void handleOutboundPacketsTo(OutputStream os, MessageConsumer consumer) {
    	
        try {
            while (true) {
            	ObjectMessage msg = (ObjectMessage) consumer.receiveNoWait();
                if (msg == null) {
                	break;
                }
            	NativePacket ehPkt = (NativePacket) msg.getObject();
            	if (ehPkt.getDstCoords().getGatewayId() == id) {
            		println("Packet received from " + ehPkt.getSrcCoords());
            		XBeeTransmittedPacket xbeePkt = new XBeeTransmittedPacket(ehPkt);
            		xbeePkt.write(os);
            	}
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    	
    }
    
    @Override
    public void run() {
        
        try {
          server = new ServerSocket(port, 1);
          println("Gateway opened on port " + server.getLocalPort());

          while (true) {
            
            Socket skt = server.accept();
            Connection jmsConnection = null;
            try {
                println("Connection established with " + skt);
                
                disconnected = false;
                
                InputStream istream = new BufferedInputStream(skt.getInputStream());
                BufferedOutputStream ostream = new BufferedOutputStream(skt.getOutputStream());
                
    	   		Context jndiContext = new InitialContext();
    	        ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup("jms/easyhome/ConnectionFactory");
    	        
                Topic outboundTopic = (Topic) jndiContext.lookup("jms/easyhome/OutboundPacketsTopic");
                Topic inboundTopic = (Topic) jndiContext.lookup("jms/easyhome/InboundPacketsTopic");
                
    	        jmsConnection = connectionFactory.createConnection();
    	        Session jmsSession = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                
                MessageConsumer outboundConsumer = jmsSession.createConsumer(outboundTopic);
                MessageProducer inboundProducer = jmsSession.createProducer(inboundTopic);
                MessageProducer outboundProducer = jmsSession.createProducer(outboundTopic);
                
                jmsConnection.start();

                while (!disconnected) {
                    
                	if (istream.available() > 0) {
	                    handleInboundPacketFrom(istream,jmsSession,inboundProducer,outboundProducer);
                	}
                    
                    handleOutboundPacketsTo(ostream,outboundConsumer);	                    
                }
            
            } catch (Exception ex) {
              System.out.println(ex);
            } finally {
              try {
            	  if (skt != null) skt.close();
              } catch (IOException ex) {
          		// Whatever the case, the connection is not available anymore
              } finally {
            	  println("Connection with " + skt + " closed");  
              }
              
        	  try {
        		  jmsConnection.close();
        	  } catch (JMSException jmsEx) {
        		// Whatever the case, the connection is not available anymore  
        	  } finally {
        		  println("JMS connection closed");
        	  }
            }
          }
        } catch (Exception ex) {
            if (ex instanceof SocketException)
            	println("Gateway cannot accept connections anymore");
            else
            	println("Gateway could not be opened");
        }
    }

    @Override
    public void open() {

        Thread thr = new Thread(this);
        thr.start();
    }
  
}