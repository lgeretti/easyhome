package it.uniud.easyhome.network;

public enum NodeLogicalType {

	END_DEVICE(2),
	ROUTER(1),
	COORDINATOR(0),
	UNDEFINED(3);
	
	private byte code;
	
	private NodeLogicalType(int val) {
		code = (byte)val;
	}
	
	public byte getCode() {
		return code;
	}
}
