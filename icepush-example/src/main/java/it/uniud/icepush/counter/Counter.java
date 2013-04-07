package it.uniud.icepush.counter;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

@ManagedBean(eager=true)
@ApplicationScoped
public class Counter {

    private static int count; // This simulates some global information you will get from a persistence layer or something else
    
    public synchronized void doIncrease() {
        count++;
    }

    public synchronized int getCount() {
        return count;
    }
}