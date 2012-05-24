package it.uniud.easyhome.jsf;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

@ManagedBean
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