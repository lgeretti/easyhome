package it.uniud.easyhome.ejb;


import it.uniud.easyhome.exceptions.MultipleLinkException;
import it.uniud.easyhome.network.Link;
import it.uniud.easyhome.network.LocalCoordinates;

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
public class LinkEJB {

	@PersistenceContext(unitName = "EasyHome-JTA")
	private EntityManager em;
	
	public void insertLink(long id, byte gatewayId, LocalCoordinates source, LocalCoordinates destination) {
		Link link = new Link(id, gatewayId, source, destination);
		
		em.persist(link);
	}
	
	public void cleanupLinks(long KEEP_ALIVE_MS) {
	
		StringBuilder queryBuilder = new StringBuilder("DELETE FROM Link l ")
		.append("WHERE l.timestamp<=:t ");
		
		TypedQuery<Link> query = em.createQuery(queryBuilder.toString(),Link.class)
		.setParameter("t", System.currentTimeMillis()-KEEP_ALIVE_MS);
		
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
			em.remove(link);
			return true;
		} else 
			return false;
	}
	
	public void removeAllLinks() {
        
        List<Link> links = getLinks();
        
        for (Link link: links)
        	em.remove(link);
	}
}
