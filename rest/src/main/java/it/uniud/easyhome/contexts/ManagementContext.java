package it.uniud.easyhome.contexts;

import it.uniud.easyhome.packets.Domain;

public enum ManagementContext implements Context {

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
	
	private ManagementContext(short code) {
		this.code = code;
	}
	
	@Override
	public short getCode() {
		return code;
	}

	@Override
	public Domain getDomain() {
		return Domain.MANAGEMENT;
	}
}
