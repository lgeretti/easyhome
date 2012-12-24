package it.uniud.easyhome.devices;

import java.util.List;

import com.sun.xml.bind.CycleRecoverable.Context;

import it.uniud.easyhome.packets.Domain;

public interface Device {
	
	public Domain getDomain();
	
	public short getCode();
	
	/**
	 * The list of actual contexts implemented by the device. This is different from the list
	 * of mandatory and optional contexts.
	 */
	public List<Context> getInputContexts();

	/**
	 * The list of actual contexts implemented by the device. This is different from the list
	 * of mandatory and optional contexts.
	 */
	public List<Context> getOutputContexts();
}