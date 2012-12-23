package it.uniud.easyhome.packets;

public enum Domain {

	MANAGEMENT((short)0),
	HOME_AUTOMATION((short)260),
	EASYHOME_MANAGEMENT((short)59984),
	EASYHOME((short)59985);
	
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
