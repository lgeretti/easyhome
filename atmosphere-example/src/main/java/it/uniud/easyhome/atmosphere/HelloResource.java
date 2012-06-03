package it.uniud.easyhome.atmosphere;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;

@Path("/hello")
public class HelloResource {

    @GET 
    @Produces("text/plain")
    public String getMessage() {
    	
        return "Hello";
        
    }
    
    @GET
    @Path("{username}")
    @Produces("application/xml")
    public String getXmlMessage(@PathParam("username") String userName) {
        
        return "<salutation><header>Hello</header><payload>" + userName + "</payload></salutation>";
        
    }
    
    @GET
    @Path("{username}")
    @Produces("text/plain")
    public String getMessage(@PathParam("username") String userName) {
        
    	return "Hello " + userName;
    	
    }
    
    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Produces("text/plain")
    public String postMessage(@FormParam("name") String name) {
    	
    	return "I got you, " + name + "!";
    	
    }
      
}
