package it.uniud.easyhome.processing;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Process {
    
    public enum Session { STATEFUL, STATELESS };
    
    public enum Interaction { ASYNC, SYNC };
    
    private int pid;
    
    private Session session;
    
    private Interaction interaction;
    
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
}
