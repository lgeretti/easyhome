package it.uniud.icepush.counter;

import javax.ws.rs.core.*;
import javax.ws.rs.*;

@Path("count")
public final class CounterResource {
    
	@GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getCount() {
        
		// Pretty stupid, only used to get the global value
        return Integer.toString(new Counter().getCount());
    }
}
