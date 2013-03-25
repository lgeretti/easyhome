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
	
	public PersistentInfo findPersistentInfoById(long id) {
		return em.find(PersistentInfo.class, id);
	}
	
	public PersistentInfo findPersistentInfoByGatewayIdAndNuid(byte gatewayId, long nuid) {
		
        CriteriaBuilder b = em.getCriteriaBuilder();
        CriteriaQuery<PersistentInfo> criteria = b.createQuery(PersistentInfo.class);
        Root<PersistentInfo> info = criteria.from(PersistentInfo.class);
        criteria.select(info).where(
        			b.and(
        			b.equal(info.get("gatewayId"), gatewayId),
        			b.equal(info.get("nuid"), nuid)));
        
        TypedQuery<PersistentInfo> query = em.createQuery(criteria);
        
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

	public boolean insertLampStateFrom(long infoId) {
		
		PersistentInfo info = findPersistentInfoById(infoId);
		
		if (info == null)
			return false;
		
		// We accept trying to create an already existing lamp, but we do not do anything (PUT semantics)
		if (em.find(LampState.class, info.getId()) == null) {
			em.persist(new LampState(info));
		}
		
		return true;
	}
	
	public boolean insertFridgeStateFrom(long nodeId) {
		
		PersistentInfo info = findPersistentInfoById(nodeId);
		
		if (info == null)
			return false;
		
		// We accept trying to create an already existing fridge, but we do not do anything (PUT semantics)
		if (em.find(FridgeState.class, info.getId()) == null) {
			em.persist(new FridgeState(info));
		}
		
		return true;
	}
	
	public boolean insertPresenceSensorStateFrom(long nodeId) {
		
		PersistentInfo info = findPersistentInfoById(nodeId);
		
		if (info == null)
			return false;
		
		// We accept trying to create an already existing presence sensor, but we do not do anything (PUT semantics)
		if (em.find(PresenceSensorState.class, info.getId()) == null) {
			em.persist(new PresenceSensorState(info));
		}
		
		return true;
	}
}
