package it.uniud.easyhome;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.xml.bind.annotation.*;

@Entity
@XmlRootElement
public class Node {

    @Id
    private int id;
    @Column(nullable = false, length = 200)
    private String name;
    
    public Node() {}
    
    public Node(int id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public void copyFrom(Node other) {
        this.id = other.id;
        this.name = other.name;
    }
    
    public int getId() {
        return this.id;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setId(int id) { 
        this.id = id;
    }
    
    public void setName(String name) {
        this.name = name;
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
