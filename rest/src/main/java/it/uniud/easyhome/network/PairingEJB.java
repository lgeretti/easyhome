package it.uniud.easyhome.network;


import it.uniud.easyhome.exceptions.MultipleLinkException;
import it.uniud.easyhome.exceptions.MultipleNodesFoundException;
import it.uniud.easyhome.exceptions.NodeNotFoundException;
import it.uniud.easyhome.processing.NodeAnnceRegistrationProcess;
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
public class PairingEJB {

	@PersistenceContext(unitName = "EasyHome-JTA")
	private EntityManager em;
	
	
	public PersistentInfo findPersistentInfoById(long id) {
		return em.find(PersistentInfo.class, id);
	}
	
	public void insertPairing(long id, PersistentInfo source, PersistentInfo destination) {
		Pairing pairing = new Pairing(id, source, destination);
		
		em.persist(pairing);
	}
	
	public Pairing findPairingById(long id) {
		return em.find(Pairing.class, id);
	}
	
	public List<Pairing> getPairings() {
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Pairing> criteria = builder.createQuery(Pairing.class);
        Root<Pairing> root = criteria.from(Pairing.class);
        criteria.select(root);
        
        TypedQuery<Pairing> query = em.createQuery(criteria);
        
        return query.getResultList();
	}
	
	public boolean removePairing(long id) {
		Pairing pairing = em.find(Pairing.class, id);
		
		if (pairing != null) {
			em.detach(pairing);
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
