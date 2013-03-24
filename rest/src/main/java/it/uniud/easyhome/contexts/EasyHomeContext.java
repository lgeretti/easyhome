package it.uniud.easyhome.contexts;

import it.uniud.easyhome.exceptions.InvalidContextException;
import it.uniud.easyhome.packets.Domain;

public enum EasyHomeContext implements Context {

	// General
	LAMP_UPDATE((short)0x0),
	LAMP_STATUS((short)0x1),
	ALARM((short)0x2);
	
	private short code;
	
	private EasyHomeContext(short code) {
		this.code = code;
	}
	
	@Override
	public short getCode() {
		return code;
	}

	@Override
	public Domain getDomain() {
		return Domain.EASYHOME;
	}
	
	public static EasyHomeContext fromCode(short code) {
		
		EasyHomeContext result = null;
		for (EasyHomeContext cxt: EasyHomeContext.values()) {
			if (cxt.getCode() == code)
				result = cxt;
		}
		
		if (result == null)
			throw new InvalidContextException();
		
		return result;
	}
}
