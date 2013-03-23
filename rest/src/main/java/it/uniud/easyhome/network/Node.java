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
    @Column(nullable = false, length = 200)
    private String name;
    @Embedded
    private GlobalCoordinates coordinates;
    @Column(nullable = false)
    private NodeLogicalType logicalType;
    @OneToOne
    private Location location;    
    @Column
    private Manufacturer manufacturer;
    @Column
    private byte powerLevel;
    
    @Embedded
    @ElementCollection
    @CollectionTable(name = "Neighbors")
    private List<LocalCoordinates> neighbors = new ArrayList<LocalCoordinates>();
    
    @Embedded
    @ElementCollection
    @CollectionTable(name = "Devices")
    private List<DeviceIdentifier> devices = new ArrayList<DeviceIdentifier>();

    private Node() {}
    
    public void setLogicalType(NodeLogicalType logicalType) {
    	this.logicalType = logicalType;
    }
    
    public void setManufacturer(Manufacturer manufacturer) {
    	this.manufacturer = manufacturer;
    }
    
    public void setPowerLevel(byte powerLevel) {
    	this.powerLevel = powerLevel;
    }
    
    public void addNeighbor(Node node) {
    	neighbors.add(new LocalCoordinates(node.coordinates.getNuid(),node.coordinates.getAddress()));
    }
    
	public void setNeighbors(List<LocalCoordinates> neighbors) {
		this.neighbors = neighbors;
	}
    
	public void setEndpoints(List<Byte> endpoints) {
		this.devices.clear();
		for (Byte ep : endpoints) {
			this.devices.add(new DeviceIdentifier(ep,HomeAutomationDevice.UNKNOWN));
		}
	}
	
	public synchronized void addDevice(byte endpoint, HomeAutomationDevice device) {
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
	}
	

	public void setName(String name) {
		this.name = name;
	}
	
	public void setLocation(Location location) {
		this.location = location;
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
        
        public Builder setName(String name) {
            if (name == null)
                throw new IllegalArgumentException();
            node.name = name;
            return this;
        }   
        
        public Builder setLogicalType(NodeLogicalType logicalType) {
        	node.logicalType = logicalType;
        	return this;
        }
        
        public Builder setManufacturer(Manufacturer manufacturer) {
        	node.manufacturer = manufacturer;
        	return this;
        }
        
        public Node build() {
        	
        	if (node.coordinates.getGatewayId() == 0)
        		throw new NodeConstructionException();
        	
        	if (node.name == null)
        		node.name = Node.nameFor(node.coordinates.getGatewayId(),node.coordinates.getAddress());
        	
            return node;
        }

		public void setLocation(Location location) {
			node.location = location;
		}
    }
    
    public String getName() {
        return this.name;
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
		return this.location;
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
    	return name;
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
        if (!this.name.equals(otherNode.name)) return false;
        if (this.logicalType != otherNode.logicalType) return false;
        if (this.manufacturer != otherNode.manufacturer) return false;
        
        return true;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;

        long result = 1;
        result = prime * result + name.hashCode();
        result = prime * result + coordinates.hashCode();
        result = prime * result + logicalType.hashCode();
        result = prime * result + manufacturer.hashCode();
        return (int)result;
    }
}
