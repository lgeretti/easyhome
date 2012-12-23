package it.uniud.easyhome.network;

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
	
	public static Manufacturer getFromShort(short code) {
		
		Manufacturer result = null;
		
		switch (code) {
			case 0x0013:
				result = DIGI;
				break;
			default:
				throw new InvalidManufacturerCodeException();
		}
		
		return result;
	}
	
}
