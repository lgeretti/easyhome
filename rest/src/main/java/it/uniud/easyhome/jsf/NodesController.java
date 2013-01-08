package it.uniud.easyhome.jsf;

import java.util.List;
import java.util.Random;

import it.uniud.easyhome.network.NetworkEJB;
import it.uniud.easyhome.network.Node;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.bean.ViewScoped;

import org.icefaces.application.PortableRenderer;
import org.icefaces.application.PushRenderer;

@ManagedBean
@SessionScoped 
public class NodesController implements Runnable {
    
    private static final String PUSH_GROUP = "nodes";
    
    private Random rnd;
    
    private volatile boolean stopped;
    
    private PortableRenderer pRenderer;
    
	@EJB
	private NetworkEJB networkEjb;
	
    @PostConstruct
    public void init() {
    	rnd = new Random();
        PushRenderer.addCurrentView(PUSH_GROUP);
        Thread thr = new Thread(this);
        thr.start();
        pRenderer = PushRenderer.getPortableRenderer();
    }
    
    public List<Node> getNodes() {
    	return networkEjb.getNodes();
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

	@Override
	public void run() {
		try {
			
			while(!stopped) {
				Thread.sleep(1000);
		    	Node node = new Node.Builder(rnd.nextLong())
				.setAddress((short)(rnd.nextInt() & 0xFFFF))
				.setGatewayId((byte)rnd.nextInt(255))
				.setCapability((byte)rnd.nextInt(255))
				.build();
		    	networkEjb.insertOrUpdateNode(node);
		    	pRenderer.render(PUSH_GROUP);
			}	
		} catch (InterruptedException ex) { }
		
	}
	
	public void doStop() {
		stopped = true;
	}

}   