package it.uniud.easyhome.devices.states;

import it.uniud.easyhome.network.PersistentInfo;

import javax.persistence.*;
import javax.xml.bind.annotation.*;

@Entity
@Table(name = "LampState")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class LampState {
	
	@Id
	private long id;
    @OneToOne
    private PersistentInfo device;
    @Column
    private byte red;
    @Column
    private byte green;
    @Column
    private byte blue;
    @Column
    private byte white;
    @Column
    private ColoredAlarm alarm;
    @Column
    private boolean occupied;

    @SuppressWarnings("unused")
	private LampState() {}
    
    public LampState(long id,  PersistentInfo device) {
    	this.id = id;
    	this.device = device;
    }
	
	public long getId() {
		return this.id;
	}

	public PersistentInfo getDevice() {
		return this.device;
	}
	
	public byte getRed() {
		return this.red;
	}
	
	public byte getGreen() {
		return this.green;
	}
	
	public byte getBlue() {
		return this.blue;
	}
	
	public byte getWhite() {
		return this.white;
	}
	
	public ColoredAlarm getAlarm() {
		return alarm;
	}
	
	public boolean isOccupied() {
		return occupied;
	}
	
	public LampState setRed(byte val) {
		red = val;
		return this;
	}
	
	public LampState setGreen(byte val) {
		green = val;
		return this;
	}
	
	public LampState setBlue(byte val) {
		blue = val;
		return this;
	}
	
	public LampState setWhite(byte val) {
		white = val;
		return this;
	}	
	
	public LampState setAlarm(ColoredAlarm val) {
		alarm = val;
		return this;
	}

	public LampState setOccupied(boolean val) {
		occupied = val;
		return this;
	}
	
    @Override
    public boolean equals(Object other) {
        
        if (!(other instanceof LampState))
            throw new IllegalArgumentException();
        LampState otherFunctionality = (LampState) other;
        
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
