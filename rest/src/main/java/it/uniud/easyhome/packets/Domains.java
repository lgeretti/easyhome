package it.uniud.easyhome.packets;

public enum Domains {

	MANAGEMENT((short)0),
	HOME_AUTOMATION((short)260);
	
	private short code;
	
	private Domains(short code) {
		this.code = code;
	}
	
	public short getCode() {
		return code;
	}
}
