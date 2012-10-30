package it.uniud.easyhome.network.mock;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import it.uniud.easyhome.common.RunnableState;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.packets.ManagementContexts;
import it.uniud.easyhome.packets.Packet;
import it.uniud.easyhome.packets.xbee.DeviceAnnounceOutpkt;
import it.uniud.easyhome.packets.xbee.NodeDescrRspOutpkt;
import it.uniud.easyhome.packets.xbee.XBeeInboundPacket;
import it.uniud.easyhome.packets.xbee.XBeeOutboundPacket;

public class MockXBeeNode implements Runnable {

    private Node node;
    
    private MockXBeeNetwork network;
    
    private Queue<XBeeInboundPacket> inboundPacketQueue;
    
    private volatile RunnableState runningState;
    
    private byte seqNumber = 0;
    
    MockXBeeNode(Node node, MockXBeeNetwork network) throws InvalidMockNodeException {
    	if (node.getLogicalType() == NodeLogicalType.UNDEFINED)
    		throw new InvalidMockNodeException();
    	this.node = node;
    	this.network = network;
    	this.runningState = RunnableState.STOPPED;
    	inboundPacketQueue = new ConcurrentLinkedQueue<XBeeInboundPacket>();
    }
    
    public long getId() {
        return node.getId();
    }
    
    public String getName() {
        return node.getName();
    }
    
    public byte getGatewayId() {
        return node.getGatewayId();
    }
    
    public short getAddress() {
        return node.getAddress();
    }
    
    public byte getCapability() {
    	return node.getCapability();
    }
    
    public NodeLogicalType getLogicalType() {
    	return node.getLogicalType();
    }
    
    public void receive(XBeeInboundPacket pkt) {
    	inboundPacketQueue.add(pkt);
    }
    
    public void transmit(XBeeOutboundPacket pkt) {
    	network.broadcast(new XBeeInboundPacket(pkt,node.getId(),node.getAddress()));
    }    
    
    public void turnOn() {
    	
    	if (runningState == RunnableState.STOPPED) {
    		Thread thr = new Thread(this);
    		thr.start();
    		runningState = RunnableState.RUNNING;
    	} else {
    		throw new IllegalStateException();
    	}
    }
    
    public void turnOff() {
    	runningState = RunnableState.STOPPING;
    }
    
    public byte nextSeqNumber() {
    	return ++seqNumber;
    }
    
    @Override
    public void run() {
    	
    	transmit(new DeviceAnnounceOutpkt(this));
    	
    	while(runningState != RunnableState.STOPPING) {
        	while(runningState != RunnableState.STOPPING) { 
        		XBeeInboundPacket pkt = inboundPacketQueue.poll();
        		if (pkt != null && pkt.getClusterId() == ManagementContexts.NODE_DESC_REQ.getCode()) { 
        			short nwkAddress = (short) ((((short)(pkt.getApsPayload()[0] & 0xFF)) << 8) + (pkt.getApsPayload()[1] & 0xFF));
        			if (nwkAddress == node.getAddress()) {
        				try {
							transmit(new NodeDescrRspOutpkt(this));
						} catch (InvalidMockNodeException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							runningState = RunnableState.STOPPING;
						}
        			}
        		}
        	}
    	}
    	
		runningState = RunnableState.STOPPED;
    }

    @Override
    public boolean equals(Object other) {
        
        if (!(other instanceof MockXBeeNode))
            throw new IllegalArgumentException();
        MockXBeeNode otherNode = (MockXBeeNode) other;
        
        return node.equals(otherNode.node);
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;

        long result = 1;
        result = prime * result + node.hashCode();
        
        return (int)result;
    }
}
