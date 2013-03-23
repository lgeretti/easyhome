package it.uniud.easyhome.ejb;


import it.uniud.easyhome.network.Location;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.network.PersistentInfo;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

@Stateless
public class PersistentInfoEJB {

	@PersistenceContext(unitName = "EasyHome-JTA")
	private EntityManager em;
	
	public List<PersistentInfo> getPersistentInfos() {
	
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<PersistentInfo> criteria = builder.createQuery(PersistentInfo.class);
        Root<PersistentInfo> info = criteria.from(PersistentInfo.class);
        criteria.select(info);
        
        TypedQuery<PersistentInfo> query = em.createQuery(criteria);
        
        return query.getResultList();
	}
	
	public PersistentInfo getPersistentInfo(byte gatewayId, long nuid) {
		
		boolean findSpecificNode = (gatewayId > 0);
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<PersistentInfo> criteria = builder.createQuery(PersistentInfo.class);
        Root<PersistentInfo> info = criteria.from(PersistentInfo.class);
        criteria.select(info);
        
        if (findSpecificNode)
        	criteria.where(builder.and(
        			builder.equal(info.get("gatewayId"), gatewayId),
        			builder.equal(info.get("nuid"), nuid)));
        
        TypedQuery<PersistentInfo> query = em.createQuery(criteria);
        
        PersistentInfo result = null;
        
        try {
        	result = query.getSingleResult();
        } catch (NoResultException ex) { }
        
        return result;
	}
	
	public PersistentInfo findPersistentInfoById(long id) {
		return em.find(PersistentInfo.class, id);
	}
	
	public PersistentInfo findPersistentInfo(PersistentInfo info) {
        return getPersistentInfo(info.getGatewayId(),info.getNuid());
	}	
	
	public List<Node> getAllNodesOfType(NodeLogicalType type) {
		return em.createQuery("SELECT n FROM Node n WHERE n.logicalType = :t",Node.class)
				   .setParameter("t", type)
				   .getResultList();
	}
	
	public void insertPersistentInfo(PersistentInfo info) {
        em.persist(info);
        updateNodeFor(info);
	}
	
	public boolean exists(PersistentInfo info) {
		PersistentInfo storedInfo = findPersistentInfo(info);
        return (storedInfo != null);
	}
	
	public void updatedUnmanaged(PersistentInfo info) {
		em.merge(info);
		updateNodeFor(info);
	}

	public void removeUnmanaged(PersistentInfo info) {
		em.remove(info);
	}
	
	/**
	 * Removes a node.
	 * 
	 * @return True if the node already existed
	 */
	public boolean removeInfo(byte gatewayId, long nuid) {
		
        PersistentInfo info = getPersistentInfo(gatewayId,nuid);
        
        boolean existed = (info != null);
        
        if (existed)
        	em.remove(info);
        
        return existed;
	}
	
	public void removeAllPersistedInfos() {
        
        List<PersistentInfo> infos = getPersistentInfos();
        
        for (PersistentInfo info: infos)
        	em.remove(info);
	}
	
	private void updateNodeFor(PersistentInfo info) {
			
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
	
	public Location getLocation(String locationName) {
		
		Location result = null;
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Location> criteria = builder.createQuery(Location.class);
        Root<Location> loc = criteria.from(Location.class);
        criteria.select(loc).where(builder.equal(loc.get("name"), locationName));
        
        TypedQuery<Location> query = em.createQuery(criteria);
        
        try {
        	result = query.getSingleResult();
        } catch (NoResultException|NonUniqueResultException ex) {
        	result = null;
        }
        
        return result;
	}
	
	public List<PersistentInfo> getPersistentInfosByLocationId(int locationId) {
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<PersistentInfo> criteria = builder.createQuery(PersistentInfo.class);
        Root<PersistentInfo> info = criteria.from(PersistentInfo.class);
        criteria.select(info).where(builder.equal(info.get("location").get("id"), locationId));
        
        TypedQuery<PersistentInfo> query = em.createQuery(criteria);
		
		return query.getResultList();
	}
}
