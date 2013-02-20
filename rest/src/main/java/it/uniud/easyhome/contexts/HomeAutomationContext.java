package it.uniud.easyhome.contexts;

import it.uniud.easyhome.exceptions.InvalidContextException;
import it.uniud.easyhome.packets.Domain;

public enum HomeAutomationContext implements Context {

	// General
	BASIC((short)0x0),
	POWER_CONFIGURATION((short)0x1),
	DEVICE_TEMPERATURE_CONFIGURATION((short)0x2),
	IDENTIFY((short)0x3),
	GROUPS((short)0x4),
	SCENES((short)0x5),
	ONOFF((short)0x6),
	ONOFF_SWITCH_CONFIGURATION((short)0x7),
	LEVEL_CONTROL((short)0x8),
	ALARMS((short)0x9),
	BINARY_INPUT((short)0xF),
	// Lighting
	COLOR_CONTROL((short)0x300),
	// Measurement and sensing
	OCCUPANCY_SENSING((short)0x406);
	
	private short code;
	
	private HomeAutomationContext(short code) {
		this.code = code;
	}
	
	@Override
	public short getCode() {
		return code;
	}

	@Override
	public Domain getDomain() {
		return Domain.HOME_AUTOMATION;
	}
	
	public static HomeAutomationContext fromCode(short code) {
		
		HomeAutomationContext result = null;
		for (HomeAutomationContext cxt: HomeAutomationContext.values()) {
			if (cxt.getCode() == code)
				result = cxt;
		}
		
		if (result == null)
			throw new InvalidContextException();
		
		return result;
	}
}
