package it.uniud.icepush.counter;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@ManagedBean(eager=true)
@ApplicationScoped
@XmlRootElement
public class Counter {

	// These static fields simulate some global information you will get from a persistence layer or something else
	@XmlElement(name="count")
    private static int count; 
	@XmlElement(name="timestamp")
	private static long timestamp;
    
    public synchronized void doIncrease() {
        count++;
        timestamp = System.currentTimeMillis();
    }

    public synchronized int getCount() {
        return count;
    }
    
    public synchronized long getTimestamp() {
    	return timestamp;
    }
}