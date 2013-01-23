package it.uniud.easyhome.network;


import it.uniud.easyhome.exceptions.MultipleNodesFoundException;
import it.uniud.easyhome.exceptions.NodeNotFoundException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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

	public List<Node> getReachableNodes() {

		List<Node> coordinators = em.createQuery("SELECT n FROM Node n WHERE n.logicalType = :t",Node.class)
											   .setParameter("t", NodeLogicalType.COORDINATOR)
											   .getResultList();
		
		List<Node> nodesFound = new ArrayList<Node>();

		Set<NodeCompactCoordinates> coordsMissing = new HashSet<NodeCompactCoordinates>();
		
		for (Node coord : coordinators) {
			byte currentGatewayId = coord.getGatewayId();
			short currentAddress = coord.getAddress();

			Set<Short> currentAddressesFound = new HashSet<Short>();
			Set<Node> currentNodesFound = new HashSet<Node>();
			Queue<Short> currentAddressesToCheck = new ConcurrentLinkedQueue<Short>();

			currentAddressesFound.add(currentAddress);
			traverseReachableNodes(coordsMissing, currentNodesFound, currentAddressesFound, currentAddressesToCheck, currentAddress, currentGatewayId);
			
			nodesFound.addAll(currentNodesFound);
		}
		
		return nodesFound;
	}

	private void traverseReachableNodes(Set<NodeCompactCoordinates> coordsMissing, Set<Node> nodesFound, Set<Short> addressesFound, Queue<Short> addressesToCheck, 
										short currentAddress, byte currentGatewayId) {
		
		Node node = findNode(currentGatewayId,currentAddress);
		
		if (node == null)
			coordsMissing.add(new NodeCompactCoordinates(currentGatewayId,currentAddress));
		else {
			System.out.println("Looking up " + node.getName());
	
			nodesFound.add(node);
			
			if (node.getLogicalType() == NodeLogicalType.ROUTER || node.getLogicalType() == NodeLogicalType.COORDINATOR) {
			
				System.out.println("Found " + node.getNeighbors().size() + " neighbors");
				
				for (Neighbor neighbor : node.getNeighbors()) {
					if (!addressesFound.contains(neighbor.getAddress())) {
						System.out.println("Address " + neighbor + " is new, adding");
						addressesToCheck.add(neighbor.getAddress());
						addressesFound.add(neighbor.getAddress());
					} else {
						System.out.println("Address " + neighbor + " is already present, not adding");
					}
				}
			}
			
			Short nextAddress = addressesToCheck.poll();
			if (nextAddress != null)
				traverseReachableNodes(coordsMissing, nodesFound, addressesFound, addressesToCheck, nextAddress, currentGatewayId);
		}
	}

	public void pruneUnreachableNodes() {
		
		List<Node> nodes = getNodes();
		List<Node> reachableNodes = getReachableNodes();
		
		for (Node node : nodes) {
			boolean found = false;
			
			for (Node reachableNode : reachableNodes) {
				if (node.equals(reachableNode)) {
					found = true;
					break;
				}
			}
			
			if (!found) {
				em.remove(node);
				System.out.println("Removed node " + node.getName());
			}
		}
	}

	public void acknowledgeNewReachableNodes() {
		
		List<Node> nodes = getNodes();
		List<Node> reachableNodes = getReachableNodes();
		
		for (Node reachableNode : reachableNodes) {
			boolean found = false;
			
			for (Node node : nodes) {
				if (reachableNode.equals(node)) {
					found = true;
					break;
				}
			}
			
			if (!found) {
				insertNode(reachableNode);
				System.out.println("Inserted node " + reachableNode.getName());
			}
		}
	}
}
