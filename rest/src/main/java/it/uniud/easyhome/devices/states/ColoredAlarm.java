package it.uniud.easyhome.devices.states;

public enum ColoredAlarm {

	RED_FIXED((byte)0x64),
    RED_BLINK((byte)0x5A),
    GREEN_FIXED((byte)0x50),
    GREEN_BLINK((byte)0x46),
    BLUE_FIXED((byte)0x3C),
    BLUE_BLINK((byte)0x32),
	NONE((byte)0x0);
    
	private byte code;
	
	private ColoredAlarm(byte code) {
		this.code = code;
	}
	
	public byte getCode() {
		return code;
	}

	public static ColoredAlarm fromCode(byte code) {
		
		ColoredAlarm result = null;
		for (ColoredAlarm alarm: ColoredAlarm.values()) {
			if (alarm.getCode() == code)
				result = alarm;
		}
		
		if (result == null)
			return NONE;
		
		return result;
	}
	
}
