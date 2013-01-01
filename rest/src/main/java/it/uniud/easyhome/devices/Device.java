package it.uniud.easyhome.devices;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.contexts.Context;

public interface Device {
	
	public Domain getDomain();	
	
	public short getCode();
	
	/**
	 * The list of actual contexts implemented by the device. This is different from the list
	 * of mandatory and optional contexts.
	 */
	public List<? extends Context> getClientContexts();

	/**
	 * The list of actual contexts implemented by the device. This is different from the list
	 * of mandatory and optional contexts.
	 */
	public List<? extends Context> getServerContexts();
}