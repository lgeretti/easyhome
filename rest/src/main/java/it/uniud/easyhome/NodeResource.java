package it.uniud.easyhome;

import java.util.List;

import javax.persistence.*;
import javax.ws.rs.core.*;
import javax.ws.rs.*;

@Path("/nodes")
public class NodeResource {
    
    @Context
    private UriInfo uriInfo;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Node> getNodes() {
        
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("EasyHome");
        EntityManager em = emf.createEntityManager();
        TypedQuery<Node> query = em.createQuery("SELECT n FROM Node n", Node.class);
        List<Node> nodes = query.getResultList();
        
        em.close();
        emf.close();
        
        return nodes;
    }
    
    @GET
    @Path("{nodeid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Node getNode(@PathParam("nodeid") int nodeId) {
        
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("EasyHome");
        EntityManager em = emf.createEntityManager();
        
        Node node = em.find(Node.class, nodeId);

        if (node == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        em.close();
        emf.close();
        
        return node;
    }
    
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insertNode(Node node) {
    	
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("EasyHome");
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        
        Node existing = em.find(Node.class, node.getId());
        
        if (existing != null) 
            throw new WebApplicationException(Response.Status.CONFLICT);
        
        tx.begin();
        em.persist(node);
        tx.commit();
       
        em.close();
        emf.close();
        
	    return Response.created(
	                         uriInfo.getAbsolutePathBuilder()
	                                .path(String.valueOf(node.getId()))
	                                .build())
	                    .build();
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateOrInsertNode(Node node) {
        
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("EasyHome");
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        Node otherNode = em.find(Node.class, node.getId());
        boolean existed = (otherNode != null);
        
        if (!existed)
            em.persist(node);
        else 
            otherNode.copyFrom(node);
        
        tx.commit();
        em.close();
        emf.close();
        
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
    public Response deleteNode(@PathParam("nodeid") int nodeId) {
        
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("EasyHome");
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        
        Node existing = em.find(Node.class, nodeId);
        
        if (existing == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        tx.begin();
        em.remove(existing);
        tx.commit();    
       
        em.close();
        emf.close();
        
        return Response.ok().build();
    }    
    
}
