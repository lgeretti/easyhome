package it.uniud.icepush.counter;

import javax.ws.rs.Path;

@Path(CounterResource.PATH)
public final class CounterResourceService implements CounterResource {
    
    public Counter getCounter() {
        
		// Pretty stupid, only used to get the singleton value
        return new Counter();
    }
}
