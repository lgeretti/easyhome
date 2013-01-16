package it.uniud.easyhome.network;

import java.io.Serializable;

/**
 * Immutable class for transferring event objects through JMS.
 * 
 * @author Luca Geretti
 *
 */
public class NetworkEvent implements Serializable {

	private static final long serialVersionUID = -926685643567817992L;

	public static enum EventKind { NODE_ADDED, NODE_REMOVED, NODE_DESCR_ACQUIRED, NODE_ENDPOINTS_ACQUIRED, SIMPLE_DESCR_ACQUIRED}; 
	
	private EventKind kind;
	private byte gid;
	private short address;
	private byte endpoint;
	
	public NetworkEvent(EventKind kind, byte gid, short address) {
		this(kind,gid,address,(byte)127);
	}
	
	public NetworkEvent(EventKind kind, byte gid, short address, byte endpoint) {
		this.kind = kind;
		this.gid = gid;
		this.address = address;
		this.endpoint = endpoint;
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
	
	public byte endpoint() {
		return endpoint;
	}
}
