package it.uniud.easyhome.jsf.counter;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.icefaces.application.PushRenderer;

@ManagedBean(eager = true)
@ViewScoped
public class PushController {
    
    private static final String PUSH_GROUP = "everyone";

    @ManagedProperty(value="#{counter}")
    private Counter counter;

    @PostConstruct
    public void init() {
        PushRenderer.addCurrentView(PUSH_GROUP);
    }
    
    public void doIncreaseCounter() {        
        counter.doIncrease();
        PushRenderer.render(PUSH_GROUP);
    }
    
    public int getCount() {
        return counter.getCount();
    }
    
    public void setCounter(Counter counter) {
        this.counter = counter;
    }

}   