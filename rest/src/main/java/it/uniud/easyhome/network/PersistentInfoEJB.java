package it.uniud.easyhome.network;


import it.uniud.easyhome.exceptions.MultipleNodesFoundException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

@Stateless
public class PersistentInfoEJB {

	@PersistenceContext(unitName = "EasyHome-JTA")
	private EntityManager em;
	
	public List<NodePersistentInfo> getPersistentInfos() {
	
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<NodePersistentInfo> criteria = builder.createQuery(NodePersistentInfo.class);
        Root<NodePersistentInfo> info = criteria.from(NodePersistentInfo.class);
        criteria.select(info);
        
        TypedQuery<NodePersistentInfo> query = em.createQuery(criteria);
        
        return query.getResultList();
	}
	
	public NodePersistentInfo getPersistentInfo(byte gatewayId, long nuid) {
		
		boolean findSpecificNode = (gatewayId > 0);
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<NodePersistentInfo> criteria = builder.createQuery(NodePersistentInfo.class);
        Root<NodePersistentInfo> info = criteria.from(NodePersistentInfo.class);
        criteria.select(info);
        
        if (findSpecificNode)
        	criteria.where(builder.and(
        			builder.equal(info.get("gatewayId"), gatewayId),
        			builder.equal(info.get("nuid"), nuid)));
        
        TypedQuery<NodePersistentInfo> query = em.createQuery(criteria);
        
        NodePersistentInfo result = null;
        
        try {
        	result = query.getSingleResult();
        } catch (NoResultException ex) { }
        
        return result;
	}
	
	public NodePersistentInfo findPersistentInfo(NodePersistentInfo info) {
        return getPersistentInfo(info.getGatewayId(),info.getNuid());
	}	
	
	public List<Node> getAllNodesOfType(NodeLogicalType type) {
		return em.createQuery("SELECT n FROM Node n WHERE n.logicalType = :t",Node.class)
				   .setParameter("t", type)
				   .getResultList();
	}
	
	public void insertPersistentInfo(NodePersistentInfo info) {
        em.persist(info);
        updateNodeFor(info);
	}
	
	public boolean exists(NodePersistentInfo info) {
		NodePersistentInfo storedInfo = findPersistentInfo(info);
        return (storedInfo != null);
	}
	
	public void updatedUnmanaged(NodePersistentInfo info) {
		em.merge(info);
		updateNodeFor(info);
	}

	public void removeUnmanaged(NodePersistentInfo info) {
		em.remove(info);
	}
	
	/**
	 * Removes a node.
	 * 
	 * @return True if the node already existed
	 */
	public boolean removeInfo(byte gatewayId, long nuid) {
		
        NodePersistentInfo info = getPersistentInfo(gatewayId,nuid);
        
        boolean existed = (info != null);
        
        if (existed)
        	em.remove(info);
        
        return existed;
	}
	
	public void removeAllPersistedInfos() {
        
        List<NodePersistentInfo> infos = getPersistentInfos();
        
        for (NodePersistentInfo info: infos)
        	em.remove(info);
	}
	
	private void updateNodeFor(NodePersistentInfo info) {
			
	        CriteriaBuilder builder = em.getCriteriaBuilder();
	        CriteriaQuery<Node> criteria = builder.createQuery(Node.class);
	        Root<Node> node = criteria.from(Node.class);
	        criteria.select(node).where(builder.and(
	        			builder.equal(node.get("coordinates").get("gatewayId"), info.getGatewayId()),
	        			builder.equal(node.get("coordinates").get("nuid"), info.getNuid())));
	        
	        TypedQuery<Node> query = em.createQuery(criteria);
	        
	        try {
	        	Node correspondingNode = query.getSingleResult();
	        	
	        	correspondingNode.setLocation(info.getLocation());
	        	if (info.getName() != null)
	        		correspondingNode.setName(info.getName());
	        	
	        	em.merge(correspondingNode);
	        	
	        } catch (NoResultException ex) {
	        	// Nothing to do in this case
	        }
	}
}
