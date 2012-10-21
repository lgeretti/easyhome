package it.uniud.easyhome.packets;

public enum Domains {

	MANAGEMENT((short)0),
	HOME_AUTOMATION((short)260),
	EASYHOME_MANAGEMENT((short)59984),
	EASYHOME((short)59985);
	
	private short code;
	
	private Domains(short code) {
		this.code = code;
	}
	
	public short getCode() {
		return code;
	}
	
	public static boolean isManagement(short domainId) {
		return (domainId == Domains.MANAGEMENT.getCode() || domainId == Domains.EASYHOME_MANAGEMENT.getCode());
	}
}
