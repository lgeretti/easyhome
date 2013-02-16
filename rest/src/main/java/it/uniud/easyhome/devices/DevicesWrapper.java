package it.uniud.easyhome.devices;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;

@Embeddable
public class DevicesWrapper {

	@ElementCollection
    @CollectionTable(name = "Devices")
	private Map<Short,HomeAutomationDevice> devices = new HashMap<Short,HomeAutomationDevice>();
	
	public Map<Short,HomeAutomationDevice> getDevices() {
		return devices;
	}
	
	public void addDevice(short endpoint, HomeAutomationDevice device) {
		devices.put(endpoint, device);
	}
}
