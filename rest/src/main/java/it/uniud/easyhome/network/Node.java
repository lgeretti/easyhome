package it.uniud.easyhome.network;

import it.uniud.easyhome.common.ConcreteClassBuilder;
import it.uniud.easyhome.exceptions.NodeConstructionException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.xml.bind.annotation.*;

@Entity
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Node {

    @Id
    private long id;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(nullable = false)
    private byte gatewayId;
    @Column(nullable = false)
    private short address;    
    
    private Node() {}

    public static class Builder implements ConcreteClassBuilder<Node> {
        
        private Node node;
        
        public Builder(long id) {
            if (id <= 0)
                throw new IllegalArgumentException();            
            node = new Node();
            node.id = id;
        }
        
        public Builder setName(String name) {
            if (name == null)
                throw new IllegalArgumentException();
            node.name = name;
            return this;
        }
        
        public Builder setGatewayId(byte gid) {
            if (gid <= 0)
                throw new IllegalArgumentException();
            node.gatewayId = gid;
            return this;
        }
        
        public Builder setAddress(short address) {
            if (address <= 0)
                throw new IllegalArgumentException();
            node.address = address;
            return this;
        }        
        
        public Node build() {
            
        	if ((node.gatewayId == 0) || (node.address == 0) || (node.name == null))
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

    @Override
    public boolean equals(Object other) {
        
        if (!(other instanceof Node))
            throw new IllegalArgumentException();
        Node otherNode = (Node) other;
        
        if (this.id != otherNode.id) return false;
        if (!this.name.equals(otherNode.name)) return false;
        if (!(this.gatewayId == otherNode.gatewayId)) return false;
        if (!(this.address == otherNode.address)) return false;
        
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
        
        return (int)result;
    }
}