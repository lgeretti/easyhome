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
import javax.persistence.criteria.Predicate;
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
	
	public PersistentInfo findPersistentInfoById(long infoId) {
		return em.find(PersistentInfo.class, infoId);
	}
	
	public List<Functionality> getFunctionalitiesByInfoId(long infoId) {
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Functionality> criteria = builder.createQuery(Functionality.class);
        Root<Functionality> func = criteria.from(Functionality.class);
        criteria.select(func).where(builder.equal(func.get("info").get("id"), infoId));
        
        TypedQuery<Functionality> query = em.createQuery(criteria);
		
		return query.getResultList();
	}

	public List<Functionality> getFunctionalitiesByFunctionalityTypeAndInfoId(long infoId, FunctionalityType funcType) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Functionality> criteria = builder.createQuery(Functionality.class);
        Root<Functionality> func = criteria.from(Functionality.class);
        
        Predicate wherePredicate = null;
        Predicate funcTypePredicate = null;
        if (funcType != null) {
        	funcTypePredicate = builder.equal(func.get("type"), funcType);
        }
        Predicate infoPredicate = null;
        if (infoId != 0) {
        	infoPredicate = builder.equal(func.get("info").get("id"), infoId);
        }        
        
        if (infoId != 0 && funcType != null) {
        	wherePredicate = builder.and(funcTypePredicate,infoPredicate);
        } else if (infoId != 0) {
        	wherePredicate = infoPredicate;
        } else 
        	wherePredicate = funcTypePredicate;
        
        criteria.select(func).where(wherePredicate);
        
        TypedQuery<Functionality> query = em.createQuery(criteria);
		
		return query.getResultList();
	}
}
