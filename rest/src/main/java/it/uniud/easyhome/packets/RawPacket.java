package it.uniud.easyhome.packets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
		
		public Builder append(byte[] bytes) {
			for (byte b : bytes)
				ba.append(b);
			
			return this;
		}
		
		@Override
		public RawPacket build() {
			return new RawPacket(ba.toByteArray());
		}
		
	}
	
	@Override
	public void write(OutputStream os) throws IOException {
		os.write(getBytes());
		os.flush();
	}
	
	@Override
	public void read(InputStream is) throws IOException {
		int numBytes = is.available();
		
		bytes = new byte[numBytes];
		
		is.read(bytes, 0, bytes.length-1);
	}
	
	@Override
	public byte[] getBytes() {
		return bytes;
	}


}
