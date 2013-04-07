package it.uniud.icepush.counter;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.icefaces.application.PushRenderer;

@ManagedBean
@ViewScoped 
public class CounterController {
    
    private static final String PUSH_GROUP = "counter";

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