package it.uniud.easyhome.contexts;

import it.uniud.easyhome.exceptions.InvalidContextException;
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
	SIMPLE_DESC_RSP((short)0x8004),
	NODE_DISCOVERY_REQ((short)0x32),
	NODE_DISCOVERY_RSP((short)0x8032),
	NODE_POWER_LEVEL_REQ((short)0x90),
	NODE_POWER_LEVEL_RSP((short)0x8090),
	NODE_POWER_LEVEL_SET_ISS((short)0x91),
	NODE_POWER_LEVEL_SET_ACK((short)0x8091);
	
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
	
	public static ManagementContext fromCode(short code) {
		
		ManagementContext result = null;
		for (ManagementContext cxt: ManagementContext.values()) {
			if (cxt.getCode() == code)
				result = cxt;
		}
		
		if (result == null)
			throw new InvalidContextException();
		
		return result;
	}
}
