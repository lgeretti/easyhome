package it.uniud.easyhome.network.mock;

import it.uniud.easyhome.common.RunnableState;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.Packet;
import it.uniud.easyhome.packets.xbee.XBeePacketToNode;
import it.uniud.easyhome.packets.xbee.XBeePacketFromNode;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/** 
 * A mock XBee network where packets can be received and forwarded to a gateway.
 * 
 * @author Luca Geretti
 *
 */
public class MockXBeeNetwork implements Runnable {

	private RunnableState runningState = RunnableState.STOPPED;
	
	private Queue<XBeePacketToNode> packetsToGateway = new ConcurrentLinkedQueue<XBeePacketToNode>();
	
	private List<MockXBeeNode> nodes = new ArrayList<MockXBeeNode>();
	
	private String gwHost;
	private int gwPort;
	
	public MockXBeeNetwork(String gwHost, int gwPort) {

		this.gwHost = gwHost;
		this.gwPort = gwPort;
	}
	
	public void broadcast(XBeePacketToNode pkt) {
		packetsToGateway.add(pkt);
	}
	
	public void inject(XBeePacketFromNode pkt) {
		for (MockXBeeNode node : nodes) {
			if (pkt.isBroadcast() || pkt.get64BitDstAddr() == node.getCoordinates().getNuid()) {
				node.receive(new XBeePacketToNode(pkt,0x0,(short)0x0));
			}
		}
	}
	
	public void register(Node node) throws InvalidMockNodeException {
		nodes.add(new MockXBeeNode(node,this));
	}
	
	public List<MockXBeeNode> getNodes() {
		return new ArrayList<MockXBeeNode>(nodes);
	}
	
	@Override
	public void run() {
		
		Socket skt = null;
		
		try {
			
			skt = new Socket(gwHost, gwPort);
			OutputStream os = new BufferedOutputStream(skt.getOutputStream());
			InputStream is = new BufferedInputStream(skt.getInputStream());
				
			while (runningState != RunnableState.STOPPING) {
				
				Packet pktToGateway = packetsToGateway.poll();
				
				if (pktToGateway != null) {
					os.write(pktToGateway.getBytes());
					os.flush();
				}
				
				if (is.available() > 0) {
					XBeePacketFromNode pktFromGateway = new XBeePacketFromNode();
					
					try {
						pktFromGateway.read(is);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					
					inject(pktFromGateway);
				}
			}
			os.close();
			
		} catch (IOException ex) {
			ex.printStackTrace();
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
    		runningState = RunnableState.STARTING;
    		Thread thr = new Thread(this);
    		thr.start();
    		for (MockXBeeNode node : nodes)
    			node.turnOn();
    		runningState = RunnableState.STARTED;
    	} else {
    		throw new IllegalStateException();
    	}
	}
	
	public void turnOff() {
		runningState = RunnableState.STOPPING;
		for (MockXBeeNode node : nodes)
			node.turnOff();
	}
	
	public void unregisterAll() {
		nodes.clear();
	}
}
