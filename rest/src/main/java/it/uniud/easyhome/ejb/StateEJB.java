package it.uniud.easyhome.ejb;


import it.uniud.easyhome.devices.PersistentInfo;
import it.uniud.easyhome.devices.states.*;
import it.uniud.easyhome.exceptions.MultipleNodesFoundException;
import it.uniud.easyhome.network.Node;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

@Stateless
public class StateEJB {

	@PersistenceContext(unitName = "EasyHome-JTA")
	private EntityManager em;
	
	public <T> List<T> getStatesOfClass(Class<T> cls) {
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<T> criteria = builder.createQuery(cls);
        Root<T> info = criteria.from(cls);
        criteria.select(info);
        
        TypedQuery<T> query = em.createQuery(criteria);
        
        return query.getResultList();		
	}
	
	public <T> T findStateByInfoId(long id, Class<T> cls) {
				
		return em.find(cls, id);		
	}
	
	public Node findNodeById(long id) {
		return em.find(Node.class, id);
	}
	
	public Node findNodeByGatewayIdAndNuid(byte gatewayId, long nuid) {
		
        CriteriaBuilder b = em.getCriteriaBuilder();
        CriteriaQuery<Node> criteria = b.createQuery(Node.class);
        Root<Node> node = criteria.from(Node.class);
        criteria.select(node).where(
        			b.and(
        			b.equal(node.get("coordinates").get("gatewayId"), gatewayId),
        			b.equal(node.get("coordinates").get("nuid"), nuid)));
        
        TypedQuery<Node> query = em.createQuery(criteria);
        
        return query.getSingleResult();
	}

	public void removeAllStates() {
		removeStates(LampState.class);
		removeStates(FridgeState.class);
		removeStates(PresenceSensorState.class);
	}
	
	private <T> void removeStates(Class<T> cls) {
		List<T> states = getStatesOfClass(cls);
		
		for (T state : states)
			em.remove(state);		
	}

	public <T> void updateManagedState(T state) {
		em.merge(state);
	}

	public boolean insertLampStateFrom(long nodeId) {
		
		Node node = findNodeById(nodeId);
		
		if (node == null)
			return false;
		
		// We accept trying to create an already existing lamp, but we do not do anything (PUT semantics)
		if (em.find(LampState.class, node.getId()) == null) {
			em.persist(new LampState(node));
		}
		
		return true;
	}
	
	public boolean insertFridgeStateFrom(long nodeId) {
		
		Node node = findNodeById(nodeId);
		
		if (node == null)
			return false;
		
		// We accept trying to create an already existing fridge, but we do not do anything (PUT semantics)
		if (em.find(FridgeState.class, node.getId()) == null) {
			em.persist(new FridgeState(node));
		}
		
		return true;
	}
	
	public boolean insertPresenceSensorStateFrom(long nodeId) {
		
		Node node = findNodeById(nodeId);
		
		if (node == null)
			return false;
		
		// We accept trying to create an already existing presence sensor, but we do not do anything (PUT semantics)
		if (em.find(PresenceSensorState.class, node.getId()) == null) {
			em.persist(new PresenceSensorState(node));
		}
		
		return true;
	}
}
