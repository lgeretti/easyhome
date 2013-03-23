package it.uniud.easyhome.devices;

public enum Manufacturer {

	DIGI ((short)0x101E), 
	UNDEFINED ((short)0x0), // Not previously defined, like before being collected from the node
	CRP ((short)0x1), 
	ELECTROLUX ((short)0x2),
	OTHER ((short)0xFFFF); // Defined but not within this enumeration
	
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
		
		// If the code is not present, we choose to let the manufacturer fall as Other, i.e., the code is always correct
		if (result == null)
			result = Manufacturer.OTHER;
		
		return result;
	}	
}
