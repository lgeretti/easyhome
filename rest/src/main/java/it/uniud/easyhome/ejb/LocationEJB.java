package it.uniud.easyhome.ejb;

import it.uniud.easyhome.network.Location;
import it.uniud.easyhome.network.LocationType;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

@Stateless
public class LocationEJB {

	@PersistenceContext(unitName = "EasyHome-JTA")
	private EntityManager em;
	
	public void insertLocation(int id, String name, LocationType type, String imgPath) {
		Location loc = new Location(id,name,type,imgPath);
		
		em.persist(loc);
	}
	
	public Location findLocationById(int id) {
		return em.find(Location.class, id);
	}

	public Location findLocation(String locationName) {
		
		Location result = null;
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Location> criteria = builder.createQuery(Location.class);
        Root<Location> loc = criteria.from(Location.class);
        criteria.select(loc).where(builder.equal(loc.get("name"), locationName));
        
        TypedQuery<Location> query = em.createQuery(criteria);
        
        try {
        	result = query.getSingleResult();
        } catch (NoResultException ex) {
        }
        
        return result;
	}
	
	public void updateUnmanaged(Location loc) {
		em.merge(loc);
	}
	
	public List<Location> getLocations() {
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Location> criteria = builder.createQuery(Location.class);
        Root<Location> root = criteria.from(Location.class);
        criteria.select(root);
        
        TypedQuery<Location> query = em.createQuery(criteria);
        
        return query.getResultList();
	}
	
	public boolean removeLocation(int id) {
		Location loc = em.find(Location.class, id);
		
		if (loc != null) {
			em.remove(loc);
			return true;
		} else {
			return false;
		}
	}
	
	public void removeAllLocations() {
        
        List<Location> locations = getLocations();
        
        for (Location loc: locations)
        	em.remove(loc);
	}
}
