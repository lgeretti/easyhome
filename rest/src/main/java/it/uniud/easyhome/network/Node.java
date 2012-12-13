package it.uniud.easyhome.network;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import it.uniud.easyhome.common.ConcreteClassBuilder;
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
    @Column
    private String location;
    @Column(nullable = false)
    private NodeLiveness liveness;
    @ElementCollection
    @CollectionTable(name = "NeighborIds")
    private List<Long> neighborIds = new ArrayList<Long>();
    
    private Node() {}
    
    public void setLogicalType(NodeLogicalType logicalType) {
    	this.logicalType = logicalType;
    }
    
    public void setLiveness(NodeLiveness liveness) {
    	this.liveness = liveness;
    }
    
    public void addNeighbor(Node node) {
    	neighborIds.add(node.getId());
    }
    
	public void setNeighbors(List<Long> neighborIds) {
		this.neighborIds = neighborIds;
	}
	
	public void setLocation(String location) throws InvalidNodeTypeException {
		if (this.logicalType == NodeLogicalType.END_DEVICE)
			throw new InvalidNodeTypeException();
		
		this.location = location;
	}

    public static class Builder implements ConcreteClassBuilder<Node> {
        
        private Node node;
        
        public Builder(long id) {
            if (id == 0)
                throw new IllegalArgumentException();            
            node = new Node();
            node.id = id;
            
            node.name = Long.toHexString(id);
            node.logicalType = NodeLogicalType.UNDEFINED;
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
            if (address == 0)
                throw new IllegalArgumentException();
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
        
        public Node build() {
            
        	if ((node.gatewayId == 0) || (node.address == 0) || (node.capability == 0))
        		throw new NodeConstructionException();
        	
            return node;
        }
    }
    
    public long getId() {
        return this.id;
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
    
    public List<Long> getNeighborIds() {
    	return this.neighborIds;
    }

    @Override
    public boolean equals(Object other) {
        
        if (!(other instanceof Node))
            throw new IllegalArgumentException();
        Node otherNode = (Node) other;
        
        if (this.id != otherNode.id) return false;
        if (!this.name.equals(otherNode.name)) return false;
        if (!(this.gatewayId == otherNode.gatewayId)) return false;
        if (!(this.address == otherNode.address)) return false;
        if (!(this.capability == otherNode.capability)) return false;
        if (!(this.logicalType == otherNode.logicalType)) return false;
        if (!(this.liveness == otherNode.liveness)) return false;
        
        return true;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;

        long result = 1;
        result = prime * result + id;
        result = prime * result + name.hashCode();
        result = prime * result + gatewayId;
        result = prime * result + address;
        result = prime * result + capability;
        result = prime * result + logicalType.hashCode();
        result = prime * result + liveness.hashCode();
        return (int)result;
    }
}
