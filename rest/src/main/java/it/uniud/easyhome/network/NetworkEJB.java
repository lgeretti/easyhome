package it.uniud.easyhome.network;


import it.uniud.easyhome.exceptions.MultipleNodesFoundException;
import it.uniud.easyhome.exceptions.NodeNotFoundException;
import it.uniud.easyhome.gateway.HubContext;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.Parameter;
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
	
	public Node findNode(byte gid, short address) {
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Node> criteria = builder.createQuery(Node.class);
        Root<Node> node = criteria.from(Node.class);
        criteria.select(node).where(builder.equal(node.get("gatewayId"), gid))
        					 .where(builder.equal(node.get("address"), address));
        
        TypedQuery<Node> query = em.createQuery(criteria);
        
        List<Node> nodes = query.getResultList();
        if (nodes.size() == 0)
        	return null;
        if (nodes.size() > 1)
        	throw new MultipleNodesFoundException();
		return nodes.get(0);
	}
	
	public Node findNode(Node node) {
        return findNode(node.getGatewayId(),node.getAddress());
	}	
	
	/**
	 * Inserts a node.
	 * 
	 * @param node The node to insert
	 * @return True if the node already existed
	 */
	public boolean insertNode(Node node) {
        boolean existed = exists(node);
        
        if (!existed)
            em.persist(node);
        
        return existed;
	}
	
	public boolean exists(Node node) {
		Node persistedNode = findNode(node);
        return (persistedNode != null);
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
	 * @return True if the node already existed
	 */
	public boolean removeNode(byte gatewayId, short address) {
		
        Node node = findNode(gatewayId,address);
        
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
	
	public List<NetworkJob> getJobsByTypeAndCoordinates(NetworkJobType type, byte gatewayId, short address, byte endpoint) {
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<NetworkJob> criteria = builder.createQuery(NetworkJob.class);
        Root<NetworkJob> job = criteria.from(NetworkJob.class);
        criteria.select(job).where(builder.equal(job.get("type"), type));
        
        if (gatewayId != 0) {
        	criteria.where(builder.equal(job.get("gatewayId"), gatewayId))
        			.where(builder.equal(job.get("address"), address))
        			.where(builder.equal(job.get("endpoint"), endpoint));
        }
        
        TypedQuery<NetworkJob> query = em.createQuery(criteria);
        
        return query.getResultList();		
	}
	
	public NetworkJob findJobById(int jobId) {
		return em.find(NetworkJob.class, jobId);
	}
	
	public boolean resetJobById(int jobId) {
        NetworkJob job = findJobById(jobId);
        
        boolean existed = (job != null);
        
        if (existed) {
        	job.reset();
        	em.merge(job);
        }
        
        return existed;		
	}
	
	/***
	 * Resets one or more jobs (one if coordinates are defined, possibly many otherwise)
	 * 
	 * @return True if at least one job has been found
	 */
	public boolean resetJobsByTypeAndCoordinates(NetworkJobType type, byte gatewayId, short address, byte endpoint) {
		List<NetworkJob> jobs = getJobsByTypeAndCoordinates(type,gatewayId,address,endpoint);
		
		for (NetworkJob job : jobs) {
			job.reset();
			em.merge(job);
		}
		
		return jobs.size() > 0;
	}
	
	public void removeAllJobs() {
        
        List<NetworkJob> jobs = getJobs();
        
        for (NetworkJob job: jobs)
        	em.remove(job);
	}

	public boolean removeJobById(int jobId) {

        NetworkJob job = findJobById(jobId);
        
        boolean existed = (job != null);
        
        if (existed)
        	em.remove(job);
        
        return existed;
	}
	
	public int removeJobs(NetworkJobType type, byte gatewayId, short address, byte endpoint) {
		
		String queryString = "DELETE FROM NetworkJob j WHERE j.type = :t AND j.gatewayId = :g AND j.address = :a AND j.endpoint = :e";
		return em.createQuery(queryString)
				.setParameter("t", type)
				.setParameter("g", gatewayId)
				.setParameter("a",address)
				.setParameter("e", endpoint)
				.executeUpdate();
	}
	
	public int removeJobs(NetworkJobType type, byte gatewayId, short address) {

		String queryString = "DELETE FROM NetworkJob j WHERE j.type = :t AND j.gatewayId = :g AND j.address = :a";
		return em.createQuery(queryString)
				.setParameter("t", type)
				.setParameter("g", gatewayId)
				.setParameter("a",address)
				.executeUpdate();
	}
}
