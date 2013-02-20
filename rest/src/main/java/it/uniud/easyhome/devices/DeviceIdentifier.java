package it.uniud.easyhome.devices;

import java.io.Serializable;

import javax.persistence.Embeddable;

@Embeddable
public class DeviceIdentifier implements Serializable {
	
	private static final long serialVersionUID = 2882522345532470176L;
	
	private byte endpoint;
	private HomeAutomationDevice device;
	
	public DeviceIdentifier(byte endpoint, HomeAutomationDevice device) {
		this.endpoint = endpoint;
		this.device = device;
	}
	
	@SuppressWarnings("unused")
	private DeviceIdentifier() { }

	public byte getEndpoint() {
		return endpoint;
	}
	
	public void setEndpoint(byte endpoint) {
		this.endpoint = endpoint;
	}
	
	public HomeAutomationDevice getDevice() {
		return device;
	}
	
	public void setDevice(HomeAutomationDevice device) {
		this.device = device;
	}
	
	@Override
	public String toString() {
		return "(endpoint: " + endpoint + "; device: " + device + ")";
	}
}
