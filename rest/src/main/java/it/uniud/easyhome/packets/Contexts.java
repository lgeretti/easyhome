package it.uniud.easyhome.packets;

public enum Contexts {

	NODE_ANNOUNCE((short)0x13);
	
	private short code;
	
	private Contexts(short code) {
		this.code = code;
	}
	
	public short getCode() {
		return code;
	}
}
