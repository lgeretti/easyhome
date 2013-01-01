package it.uniud.easyhome.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.contexts.*;

public enum HomeAutomationDevice implements Device {

	ONOFF_SWITCH((short)0x0) { 
		protected void populateInputOutputContexts() {
			addContext(HomeAutomationContext.ONOFF,ContextType.CLIENT);
		} 
	},
	LEVEL_CONTROL_SWITCH((short)0x1) { 
		protected void populateInputOutputContexts() {
			
		} 
	},
	ONOFF_OUTPUT((short)0x2) { 
		protected void populateInputOutputContexts() {
			addContext(HomeAutomationContext.ONOFF,ContextType.SERVER);
			addContext(HomeAutomationContext.SCENES,ContextType.SERVER);
			addContext(HomeAutomationContext.GROUPS,ContextType.SERVER);
		} 
	},
	LEVEL_CONTROLLABLE_OUTPUT((short)0x3) { 
		protected void populateInputOutputContexts() {
			
		} 
	},
	SIMPLE_SENSOR((short)0xC) { 
		protected void populateInputOutputContexts() {
			
		} 
	},	
	ONOFF_LIGHT((short)0x100) { 
		protected void populateInputOutputContexts() {
			
		} 
	},
	DIMMABLE_LIGHT((short)0x101) { 
		protected void populateInputOutputContexts() {
			
		} 
	},
	COLOR_DIMMABLE_LIGHT((short)0x102) { 
		protected void populateInputOutputContexts() {
			
		} 
	},	
	ONOFF_LIGHT_SWITCH((short)0x103) { 
		protected void populateInputOutputContexts() {
			
		} 
	},
	DIMMER_SWITCH((short)0x104) { 
		protected void populateInputOutputContexts() {
			
		} 
	},
	UNKNOWN((short)0xFFFF) {
		protected void populateInputOutputContexts() {
			
		}		
	};
	
	protected enum ContextType { CLIENT, SERVER };
	
	private short code;
	
	private List<HomeAutomationContext> clientContexts = new ArrayList<HomeAutomationContext>();
	private List<HomeAutomationContext> serverContexts = new ArrayList<HomeAutomationContext>();
	
	abstract protected void populateInputOutputContexts();
	
	private HomeAutomationDevice(short code) {
		this.code = code;
	}
	
	protected void addContext(HomeAutomationContext context, ContextType type) {
		switch (type) {
			case CLIENT:
				clientContexts.add(context);
				break;
			case SERVER:
				serverContexts.add(context);
		}
	}
	
	@Override
	public Domain getDomain() {
		return Domain.HOME_AUTOMATION;
	}

	@Override
	public short getCode() {
		return code;
	}

	@Override
	public List<HomeAutomationContext> getClientContexts() {
		return Collections.unmodifiableList(clientContexts);
	}

	@Override
	public List<HomeAutomationContext> getServerContexts() {
		return Collections.unmodifiableList(serverContexts);
	}

}
