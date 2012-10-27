package it.uniud.easyhome.network.mock;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import it.uniud.easyhome.common.RunnableState;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.Packet;

public class MockNode implements Runnable {

    private Node node;
    
    private MockNetwork network;
    
    private Queue<Packet> packetsFromGateway;
    
    private volatile RunnableState runningState;
    
    MockNode(Node node, MockNetwork network) {
    	this.node = node;
    	this.network = network;
    	this.runningState = RunnableState.STOPPED;
    	packetsFromGateway = new ConcurrentLinkedQueue<Packet>();
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
    
    public void post(Packet pkt) {
    	packetsFromGateway.add(pkt);
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
    
    @Override
    public void run() {
    	
    	while (runningState != RunnableState.STOPPING) {
    		
    		
    		
    	}
		runningState = RunnableState.STOPPED;
    }

    @Override
    public boolean equals(Object other) {
        
        if (!(other instanceof MockNode))
            throw new IllegalArgumentException();
        MockNode otherNode = (MockNode) other;
        
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
