package it.uniud.easyhome.jsf;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.icefaces.application.PushRenderer;

@ManagedBean
@ViewScoped
public class ClientAccess {
    
    private static final String PUSH_GROUP = "everyone";

    @ManagedProperty(value="#{counter}")
    private Counter counter;
    
    // ======================================
    // =           Public Methods           =
    // ======================================

    public ClientAccess() {
        PushRenderer.addCurrentSession(PUSH_GROUP);
    }
    
    public void doIncreaseCounter() {
        PushRenderer.render(PUSH_GROUP);
        counter.doIncrease();
    }
    
    public int getCount() {
        return counter.getCount();
    }
    
    public void setCounter(Counter counter) {
        this.counter = counter;
    }
    
    public Counter getCounter() {
        return this.counter;
    }
}