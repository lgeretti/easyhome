package it.uniud.easyhome.packets;

import org.codehaus.jackson.util.ByteArrayBuilder;

import it.uniud.easyhome.common.ConcreteClassBuilder;

public class RawPacket implements Packet {

	private byte[] bytes;
	
	private RawPacket(byte[] bytes) {
		this.bytes = bytes;
	}
	
	public static class Builder implements ConcreteClassBuilder<RawPacket> {

		ByteArrayBuilder ba;
		
		public Builder() {
			ba = new ByteArrayBuilder();
		}
		
		public Builder append(int i) {
			ba.append(i);
			return this;
		}
		
		@Override
		public RawPacket build() {
			return new RawPacket(ba.toByteArray());
		}
		
	}
	
	@Override
	public byte[] getBytes() {
		return bytes;
	}
}
