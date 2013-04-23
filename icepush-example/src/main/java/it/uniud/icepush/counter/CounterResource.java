package it.uniud.icepush.counter;

import javax.ws.rs.core.*;
import javax.ws.rs.*;

public interface CounterResource {
    
	public static String PATH = "count";
	
	@GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getCount();
}
