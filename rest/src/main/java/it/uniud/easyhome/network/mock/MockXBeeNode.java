package it.uniud.easyhome.network.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import it.uniud.easyhome.common.*;
import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.devices.HomeAutomationDevice;
import it.uniud.easyhome.devices.Manufacturer;
import it.uniud.easyhome.network.LocalCoordinates;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.GlobalCoordinates;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.xbee.*;
import it.uniud.easyhome.packets.xbee.mock.*;

public class MockXBeeNode implements Runnable {

    private Node node;
    
    private MockXBeeNetwork network;
    
    private Queue<XBeePacketToNode> inboundPacketQueue = new ConcurrentLinkedQueue<XBeePacketToNode>();
    
    private volatile RunnableState runningState = RunnableState.STOPPED;
    
    private static long LOOP_WAIT_TIME_MS = 500;
    
    private byte seqNumber = 0;
    
    MockXBeeNode(Node node, MockXBeeNetwork network) throws InvalidMockNodeException {
    	if (node.getLogicalType() == NodeLogicalType.UNDEFINED)
    		throw new InvalidMockNodeException();
    	if (node.getManufacturer() == Manufacturer.UNDEFINED)
    		throw new InvalidMockNodeException();
    	this.node = node;
    	this.network = network;
    }
    
    public GlobalCoordinates getCoordinates() {
        return node.getCoordinates();
    }
    
    public String getName() {
        return node.getName();
    }
    
    public NodeLogicalType getLogicalType() {
    	return node.getLogicalType();
    }
    
    public Manufacturer getManufacturer() {
    	return node.getManufacturer();
    }
    
    public List<MockXBeeNode> getNeighbors() throws MockXBeeNodeNotFoundException {
    	
    	List<MockXBeeNode> neighbors = new ArrayList<MockXBeeNode>();
    	
    	for (LocalCoordinates neighborCoords : node.getNeighbors()) {
    		
    		MockXBeeNode recoveredNode = getMockXBeeNode(neighborCoords);
    		
    		if (recoveredNode == null)
    			throw new MockXBeeNodeNotFoundException();
    		
    		neighbors.add(recoveredNode);
    	}
    	
    	return neighbors;
    }
    
    public List<Byte> getEndpoints() {
    	return node.getEndpoints();
    }
    
    public Map<Byte,HomeAutomationDevice> getDevices() {
    	return node.getMappedDevices();
    }
    
    private MockXBeeNode getMockXBeeNode(LocalCoordinates neighborCoords) {
    	
    	for (MockXBeeNode node : network.getNodes()) {
    		if (node.getCoordinates().getAddress() == neighborCoords.getAddress())
    			return node;
    	}
    	return null;
    }
    
    public void receive(XBeePacketToNode pkt) {
    	inboundPacketQueue.add(pkt);
    }
    
    public synchronized void transmit(XBeePacketFromNode pkt) {
    	network.broadcast(new XBeePacketToNode(pkt,node.getCoordinates().getNuid(),node.getCoordinates().getAddress()));
    }    
    
    public void turnOn() {
    	if (runningState == RunnableState.STOPPED) {
    		runningState = RunnableState.STARTING;
    		Thread thr = new Thread(this);
    		thr.start();
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
    	
    	runningState = RunnableState.STARTED;
    	transmit(new DeviceAnnounceOutpkt(this));
    	
    	try {
	    	while(runningState != RunnableState.STOPPING) {
	    		XBeePacketToNode pkt = inboundPacketQueue.poll();
	    		if (pkt != null)
	    			loopRoutine(pkt);
	    		Thread.sleep(LOOP_WAIT_TIME_MS);
	    	}
    	} catch (InterruptedException ex) {
    		
    	}
    	
		runningState = RunnableState.STOPPED;
    }
    
    private void loopRoutine(XBeePacketToNode pkt) {

    	if (pkt.getProfileId() == Domain.MANAGEMENT.getCode()) {
			if (pkt.getClusterId() == ManagementContext.NODE_DESC_REQ.getCode()) {
				short nwkAddress = ByteUtils.getShort(pkt.getApsPayload(),0,Endianness.LITTLE_ENDIAN);
				if (nwkAddress == node.getCoordinates().getAddress()) {
					try {
						transmit(new NodeDescrRspOutpkt(this,pkt.getTransactionSeqNumber()));
					} catch (InvalidMockNodeException e) {
						e.printStackTrace();
						runningState = RunnableState.STOPPING;
					}
				}
			}      
			else if (pkt.getClusterId() == ManagementContext.NODE_NEIGH_REQ.getCode()) {
				try {
					transmit(new NodeLQIRspOutpkt(this,pkt.getTransactionSeqNumber()));
				} catch (InvalidMockNodeException | MockXBeeNodeNotFoundException e) {
					e.printStackTrace();
					runningState = RunnableState.STOPPING;
				}
			}
			else if (pkt.getClusterId() == ManagementContext.ACTIVE_EP_REQ.getCode()) {
				try {
					transmit(new ActiveEpRspOutpkt(this,pkt.getTransactionSeqNumber()));
				} catch (InvalidMockNodeException | MockXBeeNodeNotFoundException e) {
					e.printStackTrace();
					runningState = RunnableState.STOPPING;
				}
			} 
			else if (pkt.getClusterId() == ManagementContext.SIMPLE_DESC_REQ.getCode()) {
				try {
					byte endpoint = pkt.getApsPayload()[2];
					transmit(new SimpleDescRspOutpkt(this,endpoint,pkt.getTransactionSeqNumber()));
				} catch (InvalidMockNodeException | MockXBeeNodeNotFoundException e) {
					e.printStackTrace();
					runningState = RunnableState.STOPPING;
				}
			}
    	}	
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
