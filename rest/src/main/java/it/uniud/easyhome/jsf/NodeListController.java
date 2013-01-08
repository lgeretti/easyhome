package it.uniud.easyhome.jsf;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.icefaces.application.PushRenderer;

@ManagedBean
@ViewScoped 
public class NodeListController {
    
    private static final String PUSH_GROUP = "nodes";

    //@ManagedProperty(value="#{nodes}")
    private NodeList nodes = new NodeList();

    @PostConstruct
    public void init() {
        PushRenderer.addCurrentView(PUSH_GROUP);
    }
    
    public void doAdd() {        
        nodes.addNode();
        PushRenderer.render(PUSH_GROUP);
    }
    
    public void doClear() {        
        nodes.clear();
        PushRenderer.render(PUSH_GROUP);
    }
    
    public int getSize() {
        return nodes.getSize();
    }
    
    public void setNodes(NodeList nodes) {
        this.nodes = nodes;
    }

}   