package it.uniud.easyhome.network;

import it.uniud.easyhome.processing.NodeAnnceRegistrationProcess;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

@Stateless
public class JobEJB {

	@PersistenceContext(unitName = "EasyHome-JTA")
	private EntityManager em;
	
	public void insertJob(int id, NetworkJobType type, byte gatewayId, short address, byte endpoint, byte tsn, byte payload) {
		
		NetworkJob job = new NetworkJob(id, type, gatewayId, address, endpoint, tsn, payload);
		
		em.persist(job);
	}
	
	public List<NetworkJob> getJobs() {
		
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<NetworkJob> criteria = builder.createQuery(NetworkJob.class);
        Root<NetworkJob> root = criteria.from(NetworkJob.class);
        criteria.select(root);
        
        TypedQuery<NetworkJob> query = em.createQuery(criteria);
        
        return query.getResultList();
	}

	public List<NetworkJob> getLatestJobs(NetworkJobType type, byte gatewayId, short address, byte endpoint, byte tsn) {
		
        StringBuilder queryBuilder = new StringBuilder("SELECT j FROM NetworkJob j WHERE j.id IN (SELECT MAX(j2.id) FROM NetworkJob j2");
        
        boolean atLeastOneClause = false;
        
        if (type != null || gatewayId != (byte)0 || tsn != (byte)0) {
        	queryBuilder.append(" WHERE ");
        }
        if (type != null) {
        	atLeastOneClause = true;
        	queryBuilder.append(" j2.type=:t ");
        }
        if (gatewayId != (byte)0) {
        	if (atLeastOneClause)
        		queryBuilder.append(" AND ");
        	atLeastOneClause = true;
        	queryBuilder.append(" j2.gatewayId=:g AND j2.address=:a AND j2.endpoint=:e ");
        }
        if (tsn != (byte)0) {
        	if (atLeastOneClause)
        		queryBuilder.append(" AND ");
        	queryBuilder.append(" j2.tsn=:n");
        }
        queryBuilder.append(" GROUP BY j2.gatewayId, j2.address, j2.endpoint)");
        
        TypedQuery<NetworkJob> query = em.createQuery(queryBuilder.toString(),NetworkJob.class);
        
        if (type != null)
        	query.setParameter("t",type);
        
        if (gatewayId != (byte)0)
        	query.setParameter("g",gatewayId)
			  	 .setParameter("a",address)
			  	 .setParameter("e",endpoint);
        
        if (tsn != (byte)0)
        	query.setParameter("n", tsn);
        
        return query.getResultList();		
	}
	
	public NetworkJob findJobById(int jobId) {
		return em.find(NetworkJob.class, jobId);
	}
	
	public void removeAllJobs() {
        
        List<NetworkJob> jobs = getJobs();
        
        for (NetworkJob job: jobs)
        	em.remove(job);
	}

	public boolean removeJobById(int jobId) {

        NetworkJob job = findJobById(jobId);
        
        boolean existed = (job != null);
        
        if (existed)
        	em.remove(job);
        
        return existed;
	}
	
	public int removeJobs(NetworkJobType type, byte gatewayId, short address, byte endpoint) {
		
		String queryString = "DELETE FROM NetworkJob j WHERE j.type = :t AND j.gatewayId = :g AND j.address = :a AND j.endpoint = :e";
		return em.createQuery(queryString)
				.setParameter("t", type)
				.setParameter("g", gatewayId)
				.setParameter("a",address)
				.setParameter("e", endpoint)
				.executeUpdate();
	}
	
	public int removeJobs(NetworkJobType type, byte gatewayId, short address) {

		String queryString = "DELETE FROM NetworkJob j WHERE j.type = :t AND j.gatewayId = :g AND j.address = :a";
		return em.createQuery(queryString)
				.setParameter("t", type)
				.setParameter("g", gatewayId)
				.setParameter("a",address)
				.executeUpdate();
	}
	
	/**
	 * Clean up jobs for which no corresponding node exist, plus node announce grace jobs that are over grace timeout. 
	 */
	public void cleanupJobs(long GRACE_TIMEOUT) {
		
		String deleteQueryString = new StringBuilder("DELETE FROM NetworkJob j WHERE j.id NOT IN ")
												.append("(SELECT DISTINCT j2.id FROM NetworkJob j2, Node n WHERE ")
												.append("j2.gatewayId = n.coordinates.gatewayId AND j2.address = n.coordinates.address)").toString();
		em.createQuery(deleteQueryString).executeUpdate();
		
		String deleteOvergraceQueryString = new StringBuilder("DELETE FROM NetworkJob j WHERE j.type=:type AND j.timestamp < :time").toString();
		em.createQuery(deleteOvergraceQueryString)
							.setParameter("type", NetworkJobType.NODE_ANNCE_GRACE)
							.setParameter("time", System.currentTimeMillis()-GRACE_TIMEOUT).executeUpdate();
	}
}
