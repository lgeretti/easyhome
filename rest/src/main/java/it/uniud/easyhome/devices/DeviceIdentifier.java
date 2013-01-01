package it.uniud.easyhome.devices;

import java.io.Serializable;

import it.uniud.easyhome.packets.Domain;

public class DeviceIdentifier implements Serializable {
	
	private static final long serialVersionUID = 2882522345532470176L;
	
	private short endpoint;
	private short domain;
	private short deviceCode;
	
	public DeviceIdentifier(short endpoint, short domain, short deviceCode) {
		this.endpoint = endpoint;
		this.domain = domain;
		this.deviceCode = deviceCode;
	}
	
	private DeviceIdentifier() { }

	public short getEndpoint() {
		return endpoint;
	}
	
	public short getDomain() {
		return domain;
	}
	
	public short getDeviceCode() {
		return deviceCode;
	}
	
	@Override
	public String toString() {
		return "(endpoint: " + endpoint + "; domain: " + domain + "; device code: 0x" + Integer.toHexString(deviceCode) + ")";
	}
}
