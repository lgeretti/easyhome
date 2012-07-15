package it.uniud.easyhome;

import java.util.List;

import javax.persistence.*;
import javax.ws.rs.core.*;
import javax.ws.rs.*;

@Path("/nodes")
public class NodeResource {
    
    private static final String PERSISTENCE_CONTEXT = "EasyHome";
    
    private static final EntityManager em = Persistence.createEntityManagerFactory(PERSISTENCE_CONTEXT)
                                                          .createEntityManager();
    
    @Context
    private UriInfo uriInfo;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Node> getNodes() {
        
        TypedQuery<Node> query = em.createQuery("SELECT n FROM Node n", Node.class);
        List<Node> nodes = query.getResultList();
        
        return nodes;
    }
    
    @GET
    @Path("{nodeid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Node getNode(@PathParam("nodeid") int nodeId) {
        
        Node node = em.find(Node.class, nodeId);

        if (node == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        return node;
    }
    
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insertNode(Node node) {

        EntityTransaction tx = em.getTransaction();
        
        Node existing = em.find(Node.class, node.getId());
        
        if (existing != null) 
            throw new WebApplicationException(Response.Status.CONFLICT);
        
        tx.begin();
        em.persist(node);
        tx.commit();
        
	    return Response.created(
	                         uriInfo.getAbsolutePathBuilder()
	                                .path(String.valueOf(node.getId()))
	                                .build())
	                    .build();
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateOrInsertNode(Node node) {
        
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        Node otherNode = em.find(Node.class, node.getId());
        boolean existed = (otherNode != null);
        
        if (!existed)
            em.persist(node);
        else 
            otherNode.copyFrom(node);
        
        tx.commit();
        
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
        
        EntityTransaction tx = em.getTransaction();
        
        Node existing = em.find(Node.class, nodeId);
        
        if (existing == null) 
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        
        tx.begin();
        em.remove(existing);
        tx.commit();    
        
        return Response.ok().build();
    }    
    
}
