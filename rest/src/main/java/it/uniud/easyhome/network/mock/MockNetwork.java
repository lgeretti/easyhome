package it.uniud.easyhome.network.mock;

import it.uniud.easyhome.common.RunnableState;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.Packet;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/** 
 * A mock channel where packets can be received and forwarded to a gateway.
 * 
 * @author Luca Geretti
 *
 */
public class MockNetwork implements Runnable {

	private RunnableState runningState;
	
	private Queue<Packet> packetsToGateway;
	
	private List<MockNode> nodes;
	
	private String gwHost;
	private int gwPort;
	
	public MockNetwork(String gwHost, int gwPort) {
		runningState = RunnableState.STOPPED;
		packetsToGateway = new ConcurrentLinkedQueue<Packet>();
		nodes = new ArrayList<MockNode>();
		
		this.gwHost = gwHost;
		this.gwPort = gwPort;
	}
	
	public void post(Packet pkt) {
		packetsToGateway.add(pkt);
	}
	
	public void register(Node node) {
		nodes.add(new MockNode(node,this));
	}
	
	public List<MockNode> getNodes() {
		return new ArrayList<MockNode>(nodes);
	}
	
	@Override
	public void run() {
		
		Socket skt = null;
		
		try {
			
			skt = new Socket(gwHost, gwPort);
			
			OutputStream os = new BufferedOutputStream(skt.getOutputStream());
			
			while (runningState != RunnableState.STOPPING) {
				
				Packet pkt = packetsToGateway.poll();
				
				if (pkt != null) {
					os.write(pkt.getBytes());
				}
			}
			os.close();
			
		} catch (IOException ex) {
		} finally {
			
			try {
				
				if (skt != null)
					skt.close();
			} catch (IOException ex) {}
			
			runningState = RunnableState.STOPPED;
		}
	}
	
	public void turnOn() {
    	if (runningState == RunnableState.STOPPED) {
    		Thread thr = new Thread(this);
    		thr.start();
    		for (MockNode node : nodes)
    			node.turnOn();
    		runningState = RunnableState.RUNNING;
    	} else {
    		throw new IllegalStateException();
    	}
	}
	
	public void turnOff() {
		runningState = RunnableState.STOPPING;
		for (MockNode node : nodes)
			node.turnOff();
	}
}
