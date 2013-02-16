package it.uniud.easyhome.devices;

import java.io.Serializable;

import javax.persistence.Embeddable;

@Embeddable
public class DeviceIdentifier implements Serializable {
	
	private static final long serialVersionUID = 2882522345532470176L;
	
	private short endpoint;
	private HomeAutomationDevice device;
	
	public DeviceIdentifier(short endpoint, HomeAutomationDevice device) {
		this.endpoint = endpoint;
		this.device = device;
	}
	
	@SuppressWarnings("unused")
	private DeviceIdentifier() { }

	public short getEndpoint() {
		return endpoint;
	}
	
	public void setEndpoint(short endpoint) {
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
