package it.uniud.easyhome.rest;

import it.uniud.easyhome.network.Node;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

@Stateless
public class NetworkResourceEJB {

	@PersistenceContext(unitName = "EasyHome-JTA")
	private EntityManager em;
	
	private int val = 0;
	
	public String getStatusMessage() {
		
		return "Counter: " + String.valueOf(++val) + "; EntityManager: " + (em == null ? "null" : (em.isOpen()? "open" : "closed"));
	}
	
	public List<Node> getNodes() {
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Node> criteria = builder.createQuery(Node.class);
        Root<Node> root = criteria.from(Node.class);
        criteria.select(root);
        
        TypedQuery<Node> query = em.createQuery(criteria);
        
        return query.getResultList();
	}
	
	public Node findNodeById(long nodeId) {
		return em.find(Node.class, nodeId);
	}
	
	/**
	 * Either inserts or updates a node.
	 * 
	 * @param node The node to insert or update
	 * @return True if the node already existed
	 */
	public boolean insertOrUpdateNode(Node node) {
        Node persistedNode = findNodeById(node.getId());
        boolean existed = (persistedNode != null);
        
        if (!existed) {
            em.persist(node);
        } else {
            em.merge(node);
        }
        
        return existed;
	}
	
	/**
	 * Removes a node.
	 * 
	 * @param nodeId The node identifier
	 * @return True if the node already existed
	 */
	public boolean removeNodeById(long nodeId) {
		
        Node node = findNodeById(nodeId);
        
        boolean existed = (node != null);
        
        if (existed)
        	em.remove(node);
        
        return existed;
	}
	
	public void removeAllNodes() {
		
        Query query = em.createQuery("DELETE FROM Node");
        query.executeUpdate();
	}
}
