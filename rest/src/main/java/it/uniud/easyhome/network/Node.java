package it.uniud.easyhome.network;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.uniud.easyhome.common.ConcreteClassBuilder;
import it.uniud.easyhome.devices.DeviceIdentifier;
import it.uniud.easyhome.devices.HomeAutomationDevice;
import it.uniud.easyhome.devices.Location;
import it.uniud.easyhome.devices.Manufacturer;
import it.uniud.easyhome.devices.PersistentInfo;
import it.uniud.easyhome.exceptions.EndpointNotFoundException;
import it.uniud.easyhome.exceptions.InvalidNodeTypeException;
import it.uniud.easyhome.exceptions.NodeConstructionException;

import javax.persistence.*;
import javax.xml.bind.annotation.*;

@Entity
@Table(name = "Node")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Node {
	
	@Id
	private long id;
    @Embedded
    private GlobalCoordinates coordinates;
    @Column(nullable = false)
    private NodeLogicalType logicalType;
    @Column
    private Manufacturer manufacturer;
    @Column
    private byte powerLevel;
    @Column
    private boolean permanent;
    @OneToOne
    private PersistentInfo info;
    
    @Embedded
    @ElementCollection
    @CollectionTable(name = "Neighbors")
    private List<LocalCoordinates> neighbors = new ArrayList<LocalCoordinates>();
    
    @Embedded
    @ElementCollection
    @CollectionTable(name = "Devices")
    private List<DeviceIdentifier> devices = new ArrayList<DeviceIdentifier>();

    private Node() {}
    
    public Node setLogicalType(NodeLogicalType logicalType) {
    	this.logicalType = logicalType;
    	return this;
    }
    
    public Node setManufacturer(Manufacturer manufacturer) {
    	this.manufacturer = manufacturer;
    	return this;
    }
    
    public Node setPowerLevel(byte powerLevel) {
    	this.powerLevel = powerLevel;
    	return this;
    }
    
    public Node setPermanent(boolean permanent) {
    	this.permanent = permanent;
    	return this;
    }
    
    public void addNeighbor(Node node) {
    	neighbors.add(new LocalCoordinates(node.coordinates.getNuid(),node.coordinates.getAddress()));
    }
    
	public void setNeighbors(List<LocalCoordinates> neighbors) {
		this.neighbors = neighbors;
	}
    
	public Node setEndpoints(List<Byte> endpoints) {
		this.devices.clear();
		for (Byte ep : endpoints) {
			this.devices.add(new DeviceIdentifier(ep,HomeAutomationDevice.UNKNOWN));
		}
		return this;
	}
	
	public synchronized Node addDevice(byte endpoint, HomeAutomationDevice device) {
		short epIndex = -1;
		for (short i=0; i<devices.size(); i++) {
			if (devices.get(i).getEndpoint() == endpoint) {
				epIndex = i;
				break;
			}
		}
		
		if (epIndex == -1)
			throw new EndpointNotFoundException();
			
		devices.get(epIndex).setDevice(device);
		return this;
	}

    public static class Builder implements ConcreteClassBuilder<Node> {
        
        private Node node;
        
        public Builder(long id, byte gatewayId, long nuid, short address) {
            if (id == 0 || nuid == 0)
                throw new IllegalArgumentException();            
            node = new Node();
            node.id = id;
            node.coordinates = new GlobalCoordinates(gatewayId, nuid, address);
            node.powerLevel = -1;
            node.logicalType = NodeLogicalType.UNDEFINED;
            node.manufacturer = Manufacturer.UNDEFINED;
        }
        
        public Builder setLogicalType(NodeLogicalType logicalType) {
        	node.logicalType = logicalType;
        	return this;
        }
        
        public Builder setManufacturer(Manufacturer manufacturer) {
        	node.manufacturer = manufacturer;
        	return this;
        }
        
        public Builder setPermanent(boolean permanent) {
        	node.permanent = permanent;
        	return this;
        }
        
        public Node build() {
        	
        	if (node.coordinates.getGatewayId() == 0)
        		throw new NodeConstructionException();
        	
            return node;
        }
    }
    
    public long getId() {
    	return this.id;
    }
    
    public String getName() {
    	
    	if (info != null)
    		return info.getName();
    	else	
    		return coordinates.toString();
    }
    
    public boolean isPermanent() {
    	return this.permanent;
    }
    
    public GlobalCoordinates getCoordinates() {
    	return coordinates;
    }
    
    public Manufacturer getManufacturer() {
    	return this.manufacturer;
    }
    
    public byte getPowerLevel() {
    	return this.powerLevel;
    }
    
    public NodeLogicalType getLogicalType() {
    	return this.logicalType;
    }
    
	public Location getLocation() {
		
		if (info == null)
			return null;
		else
			return info.getLocation();
	}
    
    public List<LocalCoordinates> getNeighbors() {
    	return this.neighbors;
    }
    
    public List<Byte> getEndpoints() {
    	List<Byte> endpoints = new ArrayList<Byte>();
    	
    	for (DeviceIdentifier devId: devices) {
    		endpoints.add(devId.getEndpoint());
    	}
    	
    	return endpoints;
    }    
    
    public Map<Byte,HomeAutomationDevice> getMappedDevices() {
    	Map<Byte,HomeAutomationDevice> result = new HashMap<Byte,HomeAutomationDevice>(devices.size());

    	for (DeviceIdentifier devId: devices) {
    		result.put(devId.getEndpoint(), devId.getDevice());
    	}    	
    	return result;
    }
    
    public String toString() {
    	return getName();
    }
    
    public Node setInfo(PersistentInfo info) {
    	this.info = info;
    	return this;
    }
    
    public static String nameFor(byte gatewayId, short address) {
    	StringBuilder strb = new StringBuilder();
    	strb.append(gatewayId)
    		.append(":")
    		.append(Integer.toHexString(0xFFFF & address));
    	return strb.toString();
    }

    @Override
    public boolean equals(Object other) {
        
        if (!(other instanceof Node))
            throw new IllegalArgumentException();
        Node otherNode = (Node) other;
        
        if (!this.coordinates.equals(otherNode.coordinates)) return false;
        if (this.logicalType != otherNode.logicalType) return false;
        if (this.manufacturer != otherNode.manufacturer) return false;
        
        return true;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;

        long result = 1;
        result = prime * result + coordinates.hashCode();
        result = prime * result + logicalType.hashCode();
        result = prime * result + manufacturer.hashCode();
        return (int)result;
    }
}
