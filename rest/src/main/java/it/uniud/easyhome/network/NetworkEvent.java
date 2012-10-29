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

	public static enum EventKind { NODE_ADDED, NODE_REMOVED}; 
	
	private EventKind kind;
	private byte gid;
	private long nuid;
	
	public NetworkEvent(EventKind kind, byte gid, long nuid) {
		this.kind = kind;
		this.gid = gid;
		this.nuid = nuid;
	}
	
	public EventKind getKind() {
		return kind;
	}
	
	public byte getGid() {
		return gid;
	}
	
	public long getNuid() {
		return nuid;
	}
}
