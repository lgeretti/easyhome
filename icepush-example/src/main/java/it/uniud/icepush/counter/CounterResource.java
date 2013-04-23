package it.uniud.icepush.counter;

import javax.ws.rs.core.*;
import javax.ws.rs.*;

public interface CounterResource {
    
	public static String PATH = "counter";
	
	@GET
    @Produces(MediaType.APPLICATION_JSON)
    public Counter getCounter();
}
