package it.uniud.easyhome.devices;

import it.uniud.easyhome.packets.Domain;

public abstract class HomeAutomationDevice implements Device {

	@Override
	public Domain getDomain() {
		return Domain.HOME_AUTOMATION;
	}

}
