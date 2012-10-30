package it.uniud.easyhome.packets;

public enum ManagementContexts {

	NODE_DESC_REQ((short)0x02),
	NODE_DESC_RSP((short)0x8002),
	NODE_ANNOUNCE((short)0x13);
	
	private short code;
	
	private ManagementContexts(short code) {
		this.code = code;
	}
	
	public short getCode() {
		return code;
	}
}
