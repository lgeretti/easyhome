package it.uniud.easyhome.devices.states;

public enum FridgeCode {

	OFF((short)200),
	STANDBY((short)222),
    ALARM1((short)251),
    ALARM2((short)252),
    ALARM3((short)253),
    ALARM4((short)254),
    ALARM5((short)255),
    ALARM6((short)256),    
    SHUTDOWN((short)999),
	UNKNOWN((short)-1);
    
	private short code;
	
	private FridgeCode(short code) {
		this.code = code;
	}
	
	public short getCode() {
		return code;
	}

	public static FridgeCode fromCode(short code) {
		
		FridgeCode result = null;
		for (FridgeCode fridgeCode: FridgeCode.values()) {
			if (fridgeCode.getCode() == code)
				result = fridgeCode;
		}
		
		if (result == null)
			return UNKNOWN;
		
		return result;
	}
	
	public boolean isAlarm() {
		return (this.equals(FridgeCode.ALARM1) || this.equals(FridgeCode.ALARM2));
	}
	
}
