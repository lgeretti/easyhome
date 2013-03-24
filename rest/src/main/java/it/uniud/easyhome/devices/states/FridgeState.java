package it.uniud.easyhome.devices.states;

import it.uniud.easyhome.devices.PersistentInfo;
import it.uniud.easyhome.network.Node;

import javax.persistence.*;
import javax.xml.bind.annotation.*;

@Entity
@Table(name = "FridgeState")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class FridgeState implements DeviceState {
	
	@Id
    @OneToOne
    private Node device;
    @Column
    private FridgeCode lastCode;
    @Column
    private boolean online;

    @SuppressWarnings("unused")
	private FridgeState() {}
    
    public FridgeState(Node device) {
    	this.device = device;
    	lastCode = FridgeCode.UNKNOWN;
    }

	public Node getDevice() {
		return this.device;
	}
	
	public FridgeCode getLastCode() {
		return this.lastCode;
	}
	
	public boolean isOnline() {
		return this.online;
	}
	
    @Override
    public boolean equals(Object other) {
        
        if (!(other instanceof FridgeState))
            throw new IllegalArgumentException();
        FridgeState otherFunctionality = (FridgeState) other;
        
        if (!this.device.equals(otherFunctionality.device)) return false;
        
        return true;
    }
    
    public FridgeState setLastCode(FridgeCode val) {
    	this.lastCode = val;
    	return this;
    }
    
    public FridgeState setOnline(boolean val) {
    	this.online = val;
    	return this;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;

        long result = 1;
        result = prime * result + device.hashCode();
        return (int)result;
    }
}
