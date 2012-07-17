package it.uniud.easyhome.network;

import it.uniud.easyhome.common.ConcreteClassBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.xml.bind.annotation.*;

@Entity
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Node {

    @Id
    private int id;
    @Column(nullable = false, length = 200)
    private String name;
    
    private Node() {}

    public static class Builder implements ConcreteClassBuilder<Node> {
        
        private Node node;
        
        public Builder(int id) {
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
        
        public Node build() {
            
            return node;
        }
    }
    
    public int getId() {
        return this.id;
    }
    
    public String getName() {
        return this.name;
    }

    @Override
    public boolean equals(Object other) {
        
        if (!(other instanceof Node))
            throw new IllegalArgumentException();
        Node otherNode = (Node) other;
        
        if (this.id != otherNode.id) return false;
        if (!this.name.equals(otherNode.name)) return false;
        
        return true;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;

        int result = 1;
        result = prime * result + id;
        result = prime * result + name.hashCode();
        
        return result;
    }
}
