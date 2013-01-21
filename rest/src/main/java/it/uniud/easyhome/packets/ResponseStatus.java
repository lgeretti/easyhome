package it.uniud.easyhome.packets;

import it.uniud.easyhome.exceptions.UnknownRequestStatusException;

public enum ResponseStatus {

	SUCCESS((byte)0),
	NOT_SUPPORTED((byte)0x84);
	
	private byte code;
	
	private ResponseStatus(byte code) {
		this.code = code;
	}
	
	public byte getCode() {
		return code;
	}
	
	public static ResponseStatus fromCode(short code) {
		
		ResponseStatus result = null;
		for (ResponseStatus status: ResponseStatus.values()) {
			if (status.getCode() == code)
				result = status;
		}
		
		if (result == null)
			throw new UnknownRequestStatusException();
		
		return result;
	}
}
