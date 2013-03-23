package it.uniud.easyhome.ejb;

import it.uniud.easyhome.devices.Functionality;
import it.uniud.easyhome.devices.FunctionalityType;
import it.uniud.easyhome.devices.PersistentInfo;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

@Stateless
public class FunctionalityEJB {

	@PersistenceContext(unitName = "EasyHome-JTA")
	private EntityManager em;
	
	public List<Functionality> getFunctionalities() {
	
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Functionality> criteria = builder.createQuery(Functionality.class);
        Root<Functionality> funcs = criteria.from(Functionality.class);
        criteria.select(funcs);
        
        TypedQuery<Functionality> query = em.createQuery(criteria);
        
        return query.getResultList();
	}
	
	public void insertFunctionality(Functionality func) {
        em.persist(func);
	}
	
	public Functionality findFunctionalityById(long id) {
		return em.find(Functionality.class, id);
	}

	public void removeUnmanaged(Functionality func) {
		em.remove(func);
	}
	
	public void removeAllFunctionalities() {
        
        List<Functionality> funcs = getFunctionalities();
        
        for (Functionality func: funcs)
        	em.remove(func);
	}
	
	public PersistentInfo findPersistentInfoById(long id) {
		return em.find(PersistentInfo.class, id);
	}
	
	public List<Functionality> getFunctionalitiesByDeviceId(long deviceId) {
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Functionality> criteria = builder.createQuery(Functionality.class);
        Root<Functionality> func = criteria.from(Functionality.class);
        criteria.select(func).where(builder.equal(func.get("device").get("id"), deviceId));
        
        TypedQuery<Functionality> query = em.createQuery(criteria);
		
		return query.getResultList();
	}

	public List<Functionality> getFunctionalitiesByFunctionalityType(FunctionalityType funcType) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Functionality> criteria = builder.createQuery(Functionality.class);
        Root<Functionality> func = criteria.from(Functionality.class);
        criteria.select(func).where(builder.equal(func.get("functionalityType"), funcType));
        
        TypedQuery<Functionality> query = em.createQuery(criteria);
		
		return query.getResultList();
	}
}
