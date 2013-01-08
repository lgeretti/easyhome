package it.uniud.easyhome.jsf;

import java.util.List;

import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.rest.NetworkResourceEJB;

import javax.ejb.EJB;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

@ManagedBean(eager=true)
@ApplicationScoped
public class NodeList {
	
	//@EJB
	//private NetworkResourceEJB networkEjb;
    
	private int size;
	
	public int getSize() {
		return size;//networkEjb.getNodes().size();
	}
	
	public void addNode() {
		size++;
	}
	
	public void clear() {
		size = 0;
	}
}