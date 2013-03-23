package it.uniud.easyhome.ejb;


import it.uniud.easyhome.devices.Pairing;
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
public class PairingEJB {

	@PersistenceContext(unitName = "EasyHome-JTA")
	private EntityManager em;
	
	
	public PersistentInfo findPersistentInfoById(long id) {
		return em.find(PersistentInfo.class, id);
	}
	
	public void insertPairing(PersistentInfo source, PersistentInfo destination) {
		Pairing pairing = new Pairing(source, destination);
		
		em.persist(pairing);
	}
	
	public Pairing findPairingBySourceId(long sourceId) {
		
        return em.find(Pairing.class, sourceId);
	}
	
	public List<Pairing> getPairings() {
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Pairing> criteria = builder.createQuery(Pairing.class);
        Root<Pairing> root = criteria.from(Pairing.class);
        criteria.select(root);
        
        TypedQuery<Pairing> query = em.createQuery(criteria);
        
        return query.getResultList();
	}
	
	public boolean removePairingBySourceId(long id) {
		Pairing pairing = findPairingBySourceId(id);
		
		if (pairing != null) {
			em.remove(pairing);
			return true;
		} else 
			return false;
	}
	
	public void removeAllPairings() {
        
        List<Pairing> pairings = getPairings();
        
        for (Pairing pairing: pairings)
        	em.remove(pairing);
	}
}
