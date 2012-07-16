package it.uniud.easyhome;

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
    
    public static class Builder implements it.uniud.easyhome.Builder<Node> {
        
        private Node node;
        
        public Builder(int id) {
            
            node = new Node();
            node.id = id;
        }
        
        Builder setName(String name) {
            node.name = name;
            
            return this;
        }
        
        public Node build() {
            if (node.name == null)
                throw new IllegalStateException("The name has not been set.");
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
            throw new ClassCastException();
        Node otherNode = (Node) other;
        
        boolean result = true;
        result &= (this.id == otherNode.id);
        result &= (this.name.equals(otherNode.name));
        
        return result;
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
