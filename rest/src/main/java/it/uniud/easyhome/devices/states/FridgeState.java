package it.uniud.easyhome.devices.states;

import it.uniud.easyhome.network.PersistentInfo;

import javax.persistence.*;
import javax.xml.bind.annotation.*;

@Entity
@Table(name = "FridgeState")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class FridgeState {
	
	@Id
    @OneToOne
    private PersistentInfo device;
    @Column
    private FridgeCode lastCode;
    @Column
    private boolean on;

    @SuppressWarnings("unused")
	private FridgeState() {}
    
    public FridgeState(PersistentInfo device) {
    	this.device = device;
    }

	public PersistentInfo getDevice() {
		return this.device;
	}
	
	public FridgeCode getLastCode() {
		return this.lastCode;
	}
	
	public boolean isOn() {
		return this.on;
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
    
    public FridgeState setOn(boolean val) {
    	this.on = val;
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
