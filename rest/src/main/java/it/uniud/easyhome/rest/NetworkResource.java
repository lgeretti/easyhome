package it.uniud.easyhome.rest;


import it.uniud.easyhome.network.Node;

import java.util.List;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.ws.rs.core.*;
import javax.ws.rs.*;

/** Handles the access to the network of nodes */
@Path("/network")
public final class NetworkResource {
    
    private static final String PERSISTENCE_CONTEXT = "EasyHome";
    
    private EntityManagerFactory emf = Persistence.createEntityManagerFactory(PERSISTENCE_CONTEXT);
    
    @Context
    private UriInfo uriInfo;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Node> getNodes() {
        
    	EntityManager em = emf.createEntityManager();
    	
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Node> criteria = builder.createQuery(Node.class);
        Root<Node> root = criteria.from(Node.class);
        criteria.select(root);
        
        TypedQuery<Node> query = em.createQuery(criteria);
        List<Node> nodes = query.getResultList();
        
        em.close();
        
        return nodes;
        
    }
    
    @GET
    @Path("{nodeid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Node getNode(@PathParam("nodeid") long nodeId) {
        
    	EntityManager em = emf.createEntityManager();
    	
        Node node = em.find(Node.class, nodeId);

        em.close();
        
        if (node == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        return node;
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateOrInsertNode(Node node) {
        
    	EntityManager em = emf.createEntityManager();
    	
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        Node persistedNode = em.find(Node.class, node.getId());
        boolean existed = (persistedNode != null);
        
        if (!existed) {
            em.persist(node);
        } else {
            em.merge(node);
        }
            
        tx.commit();
        
        em.close();
        
        if (!existed)
            return Response.created(
                             uriInfo.getAbsolutePathBuilder()
                                    .path(String.valueOf(node.getId()))
                                    .build())
                           .build();
        else
            return Response.ok().build();
    }
      
    @DELETE
    @Path("{nodeid}")
    public Response deleteNode(@PathParam("nodeid") long nodeId) {
        
    	EntityManager em = emf.createEntityManager();
    	
        EntityTransaction tx = em.getTransaction();
        
        Node existing = em.find(Node.class, nodeId);
        
        if (existing == null) {
        	em.close();
        	throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        tx.begin();
        em.remove(existing);
        tx.commit();    
        
        em.close();
        
        return Response.ok().build();
    }    
    
    /** Removes all the nodes.
     * 
     */
    @DELETE
    public Response clear() {
        
    	EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        Query query = em.createQuery("DELETE FROM Node");
                
        tx.begin();
        query.executeUpdate();
        tx.commit();  
        
        em.clear();
        
        em.close();
        
        return Response.ok().build();
    }
       
}
