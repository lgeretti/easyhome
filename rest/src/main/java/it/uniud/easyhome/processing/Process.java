package it.uniud.easyhome.processing;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Process {
    
    public enum Session { STATEFUL, STATELESS };
    
    public enum Interaction { ASYNC, SYNC };
    
    @XmlElement(name="pid")
    private int pid;
    
    @XmlElement(name="session")
    private Session session;
    
    @XmlElement(name="interaction")
    private Interaction interaction;
    
    private volatile boolean stopped = false;
    
    @SuppressWarnings("unused")
    private Process() {}
    
    protected Process(int pid, Session session, Interaction interaction) {
        this.pid = pid;
        this.session = session;
        this.interaction = interaction;
    }
    
    public final int getPid() {
        return pid;
    }
    
    public final Session getSession() {
        return session;
    }
    
    public final Interaction getInteraction() {
        return interaction;
    }
	
	public void start() {
		// Empty implementation to be overridden
	}
	
	protected boolean isStopped() {
		return stopped;
	}
	
	public void stop() {
		stopped = true;
	}
	
	protected void println(String msg) {
    	System.out.println("Pr #" + pid + ": " + msg);
    }
}
