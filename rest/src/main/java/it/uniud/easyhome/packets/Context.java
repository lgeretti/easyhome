package it.uniud.easyhome.packets;

public enum Context {

	NODE_DESC_REQ((short)0x2),
	NODE_DESC_RSP((short)0x8002),
	NODE_ANNOUNCE((short)0x13),
	NODE_NEIGH_REQ((short)0x31),
	NODE_NEIGH_RSP((short)0x8031),
	ACTIVE_EP_REQ((short)0x5),
	ACTIVE_EP_RSP((short)0x8005),
	SIMPLE_DESC_REQ((short)0x4),
	SIMPLE_DESC_RSP((short)0x8004);
	
	private short code;
	
	private Context(short code) {
		this.code = code;
	}
	
	public short getCode() {
		return code;
	}
}
