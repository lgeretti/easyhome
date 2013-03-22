package it.uniud.easyhome.devices.states;

import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.devices.InvalidColoredAlarmException;
import it.uniud.easyhome.exceptions.InvalidContextException;
import it.uniud.easyhome.packets.Domain;

public enum ColoredAlarm {

	RED_FIXED((short)0x64),
    RED_BLINK((short)0x5A),
    GREEN_FIXED((short)0x50),
    GREEN_BLINK((short)0x46),
    BLUE_FIXED((short)0x3C),
    BLUE_BLINK((short)0x32);
    
	private short code;
	
	private ColoredAlarm(short code) {
		this.code = code;
	}
	
	public short getCode() {
		return code;
	}

	public static ColoredAlarm fromCode(short code) {
		
		ColoredAlarm result = null;
		for (ColoredAlarm alarm: ColoredAlarm.values()) {
			if (alarm.getCode() == code)
				result = alarm;
		}
		
		if (result == null)
			throw new InvalidColoredAlarmException();
		
		return result;
	}
	
}
