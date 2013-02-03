package it.uniud.easyhome.network;

import java.io.Serializable;

/**
 * Immutable class for transferring event objects through JMS.
 * 
 * @author Luca Geretti
 *
 */
public class NetworkEvent implements Serializable {

	private static final long serialVersionUID = 564739889945686964L;

	public static enum EventKind { 
		NODE_ADDED, 
		NODE_REMOVED, 
		NODE_DESCR_ACQUIRED, 
		NODE_ENDPOINTS_ACQUIRED, 
		SIMPLE_DESCR_ACQUIRED, 
		NODE_NEIGHBORS_CHANGED, 
		NODE_POWER_LEVEL_SET_ISSUE, 
		NETWORK_GRAPH_MINIMIZATION
		}; 
	
	private EventKind kind;
	private byte gid;
	private short address;
	private byte endpoint;
	private byte[] payload;
	
	public NetworkEvent(EventKind kind, byte gid, short address) {
		this(kind,gid,address,(byte)127, new byte[0]);
	}
	
	public NetworkEvent(EventKind kind, byte gid, short address, byte endpoint) {
		this(kind,gid,address,endpoint, new byte[0]);
	}
	
	public NetworkEvent(EventKind kind, byte gid, short address, byte[] payload) {
		this(kind,gid,address,(byte)127, payload);
	}
	
	public NetworkEvent(EventKind kind, byte gid, short address, byte endpoint, byte[] payload) {
		this.kind = kind;
		this.gid = gid;
		this.address = address;
		this.endpoint = endpoint;
		this.payload = payload;
	}
	
	public EventKind getKind() {
		return kind;
	}
	
	public byte getGatewayId() {
		return gid;
	}

	public short getAddress() {
		return address;
	}
	
	public byte getEndpoint() {
		return endpoint;
	}
	
	public byte[] getPayload() {
		return payload;
	}
}
