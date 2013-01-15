package it.uniud.easyhome.network.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.common.RunnableState;
import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.devices.HomeAutomationDevice;
import it.uniud.easyhome.network.Manufacturer;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.Packet;
import it.uniud.easyhome.packets.xbee.XBeePacketToNode;
import it.uniud.easyhome.packets.xbee.XBeePacketFromNode;
import it.uniud.easyhome.packets.xbee.mock.ActiveEpRspOutpkt;
import it.uniud.easyhome.packets.xbee.mock.DeviceAnnounceOutpkt;
import it.uniud.easyhome.packets.xbee.mock.NodeDescrRspOutpkt;
import it.uniud.easyhome.packets.xbee.mock.NodeLQIRspOutpkt;
import it.uniud.easyhome.packets.xbee.mock.SimpleDescRspOutpkt;

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
    
    public long getId() {
        return node.getNuid();
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
    
    public Manufacturer getManufacturer() {
    	return node.getManufacturer();
    }
    
    public List<MockXBeeNode> getNeighbors() throws MockXBeeNodeNotFoundException {
    	
    	List<MockXBeeNode> neighbors = new ArrayList<MockXBeeNode>();
    	
    	for (Long neighborId : node.getNeighborIds()) {
    		
    		MockXBeeNode recoveredNode = getMockXBeeNode(neighborId);
    		
    		if (recoveredNode == null)
    			throw new MockXBeeNodeNotFoundException();
    		
    		neighbors.add(recoveredNode);
    	}
    	
    	return neighbors;
    }
    
    public List<Short> getEndpoints() {
    	return node.getEndpoints();
    }
    
    public Map<Short,HomeAutomationDevice> getDevices() {
    	return node.getMappedDevices();
    }
    
    private MockXBeeNode getMockXBeeNode(long id) {
    	
    	for (MockXBeeNode node : network.getNodes()) {
    		if (node.getId() == id)
    			return node;
    	}
    	return null;
    }
    
    public void receive(XBeePacketToNode pkt) {
    	inboundPacketQueue.add(pkt);
    }
    
    public synchronized void transmit(XBeePacketFromNode pkt) {
    	network.broadcast(new XBeePacketToNode(pkt,node.getNuid(),node.getAddress()));
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
				if (nwkAddress == node.getAddress()) {
					try {
						transmit(new NodeDescrRspOutpkt(this));
					} catch (InvalidMockNodeException e) {
						e.printStackTrace();
						runningState = RunnableState.STOPPING;
					}
				}
			}      
			else if (pkt.getClusterId() == ManagementContext.NODE_NEIGH_REQ.getCode()) {
				try {
					transmit(new NodeLQIRspOutpkt(this));
				} catch (InvalidMockNodeException | MockXBeeNodeNotFoundException e) {
					e.printStackTrace();
					runningState = RunnableState.STOPPING;
				}
			}
			else if (pkt.getClusterId() == ManagementContext.ACTIVE_EP_REQ.getCode()) {
				try {
					transmit(new ActiveEpRspOutpkt(this));
				} catch (InvalidMockNodeException | MockXBeeNodeNotFoundException e) {
					e.printStackTrace();
					runningState = RunnableState.STOPPING;
				}
			} 
			else if (pkt.getClusterId() == ManagementContext.SIMPLE_DESC_REQ.getCode()) {
				try {
					byte endpoint = pkt.getApsPayload()[2];
					transmit(new SimpleDescRspOutpkt(this,endpoint));
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
