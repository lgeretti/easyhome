package it.uniud.easyhome.ejb;


import it.uniud.easyhome.devices.Location;
import it.uniud.easyhome.devices.Manufacturer;
import it.uniud.easyhome.devices.PersistentInfo;
import it.uniud.easyhome.exceptions.*;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.NodeLogicalType;

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
public class NodeEJB {

	@PersistenceContext(unitName = "EasyHome-JTA")
	private EntityManager em;
	
	public List<Node> getNodes() {
		return getNodes((byte)0,0);
	}
	
	public List<Node> getNodes(byte gatewayId, long nuid) {
		
		boolean findSpecificNode = (gatewayId > 0);
		
        CriteriaBuilder b = em.getCriteriaBuilder();
        CriteriaQuery<Node> criteria = b.createQuery(Node.class);
        Root<Node> node = criteria.from(Node.class);
        criteria.select(node);
        
        if (findSpecificNode)
        	criteria.where(
        		b.and(
        			b.equal(node.get("coordinates").get("gatewayId"), gatewayId),
        			b.equal(node.get("coordinates").get("nuid"), nuid)));
        
        TypedQuery<Node> query = em.createQuery(criteria);
        
        List<Node> result = query.getResultList();
        
        if (findSpecificNode && result.size()>1)
        	throw new MultipleNodesFoundException();
        
        return result;
	}

	public List<Node> getInfrastructuralNodes() {
		
        CriteriaBuilder b = em.getCriteriaBuilder();
        CriteriaQuery<Node> criteria = b.createQuery(Node.class);
        Root<Node> node = criteria.from(Node.class);
        criteria.select(node).where(
        			b.or(
        				b.equal(node.get("logicalType"),NodeLogicalType.ROUTER),
        				b.equal(node.get("logicalType"),NodeLogicalType.COORDINATOR)));
        
        TypedQuery<Node> query = em.createQuery(criteria);
        
        return query.getResultList();
	}
	
	public Node findNode(byte gatewayId, short address) {
		
        CriteriaBuilder b = em.getCriteriaBuilder();
        CriteriaQuery<Node> criteria = b.createQuery(Node.class);
        Root<Node> node = criteria.from(Node.class);
        criteria.select(node).where(b.equal(node.get("coordinates").get("gatewayId"), gatewayId))
        					 .where(b.equal(node.get("coordinates").get("address"), address));
        
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
        
        acquirePersistentInfoOn(node);
        
        if (persistedNode == null) {
            em.persist(node);
        } else {
        	
        	if (node.getLogicalType() != NodeLogicalType.UNDEFINED)
        		persistedNode.setLogicalType(node.getLogicalType());
        	if (node.getManufacturer() != Manufacturer.UNDEFINED)
        		persistedNode.setManufacturer(node.getManufacturer());
        	if (node.getLocation() != null)
        		persistedNode.setLocation(node.getLocation());    
        	if (node.getName() != null)
        		persistedNode.setName(node.getName());            	

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
	
	private void acquirePersistentInfoOn(Node node) {
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<PersistentInfo> criteria = builder.createQuery(PersistentInfo.class);
        Root<PersistentInfo> info = criteria.from(PersistentInfo.class);
        criteria.select(info).where(builder.and(
        			builder.equal(info.get("gatewayId"), node.getCoordinates().getGatewayId()),
        			builder.equal(info.get("nuid"), node.getCoordinates().getNuid())));
        
        TypedQuery<PersistentInfo> query = em.createQuery(criteria);
        
        try {
        	PersistentInfo correspondingInfo = query.getSingleResult();
        	
        	if (correspondingInfo.getLocation() != null)
        		node.setLocation(correspondingInfo.getLocation());
        	if (correspondingInfo.getName() != null)
        		node.setName(correspondingInfo.getName());
        	
        } catch (NoResultException ex) {
        	// Nothing to do in this case
        }
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
	
	/**
	 * Get all the missing nodes.
	 * 
	 * We consider a missing node one that is not a coordinator (otherwise if no other node exists in the subnetwork, then
	 * the coordinator would be removed) and participates in no links (both source or destination) and that is of manufacturer DIGI or UNDEFINED.
	 * We accept to remove UNDEFINED-manufacturer nodes because it may happen that the node exits the network before being
	 * able to assess its manufacturer: in that case we would have a dangling node that would never be removed. Other manufacturers
	 * are unable to reply to the discovery protocol, hence we cannot really remove them: they will be removed manually. 
	 * Also, we exclude those nodes that currently have an announce grace job.
	 */
	private List<Node> getMissingNodes() {
		
		StringBuilder queryBuilder = new StringBuilder("SELECT n1 FROM Node n1 WHERE ")
												.append("n1.logicalType <> :clt AND ")
												.append("(n1.manufacturer=:m1 OR n1.manufacturer=:m2) AND ")
												.append("n1.id NOT IN (")
												.append("SELECT DISTINCT n2.id FROM Node n2, Link l WHERE ")
												.append("n2.coordinates.gatewayId = l.gatewayId AND (")
												.append("(n2.coordinates.nuid = l.source.nuid AND n2.coordinates.address = l.source.address) OR ")
												.append("(n2.coordinates.nuid = l.destination.nuid AND n2.coordinates.address = l.destination.address))) AND ")
												.append("n1.id NOT IN (")
												.append("SELECT DISTINCT n3.id FROM Node n3, NetworkJob j WHERE ")
												.append("n3.coordinates.gatewayId = j.gatewayId AND n3.coordinates.address = j.address AND ")
												.append("j.type = :t)");

		
		return em.createQuery(queryBuilder.toString(),Node.class)
				 .setParameter("clt", NodeLogicalType.COORDINATOR)
				 .setParameter("m1", Manufacturer.DIGI)
				 .setParameter("m2", Manufacturer.UNDEFINED)
				 .setParameter("t", NetworkJobType.NODE_ANNCE_GRACE)
				 .getResultList();
	}
	
	/**
	 * Clean up missing nodes
	 * 
	 * @return The cleaned nodes
	 */
	public List<Node> cleanupNodes() {
		
		List<Node> missingNodes = getMissingNodes();
		
		for (Node node : missingNodes) {
			em.remove(node);
		}
		
		return missingNodes;
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
        } catch (NoResultException ex) {
        	result = null;
        }
        
        return result;
	}
}
