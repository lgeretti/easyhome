package it.uniud.easyhome.network;


import it.uniud.easyhome.gateway.HubContext;

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
public class NetworkEJB {

	@PersistenceContext(unitName = "EasyHome-JTA")
	private EntityManager em;
	
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
	 * Updates a node guaranteed to be managed by the entity manager.
	 * 
	 * @param node The managed node that will be updated
	 */
	public void updateManaged(Node node) {
		em.merge(node);
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
        
        List<Node> nodes = getNodes();
        
        for (Node node: nodes)
        	em.remove(node);
	}
	
	public void insertJob(int id, NetworkJobType type, byte gatewayId, long nuid, short address, byte endpoint) {
		
		NetworkJob job = new NetworkJob(id, type, gatewayId, nuid, address, endpoint);
		
		em.persist(job);
	}
	
	public List<NetworkJob> getJobs() {
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<NetworkJob> criteria = builder.createQuery(NetworkJob.class);
        Root<NetworkJob> root = criteria.from(NetworkJob.class);
        criteria.select(root);
        
        TypedQuery<NetworkJob> query = em.createQuery(criteria);
        
        return query.getResultList();
	}
	
	public List<NetworkJob> getJobsByType(NetworkJobType type) {
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<NetworkJob> criteria = builder.createQuery(NetworkJob.class);
        Root<NetworkJob> job = criteria.from(NetworkJob.class);
        criteria.select(job).where(builder.equal(job.get("type"), type));
        
        TypedQuery<NetworkJob> query = em.createQuery(criteria);
        
        return query.getResultList();		
	}
	
	public NetworkJob findJobById(int jobId) {
		return em.find(NetworkJob.class, jobId);
	}
	
	public boolean resetJobDate(int jobId) {
        NetworkJob job = findJobById(jobId);
        
        boolean existed = (job != null);
        
        job.resetDate();
        
        if (existed)
        	em.merge(job);
        
        return existed;		
	}

	public boolean removeJobById(int jobId) {

        NetworkJob job = findJobById(jobId);
        
        boolean existed = (job != null);
        
        if (existed)
        	em.remove(job);
        
        return existed;
	}
}
