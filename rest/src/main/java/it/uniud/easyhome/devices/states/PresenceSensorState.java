package it.uniud.easyhome.devices.states;

import it.uniud.easyhome.devices.PersistentInfo;
import it.uniud.easyhome.network.Node;

import javax.persistence.*;
import javax.xml.bind.annotation.*;

@Entity
@Table(name = "PresenceSensorState")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class PresenceSensorState implements DeviceState {
	
	@Id
    @OneToOne
    private Node device;
    @Column
    private boolean online;
    @Column
    private boolean occupied;

    @SuppressWarnings("unused")
	private PresenceSensorState() {}
    
    public PresenceSensorState(Node device) {
    	this.device = device;
    }

	public Node getDevice() {
		return this.device;
	}
	
	public boolean isOccupied() {
		return this.occupied;
	}
	
	public boolean isOnline() {
		return this.online;
	}
	
	public PresenceSensorState setOccupied(boolean val) {
		this.occupied = val;
		return this;
	}
	
    public PresenceSensorState setOnline(boolean val) {
    	this.online = val;
    	return this;
    }
	
    @Override
    public boolean equals(Object other) {
        
        if (!(other instanceof PresenceSensorState))
            throw new IllegalArgumentException();
        PresenceSensorState otherFunctionality = (PresenceSensorState) other;
        
        if (!this.device.equals(otherFunctionality.device)) return false;
        
        return true;
    }
        
    @Override
    public int hashCode() {
        final int prime = 31;

        long result = 1;
        result = prime * result + device.hashCode();
        return (int)result;
    }
}
