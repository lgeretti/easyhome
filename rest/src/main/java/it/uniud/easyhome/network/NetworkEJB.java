package it.uniud.easyhome.network;


import it.uniud.easyhome.exceptions.MultipleLinkException;
import it.uniud.easyhome.exceptions.MultipleNodesFoundException;
import it.uniud.easyhome.exceptions.NodeNotFoundException;
import it.uniud.easyhome.processing.NodeDiscoveryRequestProcess;

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

	public List<Node> getInfrastructuralNodes() {
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Node> criteria = builder.createQuery(Node.class);
        Root<Node> node = criteria.from(Node.class);
        criteria.select(node).where(
        		builder.or(
        				builder.equal(node.get("logicalType"),NodeLogicalType.ROUTER),
        				builder.equal(node.get("logicalType"),NodeLogicalType.COORDINATOR)));
        
        TypedQuery<Node> query = em.createQuery(criteria);
        
        return query.getResultList();
	}
	
	public Node findNode(byte gid, short address) {
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Node> criteria = builder.createQuery(Node.class);
        Root<Node> node = criteria.from(Node.class);
        criteria.select(node).where(builder.equal(node.get("coordinates").get("gatewayId"), gid))
        					 .where(builder.equal(node.get("coordinates").get("address"), address));
        
        TypedQuery<Node> query = em.createQuery(criteria);
        
        Node result = null;
        try {
        	result = query.getSingleResult();
        } catch (NonUniqueResultException ex) {
        	throw new MultipleNodesFoundException();
        } catch (NoResultException ex) {
        }
        return result;
	}
	
	public Node findNode(Node node) {
        return findNode(node.getCoordinates().getGatewayId(),node.getCoordinates().getAddress());
	}	
	
	public List<Node> getAllNodesOfType(NodeLogicalType type) {
		return em.createQuery("SELECT n FROM Node n WHERE n.logicalType = :t",Node.class)
				   .setParameter("t", type)
				   .getResultList();
	}
	
	/**
	 * Inserts a node.
	 * 
	 * @param node The node to insert
	 * @return True if the node already existed
	 */
	public boolean insertOrUpdateNode(Node node) {
        Node persistedNode = findNode(node);
        
        if (persistedNode == null)
            em.persist(node);
        else {
        	if (node.getLogicalType() != null)
        		persistedNode.setLogicalType(node.getLogicalType());
        	if (node.getManufacturer() != null)
        		persistedNode.setManufacturer(node.getManufacturer());
        	
        	em.merge(persistedNode);
        }
        return (persistedNode != null);
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
	 * Removes a node guaranteed to be managed by the entity manager.
	 */
	public void removeUnmanaged(Node node) {
		em.remove(node);
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
	
	public void insertLink(long id, byte gatewayId, LocalCoordinates source, LocalCoordinates destination) {
		Link link = new Link(id, gatewayId, source, destination);
		
		em.persist(link);
	}
	
	public void cleanupLinks() {
	
		StringBuilder queryBuilder = new StringBuilder("DELETE FROM Link l ")
		.append("WHERE l.timestamp<=:t ");
		
		TypedQuery<Link> query = em.createQuery(queryBuilder.toString(),Link.class)
		.setParameter("t", System.currentTimeMillis()-4*NodeDiscoveryRequestProcess.DISCOVERY_REQUEST_PERIOD_MS);
		
		query.executeUpdate();
	}
	
	public Link findLinkById(long id) {
		return em.find(Link.class, id);
	}
	

	public Link findLink(byte gatewayId, LocalCoordinates source, LocalCoordinates destination) {
	
		StringBuilder queryBuilder = new StringBuilder("SELECT l FROM Link l ")
											.append("WHERE l.gatewayId=:g ")
											.append("AND l.source=:s ")
											.append("AND l.destination=:d");
											
        TypedQuery<Link> query = em.createQuery(queryBuilder.toString(),Link.class)
        						   .setParameter("g", gatewayId)
        						   .setParameter("s", source)
        						   .setParameter("d", destination);
        
        Link result = null;
        
        try {
        	result = query.getSingleResult();
        } catch (NonUniqueResultException ex) {
        	throw new MultipleLinkException();
        } catch (NoResultException ex) {
        }
        
        return result;	
	}
	
	public List<Link> getLinks() {
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Link> criteria = builder.createQuery(Link.class);
        Root<Link> root = criteria.from(Link.class);
        criteria.select(root);
        
        TypedQuery<Link> query = em.createQuery(criteria);
        
        return query.getResultList();
	}
	
	public void updateLink(Link link) {
		Link retrieved = em.find(Link.class, link.getId());
		
		if (retrieved == null)
			throw new RuntimeException("Missing link to update");
		
		retrieved.update();
		
		em.merge(retrieved);
	}
	
	public boolean removeLink(long id) {
		Link link = em.find(Link.class, id);
		
		if (link != null) {
			em.detach(link);
			return true;
		} else 
			return false;
	}
	
	public void removeAllLinks() {
        
        List<Link> links = getLinks();
        
        for (Link link: links)
        	em.remove(link);
	}
	
	public void insertJob(int id, NetworkJobType type, byte gatewayId, short address, byte endpoint, byte tsn) {
		
		NetworkJob job = new NetworkJob(id, type, gatewayId, address, endpoint, tsn);
		
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

	public List<NetworkJob> getLatestJobs(NetworkJobType type, byte gatewayId, short address, byte endpoint, byte tsn) {
		
        StringBuilder queryBuilder = new StringBuilder("SELECT j FROM NetworkJob j WHERE j.id IN (SELECT MAX(j2.id) FROM NetworkJob j2");
        
        boolean atLeastOneClause = false;
        
        if (type != null || gatewayId != (byte)0 || tsn != (byte)0) {
        	queryBuilder.append(" WHERE ");
        }
        if (type != null) {
        	atLeastOneClause = true;
        	queryBuilder.append(" j2.type=:t ");
        }
        if (gatewayId != (byte)0) {
        	if (atLeastOneClause)
        		queryBuilder.append(" AND ");
        	atLeastOneClause = true;
        	queryBuilder.append(" j2.gatewayId=:g AND j2.address=:a AND j2.endpoint=:e ");
        }
        if (tsn != (byte)0) {
        	if (atLeastOneClause)
        		queryBuilder.append(" AND ");
        	queryBuilder.append(" j2.tsn=:n");
        }
        queryBuilder.append(" GROUP BY j2.gatewayId, j2.address, j2.endpoint)");
        
        TypedQuery<NetworkJob> query = em.createQuery(queryBuilder.toString(),NetworkJob.class);
        
        if (type != null)
        	query.setParameter("t",type);
        
        if (gatewayId != (byte)0)
        	query.setParameter("g",gatewayId)
			  	 .setParameter("a",address)
			  	 .setParameter("e",endpoint);
        
        if (tsn != (byte)0)
        	query.setParameter("n", tsn);
        
        return query.getResultList();		
	}
	
	public NetworkJob findJobById(int jobId) {
		return em.find(NetworkJob.class, jobId);
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
	
	/**
	 * Get all the missing nodes.
	 * 
	 * We consider a missing node one that is not a coordinator (otherwise if no other node exists in the subnetwork, then
	 * the coordinator would be removed) and participates in no links (both source or destination) and that is of manufacturer DIGI or UNDEFINED.
	 * We accept to remove UNDEFINED-manufacturer nodes because it may happen that the node exits the network before being
	 * able to assess its manufacturer: in that case we would have a dangling node that would never be removed. Other manufacturers
	 * are unable to reply to the discovery protocol, hence we cannot really remove them: they will be removed manually. 
	 */
	private List<Node> getMissingNodes() {
		
		StringBuilder queryBuilder = new StringBuilder("SELECT n FROM Node n WHERE ")
												.append("(n.manufacturer=:m1 OR n.manufacturer=:m2) ")
												.append("AND n.logicalType <> :clt ")
												.append("AND n.coordinates.nuid NOT IN ")
												.append("(SELECT l1.source.nuid FROM Link l1)")
												.append("AND n.coordinates.nuid NOT IN ")
												.append("(SELECT l2.destination.nuid FROM Link l2)");

		return em.createQuery(queryBuilder.toString(),Node.class)
				 .setParameter("clt",NodeLogicalType.COORDINATOR)
				 .setParameter("m1",Manufacturer.DIGI)
				 .setParameter("m2", Manufacturer.UNDEFINED)
				 .getResultList();
	}
	
	
	/**
	 * Clean up missing nodes and their corresponding jobs. 
	 * 
	 * NOTE: may still remove an end device that announced itself and for which no neighbor discovered the presence yet (acceptable).
	 * 
	 * @return The cleaned nodes
	 */
	public List<Node> cleanupNodesAndJobs() {
		
		List<Node> missingNodes = getMissingNodes();
		
		String deleteQueryString = new StringBuilder("DELETE FROM NetworkJob j WHERE j.gatewayId=:g AND j.address=:a").toString();
		
		for (Node node : missingNodes) {
			em.remove(node);
			
			em.createQuery(deleteQueryString)
							.setParameter("g", node.getCoordinates().getGatewayId())
							.setParameter("a", node.getCoordinates().getAddress()).executeUpdate();
		}
		
		return missingNodes;
	}
}
