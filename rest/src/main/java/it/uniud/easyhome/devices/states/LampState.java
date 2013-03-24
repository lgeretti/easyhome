package it.uniud.easyhome.devices.states;

import it.uniud.easyhome.network.Node;

import javax.persistence.*;
import javax.xml.bind.annotation.*;

@Entity
@Table(name = "LampState")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class LampState implements DeviceState {
	
	@Id
    @OneToOne
    private Node device;
	@Column
	private boolean online;
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
    private long lastUpdate;
    
    @SuppressWarnings("unused")
	private LampState() {}
    
    public LampState(Node device) {
    	this.device = device;
    	this.alarm = ColoredAlarm.NONE;
    	this.lastUpdate = 0; // Conveniently set as the beginning of UNIX time
    }
    
	public Node getDevice() {
		return this.device;
	}
	
	public boolean isOnline() {
		return this.online;
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
	
	public long getLastUpdate() {
		return this.lastUpdate;
	}
	
    public LampState setOnline(boolean val) {
    	this.online = val;
    	return this;
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
	
	public LampState update() {
		this.lastUpdate = System.currentTimeMillis();
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
