package it.uniud.easyhome.jsf;

import java.util.Random;

import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.rest.NetworkResourceEJB;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.icefaces.application.PushRenderer;

@ManagedBean
@ViewScoped 
public class NodeListController {
    
    private static final String PUSH_GROUP = "nodes";
    
    private Random rnd;
    
	@EJB
	private NetworkResourceEJB networkEjb;

    //@ManagedProperty(value="#{nodes}")
    //private NodeList nodes = new NodeList();

    @PostConstruct
    public void init() {
    	rnd = new Random();
        PushRenderer.addCurrentView(PUSH_GROUP);
    }
    
    public void doAdd() {        
    	Node node = new Node.Builder(rnd.nextLong())
    				.setAddress((short)(rnd.nextInt() & 0xFFFF))
    				.setGatewayId((byte)rnd.nextInt(255))
    				.setCapability((byte)rnd.nextInt(255))
    				.build();
        networkEjb.insertOrUpdateNode(node);
        PushRenderer.render(PUSH_GROUP);
    }
    
    public void doClear() {        
        networkEjb.removeAllNodes();
        PushRenderer.render(PUSH_GROUP);
    }
    
    public int getSize() {
        return networkEjb.getNodes().size();
    }
    
    /*public void setNodes(NodeList nodes) {
        this.nodes = nodes;
    }*/

}   