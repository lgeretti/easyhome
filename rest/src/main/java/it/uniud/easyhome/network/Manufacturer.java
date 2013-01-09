package it.uniud.easyhome.network;

import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.exceptions.InvalidContextException;
import it.uniud.easyhome.exceptions.InvalidManufacturerCodeException;

public enum Manufacturer {

	DIGI ((short)0x0013), 
	UNDEFINED ((short)0x0);
	
	private short code;
	
	private Manufacturer(short code) {
		this.code = code;
	}
	
	public short getCode() {
		return code;
	}

	public static Manufacturer fromCode(short code) {
		
		Manufacturer result = null;
		for (Manufacturer cxt: Manufacturer.values()) {
			if (cxt.getCode() == code)
				result = cxt;
		}
		
		if (result == null)
			throw new InvalidManufacturerCodeException();
		
		return result;
	}	
}
