package it.uniud.easyhome.network;

public enum NodeLogicalType {

	COORDINATOR(0),
	ROUTER(1),
	END_DEVICE(2),
	UNDEFINED(3);
	
	private byte code;
	
	private NodeLogicalType(int val) {
		code = (byte)val;
	}
	
	public byte getCode() {
		return code;
	}
}
