package it.uniud.easyhome.packets;

public enum Domain {

	MANAGEMENT((short)0),
	HOME_AUTOMATION((short)0x0104),
	EASYHOME_MANAGEMENT((short)0xEA50),
	EASYHOME((short)0xEA51);
	
	private short code;
	
	private Domain(short code) {
		this.code = code;
	}
	
	public short getCode() {
		return code;
	}
	
	public static boolean isManagement(short domainId) {
		return (domainId == Domain.MANAGEMENT.getCode() || domainId == Domain.EASYHOME_MANAGEMENT.getCode());
	}
}
