package it.uniud.icepush.counter;

import javax.ws.rs.Path;

@Path(CounterResource.PATH)
public final class CounterResourceService implements CounterResource {
    
    public String getCount() {
        
		// Pretty stupid, only used to get the global value
        return Integer.toString(new Counter().getCount());
    }
}
