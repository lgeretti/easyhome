package it.uniud.easyhome.jsf.counter;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

@ManagedBean(eager=true)
@ApplicationScoped
public class Counter {

    private int count;
    
    public synchronized void doIncrease() {
        count++;
    }

    public synchronized int getCount() {
        return count;
    }
}