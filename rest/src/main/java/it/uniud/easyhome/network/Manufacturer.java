package it.uniud.easyhome.network;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.exceptions.InvalidContextException;
import it.uniud.easyhome.exceptions.InvalidManufacturerCodeException;

public enum Manufacturer {

	DIGI ((short)0x101E), 
	UNDEFINED ((short)0x0), // Not previously defined, like before being collected from the node
	UNKNOWN ((short)0xFFFF); // Defined but not within this enumeration
	
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
			result = Manufacturer.UNKNOWN;
		
		return result;
	}	
}
