package it.uniud.easyhome.network;


import it.uniud.easyhome.devices.states.*;

import java.util.List;

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
public class StateEJB {

	@PersistenceContext(unitName = "EasyHome-JTA")
	private EntityManager em;
	
	public List<LampState> getLampStates() {
	
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<LampState> criteria = builder.createQuery(LampState.class);
        Root<LampState> info = criteria.from(LampState.class);
        criteria.select(info);
        
        TypedQuery<LampState> query = em.createQuery(criteria);
        
        return query.getResultList();
	}
	
	public List<FridgeState> getFridgeStates() {
	
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<FridgeState> criteria = builder.createQuery(FridgeState.class);
        Root<FridgeState> info = criteria.from(FridgeState.class);
        criteria.select(info);
        
        TypedQuery<FridgeState> query = em.createQuery(criteria);
        
        return query.getResultList();
	}
	
	public PersistentInfo findPersistentInfoById(long id) {
		return em.find(PersistentInfo.class, id);
	}
	
	public FridgeState findFridgeStateByInfoId(long id) {
		
		PersistentInfo info = findPersistentInfoById(id);
		
		if (info == null)
			return null;
		
		return em.find(FridgeState.class, info);
	}
	
	public LampState findLampStateByInfoId(long id) {
		
		PersistentInfo info = findPersistentInfoById(id);
		
		if (info == null)
			return null;
		
		return em.find(LampState.class, info);
	}

	public void removeAllStates() {
		removeLampStates();
		removeFridgeStates();
	}
	
	private void removeLampStates() {
		
		List<LampState> lampStates = getLampStates();
		
		for (LampState st : lampStates)
			em.remove(st);
	}
	
	private void removeFridgeStates() {
		
		List<FridgeState> fridgeStates = getFridgeStates();
		
		for (FridgeState st : fridgeStates)
			em.remove(st);
	}

	public void updateManagedLamp(LampState lamp) {
		em.merge(lamp);
	}
	
	public void updateManagedFridge(FridgeState fridge) {
		em.merge(fridge);
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
	
	public boolean insertFridgeStateFrom(long infoId) {
		
		PersistentInfo info = findPersistentInfoById(infoId);
		
		if (info == null)
			return false;
		
		// We accept trying to create an already existing fridge, but we do not do anything (PUT semantics)
		if (em.find(FridgeState.class, info.getId()) == null) {
			em.persist(new FridgeState(info));
		}
		
		return true;
	}
}
