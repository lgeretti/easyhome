package it.uniud.easyhome.network;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.uniud.easyhome.common.ConcreteClassBuilder;
import it.uniud.easyhome.devices.DeviceIdentifier;
import it.uniud.easyhome.devices.HomeAutomationDevice;
import it.uniud.easyhome.exceptions.EndpointNotFoundException;
import it.uniud.easyhome.exceptions.InvalidNodeTypeException;
import it.uniud.easyhome.exceptions.NodeConstructionException;

import javax.persistence.*;
import javax.xml.bind.annotation.*;

@Entity
@Table(name = "Node")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Node implements Serializable {

	private static final long serialVersionUID = -239673332667054641L;
	
	@Id
	private long id;
	@Column(nullable = false)
    private long nuid;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(nullable = false)
    private byte gatewayId;
    @Column(nullable = false)
    private short address;    
    @Column(nullable = false)
    private byte capability;
    @Column(nullable = false)
    private NodeLogicalType logicalType;
    @Column(nullable = false)
    private NodeLiveness liveness;
    @Column
    private String location;    
    @Column
    private Manufacturer manufacturer;
    
    @Embedded
    @ElementCollection
    @CollectionTable(name = "Neighbors")
    private List<Neighbor> neighbors = new ArrayList<Neighbor>();
    
    @Embedded
    @ElementCollection
    @CollectionTable(name = "Devices")
    private List<DeviceIdentifier> devices = new ArrayList<DeviceIdentifier>();

    private Node() {}
    
    public void setLogicalType(NodeLogicalType logicalType) {
    	this.logicalType = logicalType;
    }
    
    public void setLiveness(NodeLiveness liveness) {
    	this.liveness = liveness;
    }
    
    public void setManufacturer(Manufacturer manufacturer) {
    	this.manufacturer = manufacturer;
    }
    
    public void addNeighbor(Node node) {
    	neighbors.add(new Neighbor(node.getNuid(),node.getAddress()));
    }
    
	public void setNeighbors(List<Neighbor> neighbors) {
		this.neighbors = neighbors;
	}
    
	public void setEndpoints(List<Short> endpoints) {
		this.devices.clear();
		for (Short ep : endpoints) {
			this.devices.add(new DeviceIdentifier(ep,HomeAutomationDevice.UNKNOWN));
		}
	}
	
	public synchronized void addDevice(short endpoint, HomeAutomationDevice device) {
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
	
	public void setLocation(String location) throws InvalidNodeTypeException {
		if (this.logicalType == NodeLogicalType.END_DEVICE)
			throw new InvalidNodeTypeException();
		
		this.location = location;
	}

    public static class Builder implements ConcreteClassBuilder<Node> {
        
        private Node node;
        
        public Builder(long id, long nuid) {
            if (id == 0 || nuid == 0)
                throw new IllegalArgumentException();            
            node = new Node();
            node.id = id;
            node.nuid = nuid;
            
            node.logicalType = NodeLogicalType.UNDEFINED;
            node.manufacturer = Manufacturer.UNDEFINED;
            node.liveness = NodeLiveness.OK;
        }
        
        public Builder setName(String name) {
            if (name == null)
                throw new IllegalArgumentException();
            node.name = name;
            return this;
        }
        
        public Builder setGatewayId(byte gid) {
            if (gid == 0)
                throw new IllegalArgumentException();
            node.gatewayId = gid;
            return this;
        }
        
        public Builder setAddress(short address) {
            node.address = address;
            return this;
        }        
        
        public Builder setCapability(byte capability) {
        	if (capability == 0)
                throw new IllegalArgumentException();
            node.capability = capability;
            return this;        		
        }
        
        public Builder setLogicalType(NodeLogicalType logicalType) {
        	node.logicalType = logicalType;
        	return this;
        }
        
        public Builder setLiveness(NodeLiveness liveness) {
        	node.liveness = liveness;
        	return this;
        }
        
        public Builder setManufacturer(Manufacturer manufacturer) {
        	node.manufacturer = manufacturer;
        	return this;
        }
        
        public Node build() {
        	
        	if ((node.gatewayId == 0) || (node.capability == 0))
        		throw new NodeConstructionException();
        	
        	if (node.name == null)
        		node.name = String.valueOf(node.gatewayId) + ":" + Long.toHexString(0xFFFF & node.address);
        	
            return node;
        }
    }
    
    public long getNuid() {
        return this.nuid;
    }
    
    public String getHexNuid() {
    	return "0x" + Long.toHexString(nuid);
    }
    
    public String getName() {
        return this.name;
    }
    
    public byte getGatewayId() {
        return this.gatewayId;
    }
    
    public short getAddress() {
        return this.address;
    }
    
    public byte getCapability() {
    	return this.capability;
    }
    
    public Manufacturer getManufacturer() {
    	return this.manufacturer;
    }
    
    public NodeLogicalType getLogicalType() {
    	return this.logicalType;
    }
    
	public String getLocation() throws InvalidNodeTypeException {
		if (this.logicalType == NodeLogicalType.END_DEVICE)
			throw new InvalidNodeTypeException();
		
		return this.location;
	}
    
    public NodeLiveness getLiveness() {
    	return this.liveness;
    }
    
    public List<Neighbor> getNeighbors() {
    	return this.neighbors;
    }
    
    public List<Short> getEndpoints() {
    	List<Short> endpoints = new ArrayList<Short>();
    	
    	for (DeviceIdentifier devId: devices) {
    		endpoints.add(devId.getEndpoint());
    	}
    	
    	return endpoints;
    }    
    
    public Map<Short,HomeAutomationDevice> getMappedDevices() {
    	Map<Short,HomeAutomationDevice> result = new HashMap<Short,HomeAutomationDevice>(devices.size());

    	for (DeviceIdentifier devId: devices) {
    		result.put(devId.getEndpoint(), devId.getDevice());
    	}    	
    	return result;
    }

    @Override
    public boolean equals(Object other) {
        
        if (!(other instanceof Node))
            throw new IllegalArgumentException();
        Node otherNode = (Node) other;
        
        if (this.nuid != otherNode.nuid) return false;
        if (!this.name.equals(otherNode.name)) return false;
        if (this.gatewayId != otherNode.gatewayId) return false;
        if (this.address != otherNode.address) return false;
        if (this.capability != otherNode.capability) return false;
        if (this.logicalType != otherNode.logicalType) return false;
        if (this.liveness != otherNode.liveness) return false;
        if (this.manufacturer != otherNode.manufacturer) return false;
        
        return true;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;

        long result = 1;
        result = prime * result + nuid;
        result = prime * result + name.hashCode();
        result = prime * result + gatewayId;
        result = prime * result + address;
        result = prime * result + capability;
        result = prime * result + logicalType.hashCode();
        result = prime * result + liveness.hashCode();
        result = prime * result + manufacturer.hashCode();
        return (int)result;
    }
}
